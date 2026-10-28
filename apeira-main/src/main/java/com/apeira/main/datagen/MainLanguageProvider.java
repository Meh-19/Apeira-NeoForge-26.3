package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraLanguageProvider;
import com.apeira.core.registry.ShapeSet;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainBuildingBlocks;
import com.apeira.main.registry.MainCosmetics;
import com.apeira.main.registry.MainCrates;
import com.apeira.main.registry.MainCrystals;
import com.apeira.main.registry.MainDyedBlocks;
import com.apeira.main.registry.MainFarming;
import com.apeira.main.registry.MainEffects;
import com.apeira.main.registry.MainEntities;
import com.apeira.main.registry.MainNpcs;
import com.apeira.main.registry.MainGear;
import com.apeira.main.registry.MainItems;
import com.apeira.main.registry.MainLanterns;
import com.apeira.main.registry.MainMaterials;
import com.apeira.main.registry.MainPlants;
import com.apeira.main.registry.MainRealmBlocks;
import com.apeira.main.registry.MainRocks;
import com.apeira.main.registry.MainRubies;
import com.apeira.main.registry.MainSpecialBlocks;
import com.apeira.main.registry.MainStones;
import com.apeira.main.registry.MainWoods;
import com.apeira.main.world.MainBiomes;

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
        // Xenon's biomes: the ids in title case (Xenon named only The Depths, the same way).
        MainBiomes.ALL.forEach(biome -> add("biome.apeira." + biome.identifier().getPath(), title(biome.identifier().getPath())));

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

        rubies();
        farming();

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
        item(MainMaterials.CROCODILE_TOOTH, "Crocodile Tooth");
        mobs();
    }

    /** Xenon's mobs, their subtitles (Xenon's, for the wraith), and the capture net with Xenon's tooltip text. */
    private void mobs() {
        add(MainEntities.TURKEY.get(), "Turkey");
        add(MainEntities.GRIZZLY_BEAR.get(), "Grizzly Bear");
        add(MainEntities.CROCODILE.get(), "Crocodile");
        add(MainEntities.WRAITH.get(), "Wraith");
        add(MainNpcs.CONDUCTOR_CARLO.get(), "Conductor Carlo");
        add(MainNpcs.CONDUCTOR_MARCO.get(), "Conductor Marco");
        add(MainNpcs.CONDUCTOR_GINO.get(), "Conductor Gino");
        add(MainNpcs.CONDUCTOR_ANTONIO.get(), "Conductor Antonio");
        add("subtitles.apeira.entity.wraith.ambient", "Wraith whispers");
        add("subtitles.apeira.entity.wraith.target", "Wraith notices you");
        add("subtitles.apeira.entity.wraith.hurt", "Wraith hurts");
        add("subtitles.apeira.entity.wraith.death", "Wraith dies");
        add("subtitles.apeira.entity.wraith.bite", "Wraith bites");
        add("subtitles.apeira.entity.wraith.apparate", "Wraith vanishes");
        item(MainGear.CAPTURE_NET, "Capture Net");
        add("tooltip.apeira.capture_net.mob", "Mob: %s");
        add("tooltip.apeira.capture_net.1", "Collect and drop off");
        add("tooltip.apeira.capture_net.2", "mobs where you please");
        add("message.apeira.capture_net.weaken", "This mob must be weakened in order to be captured!");
        MainGear.shadowTools().forEach(tool -> item(tool, title(tool.getId().getPath())));
        MainGear.shadowArmor().forEach(armor -> item(armor, title(armor.getId().getPath())));
        tooltip("shadow_gear", "Shadow Gear");
        tooltip("shadow_sword.1", "1.5× damage to unaware enemies");
        tooltip("shadow_sword.2", "Stacks with Shadow Mode & Backstab");
        tooltip("shadow_armor.detection", "Reduces mob detection range by %s blocks");
        tooltip("shadow_armor.set", "Full set: enter Shadow Mode when hit");
        addEffect(MainEffects.SHADOW_MODE, "Shadow Mode");
        item(MainGear.ROCKET_BOOTS, "Rocket Boots");
        tooltip("rocket_boots.1", "Double-jump to fly");
        tooltip("rocket_boots.2", "Creative-style flight");
        tooltip("rocket_boots.fuel", "Fuel: %s min");
        message("rocket_boots.low_fuel", "⚠ Rocket boots fuel is running low!");
        navigator();
        realms();
        realmMenus();
        profilesAndParties();
    }

    /** Origin Realms' farming: its items and blocks, the planter box menu, crops, fertilizers and tooltips. */
    private void farming() {
        item(MainFarming.APPLE_SEEDS, "Apple Seed");
        item(MainFarming.BANANA_SEEDS, "Banana Seed");
        item(MainFarming.MANGO_PIT, "Mango Pit");
        item(MainFarming.CORN_SEEDS, "Corn Seed");
        item(MainFarming.PINEAPPLE_SEEDS, "Pineapple Seed");
        item(MainFarming.EGG_PLANT_SEEDS, "Eggplant Seed");
        item(MainFarming.LETTUCE_SEEDS, "Lettuce Seed");
        item(MainFarming.CHILLI_SEEDS, "Chili Seed");
        item(MainFarming.BANANA, "Banana");
        item(MainFarming.MANGO, "Mango");
        item(MainFarming.CORN, "Corn");
        item(MainFarming.PINEAPPLE, "Pineapple");
        item(MainFarming.EGG_PLANT, "Eggplant");
        item(MainFarming.LETTUCE, "Lettuce");
        item(MainFarming.CHILLI, "Chili");
        item(MainFarming.GOLDEN_BANANA, "Golden Banana");
        item(MainFarming.GOLDEN_MANGO, "Golden Mango");
        item(MainFarming.GOLDEN_CORN, "Golden Corn");
        item(MainFarming.GOLDEN_PINEAPPLE, "Golden Pineapple");
        item(MainFarming.GOLDEN_EGG_PLANT, "Golden Eggplant");
        item(MainFarming.GOLDEN_LETTUCE, "Golden Lettuce");
        item(MainFarming.GOLDEN_CHILLI, "Golden Chili");
        item(MainFarming.BAG_O_SOIL, "Bag O' Soil");
        item(MainFarming.FERTILIZER_MULTIPLIER, "Produce Multiplier Fertilizer");
        item(MainFarming.FERTILIZER_SPEED, "Speed-Grow Fertilizer");
        item(MainFarming.FERTILIZER_STAR, "3-Star Guarantee Fertilizer");
        item(MainFarming.FERTILIZER_GOLD, "Golden Goods Fertilizer");
        item(MainFarming.WOODEN_WATERING_CAN, "Wooden Watering Can");
        block(MainFarming.PLANTER_BOX, "Planter Box");
        block(MainFarming.WOODEN_SPRINKLER, "Wooden Sprinkler");
        block(MainFarming.IRON_SPRINKLER, "Iron Sprinkler");

        add("crop.apeira.apple", "Apple Tree");
        add("crop.apeira.banana", "Banana Tree");
        add("crop.apeira.mango", "Mango Tree");
        add("crop.apeira.corn", "Corn");
        add("crop.apeira.pineapple", "Pineapple");
        add("crop.apeira.egg_plant", "Eggplant");
        add("crop.apeira.lettuce", "Lettuce");
        add("crop.apeira.chilli", "Chili");
        add("fertilizer.apeira.produce_multiplier", "Produce Multiplier");
        add("fertilizer.apeira.produce_multiplier.description", "Harvests give 2 or 3 crops");
        add("fertilizer.apeira.speed_grow", "Speed-Grow");
        add("fertilizer.apeira.speed_grow.description", "The crop ripens after one watered day");
        add("fertilizer.apeira.three_star_guarantee", "3-Star Guarantee");
        add("fertilizer.apeira.three_star_guarantee.description", "Harvests are always 3-star");
        add("fertilizer.apeira.golden_goods", "Golden Goods");
        add("fertilizer.apeira.golden_goods.description", "The crop always ripens golden");

        add("tooltip.apeira.farming.seed", "Grows in %s days");
        add("tooltip.apeira.farming.plant_in_planter", "Plant in a Planter Box");
        add("tooltip.apeira.farming.soil", "Raises a crop's star rating");
        add("tooltip.apeira.farming.watering_can", "Water: %s/%s");
        add("message.apeira.farming.sprinkler_full", "The sprinkler is full");

        add("container.apeira.planter_box", "Planter Box");
        add("container.apeira.planter_box.info", "Planter Box");
        add("container.apeira.planter_box.info1", "Seeds, soil and fertilizer go in the slots;");
        add("container.apeira.planter_box.info2", "the next crop is planted from them.");
        add("container.apeira.planter_box.info3", "Water the box every day: crops grow");
        add("container.apeira.planter_box.info4", "one stage each morning at 7:00 AM.");
        add("container.apeira.planter_box.soiled", "Soiled");
        add("container.apeira.planter_box.not_soiled", "Not soiled");
        add("container.apeira.planter_box.watered", "Watered today");
        add("container.apeira.planter_box.not_watered", "Not watered today");
        add("container.apeira.planter_box.no_fertilizer", "No fertilizer");
        add("container.apeira.planter_box.empty", "Nothing planted");
        add("container.apeira.planter_box.empty1", "Put seeds in to plant them");
        add("container.apeira.planter_box.growing", "Day %s of %s");
        add("container.apeira.planter_box.needs_water", "Needs water to grow");
        add("container.apeira.planter_box.ripe", "Ripe: right-click the box to harvest");
        add("container.apeira.planter_box.ripe_golden", "Golden! Right-click the box to harvest");
    }

    /** The ruby deposit, its menu and sounds, the ruby commands' messages and the profile's ruby line. */
    private void rubies() {
        block(MainRubies.RUBY_DEPOSIT, "Ruby Deposit");
        add("container.apeira.ruby_deposit", "Ruby Deposit");
        add("container.apeira.ruby_deposit.info", "Learn more about Rubies");
        add("container.apeira.ruby_deposit.info1", "Put rubies in the slots above");
        add("container.apeira.ruby_deposit.info2", "to add them to your balance.");
        add("subtitles.apeira.ui.ruby_gain", "Rubies gained");
        addId("navigator", "lore.rubies", "Rubies: %s");
        addId("message", "rubies.balance", "You have %s rubies.");
        addId("message", "rubies.pay.self", "You can't pay yourself.");
        addId("message", "rubies.pay.full", "%s can't hold that many rubies.");
        addId("message", "rubies.pay.not_enough", "You don't have %s rubies (you have %s).");
        addId("message", "block.self", "You can't block yourself.");
        addId("message", "block.already", "You already have %s blocked.");
        addId("message", "block.not_blocked", "You don't have %s blocked.");
        addId("message", "block.blocked", "You blocked %s.");
        addId("message", "block.unblocked", "You unblocked %s.");
        addId("message", "rubies.pay.blocked", "You have %s blocked!");
        addId("message", "rubies.pay.sent", "You've paid %s rubies to %s!");
        addId("message", "rubies.pay.received", "You've received %s rubies from %s!");
        addId("message", "rubies.pay.received_offline", "You've received %s rubies from %s while you were offline!");
        addId("message", "rubies.set", "%s now has %s rubies.");
    }

    /** The profile pages, the party roster and popups, and the party messages. */
    private void profilesAndParties() {
        add("key.apeira.open_profile", "Open Profile");
        addId("navigator", "party.create", "Create a Party");
        addId("navigator", "party.create.desc", "Start a party, then invite players from their profile or with /party invite");
        addId("navigator", "party.manage", "Party");
        addId("navigator", "party.manage.desc", "Party chat, leave or disband; /party shows the whole roster");
        addId("navigator", "party_chat", "Party chat");
        addId("navigator", "party_chat.party", "Party only");
        addId("navigator", "party_chat.global", "Everyone");
        addId("navigator", "party_chat.info", "Party chat");
        addId("navigator", "party_chat.info.desc", "Send your chat messages to your party only");
        addId("navigator", "party_warp", "Warp party");
        addId("navigator", "party_disband", "Disband party");
        addId("navigator", "party_disband.desc", "End the party for every member");
        addId("navigator", "party_leave", "Leave party");
        addId("navigator", "party_leave.desc", "The next member leads if you led it");
        addId("navigator", "party_transfer", "Make leader");
        addId("navigator", "party_transfer.desc", "Hand the party's lead to this member");
        addId("navigator", "party_transfer.pick", "Make leader");
        addId("navigator", "party_transfer.pick.desc", "Pick a member first");
        addId("navigator", "party_kick", "Kick from party");
        addId("navigator", "party_kick.desc", "Remove this member from the party");
        addId("navigator", "party_kick.pick", "Kick from party");
        addId("navigator", "party_kick.pick.desc", "Pick a member first");
        addId("navigator", "learn_party", "Learn more about Parties");
        addId("navigator", "learn_party.desc", "Invite with /party invite [name], chat with /party chat");
        addId("navigator", "hint.party", "/party invite [name]");
        addId("navigator", "party_invite.desc", "Invite this player to your party");
        addId("navigator", "party_invite.member", "In your Party");
        addId("navigator", "party_invite.invited", "Invited to your Party");
        addId("navigator", "journal", "Journal");
        addId("navigator", "featured_badge", "Featured Badge");
        addId("navigator", "realm_teleport.own", "Teleport to your Realm");
        addId("navigator", "action.open_world_teleport", "Left-click to teleport");
        addId("navigator", "action.open_world_list", "Right-click to view all worlds");
        addId("navigator", "action.set_spawn_balloon", "Left-click to edit with balloon");
        addId("navigator", "action.set_spawn_here", "Right-click to set without balloon");
        addId("navigator", "realm_spawn", "Teleport to Realm Spawn");
        addId("navigator", "gravestones", "Gravestones");
        addId("navigator", "player_settings", "Player settings");
        addId("navigator", "cosmetic_slot.equip.hat", "Equip a hat cosmetic in the");
        addId("navigator", "cosmetic_slot.equip.back", "Equip a back cosmetic in the");
        addId("navigator", "cosmetic_slot.equip.prop", "Equip a prop cosmetic in the");
        addId("navigator", "cosmetic_slot.equip.balloon", "Equip a balloon cosmetic in the");
        addId("navigator", "cosmetic_slot.equip.where", "wardrobe at Cosmo's Cosmetics");
        addId("navigator", "cosmetic_slot.none.hat", "Not wearing hat");
        addId("navigator", "cosmetic_slot.none.back", "Not wearing back swag");
        addId("navigator", "cosmetic_slot.none.prop", "Not holding prop");
        addId("navigator", "cosmetic_slot.none.balloon", "Not holding balloon");
        addId("navigator", "status.online", "Currently online");
        addId("navigator", "status.offline", "Currently offline");
        addId("navigator", "lore.join_date", "Join date: %s");
        addId("navigator", "status.afk", "AFK");
        addId("navigator", "lore.party_role", "Role: %s");
        addId("navigator", "lore.party_leader", "Leader");
        addId("navigator", "lore.party_member", "Member");
        addId("navigator", "lore.party_invited", "Invited");
        addId("navigator", "lore.afk", "AFK: %s");
        addId("navigator", "lore.playtime", "Playtime: %s");
        addId("navigator", "action.edit_party_member", "Edit member");
        addId("navigator", "action.cancel_party_invite", "Take back invite");
        message("party.already", "You're already in a party");
        message("party.created", "Created a party");
        message("party.not_leader", "Only the party leader can do that");
        message("party.already_member", "%s is already in your party");
        message("party.target_in_party", "%s is already in a party");
        message("party.offline", "%s is not online");
        message("party.full", "Your party is full");
        message("party.invite", "Invited %s to your party");
        message("party.invited", "%s invited you to their party! %s");
        message("party.invited.hover", "Join the party");
        message("party.no_invite", "%s hasn't invited you to a party");
        message("party.declined", "Declined %s's party invite");
        message("party.declined_by", "%s declined the party invite");
        message("party.invite_cancelled", "Took back the party invite to %s");
        message("party.none", "You're not in a party");
        message("party.left_self", "You left the party");
        message("party.disbanded", "%s disbanded the party");
        message("party.kicked", "%s was kicked from the party");
        message("party.new_leader", "%s now leads the party");
        message("party.chat_on", "Party chat on: your messages go to your party only");
        message("party.chat_off", "Party chat off");
        message("party.left", "%s left the party");
        message("party.joined", "%s joined the party");
        message("party.chat", "[Party] <%s> %s");
    }
    /** The onboarding screen's title and the realm messages; the screen's cards are textures. */
    private void realms() {
        add("screen.apeira.onboarding", "Create Your Realm");
        message("realm.home", "Welcome home!");
        message("realm.exists", "You already have a Realm");
        message("realm.none", "You don't have a Realm yet. Use /realm create to make one");
    }

    /** The NPC settings popup, its messages, the anchor placement cursor, voices and dialogue. */
    private void npcs() {
        addId("navigator", "npc_visibility.info", "Set your npc visibility to visible or invisible");
        addId("navigator", "npc_visibility", "NPC Visibility");
        addId("navigator", "npc_visibility.visible", "Visible");
        addId("navigator", "npc_visibility.invisible", "Invisible");
        add("subtitles.apeira.npc.voice.male1", "NPC speaks");
        add("dialogue.apeira.conductor_carlo.greeting",
                "Hi there, traveller! The skies are looking clear and perfect for flight! Where are we heading today?");
        add("dialogue.apeira.conductor_marco.greeting",
                "Full of air and ready to lift off. not a better day for it! Where are we flying today, traveler?");
        add("dialogue.apeira.conductor_gino.greeting",
                "Another perfect day for flight, I hope these trees don't get in the way! Now tell me, where's your destination?");
        add("dialogue.apeira.conductor_antonio.greeting",
                "What a lovely place to lift off, these fields make flying the joy of my day. Where would you like to go?");
        addId("navigator", "npc_anchor", "Set Anchor");
        addId("navigator", "npc_anchor.desc", "Pick the spot this NPC stands on");
        addId("navigator", "action.npc_anchor_place", "Left-click to place");
        message("npc.not_owner", "Only the Realm's owner can edit this NPC");
        message("npc.shown", "%s is now visible");
        message("npc.hidden", "%s is now invisible");
        message("npc.anchored", "%s moved to its new anchor");
        message("npc.anchor_blocked", "The NPC doesn't fit there");
        add("hud.apeira.npc_anchor.aim", "Look at where the NPC should stand");
        add("hud.apeira.npc_anchor.clear", "The NPC fits here");
        add("hud.apeira.npc_anchor.blocked", "Something is in the way");
        add("hud.apeira.npc_anchor.confirm", "Left-click to place the NPC");
        add("hud.apeira.npc_anchor.cancel", "Right-click/ESC to cancel");
    }

    /** The navigator's title, its buttons' tooltips (Origin Realms' wording) and its messages. */
    private void navigator() {
        add("screen.apeira.navigator", "Navigator");
        addId("navigator", "realms", "Realm");
        addId("navigator", "realms.desc", "Go to your personal realm or visit one you're invited to");
        addId("navigator", "open_worlds", "Open World");
        addId("navigator", "open_worlds.desc", "Visit an open world, gathering blocks, loot, and slaying mobs");
        addId("navigator", "homes", "Homes");
        addId("navigator", "homes.desc", "Create personal waypoints at the locations that mean the most");
        addId("navigator", "spawn", "Spawn");
        addId("navigator", "spawn.desc", "Teleport to your spawn point");
        addId("navigator", "back", "← Back");
        addId("navigator", "close", "Close");
        addId("navigator", "realm_teleport", "Teleport to your Realm");
        addId("navigator", "realm_access", "Realm access");
        addId("navigator", "realm_settings", "Realm settings");
        addId("navigator", "open_world", "Open World travel");
        for (String colour : new String[] {"red", "orange", "yellow", "green", "blue", "purple"}) {
            addId("navigator", "open_world." + colour, title(colour) + " Open World");
        }
        message("navigator.spawn", "Teleported to your spawn point");
        message("navigator.unavailable", "%s is not available yet");
    }

    /** The Realms pages' tooltips (Origin Realms' wording), roles and realm and friend messages. */
    private void realmMenus() {
        addId("navigator", "realm_view", "Realm view");
        addId("navigator", "not_available", "Not available yet");
        addId("navigator", "realms_tab", "Realms");
        addId("navigator", "events", "Events");
        addId("navigator", "profile", "Your Profile");
        addId("navigator", "badges", "Badges");
        addId("navigator", "social", "Social");
        addId("navigator", "auction_house", "Auction House");
        addId("navigator", "store", "Store");
        addId("navigator", "realm_settings.close", "Realm settings");
        addId("navigator", "learn_realms", "Learn more about Realms");
        addId("navigator", "access.in_realm", "In Realm");
        addId("navigator", "access.members", "Members");
        addId("navigator", "access.banned", "Banned players");
        addId("navigator", "previous_page", "Previous page");
        addId("navigator", "next_page", "Next page");
        addId("navigator", "filter", "Filter by");
        addId("navigator", "filter.all", "All");
        addId("navigator", "filter.added_to", "Added to");
        addId("navigator", "filter.friends", "Friends");
        addId("navigator", "filter.resident", "Resident");
        addId("navigator", "filter.trusted", "Trusted");
        addId("navigator", "sort", "Sort by");
        addId("navigator", "sort.alpha_asc", "Alphabetical");
        addId("navigator", "sort.count_asc", "Players (ascending)");
        addId("navigator", "sort.count_desc", "Players (descending)");
        addId("navigator", "sort.rank", "Role");
        addId("navigator", "sort.last_online", "Last Online");
        addId("navigator", "sort.time_asc", "Time (ascending)");
        addId("navigator", "sort.time_desc", "Time (descending)");
        addId("navigator", "hint.cmd", "CMD: ");
        addId("navigator", "hint.invite", "/realm invite [name]");
        addId("navigator", "hint.ban", "/realm ban [name]");
        addId("navigator", "hint.sethome", "/sethome [name]");
        addId("navigator", "kick_player", "Kick player from world");
        addId("navigator", "kick_member", "Kick member from Realm");
        addId("navigator", "ban_member", "Ban member from Realm");
        addId("navigator", "invite_player", "Invite player to Realm");
        addId("navigator", "invite_player.off", "Already a member or invited");
        addId("navigator", "role_button.trusted", "Trusted role");
        addId("navigator", "role_button.resident", "Resident role");
        addId("navigator", "perk.open_chests", "Open chests");
        addId("navigator", "perk.break_blocks", "Break blocks");
        addId("navigator", "perk.place_blocks", "Place blocks");
        addId("navigator", "visiting_rule", "Visiting rule");
        addId("navigator", "visiting_rule.everyone", "Everyone");
        addId("navigator", "visiting_rule.friends", "Friends");
        addId("navigator", "visiting_rule.members", "Members");
        addId("navigator", "visiting_rule.nobody", "Just you");
        addId("navigator", "realm_chat.info", "Set your Realm chat");
        addId("navigator", "realm_chat.info.desc", "to private or global");
        addId("navigator", "realm_chat", "Realm chat toggle");
        addId("navigator", "realm_chat.private", "Private chat");
        addId("navigator", "realm_chat.global", "Global chat");
        addId("navigator", "realm_access_toggle.info", "Set visitor access to your");
        addId("navigator", "realm_access_toggle.info.desc", "Realm when you're offline");
        addId("navigator", "realm_access_toggle", "Realm access toggle");
        addId("navigator", "realm_access_toggle.always", "Always accessible");
        addId("navigator", "realm_access_toggle.online", "If you're online");
        addId("navigator", "set_spawn", "Change Realm spawnpoint");
        addId("navigator", "reset", "Reset a Realm dimension");
        addId("navigator", "reset.overworld", "Reset your Overworld");
        addId("navigator", "reset.overworld.off", "Travel to your Overworld");
        addId("navigator", "reset.overworld.off.desc", "to reset it");
        addId("navigator", "reset.nether.off", "Travel to your Nether");
        addId("navigator", "reset.nether.off.desc", "to reset it");
        addId("navigator", "reset.aether.off", "Travel to your Aether");
        addId("navigator", "reset.aether.off.desc", "to reset it");
        addId("navigator", "reset.confirm", "Confirm reset");
        addId("navigator", "reset.confirm.desc", "Your Overworld becomes a new %s Realm");
        addId("navigator", "balloon_color", "Color Realm balloon");
        addId("navigator", "balloon_color.desc", "Pick a colour for your balloon");
        addId("navigator", "action.balloon_paint", "Left-click to paint the balloon");
        addId("navigator", "preset.lush", "Lush");
        addId("navigator", "preset.arid", "Arid");
        addId("navigator", "preset.cold", "Cold");
        addId("navigator", "preset.arctic", "Arctic");
        addId("navigator", "preset.tropical", "Tropical");
        addId("navigator", "dimension.overworld", "Overworld");
        addId("navigator", "dimension.nether", "Nether");
        addId("navigator", "dimension.aether", "Aether");
        addId("navigator", "lore.players", "Players: %s");
        addId("navigator", "lore.created", "Created: %s ago");
        addId("navigator", "lore.role", "Role: %s");
        addId("navigator", "lore.joined", "Joined: %s");
        addId("navigator", "lore.joined_ago", "Joined: %s ago");
        addId("navigator", "lore.online", "Online: %s");
        addId("navigator", "lore.last_online", "Last Online: %s ago");
        addId("navigator", "lore.banned", "Banned: %s");
        addId("navigator", "lore.now", "Now");
        addId("navigator", "action.visit_realm", "Left-click to visit Realm");
        addId("navigator", "action.edit_player", "Left-click to edit player");
        addId("navigator", "action.edit_member", "Left-click to edit member");
        addId("navigator", "action.unban", "Left-click to unban player");
        addId("navigator", "role.owner", "Owner");
        addId("navigator", "role.trusted", "Trusted");
        addId("navigator", "role.resident", "Resident");
        addId("navigator", "role.visitor", "Visitor");
        message("realm.visiting", "Welcome to %s's Realm!");
        message("realm.no_realm", "%s doesn't have a Realm");
        message("realm.cannot_visit", "You can't visit %s's Realm right now");
        message("realm.removed", "You can no longer be in %s's Realm");
        message("realm.not_here", "%s isn't in your Realm");
        message("realm.kicked", "%s kicked you from their Realm");
        message("realm.kick", "Kicked %s from your Realm");
        message("realm.banned", "%s banned you from their Realm");
        message("realm.ban", "Banned %s from your Realm");
        message("realm.unban", "Unbanned %s");
        message("realm.already_member", "%s is already a member of your Realm");
        message("realm.invite", "Invited %s to your Realm");
        message("realm.invited", "! %s invited you to join their Realm! %s or use /navigator to view in Realm Menu");
        message("realm.invite.accept", "[ACCEPT]");
        message("realm.invite.accept.hover", "Click to accept");
        message("realm.no_invite", "You have no invite from %s");
        message("realm.joined", "You are now a Resident of %s's Realm");
        message("realm.accepted", "%s accepted your Realm invite");
        message("realm.not_member", "%s is not a member of your Realm");
        message("realm.member", "%s is a member of your Realm: %s");
        message("realm.role", "%s is now %s");
        message("realm.remove", "Removed %s from your Realm");
        message("realm.not_member_of", "You are not a member of %s's Realm");
        message("realm.left", "You left %s's Realm");
        message("realm.not_in_realm", "Stand in your Realm to do that");
        message("realm.spawn_set", "Realm spawn moved to the balloon");
        message("realm.spawn_set_here", "Realm spawn set where you stand, without the balloon");
        message("realm.setspawn.blocked", "The balloon doesn't fit there");
        message("realm.balloon.painted", "Realm balloon painted %s");
        message("realm.balloon.unpainted", "Unpainted");
        message("realm.balloon.unknown", "Unknown balloon colour %s");
        block(MainRealmBlocks.STARTER_BALLOON, "Starter Balloon");
        add("hud.apeira.setspawn.aim", "Look at where the balloon should stand");
        add("hud.apeira.setspawn.clear", "The balloon fits here");
        add("hud.apeira.setspawn.blocked", "Something is in the way");
        add("hud.apeira.setspawn.confirm", "Left-click to set spawn");
        add("hud.apeira.setspawn.cancel", "Right-click/ESC to cancel");
        add("hud.apeira.top_bar.realm", "%s's Realm");
        npcs();
        add("hud.apeira.top_bar.balloon_platform", "Air Balloon Platform");
        message("realm.reset.stand_in", "Stand in your Realm's Overworld to reset it");
        message("realm.reset.moved", "This Realm is being reset");
        message("realm.reset", "Your Overworld was reset as a %s Realm");
        message("realm.reset.travel", "Travel to your %s to reset it");
        message("realm.unknown_player", "No player named %s has joined");
        message("realm.private_chat", "[Realm] <%s> %s");
        message("realm.denied.build", "You can't build in this Realm");
        message("realm.denied.trusted", "Only Trusted members can do that here");
        message("realm.denied.visitor", "Visitors can only look around in this Realm");
        message("friend.request", "%s wants to be friends! %s");
        message("friend.requested", "Sent a friend request to %s");
        message("friend.added", "You are now friends with %s");
        message("friend.no_request", "%s hasn't asked to be friends");
        message("friend.not_friends", "You aren't friends with %s");
        message("friend.removed", "You are no longer friends with %s");
        message("friend.list", "Friends: %s");
        addId("navigator", "rename_home", "Rename home");
        addId("navigator", "rename_home.hint", "New name, then Enter");
        addId("navigator", "home.realm_world", "%s's Realm");
        addId("navigator", "dimension.end", "The End");
        addId("navigator", "lore.created_on", "Created: %s");
        addId("navigator", "lore.world", "World: %s");
        addId("navigator", "lore.position", "Position: %s");
        addId("navigator", "action.teleport", "Left-click to teleport");
        addId("navigator", "action.delete", "Right-click to delete");
        addId("navigator", "action.rename", "Shift-click to rename");
        addId("navigator", "party", "Party controls");
        addId("navigator", "learn_social", "Learn more about Social");
        addId("navigator", "filter.online", "Online");
        addId("navigator", "filter.offline", "Offline");
        addId("navigator", "filter.requests", "Friend requests");
        addId("navigator", "hint.friend", "/friend add [name]");
        addId("navigator", "lore.request", "Request: %s");
        addId("navigator", "lore.request.incoming", "Wants to be your friend");
        addId("navigator", "lore.request.sent", "Sent, waiting for an answer");
        addId("navigator", "action.accept_friend", "Left-click to accept");
        addId("navigator", "action.decline_friend", "Right-click to decline");
        addId("navigator", "action.view_profile", "Left-click to view Profile");
        addId("navigator", "action.cancel_friend", "Right-click to take back");
        addId("navigator", "action.unfriend", "Shift-click to unfriend");
        addId("navigator", "party_invite", "Invite to Party");
        addId("navigator", "trade_invite", "Invite to trade");
        addId("navigator", "remove_realm", "Remove from Realm");
        addId("navigator", "invite_player.invited", "Invited to your Realm");
        addId("navigator", "unfriend", "Unfriend");
        addId("navigator", "friend_request.incoming", "Accept friend request");
        addId("navigator", "friend_request.incoming.desc", "Right-click to decline");
        addId("navigator", "friend_request.sent", "Friend request sent");
        addId("navigator", "friend_request.sent.desc", "Right-click to take it back");
        addId("navigator", "friend_request.send", "Send friend request");
        addId("navigator", "view_auctions", "View auctions");
        addId("navigator", "teleport_realm", "Teleport to their Realm");
        message("friend.already", "You're already friends with %s");
        message("friend.declined", "Declined %s's friend request");
        message("friend.no_sent_request", "You haven't asked %s to be friends");
        message("friend.cancelled", "Took back your friend request to %s");
        message("home.bad_name", "Home names are 1 to %s letters, digits, _ or -");
        message("home.full", "You can have at most %s homes");
        message("home.set", "Home %s set");
        message("home.missing_world", "The world of home %s isn't loaded");
        message("home.teleported", "Teleported to %s");
        message("home.deleted", "Deleted home %s");
        message("home.taken", "You already have a home called %s");
        message("home.renamed", "Renamed home %s to %s");
        message("home.unknown", "That home doesn't exist");
        message("dummy.bad_name", "Dummy names are 3 to 16 letters, digits or _");
        message("dummy.exists", "%s is already online or a dummy");
        message("dummy.added", "Added dummy player %s");
        message("dummy.removed", "Removed dummy player %s");
        message("dummy.unknown", "No dummy player named %s");
        message("dummy.list", "%s dummy players: %s");
        message("dummy.bad_preset", "Unknown realm region %s: lush, arid, cold, arctic or tropical");
        message("dummy.has_realm", "%s already has a realm");
        add("message.apeira.dev.commands", "%s commands added by mods:");
        message("dummy.no_realm", "%s has no realm yet: /apeiradev dummy create_realm %s");
        message("dummy.realm_created", "%s claimed a %s realm (%s)");
        message("dummy.realm_reset", "%s reset their realm to %s");
        message("dummy.already_member", "You are already a member of %s's realm");
        message("dummy.not_member", "You are not a member of %s's realm");
        message("dummy.not_in_realm", "You are not in %s's realm");
        message("dummy.role", "%s made you %s in their realm");
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
