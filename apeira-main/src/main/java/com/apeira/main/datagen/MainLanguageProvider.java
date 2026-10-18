package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraLanguageProvider;
import com.apeira.core.registry.ShapeSet;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainBuildingBlocks;
import com.apeira.main.registry.MainCosmetics;
import com.apeira.main.registry.MainCrates;
import com.apeira.main.registry.MainCrystals;
import com.apeira.main.registry.MainDyedBlocks;
import com.apeira.main.registry.MainItems;
import com.apeira.main.registry.MainLanterns;
import com.apeira.main.registry.MainMaterials;
import com.apeira.main.registry.MainPlants;
import com.apeira.main.registry.MainRocks;
import com.apeira.main.registry.MainSpecialBlocks;
import com.apeira.main.registry.MainStones;
import com.apeira.main.registry.MainWoods;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.registries.DeferredBlock;

/** English names for Apeira's blocks, items and creative tabs. */
public final class MainLanguageProvider extends ApeiraLanguageProvider {
    private static final String CARVED = "_planks_carved";
    private static final String FLOWERS = "wisteria_flowers_";

    public MainLanguageProvider(PackOutput output, String namespace) {
        super(output, namespace);
    }

    @Override
    protected void addTranslations() {
        // ORForged's names.
        item(MainItems.PLATINUM, "Platinum");
        item(MainItems.RUBY, "Ruby");
        MainItems.tools().forEach(tool -> item(tool, title(tool.getId().getPath())));
        MainItems.armor().forEach(armor -> item(armor, title(armor.getId().getPath())));
        block(MainBlocks.PLATINUM_BLOCK, "Block of Platinum");
        block(MainBlocks.PLATINUM_BRICKS, "Platinum Bricks");
        block(MainBlocks.PLATINUM_PILLAR, "Platinum Pillar");
        block(MainBlocks.PLATINUM_ORE, "Platinum Ore");
        block(MainBlocks.DEEPSLATE_PLATINUM_ORE, "Deepslate Platinum Ore");
        block(MainBlocks.RUBY_BLOCK, "Block of Ruby");
        block(MainBlocks.RUBY_BRICKS, "Ruby Bricks");
        block(MainBlocks.RUBY_PILLAR, "Ruby Pillar");
        block(MainBlocks.RUBY_ORE, "Ruby Ore");
        block(MainBlocks.DEEPSLATE_RUBY_ORE, "Deepslate Ruby Ore");
        block(MainBlocks.DIAMOND_BRICK, "Diamond Brick");
        for (MainBlocks.Mineral mineral : MainBlocks.vanillaMinerals()) {
            if (mineral.brick() != MainBlocks.DIAMOND_BRICK) {
                titled(mineral.brick());
            }
            titled(mineral.pillar());
        }
        woods();
        // ORForged named its stained stone "Stone", like vanilla's; the ids' names keep them apart.
        MainStones.all().forEach(this::titled);
        MainLanterns.all().forEach(lantern -> titled(lantern.block()));
        xenon();

        // The Crates mod's crate names.
        block(MainCrates.AETHER_CRATE, "Aether Crate");
        block(MainCrates.ANIMAL_CRATE, "Animal Crate");
        block(MainCrates.AQUATIC_CRATE, "Aquatic Crate");
        block(MainCrates.ATLANTIS_CRATE, "Atlantis Crate");
        block(MainCrates.BATHROOM_CRATE, "Bathroom Crate");
        block(MainCrates.BEACH_CRATE, "Beach Crate");
        block(MainCrates.BUSINESS_CRATE, "Business Crate");
        block(MainCrates.CAMPING_CRATE, "Camping Crate");
        block(MainCrates.CONSTRUCTION_CRATE, "Construction Crate");
        block(MainCrates.EASTER_CRATE, "Easter Crate");
        block(MainCrates.FAST_FOOD_CRATE, "Fast Food Crate");
        block(MainCrates.FESTIVE_CRATE, "Festive Crate");
        block(MainCrates.FUN_FAIR_CRATE, "Fun Fair Crate");
        block(MainCrates.GARDEN_CRATE, "Garden Crate");
        block(MainCrates.ZODIAC_CRATE, "Zodiac Crate");

        creativeTab("content", "Apeira Content");
        creativeTab("cosmetics", "Cosmetics");

        cosmetics();
    }

