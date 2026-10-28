package com.apeira.main.farming;

import org.jspecify.annotations.Nullable;

import com.apeira.main.registry.MainFarming;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A planter box's crop and stock.
 *
 * <p>The stock is three slots of up to 64 ({@link #SEEDS}, {@link #SOIL}, {@link #FERTILIZER}, one fertilizer type),
 * filled through the menu, by right-clicking the box with them, or by hoppers; nothing can be pulled back out but by
 * the menu. When the box has no crop and seeds in stock, it plants one at once, soiled and fertilized from the stock
 * if it has soil or fertilizer: this is how soil and fertilizer in stock "prime" the next crops.
 *
 * <p>The crop is a {@link FarmCrop}, its stage (1 when planted, {@link FarmCrop#stages()} when ripe), whether it is
 * soiled, its fertilizer, and whether it ripened golden. It grows one stage at each 7:00 AM tick of the
 * {@link FarmClock} for which the box was watered the day before ({@link #wateredDay}, set by a water bucket or a
 * watering can, or a {@link SprinklerBlock} in range); watering lasts one day. A box unloaded at 7:00 AM catches up
 * when it loads ({@link #catchUp}), from {@link #lastDay}, the last morning it counted.
 */
public class PlanterBoxBlockEntity extends BlockEntity implements Container {
    public static final int SEEDS = 0;
    public static final int SOIL = 1;
    public static final int FERTILIZER = 2;
    public static final int SLOTS = 3;
    public static final int MAX_STACK = 64;
    /** At most this many missed mornings are caught up at once (a box unloaded for longer just counts the last ones). */
    private static final long MAX_CATCH_UP = 64L;
    private static final long NEVER = Long.MIN_VALUE;

    // Menu data: the crop's ordinal (-1 for none), its stage, flags and the fertilizer's ordinal (-1 for none).
    public static final int DATA_CROP = 0;
    public static final int DATA_STAGE = 1;
    public static final int DATA_FLAGS = 2;
    public static final int DATA_FERTILIZER = 3;
    public static final int DATA_COUNT = 4;
    public static final int FLAG_SOILED = 1;
    public static final int FLAG_WET = 2;
    public static final int FLAG_GOLDEN = 4;
    public static final int FLAG_RIPE = 8;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private @Nullable FarmCrop crop;
    private int stage;
    private boolean soiled;
    private @Nullable Fertilizer fertilizer;
    private boolean golden;
    private long wateredDay = NEVER;
    private long lastDay = NEVER;
    /** Set while the box is being removed, so nothing replants it or touches its block. */
    private boolean removing;

    /** What the planter box menu shows, read on the server and synced through its data slots. */
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_CROP -> crop == null ? -1 : crop.ordinal();
                case DATA_STAGE -> stage;
                case DATA_FLAGS -> (soiled ? FLAG_SOILED : 0) | (isWet() ? FLAG_WET : 0) | (golden ? FLAG_GOLDEN : 0) | (isRipe() ? FLAG_RIPE : 0);
                case DATA_FERTILIZER -> fertilizer == null ? -1 : fertilizer.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PlanterBoxBlockEntity(BlockPos pos, BlockState state) {
        super(MainFarming.PLANTER_BOX_ENTITY.get(), pos, state);
    }

    // The crop

    public @Nullable FarmCrop crop() {
        return crop;
    }

    public int stage() {
        return stage;
    }

    public boolean isGolden() {
        return golden;
    }

    public boolean isSoiled() {
        return soiled;
    }

    public @Nullable Fertilizer fertilizer() {
        return fertilizer;
    }

    public boolean isRipe() {
        return crop != null && stage >= crop.stages();
    }

    /** Plants {@code seed}'s crop if the box is empty, taking soil and fertilizer from the stock. */
    public boolean plant(FarmCrop seed) {
        if (crop != null) {
            return false;
        }
        crop = seed;
        stage = 1;
        golden = false;
        soiled = takeFromStock(SOIL);
        fertilizer = Fertilizer.of(items.get(FERTILIZER));
        if (fertilizer != null) {
            takeFromStock(FERTILIZER);
        }
        sync();
        return true;
    }

    /** Soils the growing crop. False if there is none or it is already soiled. */
    public boolean applySoil() {
        if (crop == null || soiled) {
            return false;
        }
        soiled = true;
        sync();
        return true;
    }

    /** Gives the growing crop {@code type}. False if there is no crop or it already has a fertilizer. */
    public boolean applyFertilizer(Fertilizer type) {
        if (crop == null || fertilizer != null) {
            return false;
        }
        fertilizer = type;
        if (type == Fertilizer.GOLDEN_GOODS && isRipe()) {
            golden = true;
        }
        sync();
        return true;
    }

    /** Waters the box for today. */
    public void water() {
        if (level instanceof ServerLevel serverLevel) {
            wateredDay = FarmingService.day(serverLevel);
            sync();
        }
    }

    /**
     * Harvests a ripe crop: the produce goes to {@code player} (dropped on the box if it does not fit), or onto the box when
     * automated, and the next crop is planted from the stock. False if the crop is not ripe.
     */
    public boolean harvest(@Nullable Player player, ItemStack tool, boolean automated) {
        if (!(level instanceof ServerLevel serverLevel) || !isRipe() || crop == null) {
            return false;
        }
        ItemStack produce = CropQuality.harvest(crop, golden, soiled, fertilizer, tool, automated || player == null, serverLevel.getRandom());
        if (player != null && !automated) {
            player.getInventory().add(produce);
        }
        if (!produce.isEmpty()) {
            Block.popResource(serverLevel, worldPosition.above(), produce);
        }
        serverLevel.playSound(null, worldPosition, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        clearCrop();
        tryPlant();
        sync();
        return true;
    }

    private void clearCrop() {
        crop = null;
        stage = 0;
        soiled = false;
        fertilizer = null;
        golden = false;
    }

    /** Plants from the seed stock if the box is empty (server side). */
    private void tryPlant() {
        if (crop == null && !removing && level != null && !level.isClientSide()) {
            FarmCrop seed = FarmCrop.of(items.get(SEEDS));
            if (seed != null) {
                takeFromStock(SEEDS);
                plant(seed);
            }
        }
    }

    private boolean takeFromStock(int slot) {
        ItemStack stack = items.get(slot);
        if (stack.isEmpty()) {
            return false;
        }
        stack.shrink(1);
        return true;
    }

    // Watering and growth

    /** Whether the box is watered today: by hand, or by a sprinkler in range. Clients read the block state. */
    public boolean isWet() {
        if (level instanceof ServerLevel serverLevel) {
            return wateredOn(serverLevel, FarmingService.day(serverLevel));
        }
        return getBlockState().getValue(PlanterBoxBlock.WET);
    }

    private boolean wateredOn(ServerLevel serverLevel, long day) {
        return wateredDay == day || FarmClock.get(serverLevel.getServer()).sprinklerWaters(serverLevel.dimension(), worldPosition, day);
    }

    /** Grows the crop for every morning since {@link #lastDay} for which it was watered the day before. */
    void catchUp(ServerLevel serverLevel) {
        long day = FarmingService.day(serverLevel);
        if (lastDay == NEVER || lastDay > day) {
            lastDay = day;
            setChanged();
            refreshState();
            return;
        }
        boolean grew = false;
        for (long morning = Math.max(lastDay, day - MAX_CATCH_UP) + 1; morning <= day; morning++) {
            if (crop != null && !isRipe() && wateredOn(serverLevel, morning - 1)) {
                stage = fertilizer == Fertilizer.SPEED_GROW ? crop.stages() : stage + 1;
                if (isRipe()) {
                    golden = CropQuality.ripensGolden(soiled, fertilizer, serverLevel.getRandom());
                }
                grew = true;
            }
        }
        lastDay = day;
        if (grew) {
            sync();
        } else {
            setChanged();
            refreshState();
        }
    }

    /** Sends the crop to clients and updates the block's wet and soiled look. */
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide() && !removing) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            refreshState();
        }
    }

    /** Sets {@link PlanterBoxBlock#WET} and {@link PlanterBoxBlock#SOILED} to what the box is now (server side). */
    void refreshState() {
        if (level == null || level.isClientSide() || removing) {
            return;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof PlanterBoxBlock)) {
            return;
        }
        boolean soil = crop != null ? soiled : !items.get(SOIL).isEmpty();
        BlockState updated = state.setValue(PlanterBoxBlock.WET, isWet()).setValue(PlanterBoxBlock.SOILED, soil);
        if (updated != state) {
            level.setBlock(worldPosition, updated, Block.UPDATE_CLIENTS);
        }
    }

    /** Moves as much of {@code stack} into its stock slot as fits. Returns how many items moved. */
    public int addToStock(ItemStack stack) {
        int slot = stockSlot(stack);
        if (slot < 0 || !canPlaceItem(slot, stack)) {
            return 0;
        }
        ItemStack current = items.get(slot);
        int moved = Math.min(stack.getCount(), MAX_STACK - current.getCount());
        if (moved <= 0) {
            return 0;
        }
        if (current.isEmpty()) {
            items.set(slot, stack.copyWithCount(moved));
        } else {
            current.grow(moved);
        }
        stack.shrink(moved);
        onStockChanged();
        return moved;
    }

    /** The stock slot an item goes into, or -1. */
    public static int stockSlot(ItemStack stack) {
        if (FarmCrop.of(stack) != null) {
            return SEEDS;
        }
        if (stack.getItem() instanceof SoilItem) {
            return SOIL;
        }
        return Fertilizer.of(stack) != null ? FERTILIZER : -1;
    }

    private void onStockChanged() {
        setChanged();
        tryPlant();
        refreshState();
    }

    // Lifecycle

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel) {
            FarmingService.track(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        FarmingService.untrack(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        FarmingService.untrack(this);
    }

    /**
     * The box is gone (a player breaking it harvests a ripe crop first, in {@link PlanterBoxBlock#playerWillDestroy}):
     * the stock drops, a ripe crop is harvested automatically and an unripe one gives its seed back.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) {
            return;
        }
        removing = true;
        Containers.dropContents(level, pos, items);
        items.clear();
        if (crop != null) {
            if (isRipe()) {
                harvest(null, ItemStack.EMPTY, true);
            } else {
                Block.popResource(level, pos, new ItemStack(MainFarming.seed(crop)));
            }
            clearCrop();
        }
    }

    // Saving and syncing

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        crop = parse(FarmCrop.class, input.getStringOr("crop", ""));
        stage = input.getIntOr("stage", 0);
        soiled = input.getBooleanOr("soiled", false);
        fertilizer = parse(Fertilizer.class, input.getStringOr("fertilizer", ""));
        golden = input.getBooleanOr("golden", false);
        wateredDay = input.getLongOr("watered_day", NEVER);
        lastDay = input.getLongOr("last_day", NEVER);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        if (crop != null) {
            output.putString("crop", crop.name());
            output.putInt("stage", stage);
            output.putBoolean("soiled", soiled);
            output.putBoolean("golden", golden);
            if (fertilizer != null) {
                output.putString("fertilizer", fertilizer.name());
            }
        }
        output.putLong("watered_day", wateredDay);
        output.putLong("last_day", lastDay);
    }

    private static <E extends Enum<E>> @Nullable E parse(Class<E> type, String name) {
        if (name.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    // Container: the stock

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            onStockChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        onStockChanged();
    }

    @Override
    public int getMaxStackSize() {
        return MAX_STACK;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SEEDS -> FarmCrop.of(stack) != null;
            case SOIL -> stack.getItem() instanceof SoilItem;
            case FERTILIZER -> Fertilizer.of(stack) != null && (items.get(FERTILIZER).isEmpty() || ItemStack.isSameItem(items.get(FERTILIZER), stack));
            default -> false;
        };
    }

    /** Hoppers may fill the stock but not empty it. */
    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }
}
