package com.apeira.archae.assembly;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.apeira.archae.registry.ArchaeBlocks;
import com.apeira.archae.registry.ArchaeMenus;
import com.apeira.archae.registry.ArchaeSounds;
import com.apeira.archae.registry.ArchaeTags;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Argon's assembly table ({@code AssemblyTableScreenHandler}): put fragments in the input slot, pick one of the
 * artifacts they make, and take it from the output slot, which uses up the recipe's cost of fragments.
 *
 * <p>The screen sends its picks as vanilla menu button clicks ({@link #clickMenuButton}, the recipe's index among the
 * fragment's recipes) and the pick is synced back through a data slot. Both sides list the recipes from
 * {@link AssemblyRecipes}, in the same order. As in Argon, putting in a new fragment selects its first recipe.
 */
public class AssemblyTableMenu extends AbstractContainerMenu {
    private static final int INPUT_SLOT = 0;
    private static final int RESULT_SLOT = 1;
    private static final int INVENTORY_START = 2;
    private static final int INVENTORY_END = INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final Level level;
    private final DataSlot selected = DataSlot.standalone();
    private final Container input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final ResultContainer result = new ResultContainer();
    private List<AssemblyRecipe> recipes = List.of();
    private @Nullable Item fragment;

    /** The client's menu, opened by the server. */
    public AssemblyTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public AssemblyTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ArchaeMenus.ASSEMBLY_TABLE.get(), containerId);
        this.access = access;
        this.level = inventory.player.level();
        addDataSlot(selected);

        addSlot(new Slot(input, 0, 26, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ArchaeTags.FRAGMENTS);
            }
        });
        addSlot(new Slot(result, 0, 134, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                AssemblyRecipe recipe = selectedRecipe();
                return recipe != null && input.getItem(0).getCount() >= recipe.cost();
            }

            @Override
            public void onTake(Player player, ItemStack carried) {
                AssemblyRecipe recipe = selectedRecipe();
                if (recipe != null) {
                    input.removeItem(0, recipe.cost());
                    if (!player.level().isClientSide()) {
                        player.level().playSound(null, player.blockPosition(), ArchaeSounds.TAKE_RESULT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                }
                updateResult();
                super.onTake(player, carried);
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
    }

    // Recipes, read by the screen

    /** The recipes of the fragment in the input slot. */
    public List<AssemblyRecipe> recipes() {
        return recipes;
    }

    /** The index of the picked recipe in {@link #recipes()}, or -1. */
    public int selectedIndex() {
        return selected.get();
    }

    /** How many fragments are in the input slot. */
    public int fragmentCount() {
        return input.getItem(0).getCount();
    }

    private @Nullable AssemblyRecipe selectedRecipe() {
        int index = selected.get();
        return index >= 0 && index < recipes.size() ? recipes.get(index) : null;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= recipes.size()) {
            return false;
        }
        selected.set(id);
        updateResult();
        return true;
    }

    // Result

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == input) {
            ItemStack stack = input.getItem(0);
            Item item = stack.isEmpty() ? null : stack.getItem();
            if (item != fragment) {
                fragment = item;
                recipes = item == null ? List.of() : AssemblyRecipes.forFragment(level, item);
                selected.set(recipes.isEmpty() ? -1 : 0);
            }
            updateResult();
        }
    }

    private void updateResult() {
        AssemblyRecipe recipe = selectedRecipe();
        result.setItem(0, recipe != null && input.getItem(0).getCount() >= recipe.cost() ? recipe.assemble() : ItemStack.EMPTY);
    }

    // Closing and shift-clicking

    @Override
    public void removed(Player player) {
        super.removed(player);
        result.removeItemNoUpdate(0);
        access.execute((level, pos) -> clearContainer(player, input));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ArchaeBlocks.ASSEMBLY_TABLE.get());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != result && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex == RESULT_SLOT || slotIndex == INPUT_SLOT) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
            if (slotIndex == RESULT_SLOT) {
                slot.onQuickCraft(stack, original);
            }
        } else if (!stack.is(ArchaeTags.FRAGMENTS) || !moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        }
        slot.setChanged();
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        // Taking the result uses up its fragments, as taking it by hand does.
        slot.onTake(player, stack);
        return original;
    }
}