    /**
     * The woods' names, ORForged's: each block's name in title case, except carved planks ("Carved Maple Planks") and
     * wisteria flowers ("Blue Wisteria Flowers").
     */
    private void woods() {
        for (var block : MainWoods.all()) {
            String path = block.getId().getPath();
            String name;
            if (path.endsWith(CARVED)) {
                name = "Carved " + title(path.substring(0, path.length() - CARVED.length())) + " Planks";
            } else if (path.startsWith(FLOWERS)) {
                name = title(path.substring(FLOWERS.length())) + " Wisteria Flowers";
            } else {
                name = title(path);
            }
            block(block, name);
        }
    }

    /**
     * Xenon's names: each block's id in title case, except the coloured blocks, which put their colour first ("White
     * Concrete Bricks Stairs"). Xenon misspelled its orange crystal bricks "Orange Crystal sBricks", and named its tall frosted grass "Cotton Grass".
     */
    private void xenon() {
        MainRocks.all().forEach(this::titled);
        titled(MainDyedBlocks.TERRACOTTA_SHINGLES);
        MainDyedBlocks.TERRACOTTA_SHINGLES_SHAPES.shapes().forEach(this::titled);
        for (var dyed : MainDyedBlocks.CONCRETE_BRICKS) {
            dyed(dyed, "Concrete Bricks");
        }
        for (var dyed : MainDyedBlocks.TERRACOTTA_SHINGLES_COLOURS) {
            dyed(dyed, "Terracotta Shingles");
        }
        for (var dyed : MainDyedBlocks.QUILTED_WOOL) {
            dyed(dyed, "Quilted Wool");
        }
        MainCrystals.all().forEach(this::titled);
        MainBuildingBlocks.all().forEach(this::titled);
        MainPlants.all().stream().filter(plant -> plant != MainPlants.TALL_FROSTED_GRASS).forEach(this::titled);
        block(MainPlants.TALL_FROSTED_GRASS, "Cotton Grass");
        MainSpecialBlocks.all().forEach(this::titled);
        item(MainMaterials.BLIGHT_ESSENCE, "Blight Essence");
        item(MainMaterials.DOLOMITE_DUST, "Dolomite Dust");
        item(MainMaterials.RAW_TURKEY, "Raw Turkey");
        item(MainMaterials.COOKED_TURKEY, "Cooked Turkey");
    }

    /** A coloured block and its shapes as {@code <Colour> <name>} and {@code <Colour> <name> Stairs}. */
    private void dyed(MainDyedBlocks.Dyed dyed, String name) {
        String colour = title(dyed.colour().getSerializedName());
        block(dyed.block(), colour + " " + name);
        ShapeSet shapes = dyed.shapes();
        if (shapes.stairs() != null) {
            block(shapes.stairs(), colour + " " + name + " Stairs");
        }
        if (shapes.slab() != null) {
            block(shapes.slab(), colour + " " + name + " Slab");
        }
        if (shapes.wall() != null) {
            block(shapes.wall(), colour + " " + name + " Wall");
        }
    }

    /** Names a block after its id, in title case. */
    private void titled(DeferredBlock<?> block) {
        block(block, title(block.getId().getPath()));
    }

    /** {@code dark_oak_beam} as {@code Dark Oak Beam}. */
    private static String title(String path) {
        StringBuilder title = new StringBuilder();
        for (String word : path.split("_")) {
            if (!title.isEmpty()) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return title.toString();
    }

    /** The cosmetics' names (the Crates mod's) and the crate each is exclusive to. */
    private void cosmetics() {
        item(MainCosmetics.SPARTAN_HELMET, "Spartan Helmet");
        item(MainCosmetics.LIGHTNING_BOLT, "Lightning Bolt");
        item(MainCosmetics.BLITZ_AURA, "Blitz Aura");
        item(MainCosmetics.SPARTAN_ROBE, "Spartan Robe");
        item(MainCosmetics.CLOUD_WINGS, "Cloud Wings");
        item(MainCosmetics.LYRE, "Lyre");
        item(MainCosmetics.SOLAR_STAFF, "Solar Staff");
        item(MainCosmetics.MOOSE_ANTLERS, "Moose Antlers");
        item(MainCosmetics.GORILLA_MASK, "Gorilla Mask");
        item(MainCosmetics.MASCOT_DOG_HELMET, "Mascot Dog Helmet");
        item(MainCosmetics.MONKEY_BACKSWAG, "Monkey Backswag");
        item(MainCosmetics.GIRAFFE_HELMET, "Giraffe Helmet");
        item(MainCosmetics.LIFE_RING, "Life Ring");
        item(MainCosmetics.SHARK_TAIL, "Shark Tail");
        item(MainCosmetics.ANCHOR, "Anchor");
        item(MainCosmetics.JELLYFISH_HAT, "Jellyfish Hat");
        item(MainCosmetics.FISH_HAND, "Fish Hand");
        item(MainCosmetics.DIVER_HELMET, "Diver Helmet");
        item(MainCosmetics.SHARK_HAT, "Shark Hat");
        item(MainCosmetics.CRAB_CLAW, "Crab Claw");
        item(MainCosmetics.POSEIDONS_CROWN, "Poseidon's Crown");
        item(MainCosmetics.POSEIDONS_ROBE, "Poseidon's Robe");
        item(MainCosmetics.POSEIDONS_TRIDENT, "Poseidon's Trident");
        item(MainCosmetics.PLUNGER, "Plunger");
        item(MainCosmetics.RUBBER_DUCKIE_HAT, "Rubber Duckie Hat");
        item(MainCosmetics.TUB, "Tub");
        item(MainCosmetics.ICE_CREAM_CONE, "Ice Cream Cone");
        item(MainCosmetics.FROGGY_HAT, "Froggy Hat");
        item(MainCosmetics.FRUIT_CROWN, "Fruit Crown");
        item(MainCosmetics.SURFBOARD, "Surfboard");
        item(MainCosmetics.FLAMINGO_FLOATY, "Flamingo Floaty");
        item(MainCosmetics.BEACH_PACK, "Beach Pack");
        item(MainCosmetics.MONEY_SACK, "Money Sack");
        item(MainCosmetics.FLASHLIGHT, "Flashlight");
        item(MainCosmetics.LOG_BUNDLE, "Log Bundle");
        item(MainCosmetics.CHAINSAW, "Chainsaw");
        item(MainCosmetics.CUCKOO_CLOCK, "Cuckoo Clock");
        item(MainCosmetics.HARD_HAT, "Hard Hat");
        item(MainCosmetics.TREADS, "Treads");
        item(MainCosmetics.CRANE, "Crane");
        item(MainCosmetics.HATCHED_EGG, "Hatched Egg");
        item(MainCosmetics.CARROT_CANE, "Carrot Cane");
        item(MainCosmetics.HATCHED_EGG_HAT, "Hatched Egg Hat");
        item(MainCosmetics.BUNNY_BEANIE, "Bunny Beanie");
        item(MainCosmetics.EGG_BASKET, "Egg Basket");
        item(MainCosmetics.BUNNY_HEADBAND, "Bunny Headband");
        item(MainCosmetics.COMICALLY_LARGE_FORK, "Comically Large Fork");
        item(MainCosmetics.COMICALLY_LARGE_SPOON, "Comically Large Spoon");
        item(MainCosmetics.FRY_PACK, "Fry Pack");
        item(MainCosmetics.BURGER_CAP, "Burger Cap");
        item(MainCosmetics.CHRISTMAS_LIGHTS_ARM, "Christmas Lights Arm");
        item(MainCosmetics.CANDY_CANE_STAFF, "Candy Cane Staff");
        item(MainCosmetics.REINDEER_ANTLERS, "Reindeer Antlers");
        item(MainCosmetics.SANTA_HAT, "Santa Hat");
        item(MainCosmetics.ELF_HAT, "Elf Hat");
        item(MainCosmetics.FESTIVE_CANDLE, "Festive Candle");
        item(MainCosmetics.CHRISTMAS_TREE_HAT, "Christmas Tree Hat");
        item(MainCosmetics.FESTIVE_BELLS, "Festive Bells");
        item(MainCosmetics.PARTY_HAT, "Party Hat");
        item(MainCosmetics.BONE_STAFF, "Bone Staff");
        item(MainCosmetics.DEMON_HORNS, "Demon Horns");
        item(MainCosmetics.WRAITH_HOOD, "Wraith Hood");
        item(MainCosmetics.GOTHIC_LANTERN, "Gothic Lantern");
        item(MainCosmetics.DEMON_WINGS, "Demon Wings");
        item(MainCosmetics.TURKEY_HELMET, "Turkey Helmet");
        item(MainCosmetics.TURKEY_TAIL, "Turkey Tail");
        item(MainCosmetics.CORN_STALK_STAFF, "Corn Stalk Staff");
        item(MainCosmetics.MAPLE_LEAF_CROWN, "Maple Leaf Crown");
        item(MainCosmetics.PILGRIM_HAT, "Pilgrim Hat");
        item(MainCosmetics.PUMPKIN_BEANIE, "Pumpkin Beanie");
        item(MainCosmetics.MAPLE_LEAF_WINGS, "Maple Leaf Wings");
        item(MainCosmetics.SCARECROW_HAT, "Scarecrow Hat");
        item(MainCosmetics.SUNFLOWER_UMBRELLA, "Sunflower Umbrella");
        item(MainCosmetics.BLUNDERBUSS, "Blunderbuss");
        item(MainCosmetics.CHEF_HAT, "Chef Hat");
        item(MainCosmetics.APRON, "Apron");
        item(MainCosmetics.PANCAKE_STACK, "Pancake Stack");
        item(MainCosmetics.SOUP_POT, "Soup Pot");
        item(MainCosmetics.LOVELY_RIBBON, "Lovely Ribbon");
        item(MainCosmetics.HEART_HEADBAND, "Heart Headband");
        item(MainCosmetics.FLOWER_BOUQUET, "Flower Bouquet");
        item(MainCosmetics.ANGELIC_HALO, "Angelic Halo");
        item(MainCosmetics.TEDDY_HELMET, "Teddy Helmet");
        item(MainCosmetics.ANGELIC_WINGS, "Angelic Wings");
        item(MainCosmetics.TEDDY_BEAR, "Teddy Bear");
        item(MainCosmetics.CUPIDS_BOW, "Cupid's Bow");
        item(MainCosmetics.LUNAR_CROWN, "Lunar Crown");
        item(MainCosmetics.LUNAR_BANNERS, "Lunar Banners");
        item(MainCosmetics.LOTUS_LANTERN, "Lotus Lantern");
        item(MainCosmetics.LUNAR_LANTERN, "Lunar Lantern");
        item(MainCosmetics.PELLET_DRUM, "Pellet Drum");
        item(MainCosmetics.FIREWORK_BACKPACK, "Firework Backpack");
        item(MainCosmetics.HONGBAO_SACK, "Hongbao Sack");
        item(MainCosmetics.JESTER_HAT, "Jester Hat");
        item(MainCosmetics.KNIGHT_ARMOR, "Knight Armor");
        item(MainCosmetics.KNIGHT_HELMET, "Knight Helmet");
        item(MainCosmetics.LUTE, "Lute");
        item(MainCosmetics.DRAGON_WINGS, "Dragon Wings");
        item(MainCosmetics.ROYAL_CROWN, "Royal Crown");
        item(MainCosmetics.MINER_CAP, "Miner Cap");
        item(MainCosmetics.ORE_BACKPACK, "Ore Backpack");
        item(MainCosmetics.DRILL, "Drill");
        item(MainCosmetics.CRYSTAL_SPIKES, "Crystal Spikes");
        item(MainCosmetics.CATRINA_CROWN, "Catrina Crown");
        item(MainCosmetics.SUGAR_SKULL_MASK, "Sugar Skull Mask");
        item(MainCosmetics.CANDLE_SOMBRERO, "Candle Sombrero");
        item(MainCosmetics.CATRIN_CLOAK, "Catrin Cloak");
        item(MainCosmetics.CATRIN_GUITAR, "Catrin Guitar");
        item(MainCosmetics.EDM_HEADSET, "EDM Headset");
        item(MainCosmetics.SAXOPHONE, "Saxophone");
        item(MainCosmetics.GUITAR, "Guitar");
        item(MainCosmetics.TUBA, "Tuba");
        item(MainCosmetics.EDM_AURA, "EDM Aura");
        item(MainCosmetics.BAMBOO_HAT, "Bamboo Hat");
        item(MainCosmetics.ORNATE_FAN, "Ornate Fan");
        item(MainCosmetics.BLOSSOM_HAIR_PIECE, "Blossom Hair Piece");
        item(MainCosmetics.ORNATE_PARASOL, "Ornate Parasol");
        item(MainCosmetics.VEILED_HAT, "Veiled Hat");
        item(MainCosmetics.CHERRY_BLOSSOM_POT, "Cherry Blossom Pot");
        item(MainCosmetics.DAO_BLADE, "Dao Blade");
        item(MainCosmetics.LEI_NECKLACE, "Lei Necklace");
        item(MainCosmetics.SNORKEL_MASK, "Snorkel Mask");
        item(MainCosmetics.TIKI_MASK, "Tiki Mask");
        item(MainCosmetics.BEACH_BOOMBOX, "Beach Boombox");
        item(MainCosmetics.TROPICAL_HEADPIECE, "Tropical Headpiece");
        item(MainCosmetics.SEA_SHELL, "Sea Shell");
        item(MainCosmetics.FIRE_BATON, "Fire Baton");
        item(MainCosmetics.LEAF_WINGS, "Leaf Wings");
        item(MainCosmetics.LEAF_CROWN, "Leaf Crown");
        item(MainCosmetics.GNOME_HAT, "Gnome Hat");
        item(MainCosmetics.SHAMROCK_UMBRELLA, "Shamrock Umbrella");
        item(MainCosmetics.SHAMROCK_HAT, "Shamrock Hat");
        item(MainCosmetics.RAINBOW_AURA, "Rainbow Aura");
        item(MainCosmetics.ASTRONAUT_HELMET, "Astronaut Helmet");
        item(MainCosmetics.ASTRONAUT_AIR_TANK, "Astronaut Air Tank");
        item(MainCosmetics.SUSSY_HELMET, "Sussy Helmet");
        item(MainCosmetics.STELLAR_HAT, "Stellar Hat");
        item(MainCosmetics.FRANKENSTEIN_HAT, "Frankenstein Hat");
        item(MainCosmetics.CANDY_BUCKET, "Candy Bucket");
        item(MainCosmetics.JACKO_HELMET, "Jack-o'-Lantern Helmet");
        item(MainCosmetics.EYEBALL, "Eyeball");
        item(MainCosmetics.MEAT_CLEAVER, "Meat Cleaver");
        item(MainCosmetics.MAGIC_BROOM, "Magic Broom");
        item(MainCosmetics.PLAGUE_DOCTOR_MASK, "Plague Doctor Mask");
        item(MainCosmetics.MONSTER_SLAYER_GEAR, "Monster Slayer Gear");
        item(MainCosmetics.SPIDER_LEGS, "Spider Legs");
        item(MainCosmetics.MEDUSA_HAT, "Medusa Hat");
        item(MainCosmetics.FLOWER_BASKET, "Flower Basket");
        item(MainCosmetics.BONNET, "Bonnet");
        item(MainCosmetics.BUTTERFLY_ANTENNAE, "Butterfly Antennae");
        item(MainCosmetics.LADYBUG_WINGS, "Ladybug Wings");
        item(MainCosmetics.BUTTERFLY_WINGS, "Butterfly Wings");
        item(MainCosmetics.BLOOM_AURA, "Bloom Aura");
        item(MainCosmetics.BLOOMING_UMBRELLA, "Blooming Umbrella");
        item(MainCosmetics.GIFT_SACK, "Gift Sack");
        item(MainCosmetics.SHAKO_HAT, "Shako Hat");
        item(MainCosmetics.TOY_WINDER, "Toy Winder");
        item(MainCosmetics.DRUM_KIT, "Drum Kit");
        item(MainCosmetics.WESTERN_HAT, "Western Hat");
        item(MainCosmetics.CACTUS_HELMET, "Cactus Helmet");
        item(MainCosmetics.CACTUS_SUIT, "Cactus Suit");
        item(MainCosmetics.WINTER_SCARF, "Winter Scarf");
        item(MainCosmetics.SKI_HAT, "Ski Hat");
        item(MainCosmetics.MITTENS, "Mittens");
        item(MainCosmetics.SNOW_GLOBE, "Snow Globe");
        item(MainCosmetics.SNOWMAN_HELMET, "Snowman Helmet");
        item(MainCosmetics.SNOWMAN_SUIT, "Snowman Suit");
        item(MainCosmetics.SNOW_SHOVEL, "Snow Shovel");
        item(MainCosmetics.SKIIS, "Skiis");
        item(MainCosmetics.WIZARD_HAT, "Wizard Hat");
        item(MainCosmetics.WIZARD_STAFF, "Wizard Staff");
        item(MainCosmetics.WITCH_CAULDRON_COSMETIC, "Witch Cauldron");
        item(MainCosmetics.WIZARD_ROBE, "Wizard Robe");
        item(MainCosmetics.DRAGON_FLOAT_CLOAK, "Dragon Float Cloak");
        item(MainCosmetics.DRAGON_FLOAT_HELMET, "Dragon Float Helmet");
        item(MainCosmetics.PANDA_BACKPACK, "Panda Backpack");
        item(MainCosmetics.LUCKY_CAT, "Lucky Cat");

        tooltip("exclusive.aether_crate", "Aether Crate Exclusive");
        tooltip("exclusive.animal_crate", "Animal Crate Exclusive");
        tooltip("exclusive.aquatic_crate", "Aquatic Crate Exclusive");
        tooltip("exclusive.atlantis_crate", "Atlantis Crate Exclusive");
        tooltip("exclusive.bathroom_crate", "Bathroom Crate Exclusive");
        tooltip("exclusive.beach_crate", "Beach Crate Exclusive");
        tooltip("exclusive.business_crate", "Business Crate Exclusive");
        tooltip("exclusive.camping_crate", "Camping Crate Exclusive");
        tooltip("exclusive.construction_crate", "Construction Crate Exclusive");
        tooltip("exclusive.easter_crate", "Easter Crate Exclusive");
        tooltip("exclusive.fast_food_crate", "Fast Food Crate Exclusive");
        tooltip("exclusive.festive_crate", "Festive Crate Exclusive");
        tooltip("exclusive.fun_fair_crate", "Fun Fair Crate Exclusive");
        tooltip("exclusive.gothic_crate", "Gothic Crate Exclusive");
        tooltip("exclusive.harvest_crate", "Harvest Crate Exclusive");
        tooltip("exclusive.kitchen_crate", "Kitchen Crate Exclusive");
        tooltip("exclusive.lovely_crate", "Lovely Crate Exclusive");
        tooltip("exclusive.lunar_crate", "Lunar Crate Exclusive");
        tooltip("exclusive.medieval_crate", "Medieval Crate Exclusive");
        tooltip("exclusive.mining_crate", "Mining Crate Exclusive");
        tooltip("exclusive.muerte_crate", "Muerte Crate Exclusive");
        tooltip("exclusive.music_crate", "Music Crate Exclusive");
        tooltip("exclusive.palace_crate", "Palace Crate Exclusive");
        tooltip("exclusive.paradise_crate", "Paradise Crate Exclusive");
        tooltip("exclusive.shamrock_crate", "Shamrock Crate Exclusive");
        tooltip("exclusive.space_crate", "Space Crate Exclusive");
        tooltip("exclusive.spooky_crate", "Spooky Crate Exclusive");
        tooltip("exclusive.spring_crate", "Spring Crate Exclusive");
        tooltip("exclusive.toy_crate", "Toy Crate Exclusive");
        tooltip("exclusive.western_crate", "Western Crate Exclusive");
        tooltip("exclusive.winter_crate", "Winter Crate Exclusive");
        tooltip("exclusive.wizardry_crate", "Wizardry Crate Exclusive");
        tooltip("exclusive.zodiac_crate", "Zodiac Crate Exclusive");
    }
}
