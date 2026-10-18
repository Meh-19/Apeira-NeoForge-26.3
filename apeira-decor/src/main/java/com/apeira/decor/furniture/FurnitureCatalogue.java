package com.apeira.decor.furniture;

import static com.apeira.decor.furniture.Furniture.piece;

import java.util.ArrayList;
import java.util.List;

/**
 * Every furniture piece, generated from the legacy mods' models: the Crates mod's furniture, grouped by the crate it
 * is exclusive to and in its order, then ORFurniture's and CosmoLib's. Shapes are in pixels, facing north. Generated
 * once; edit it by hand from now on.
 */
final class FurnitureCatalogue {
    private FurnitureCatalogue() {}

    static List<Furniture> all() {
        List<Furniture> all = new ArrayList<>();
        aetherCrate(all);
        animalCrate(all);
        aquaticCrate(all);
        atlantisCrate(all);
        bathroomCrate(all);
        beachCrate(all);
        businessCrate(all);
        campingCrate(all);
        constructionCrate(all);
        easterCrate(all);
        fastFoodCrate(all);
        festiveCrate(all);
        funFairCrate(all);
        gardenCrate(all);
        gothicCrate(all);
        harvestCrate(all);
        kitchenCrate(all);
        lovelyCrate(all);
        lunarCrate(all);
        medievalCrate(all);
        miningCrate(all);
        muerteCrate(all);
        musicCrate(all);
        palaceCrate(all);
        paradiseCrate(all);
        shamrockCrate(all);
        spaceCrate(all);
        spookyCrate(all);
        springCrate(all);
        toyCrate(all);
        westernCrate(all);
        winterCrate(all);
        wizardryCrate(all);
        zodiacCrate(all);
        legacy(all);
        return List.copyOf(all);
    }

    /** The Aether Crate's furniture. */
    private static void aetherCrate(List<Furniture> all) {
        all.add(piece("sheepuff_plushie", "aether_crate").paintable().shape(
                0, 1, 0, 16, 16, 16));
        all.add(piece("zeus_statue", "aether_crate").paintable().large(2, -16, 0).shape(
                2.5, 0, 5, 13.5, 16, 11,
                1.5, 5, 4, 14.5, 16, 12,
                11.4808, 13.9293, 4.682, 16, 16, 9.682));
    }

    /** The Animal Crate's furniture. */
    private static void animalCrate(List<Furniture> all) {
        all.add(piece("capybara_plushie", "animal_crate").paintable().shape(
                2.21, 0, 1.1533, 6.1875, 2.25, 5.1308,
                9.8125, 0, 1.1533, 13.79, 2.25, 5.1308,
                4.625, 0, 3.5, 11.375, 6.75, 13.625,
                2.2498, 0, 12.0227, 6.2273, 2.25, 16,
                9.7727, 0, 12.0227, 13.7502, 2.25, 16,
                5.1875, 5.625, 0, 10.8125, 11.25, 5.75,
                4.392, 10.6875, 4.625, 5.1875, 11.8125, 5.4205,
                10.8125, 10.6875, 4.625, 11.608, 11.8125, 5.4205));
        all.add(piece("cat_plushie", "animal_crate").paintable().shape(
                4.0625, 0, 0, 11.9375, 5.625, 5.75,
                0, 0, 4.625, 4.361, 2.25, 8.4258,
                3.5, 0, 4.625, 12.5, 7.875, 15.875,
                11.639, 0, 4.625, 16, 2.25, 8.4258,
                0.318, 0, 12.5, 5.091, 2.25, 16,
                10.909, 0, 12.5, 15.682, 2.25, 16,
                6.875, 1.1815, 14.284, 9.125, 6.75, 16,
                5.1875, 5.625, 1.25, 6.3125, 6.75, 3.5,
                9.6875, 5.625, 1.25, 10.8125, 6.75, 3.5));
        all.add(piece("dog_plushie", "animal_crate").paintable().shape(
                0, 0, 0, 16, 4, 12,
                4, 0, 12, 16, 4, 16,
                4, 4, 0, 12, 16, 12,
                0, 8, 4, 4, 16, 8,
                12, 8, 4, 16, 16, 8));
        all.add(piece("froggy_chair", "animal_crate").paintable().seat(12.06).shape(
                2.0705, 0.6118, 0, 5.0855, 10.666, 5.6547,
                10.9145, 0.6118, 0, 13.9295, 10.666, 5.6547,
                2.0705, 0.6118, 10.3453, 5.0855, 10.666, 16,
                10.9145, 0.6118, 10.3453, 13.9295, 10.666, 16,
                1.97, 9.045, 1.97, 14.03, 12.06, 14.03,
                1.97, 10.9371, 11.0834, 14.03, 16, 16));
        all.add(piece("giraffe_plushie", "animal_crate").paintable().shape(
                2.2108, 0, 0.3854, 6.8726, 2.25, 7.4827,
                9.1274, 0, 0.3854, 13.7892, 2.25, 7.4827,
                5.1875, 0, 2.375, 10.8125, 5.625, 13.625,
                2.9375, 0, 10.25, 5.1875, 2.25, 14.75,
                10.8125, 0, 10.25, 13.0625, 2.25, 14.75,
                6.875, 0.852, 12.034, 9.125, 5.625, 16,
                6.3125, 3.5463, 0.9169, 9.6875, 13.1527, 7.4791,
                7.4375, 4.8378, 2.7434, 8.5625, 16, 8.5185,
                6.3125, 10.5696, 0, 9.6875, 16, 4.035));
        all.add(piece("cat_clock", "animal_crate").paintable().shape(
                6.995, 0, 13.5275, 9.005, 6.03, 15.5375,
                3.4775, 6.03, 13.025, 12.5225, 8.04, 16,
                3.4775, 8.04, 14.03, 12.5225, 16, 16,
                1.4675, 9.7233, 12.0703, 3.4775, 16, 16,
                12.5225, 9.7233, 12.0703, 14.5325, 16, 16));
        all.add(piece("elephant_inflatable", "animal_crate").paintable().large(2, 0, 0).shape(
                0, 0, 15, 5, 14, 16,
                11, 0, 15, 16, 14, 16,
                0, 14, 0, 16, 16, 16));
    }

    /** The Aquatic Crate's furniture. */
    private static void aquaticCrate(List<Furniture> all) {
        all.add(piece("giant_oyster", "aquatic_crate").paintable().large(1, -14, 0).shape(
                0, 0, 0, 16, 11, 16,
                0, 6, 7.581, 16, 16, 16,
                15, 9, 2.75, 16, 14, 6.75));
        all.add(piece("fishing_boat", "aquatic_crate").paintable().large(2, -6, 0).shape(
                6, 2, 0, 10, 6, 16,
                0, 5.8, 0, 16, 16, 16));
        all.add(piece("fishing_dock", "aquatic_crate").large(2, -6.5, 0).shape(
                0, 12, 7.5, 16, 15, 15.5,
                0, 13, 0, 16, 16, 7.5));
        all.add(piece("large_jellyfish", "aquatic_crate").paintable().large(1, -12, 0).shape(
                0, 0, 0, 16, 16, 16));
    }

    /** The Atlantis Crate's furniture. */
    private static void atlantisCrate(List<Furniture> all) {
        all.add(piece("atlantean_chair", "atlantis_crate").paintable().seat(13.5).large(1, -12, 0).shape(
                0, 0, 7, 5, 5, 9,
                11, 0, 7, 16, 5, 9,
                3, 5, 7, 5, 10, 9,
                11, 5, 7, 13, 10, 9,
                1, 7.9354, 3.7088, 15, 16, 16,
                0, 12.6863, 13, 16, 16, 15));
        all.add(piece("atlantean_table", "atlantis_crate").paintable().shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("atlantean_lantern", "atlantis_crate").paintable().shape(
                2, 0, 2, 14, 4, 14,
                4, 4, 4, 12, 12, 12,
                2, 12, 2, 14, 16, 14));
        all.add(piece("atlantean_pillar", "atlantis_crate").paintable().large(2, -14.5, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("atlantean_throne", "atlantis_crate").paintable().seat(11).large(2, -16, 0).shape(
                0, 0, 0, 16, 15, 15,
                0, 0, 15, 16, 16, 16));
        all.add(piece("poseidon_statue", "atlantis_crate").paintable().large(2, -8, 0).shape(
                2.5, 0, 5, 13.5, 16, 11,
                1.5, 6, 4, 14.5, 16, 12));
        all.add(piece("atlantean_shrine", "atlantis_crate").paintable().large(2, -16, 0).shape(
                0, 0, 0, 16, 16, 16));
    }

    /** The Bathroom Crate's furniture. */
    private static void bathroomCrate(List<Furniture> all) {
        all.add(piece("bathroom_bin", "bathroom_crate").paintable().shape(
                3, 0, 3, 13, 10, 13,
                5, 6.1, 12.4, 11, 12.1, 13.6,
                3.51, 7.8457, 2.9281, 5.51, 14.1543, 7.0719,
                3.5, 8, 3.5, 12.5, 12, 12.5,
                7.1716, 8.1716, 8.5, 12.8284, 13.8284, 10.5,
                2.5, 10, 2.5, 13.5, 12, 13.5));
        all.add(piece("shower_head", "bathroom_crate").paintable().shape(
                2.5, 0, 2.5, 13.5, 2, 13.5,
                7, 2, 7, 9, 12, 9,
                5, 12, 5, 11, 13, 11));
        all.add(piece("bathroom_mirror", "bathroom_crate").paintable().shape(
                0, 0, 14, 16, 16, 16));
        all.add(piece("bathroom_rug", "bathroom_crate").paintable().shape(
                0, 0, 0, 16, 1, 16));
        all.add(piece("toilet", "bathroom_crate").paintable().shape(
                3.5, 0, 0, 12.5, 8, 15,
                2, 4, 0, 14, 9, 11,
                1, 8, 11, 15, 16, 16,
                1.5, 8.5, 0, 14.5, 10.5, 11.5));
        all.add(piece("washing_machine", "bathroom_crate").paintable().shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("bathtub", "bathroom_crate").paintable().large(1, -3, 0).shape(
                0, 2, 2, 16, 16, 16,
                11, 3.5, 0.5, 16, 16, 3.5));
    }

    /** The Beach Crate's furniture. */
    private static void beachCrate(List<Furniture> all) {
        all.add(piece("beach_umbrella", "beach_crate").paintable().large(1, -16, 0).shape(
                7, 0, 7, 9, 16, 9));
        all.add(piece("moai_statue", "beach_crate").large(1, -16, 0).shape(
                1, 0, 2, 15, 12, 14,
                0, 0, 6, 1, 10, 12,
                15, 0, 6, 16, 10, 12,
                0, 12, 0, 16, 16, 16));
        all.add(piece("sandbox", "beach_crate").paintable().shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("potted_palm_tree", "beach_crate").paintable().large(2, -12, 0).shape(
                2, 0, 2, 14, 8, 14,
                4, 8, 4, 12, 16, 12));
        all.add(piece("volleyball_net", "beach_crate").paintable().large(2, 0, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("ice_cream_signage", "beach_crate").paintable().large(2, 0, 0).shape(
                2, 0, 2, 14, 2, 14,
                6, 0, 6, 10, 14, 10,
                0, 4.8201, 8.6072, 16, 16, 16,
                0, 7.3137, 1.4343, 16, 16, 16,
                0, 7.5572, 0, 16, 16, 11.3994));
        all.add(piece("slushy_signage", "beach_crate").paintable().large(2, 0, 0).shape(
                2, 0, 2, 14, 2, 14,
                6, 0, 6, 10, 14, 10,
                0, 2.7499, 0, 16, 16, 16));
        all.add(piece("tiki_statue", "beach_crate").paintable().shape(
                0.05, 0, 0.05, 15.95, 3.9, 15.95,
                0.1, 3.8, 10.95, 15.9, 13.6, 15.75));
        all.add(piece("food_truck", "beach_crate").paintable().large(2, -4.545, 0).shape(
                0, 4.04, 0, 16, 16, 16));
    }

    /** The Business Crate's furniture. */
    private static void businessCrate(List<Furniture> all) {
        all.add(piece("laptop", "business_crate").paintable().shape(
                1, 0, 3, 15, 1, 13,
                1.005, 0.213, 12.769, 14.995, 10.7583, 16));
        all.add(piece("office_chair", "business_crate").paintable().seat(10).shape(
                0, 0, 0, 16, 4, 16,
                4, 4, 4, 12, 12, 12,
                0, 8, 0, 16, 12, 4,
                0, 8, 4, 4, 16, 16,
                12, 8, 4, 16, 16, 16,
                4, 8, 12, 12, 16, 16));
        all.add(piece("desk_lamp", "business_crate").paintable().shape(
                4, 0, 4, 12, 1, 12,
                7.5, 1, 7.5, 8.5, 7, 8.5,
                7.505, 6.6177, 6.0463, 8.495, 10.6959, 8.5009,
                5, 7.4755, 0, 11, 14.9322, 6.4281,
                6, 10.3128, 4.5147, 10, 15.1564, 8.817));
        all.add(piece("office_blinds", "business_crate").paintable().large(1, -16, 0).shape(
                0, 2.1019, 11.2284, 16, 4.0154, 15.8478,
                0, 6.1019, 11.2284, 16, 8.0154, 15.8478,
                0, 10.1019, 11.2284, 16, 12.0154, 15.8478,
                0, 14.1019, 11.2284, 16, 16, 15.8478));
        all.add(piece("office_desk", "business_crate").paintable().shape(
                0, 0, 1, 16, 15, 15,
                0, 15, 0, 16, 16, 16));
        all.add(piece("printing_machine", "business_crate").paintable().large(1.005, 0, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("revolving_door", "business_crate").paintable().large(2, -16, 0).shape(
                0, 0, 0, 16, 16, 16));
    }

    /** The Camping Crate's furniture. */
    private static void campingCrate(List<Furniture> all) {
        all.add(piece("young_cyprus", "camping_crate").paintable().large(2, -8, 0).shape(
                5, 0, 5, 11, 16, 11,
                10.9986, 9, 0, 16, 16, 4.9986,
                0, 11, 0, 5.0014, 16, 4.9986,
                0, 11, 11.0014, 5.0014, 16, 16,
                10.9986, 11, 11.0014, 16, 16, 16,
                4.5, 13.8086, 0, 11.5, 16, 4.8771,
                0, 13.8086, 4.5, 4.8771, 16, 11.5,
                11.1229, 13.8086, 4.5, 16, 16, 11.5,
                4.5, 13.8086, 11.1229, 11.5, 16, 16));
        all.add(piece("bonfire", "camping_crate").large(2, -12.5, 0).shape(
                0, 5, 0, 16, 11, 16,
                0, 5, 0, 16, 16, 3,
                0, 5, 0, 3, 16, 16,
                13, 5, 0, 16, 16, 16,
                0, 5, 13, 16, 16, 16,
                0, 15, 0, 16, 16, 16));
        all.add(piece("campervan", "camping_crate").paintable().large(2, -8, 0).shape(
                0, 4, 0, 16, 16, 16));
    }

    /** The Construction Crate's furniture. */
    private static void constructionCrate(List<Furniture> all) {
        all.add(piece("i_beam", "construction_crate").paintable().large(2, 0, 0).shape(
                0, 0, 0, 16, 3.006, 16,
                0, 3.006, 5.996, 16, 13.026, 10.004,
                0, 13.026, 0, 16, 16, 16));
        all.add(piece("metal_scaffolding", "construction_crate").paintable().shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("porta_potty", "construction_crate").paintable().large(2, -0.125, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("cement_mixer", "construction_crate").paintable().large(1.005, 0, 0).shape(
                15.035, 0, 0, 16, 6.03, 1.97,
                15.035, 0, 14.03, 16, 6.03, 16,
                0, 11.055, 6.995, 16, 13.065, 9.005,
                0.965, 13.4604, 0, 15.035, 16, 16));
        all.add(piece("forklift", "construction_crate").paintable().large(2, 0, 0).shape(
                0, 2, 0, 16, 14, 16,
                0, 14, 0, 16, 16, 4,
                0, 14, 13, 16, 16, 16));
        all.add(piece("excavator", "construction_crate").paintable().large(2, -11, 0).shape(
                10, 0, 0, 16, 16, 16,
                0, 10, 8, 10, 16, 16));
        all.add(piece("construction_crane", "construction_crate").paintable().large(2, -8, 0).shape(
                4, 0, 4, 12, 3, 12,
                4.5, 3, 4.5, 11.5, 8, 11.5,
                5.5, 3, 5.5, 10.5, 16, 10.5,
                4, 8, 4, 12, 9, 12));
    }

    /** The Easter Crate's furniture. */
    private static void easterCrate(List<Furniture> all) {
        all.add(piece("spring_balloons", "easter_crate").paintable().large(1, -15.75, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("mother_goose", "easter_crate").paintable().large(2, -0.4823, 0).shape(
                0, 0.1647, 0, 16, 16, 16));
        all.add(piece("froggy_bean_bag", "easter_crate").paintable().shape(
                0, 0, 0, 16, 7, 16));
    }

    /** The Fast Food Crate's furniture. */
    private static void fastFoodCrate(List<Furniture> all) {
        all.add(piece("condiment_caddy", "fast_food_crate").paintable().shape(
                0, 0, 0, 16, 8, 16,
                0, 8, 0, 8, 12, 16,
                8, 8, 4, 16, 12, 16,
                4, 12, 4, 12, 16, 12,
                0, 12, 8, 4, 16, 16,
                4, 12, 12, 8, 16, 16));
        all.add(piece("burger_tray", "fast_food_crate").paintable().shape(
                0, 0, 0, 16, 8, 16,
                4, 8, 0, 16, 12, 12));
        all.add(piece("cone_light", "fast_food_crate").paintable().large(1, -8.4, 0).shape(
                5.05, 0, 5.05, 10.95, 3.9, 10.95,
                4, 2.4, 4, 12, 7.4, 12,
                5.5, 7.4, 5.5, 10.5, 13.4, 10.5));
        all.add(piece("restaurant_table", "fast_food_crate").paintable().shape(
                0, 0, 0, 16, 2, 16,
                6.5, 2, 6.5, 9.5, 13, 9.5,
                0, 13, 0, 16, 16, 16));
        all.add(piece("hotdog_tray", "fast_food_crate").paintable().shape(
                7.9476, 0.1519, 2, 16, 4.9955, 13,
                0, 0.1519, 3, 8.0524, 4.9955, 14,
                10.75, 1.6, 1, 13.75, 4.6, 14,
                2.25, 1.6, 2, 5.25, 4.6, 15));
        all.add(piece("donut_box", "fast_food_crate").paintable().shape(
                0, 0, 0, 16, 8, 16,
                0, 8, 8, 16, 12, 16));
        all.add(piece("restaurant_booth", "fast_food_crate").paintable().shape(
                0, 0, 2, 16, 10, 16,
                0, 8, 15, 16, 16, 16));
        all.add(piece("vending_machine", "fast_food_crate").paintable().large(1, -16, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("burger_signage", "fast_food_crate").paintable().large(2, -11, 0).shape(
                2, 0, 2, 14, 2, 14,
                6, 0, 6, 10, 16, 10,
                0, 10, 0, 16, 16, 16));
        all.add(piece("hotdog_signage", "fast_food_crate").paintable().large(2, 0, 0).shape(
                2, 0, 2, 14, 2, 14,
                6, 0, 6, 10, 16, 10,
                0, 6.6683, 0, 16, 16, 13.8985,
                0, 12.3431, 0, 16, 16, 16));
        all.add(piece("donut_signage", "fast_food_crate").paintable().large(2, -8, 0).shape(
                2, 0, 2, 14, 2, 14,
                6, 2, 6, 10, 16, 10,
                0, 5.1409, 0, 16, 16, 16));
    }

    /** The Festive Crate's furniture. */
    private static void festiveCrate(List<Furniture> all) {
        all.add(piece("festive_cutout", "festive_crate").paintable().large(2, -4.4625, 0).shape(
                0, 6.5313, 7.1, 8.1871, 16, 9.1,
                9.7316, 6.6089, 5.85, 16, 16, 7.85,
                6.0326, 13.8718, 6, 13.8107, 16, 8,
                12, 13.925, 5.75, 16, 16, 7.75));
        all.add(piece("santa_sleigh", "festive_crate").paintable().large(1.1, 0, 0).shape(
                0, 7.7, 0, 16, 16, 16));
        all.add(piece("christmas_tree", "festive_crate").paintable().large(1.195, -14, 0).shape(
                6.2075, 1.195, 6.2075, 9.7925, 16, 9.7925,
                0, 6.85, 0, 16, 16, 16));
    }

    /** The Fun Fair Crate's furniture. */
    private static void funFairCrate(List<Furniture> all) {
        all.add(piece("cotton_candy_tray", "fun_fair_crate").paintable().shape(
                0, 0.4567, 2.2039, 16, 6, 4.5,
                0, 0.4567, 11.5, 16, 6, 13.7961,
                0, 6, 2.5, 16, 8, 13.5,
                1.5, 6.5941, 6.103, 2.5, 11.5962, 8.9403,
                7.5, 6.5941, 6.103, 8.5, 11.5962, 8.9403,
                13.5, 6.5941, 6.103, 14.5, 11.5962, 8.9403,
                0, 10.1868, 5.9073, 4.7, 16, 13.3455,
                5.3, 10.1868, 5.9073, 10.7, 16, 13.3455,
                11.3, 10.1868, 5.9073, 16, 16, 13.3455));
        all.add(piece("circus_podium", "fun_fair_crate").paintable().shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("popcorn_bucket", "fun_fair_crate").paintable().shape(
                4, 0, 4, 12, 16, 12,
                4, 8, 0, 12, 12, 4,
                0, 8, 4, 4, 12, 12,
                12, 8, 4, 16, 12, 16,
                4, 8, 12, 12, 12, 16));
        all.add(piece("food_stall", "fun_fair_crate").paintable().large(1, -15, 0).shape(
                0, 0.0402, 6.5, 1.3425, 4.8837, 9.5,
                0, 4, 0, 16, 16, 16));
        all.add(piece("bumper_car", "fun_fair_crate").paintable().large(2, -0.005, 0).shape(
                0, 0, 0, 16, 4.02, 16,
                0, 4, 6.99, 16, 16, 13.01));
        all.add(piece("balloon_bundle", "fun_fair_crate").paintable().large(1, -15, 0).shape(
                7.555, 0.9599, 0, 8.445, 16, 7.4751,
                7.555, 0.9599, 8.5249, 8.445, 16, 16));
        all.add(piece("high_striker", "fun_fair_crate").paintable().large(1, -16, 0).shape(
                0.49, 0, 0, 15.51, 2.02, 10.01,
                13.0101, 0.8, 0, 16, 3.2, 3.5965,
                7.6361, 1, 0, 16, 3, 3.3137,
                6.5, 2, 0, 9.5, 9, 2.5,
                0.5, 2, 7, 15.5, 16, 10,
                5.49, 6.99, 0, 10.51, 9.01, 3.51));
        all.add(piece("circus_tent", "fun_fair_crate").paintable().large(2, -10.5, 0).shape(
                0, 6.05, 0, 16, 16, 16));
    }

    /** The Garden Crate's furniture. */
    private static void gardenCrate(List<Furniture> all) {
        all.add(piece("garden_bench", "garden_crate").paintable().seat(11.75).large(1, -1.75, 0).shape(
                0, 9.2803, 13.6501, 1, 16, 16,
                15, 9.2803, 13.6501, 16, 16, 16,
                0, 9.75, 2, 16, 11.75, 14,
                0, 13.3142, 14.3024, 16, 16, 16));
        all.add(piece("garden_chair", "garden_crate").paintable().seat(10).shape(
                0.0454, 0, 2, 5.3373, 9.0021, 4,
                10.6627, 0, 2, 15.9546, 9.0021, 4,
                0.0454, 0, 12, 5.3373, 9.0021, 14,
                10.6627, 0, 12, 15.9546, 9.0021, 14,
                1, 8, 1, 15, 10, 15,
                0, 9.9043, 14.781, 16, 16, 16,
                0, 10.0957, 14.319, 16, 16, 16));
        all.add(piece("garden_table", "garden_crate").paintable().shape(
                0, 3.8322, 6.5, 2.9347, 16, 9.5,
                13.0653, 3.8322, 6.5, 16, 16, 9.5,
                6.5, 3.8323, 0, 9.5, 16, 2.9347,
                6.5, 3.8323, 13.0653, 9.5, 16, 16,
                0, 10.5421, 0, 16, 14.5421, 16));
        all.add(piece("garden_tools", "garden_crate").paintable().shape(
                5.9, 0.2871, 2.174, 8.1, 16, 12.3194,
                1.5, 0.35, 6, 3.5, 2.35, 11,
                12.5, 0.35, 6, 14.5, 2.35, 11,
                0, 0.35, 11, 16, 2.35, 13,
                14, 2.425, 8.5, 16, 6.425, 9.5,
                0, 4.25, 8.9, 1.35, 16, 10.1,
                13.25, 15.325, 6, 14.25, 16, 11,
                0, 15.35, 11, 16, 16, 13));
        all.add(piece("tall_hedge", "garden_crate").large(2, -1, 0).shape(
                0, 0, 2.3431, 9.6569, 16, 13.6569,
                6.3431, 2, 2.3431, 16, 16, 13.6569,
                2.3431, 3, 0, 13.6569, 16, 8.6569,
                6.3431, 5, 1.3431, 16, 16, 12.6569,
                2.3431, 6, 7.3431, 13.6569, 16, 16));
        all.add(piece("yard_gnome", "garden_crate").paintable().shape(
                4, 0, 5.5, 12, 2, 10.5,
                5, 2, 7, 11, 4, 10,
                3.5862, 2.5849, 5.5, 5.0004, 3.9991, 11.5,
                10.9996, 2.5849, 5.5, 12.4138, 3.9991, 11.5,
                4.9, 3.9, 5.4, 11.1, 16, 11.6,
                1.3355, 3.9064, 7.4, 5.7406, 10.4763, 9.6,
                10.2594, 3.9064, 7.4, 14.6645, 10.4763, 9.6,
                7, 11, 3.5, 9, 13, 5.5,
                3, 12, 7.5, 5, 15, 8.5,
                11, 12, 7.5, 13, 15, 8.5));
        all.add(piece("young_tree", "garden_crate").large(2, -16, 0).shape(
                8, 0, 6, 16, 16, 14,
                4, 0, 8, 8, 4, 12));
        all.add(piece("hydrangea_bush", "garden_crate").paintable().large(1.785, -5.7746, 0).shape(
                0, 9.2367, 0, 16, 16, 16));
        all.add(piece("ivy_trellis", "garden_crate").large(2, -1, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("push_mower", "garden_crate").paintable().shape(
                0, 2, 0, 16, 6, 15,
                0, 4.5858, 14, 1, 16, 16,
                15, 4.5858, 14, 16, 16, 16,
                3, 6, 1, 13, 12, 11,
                2.55, 6.04, 0.46, 13.45, 12.58, 11.36,
                0, 10, 9.5132, 3.8586, 13, 16));
        all.add(piece("ivy_pergola", "garden_crate").large(1.995, -0.5, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("garden_fountain", "garden_crate").paintable().large(2, -16, 0).shape(
                0, 4, 0, 16, 16, 16));
    }

    /** The Gothic Crate's furniture. */
    private static void gothicCrate(List<Furniture> all) {
        all.add(piece("coffin", "gothic_crate").paintable().shape(
                1, 0, 0, 15, 9, 15,
                0.4721, 8.7505, 0, 16, 16, 16));
        all.add(piece("gargoyle", "gothic_crate").paintable().shape(
                0, 0, 0, 16, 8, 16,
                1.5, 5.5699, 0.3635, 4.5, 8.5657, 3.9005,
                11.5, 5.5699, 0.3635, 14.5, 8.5657, 3.9005,
                0, 7, 6.7305, 2.4166, 16, 15.2695,
                13.5834, 7, 6.7305, 16, 16, 15.2695,
                1.5, 7.4176, 1.1288, 4.5, 16, 7.7273,
                11.5, 7.4176, 1.1288, 14.5, 16, 7.7273,
                1, 8, 7, 15, 16, 15,
                0, 11.6612, 12, 2.7678, 16, 13.1,
                13.2322, 11.6612, 12, 16, 16, 13));
        all.add(piece("gothic_chandelier", "gothic_crate").paintable().large(2, -3.5, 0).shape(
                7.5, 0, 7.5, 8.5, 7, 8.5,
                6.5, 7, 6.5, 9.5, 16, 9.5));
        all.add(piece("skeleton_hand_chair", "gothic_crate").seat(8).large(1.005, -4, 0).shape(
                2.975, 0, 5.99, 6.995, 12.06, 10.01,
                9.005, 0, 5.99, 13.025, 12.06, 10.01,
                0, 11.055, 0, 16, 16, 16));
        all.add(piece("caged_skeleton", "gothic_crate").large(2, 0, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("graveyard_shrine", "gothic_crate").paintable().large(1.995, -3, 0).shape(
                0, 0, 0, 16, 15.96, 16));
        all.add(piece("organ", "gothic_crate").paintable().large(2, -16, 0).shape(
                2, 0, 2, 6, 2, 8,
                10, 0, 2, 14, 2, 8,
                0, 0, 8, 16, 16, 16,
                0, 13, 0, 16, 16, 8));
        all.add(piece("gothic_throne", "gothic_crate").paintable().seat(8).large(2, -16, 0).shape(
                0, 10, 0, 16, 16, 16));
    }

    /** The Harvest Crate's furniture. */
    private static void harvestCrate(List<Furniture> all) {
        all.add(piece("autumn_scarecrow", "harvest_crate").paintable().large(1.125, -16, 0).shape(
                6.875, 0, 6.875, 9.125, 16, 9.125,
                0, 6.1546, 6.3125, 7.8401, 16, 9.6875,
                8.1599, 6.1546, 6.3125, 16, 16, 9.6875));
        all.add(piece("feast_table", "harvest_crate").paintable().large(2, 0, 0).shape(
                0, 5.8, 0, 16, 16, 16));
        all.add(piece("hay_wagon", "harvest_crate").paintable().large(1.995, 0, 0).shape(
                0, 10.9725, 0, 16, 15.96, 16));
    }

    /** The Kitchen Crate's furniture. */
    private static void kitchenCrate(List<Furniture> all) {
        all.add(piece("kitchen_stool", "kitchen_crate").paintable().seat(12).shape(
                2, 0, 1.8689, 4, 10, 3.8689,
                12, 0, 1.8689, 14, 10, 3.8689,
                2, 0, 12.1311, 4, 10, 14.1311,
                12, 0, 12.1311, 14, 10, 14.1311,
                1, 10, 0.8689, 15, 12, 14.8689,
                0.99, 10.4488, 12.2755, 15.01, 16, 16));
        all.add(piece("kitchen_cabinet", "kitchen_crate").paintable().shape(
                0, 0, 2, 16, 14, 16,
                6, 3.5, 1, 7, 5.5, 2,
                9, 3.5, 1, 10, 5.5, 2,
                7, 12, 1, 9, 13, 2,
                0, 14, 0, 16, 16, 16));
        all.add(piece("kitchen_sink", "kitchen_crate").paintable().shape(
                14, 0, 1, 16, 14, 16,
                0, 0, 2, 14, 14, 16,
                1, 6.75, 1, 3, 8.75, 2,
                0, 12, 4, 8, 16, 12,
                0, 14, 0, 16, 16, 16));
        all.add(piece("kitchen_shelf", "kitchen_crate").paintable().large(1, -8, 0).shape(
                0, 4, 0, 16, 12, 16,
                0, 12, 0, 16, 16, 12,
                0, 12, 12, 4, 16, 16,
                12, 12, 12, 16, 16, 16));
        all.add(piece("kitchen_island", "kitchen_crate").paintable().large(2, 0.4375, 0.4375).shape(
                0, 0.4375, 12.75, 16, 16, 16,
                0, 14.4375, 1.75, 16, 16, 12.75));
    }

    /** The Lovely Crate's furniture. */
    private static void lovelyCrate(List<Furniture> all) {
        all.add(piece("elegant_chandelier", "lovely_crate").large(2, -12, 0).shape(
                1.3943, 0, 1.5407, 4.9298, 16, 5.0763,
                11.0702, 0, 10.9237, 14.6057, 16, 14.4593,
                11.0702, 2.5, 1.5407, 14.6057, 16, 5.0763,
                1.3943, 2.5, 10.9237, 4.9298, 16, 14.4593));
        all.add(piece("elegant_swing", "lovely_crate").paintable().large(2, -12, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("elegant_gazebo", "lovely_crate").paintable().large(2, -16, 0).shape(
                0, 0, 0, 16, 16, 16));
    }

    /** The Lunar Crate's furniture. */
    private static void lunarCrate(List<Furniture> all) {
        all.add(piece("lunar_shrine", "lunar_crate").paintable().large(2, -5.75, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("floating_lantern", "lunar_crate").paintable().large(1, -14.675, 0).shape(
                4, 14, 4, 12, 16, 12));
    }

    /** The Medieval Crate's furniture. */
    private static void medievalCrate(List<Furniture> all) {
        all.add(piece("medieval_chair", "medieval_crate").paintable().seat(11).shape(
                0, 0, 1, 2, 15, 3,
                14, 0, 1, 16, 15, 3,
                0, 0, 13, 2, 16, 15,
                14, 0, 13, 16, 16, 15,
                2, 7, 1, 14, 11, 15,
                0, 15, 0, 2.5, 16, 3,
                13.5, 15, 0, 16, 16, 3));
        all.add(piece("medieval_table", "medieval_crate").shape(
                6.5, 1, 0, 9.5, 4, 16,
                0, 13, 0, 16, 16, 16));
        all.add(piece("royal_banner", "medieval_crate").paintable().large(1, -16, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("medieval_candelabra", "medieval_crate").large(1, -6, 0).shape(
                4, 0, 4, 12, 2, 12,
                6, 2, 6, 10, 6, 10,
                7, 6, 7, 9, 16, 9));
        all.add(piece("armor_display", "medieval_crate").paintable().large(1, -16, 0).shape(
                0, 0, 2, 16, 4, 14,
                4, 4, 6, 12, 16, 10));
        all.add(piece("dungeon_cage", "medieval_crate").large(1, -16, 0).shape(
                0, 0, 0, 1, 16, 1,
                15, 0, 0, 16, 16, 1,
                0, 0, 15, 1, 16, 16,
                15, 0, 15, 16, 16, 16,
                1, 3, 0, 15, 6, 1,
                0, 3, 1, 1, 6, 15,
                15, 3, 1, 16, 6, 15,
                1, 3, 15, 15, 6, 16));
        all.add(piece("royal_throne", "medieval_crate").paintable().seat(9.99).large(2, -16, 0).shape(
                0, 4.01, 0.01, 16, 9.99, 15.99,
                0, 10, 0, 16, 16, 16));
    }

    /** The Mining Crate's furniture. */
    private static void miningCrate(List<Furniture> all) {
        all.add(piece("detonator", "mining_crate").paintable().shape(
                1, 0, 11.5, 15, 16, 15.5,
                1, 0.5, 1, 15, 16, 15));
        all.add(piece("metal_minecart", "mining_crate").paintable().shape(
                0, 3, 0, 16, 16, 16));
        all.add(piece("ore_pile", "mining_crate").paintable().shape(
                13.3065, 0, 13.8883, 16, 6, 16,
                7, 1.6718, 0, 13, 9.5112, 2.1501,
                3, 1.6718, 9.8499, 9, 9.5112, 16,
                0, 1.75, 0, 10.0711, 10.75, 9.0711,
                5.9289, 1.75, 4.9289, 16, 10.75, 16,
                0, 2, 13.3869, 3.4609, 8, 16,
                11.6585, 2.1791, 0, 16, 14.3209, 8,
                0, 2.1791, 6, 4.3415, 14.3209, 16,
                0, 6.75, 0, 12, 16, 12,
                10.3768, 10.8, 6.6119, 15.6031, 14.8, 11.8382));
        all.add(piece("crystal_deposit", "mining_crate").paintable().shape(
                9, 0, 1, 16, 5, 8,
                0, 0, 4, 10, 7, 14,
                15.0251, 2.5251, 2.5, 16, 7.4749, 4.5,
                5.5, 3.0251, 0, 7.5, 7.9749, 4.7249,
                2.5, 3.0251, 13.2751, 4.5, 7.9749, 16,
                8, 3.109, 0, 15, 16, 3.8115,
                11.9385, 4.359, 2.25, 16, 16, 9.25,
                0, 5.609, 4.25, 4.0615, 16, 11.25,
                1, 5.8754, 9.3491, 10, 16, 16,
                7.2779, 6.3425, 5.5, 10.7221, 14.6575, 10.5));
        all.add(piece("scaffolding_beam", "mining_crate").large(2, -16, 0).shape(
                0, 0, 0, 2, 16, 2,
                14, 0, 0, 16, 16, 2,
                0, 0, 14, 2, 16, 16,
                14, 0, 14, 16, 16, 16,
                15, 5, 2, 16, 8, 14,
                0, 5.9792, 0, 16, 16, 1,
                0, 5.9792, 15, 16, 16, 16));
        all.add(piece("scaffolding_archway", "mining_crate").large(2, 0, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("geyser", "mining_crate").paintable().large(2, -8.5265, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("large_brazier", "mining_crate").large(2, -13.97, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("drill_excavator", "mining_crate").paintable().large(2, -8.47, 0).shape(
                0, 7.7, 7.23, 16, 12.32, 16,
                1.84, 12.32, 13.39, 14.16, 15.4, 16,
                0, 14.63, 4.15, 16, 16, 16));
    }

    /** The Muerte Crate's furniture. */
    private static void muerteCrate(List<Furniture> all) {
        all.add(piece("potted_marigolds", "muerte_crate").paintable().large(1, -7, 0).shape(
                2, 0, 2, 14, 7, 14,
                0, 7, 0, 16, 16, 16));
        all.add(piece("marigold_arch", "muerte_crate").paintable().large(2, -5.5, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("sugar_skeleton", "muerte_crate").paintable().large(1, -11.75, 0).shape(
                8.5, 0.6058, 1.4326, 11.5, 16, 10.7098,
                4.5, 0.6058, 6.7902, 7.5, 16, 16,
                2.5, 15.425, 4.5, 13.5, 16, 11.5));
        all.add(piece("marigold_altar", "muerte_crate").paintable().large(2, -0.05, 0).shape(
                0, 3.1, 0, 16, 16, 16));
    }

    /** The Music Crate's furniture. */
    private static void musicCrate(List<Furniture> all) {
        all.add(piece("boombox", "music_crate").paintable().shape(
                0, 0, 3, 16, 16, 13));
        all.add(piece("keyboard_piano", "music_crate").paintable().shape(
                0, 1.1077, 5.99, 16, 16, 10.01,
                0, 15.075, 2.4725, 16, 16, 13.5275));
        all.add(piece("drumset", "music_crate").paintable().large(2, -8, 0).shape(
                2.05, 0, 0, 16, 15, 3.25,
                0, 0, 3.2396, 6.5604, 12, 15.2604,
                8.05, 0, 3.25, 11.05, 3, 10.641,
                1.5396, 0, 15.2396, 13.5604, 8, 16,
                9.05, 3, 3.35, 10.05, 6.6955, 4.8807,
                13.55, 5, 4.25, 16, 16, 14.25,
                8.55, 6.0439, 3.7952, 10.55, 8.657, 6.4083,
                0, 10, 4.25, 5.55, 16, 14.25));
        all.add(piece("dj_booth", "music_crate").paintable().large(2, 0, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("grand_piano", "music_crate").paintable().large(2, 0, 0).shape(
                0, 12, 0, 16, 16, 16));
        all.add(piece("concert_stage", "music_crate").paintable().large(1, -16, 0).shape(
                0, 0, 0, 16, 8, 16));
    }

    /** The Palace Crate's furniture. */
    private static void palaceCrate(List<Furniture> all) {
        all.add(piece("stone_lion", "palace_crate").large(2, 0.25, 0.25).shape(
                0, 0.25, 0, 16, 16, 16));
        all.add(piece("lantern_lamppost", "palace_crate").paintable().large(1.5, -5.8802, 0).shape(
                7.25, 3.6457, 4.8558, 8.75, 11.6006, 16,
                0.3557, 3.6457, 11.75, 15.6443, 11.6006, 13.25,
                7.25, 4.3203, 11.75, 8.75, 16, 13.25));
        all.add(piece("potted_cherry_blossom", "palace_crate").paintable().large(2, -15.6, 0).shape(
                0, 0, 0, 16, 12.4, 16,
                6.9, 12.2, 7.15, 8.9, 16, 9.15));
        all.add(piece("huangshan_pine", "palace_crate").large(2, -9, 0).shape(
                0.5, 0, 2, 3.5, 3, 5,
                0, 0, 5, 5, 16, 11,
                5, 0, 6.5, 8, 3, 9.5,
                0.5, 0, 11, 3.5, 3, 14));
        all.add(piece("lunar_archway", "palace_crate").paintable().large(2, -16, 0).shape(
                0, 0, 0, 16, 16, 16));
    }

    /** The Paradise Crate's furniture. */
    private static void paradiseCrate(List<Furniture> all) {
        all.add(piece("thatch_umbrella", "paradise_crate").large(2, 0, 0).shape(
                7, 0, 7, 9, 16, 9));
        all.add(piece("surfboard_rack", "paradise_crate").paintable().large(2, -5, 0).shape(
                0, 1.0512, 0, 6.5, 16, 12.4856,
                9.5, 1.0512, 0, 16, 16, 12.4856));
        all.add(piece("tiki_bar", "paradise_crate").paintable().large(2, -2, 0).shape(
                0, 2, 0, 16, 16, 16));
        all.add(piece("large_tiki_totem", "paradise_crate").paintable().large(2, -16, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("tiki_gazebo", "paradise_crate").paintable().large(2, -16, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("sailboat", "paradise_crate").paintable().shape(
                0, 0, 0, 16, 12, 16,
                4, 12, 0, 12, 16, 4,
                4, 12, 8, 16, 16, 12,
                8, 12, 12, 16, 16, 16));
    }

    /** The Shamrock Crate's furniture. */
    private static void shamrockCrate(List<Furniture> all) {
        all.add(piece("clover_balloons", "shamrock_crate").paintable().large(1, -15.75, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("giant_clover", "shamrock_crate").paintable().large(2, -4.625, 0).shape(
                6.5, 2.25, 6.5, 9.5, 16, 9.5,
                0, 5.4106, 5.25, 4.4836, 8.0894, 12.25,
                9.5164, 5.4106, 9.75, 15.9836, 8.0894, 16,
                5.25, 7.1606, 0, 12.25, 9.8394, 4.9836));
        all.add(piece("giant_rainbow", "shamrock_crate").paintable().shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("fairy_tree", "shamrock_crate").paintable().large(2, -16, 0).shape(
                0, 0, 0, 16, 16, 16));
    }

    /** The Space Crate's furniture. */
    private static void spaceCrate(List<Furniture> all) {
        all.add(piece("control_panel", "space_crate").paintable().shape(
                0, 0, 1, 15, 14, 16,
                0, 9.9474, 1.584, 14.99, 16, 13.2512,
                0, 13.0761, 0, 16, 16, 11.4692,
                0, 14, 11, 15, 16, 16));
        all.add(piece("metal_crate", "space_crate").paintable().shape(
                1, 0.01, 0.005, 15, 16, 15.985,
                14.99, 1.01, 1.01, 16, 15.01, 15.01,
                0, 1.02, 1.02, 1, 15, 15,
                0, 6.71, 0, 4.0827, 10.71, 2.6027,
                11.9173, 6.71, 0, 16, 10.71, 2.6027,
                0, 6.71, 13.3973, 4.0827, 10.71, 16,
                11.9173, 6.71, 13.3973, 16, 10.71, 16));
        all.add(piece("solar_panel", "space_crate").large(2, 0.01, 0.01).shape(
                0, 11.1013, 0, 16, 16, 16));
        all.add(piece("tube_light", "space_crate").paintable().large(1, -4.8149, 0).shape(
                8.9993, 1.7574, 7, 14.6561, 7.4142, 9,
                7, 1.7574, 8.9993, 9, 7.4142, 14.6561,
                7, 1.7581, 1.3432, 9, 7.4149, 7,
                1.3432, 1.7581, 7, 7, 7.4149, 9,
                6, 5.4149, 6, 10, 6.4149, 10,
                7, 6.4149, 7, 9, 11.4149, 9,
                6, 11.4149, 6, 10, 16, 10));
        all.add(piece("telescope", "space_crate").paintable().shape(
                7.5, 1.3668, 7.3472, 8.5, 16, 16,
                0, 1.3668, 7.5, 8.6528, 16, 8.5,
                7.5, 1.3715, 0, 8.5, 16, 8.6537,
                7.3463, 1.3715, 7.5, 16, 16, 8.5));
        all.add(piece("cryotank", "space_crate").paintable().large(2, -7, 0).shape(
                0, 0, 0, 16, 14, 16,
                0, 14.02, 0, 16, 16, 16));
        all.add(piece("space_buggy", "space_crate").paintable().large(2, 0, 0).shape(
                0, 3, 0, 16, 6, 16,
                5, 6, 1, 11, 15, 6,
                0, 6, 8, 7, 12, 16,
                9, 6, 8, 16, 12, 16,
                1.4645, 11, 1.4645, 5, 16, 5,
                11, 11, 1.4645, 14.5355, 16, 5,
                3, 12.3212, 0, 13, 16, 7));
    }

    /** The Spooky Crate's furniture. */
    private static void spookyCrate(List<Furniture> all) {
        all.add(piece("witch_cauldron", "spooky_crate").paintable().large(1, -16, 0).shape(
                0, 0, 0, 1, 2, 1,
                15, 0, 0, 16, 2, 1,
                0, 0, 15, 1, 2, 16,
                15, 0, 15, 16, 2, 16,
                0, 2, 0, 16, 13, 16,
                1.1716, 14, 1.7574, 6.8284, 16, 7.4142,
                10.364, 14, 10.9497, 16, 16, 16));
        all.add(piece("charlotte", "spooky_crate").paintable().large(2, -7.6894, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("halloween_balloons", "spooky_crate").paintable().large(1, -15, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("summoning_circle", "spooky_crate").paintable().shape(
                0, 0, 0, 16, 16, 16));
    }

    /** The Spring Crate's furniture. */
    private static void springCrate(List<Furniture> all) {
        all.add(piece("flower_arch", "spring_crate").paintable().large(1.005, -16, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("spring_swing", "spring_crate").paintable().large(2, -3.2228, 0).shape(
                0, 12.7246, 0, 16, 16, 13.8275));
        all.add(piece("flower_pinwheel", "spring_crate").paintable().large(2, -10.6, 0).shape(
                6.5, 0, 6.5433, 9.5, 16, 9.5433,
                9.0055, 0.1184, 3.5, 16, 7.8966, 12.5,
                0, 0.1185, 3.5, 6.5482, 7.8967, 12.5));
        all.add(piece("giant_flower", "spring_crate").paintable().large(2, -8, 0).shape(
                3.5, 0, 0, 12.5, 9.1924, 6.5,
                0, 0, 4.5, 6.5, 7.0711, 11.5,
                9.5, 0, 4.5, 16, 7.0711, 11.5,
                6.5, 0, 6.5, 9.5, 16, 9.5,
                3.5, 0, 9.5, 12.5, 9.1924, 16));
        all.add(piece("flower_wagon", "spring_crate").paintable().large(1.995, -5, 0).shape(
                0, 9.975, 0, 16, 16, 16));
    }

    /** The Toy Crate's furniture. */
    private static void toyCrate(List<Furniture> all) {
        all.add(piece("train_engine", "toy_crate").paintable().large(2, -5, 0).shape(
                0, 1, 0, 1, 6, 5,
                15, 1, 0, 16, 6, 5,
                0, 6, 0, 16, 10, 16,
                2, 10, 0, 14, 16, 9,
                0, 10, 9, 16, 16, 16));
        all.add(piece("gingerbread_house", "toy_crate").large(1, -16, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("train_cart", "toy_crate").paintable().large(2, -4.5, 0).shape(
                0, 5, 0, 16, 16, 16));
        all.add(piece("reindeer_rocker", "toy_crate").paintable().large(1, -12, 0).shape(
                2, 0, 0, 7, 2, 16,
                9, 0, 0, 14, 2, 16,
                3, 1.8822, 0, 6, 12.269, 3.1624,
                10, 1.8822, 0, 13, 12.269, 3.1624,
                3, 1.8822, 12.8376, 6, 12.269, 16,
                10, 1.8822, 12.8376, 13, 12.269, 16,
                2.5, 11, 0, 13.5, 16, 16,
                2, 12.5, 3, 14, 16, 13));
        all.add(piece("nutcracker", "toy_crate").paintable().large(1, -12, 0).shape(
                5, 0, 6.5, 11, 14, 9.5,
                1, 10, 6, 4, 16, 10,
                12, 10, 6, 15, 16, 10,
                4, 14, 6, 12, 16, 10));
        all.add(piece("train_set", "toy_crate").paintable().shape(
                0, 2, 0, 1.0078, 8, 0.8865));
        all.add(piece("jingle_inflatable", "toy_crate").paintable().large(2, -4.5, 0).shape(
                2, 0, 4, 14, 5, 12,
                3, 5, 5, 13, 16, 11));
        all.add(piece("festive_balloons", "toy_crate").paintable().large(1, -12.75, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("santa_chair", "toy_crate").paintable().seat(12).large(2, -4.5, 0).shape(
                0, 3, 11, 16, 7, 15,
                0, 7, 0, 16, 12, 11,
                0, 12, 10, 16, 16, 15));
    }

    /** The Western Crate's furniture. */
    private static void westernCrate(List<Furniture> all) {
        all.add(piece("flat_cactus", "western_crate").paintable().shape(
                0, 0, 0, 12, 16, 16,
                12, 0, 4, 16, 16, 16));
        all.add(piece("saloon_chair", "western_crate").paintable().seat(10).shape(
                1.3531, 0, 2.268, 3.3531, 8, 4.268,
                12.6469, 0, 2.268, 14.6469, 8, 4.268,
                1.3531, 0, 11.732, 3.3531, 8, 13.732,
                12.6469, 0, 11.732, 14.6469, 8, 13.732,
                1.354, 8, 1.5609, 15.354, 10, 14.5609));
        all.add(piece("saloon_table", "western_crate").shape(
                5, 0, 0, 11, 4, 16,
                0, 0, 5, 16, 4, 11,
                6, 4, 0, 10, 12, 16,
                0, 4, 6, 16, 12, 10,
                0, 12, 0, 16, 15, 16));
        all.add(piece("saloon_sign", "western_crate").paintable().large(2, -0.01, 0).shape(
                0, 0.02, 6, 16, 5.02, 10,
                0, 2, 5.98, 16, 4.04, 10.02));
        all.add(piece("saloon_bench", "western_crate").seat(9).shape(
                0, 6, 2, 16, 9, 15,
                0, 13.7315, 15.3363, 16, 16, 16));
        all.add(piece("covered_wagon", "western_crate").paintable().large(2, -0.5, 0).shape(
                0, 12, 0, 16, 16, 16));
        all.add(piece("water_tower", "western_crate").large(2, -13, 0).shape(
                5, 0, 0, 11, 16, 3.616,
                0, 0, 5, 3.616, 16, 11,
                12.384, 0, 5, 16, 16, 11,
                5, 0, 12.384, 11, 16, 16,
                0, 5, 0, 5.5251, 8, 5.5251,
                10.4749, 5, 0, 16, 8, 5.5251,
                0, 5, 10.4749, 5.5251, 8, 16,
                10.4749, 5, 10.4749, 16, 8, 16,
                0, 11, 0, 6.9393, 14, 6.9393));
    }

    /** The Winter Crate's furniture. */
    private static void winterCrate(List<Furniture> all) {
        all.add(piece("frost_table", "winter_crate").shape(
                0.9289, 0, 0.9289, 15.0711, 16, 15.0711,
                7, 15.275, 0, 9, 16, 16,
                0, 15.275, 7, 16, 16, 9));
    }

    /** The Wizardry Crate's furniture. */
    private static void wizardryCrate(List<Furniture> all) {
        all.add(piece("potted_glowshroom", "wizardry_crate").paintable().shape(
                2.3431, 0, 2.3431, 13.6569, 4, 13.6569,
                8.75, 1.7046, 6.8598, 10.75, 13.5566, 13.2998,
                5.25, 2.4002, 2.7002, 7.25, 10.5566, 7.6095,
                2.25, 9.75, 0, 10.25, 11.75, 7,
                4.25, 11.75, 1, 8.25, 15.75, 5,
                5.75, 12.75, 9, 13.75, 14.75, 16,
                7.75, 14.75, 11, 11.75, 16, 15));
        all.add(piece("magic_book_stack", "wizardry_crate").paintable().shape(
                0, 0.075, 0.4708, 16, 4.075, 15.9911,
                1.025, 4.25, 3.75, 15.025, 8.25, 14.75,
                0, 7.25, 5.8488, 4.4354, 14.25, 10.1512,
                1.025, 8.275, 0.825, 15.025, 12.275, 11.825,
                0, 12.325, 0.4708, 16, 16, 15.9911));
        all.add(piece("magic_candles", "wizardry_crate").shape(
                0.8826, 0, 11.724, 4.0183, 11.4, 14.8598,
                12.4447, 2, 0.4405, 15.5804, 13.4, 3.5763,
                12.8, 4, 13.8, 15.2, 15.4, 16,
                0.8, 8, 0.8, 3.2, 16, 3.2));
        all.add(piece("potion_station", "wizardry_crate").paintable().large(1, -16, 0).shape(
                0, 5, 0, 16, 7, 16,
                10.5253, 6.8277, 0, 15.0519, 14.0602, 6,
                15, 7, 0, 16, 14, 6,
                0, 13.853, 14.7367, 5.5, 16, 16,
                0, 14, 0, 16, 16, 16));
        all.add(piece("runic_statue", "wizardry_crate").paintable().large(1, -12, 0).shape(
                1.1955, 0, 2.5, 5.8806, 5.1924, 5.5,
                10.1194, 0, 2.5, 14.8045, 5.1924, 5.5,
                3, 0, 3, 13, 3, 13,
                1.1955, 0, 10.5, 5.8806, 5.1924, 13.5,
                10.1194, 0, 10.5, 14.8045, 5.1924, 13.5,
                9.1402, 1.7193, 7.3743, 10.6402, 5.5269, 10.0995,
                4.9765, 1.8546, 6.5, 7.7017, 5.6622, 8,
                6.8771, 2.8529, 7.4874, 8.8771, 7.8529, 9.4874,
                4, 11, 6, 12.5, 13.5, 10,
                1, 13.5, 6, 15, 16, 10));
        all.add(piece("magic_cauldron", "wizardry_crate").paintable().large(2, -15.6958, 0).shape(
                0, 10.6472, 0, 3.0824, 16, 2.75,
                12.9176, 10.6472, 0, 16, 16, 2.75,
                0, 10.6472, 13.25, 3.0824, 16, 16,
                12.9176, 10.6472, 13.25, 16, 16, 16,
                0, 10.7128, 0, 3.25, 16, 2.4672,
                12.75, 10.7128, 0, 16, 16, 2.4672,
                0, 10.7128, 13.5328, 3.25, 16, 16,
                12.75, 10.7128, 13.5328, 16, 16, 16));
    }

    /** The Zodiac Crate's furniture. */
    private static void zodiacCrate(List<Furniture> all) {
        all.add(piece("koi_pond", "zodiac_crate").paintable().large(1, -16, 0).shape(
                0, 0, 0, 16, 4, 16));
        all.add(piece("zodiac_tapestry", "zodiac_crate").paintable().large(2, 0, 0).shape(
                0, 0, 0, 16, 16, 16));
        all.add(piece("carp_streamers", "zodiac_crate").paintable().large(1.57, -16, 0).shape(
                10.9889, 0.1574, 7.215, 16, 8.4836, 8.785));
        all.add(piece("lunar_horse_statue", "zodiac_crate").paintable().large(1, -16, 0).shape(
                1.83, 6.7384, 0, 6.67, 16, 7.0373,
                0, 7.6173, 10.6851, 5, 16, 16,
                11, 7.6173, 10.6851, 16, 16, 16,
                2, 10.2692, 0, 14, 16, 16));
    }

    /** ORFurniture's and CosmoLib's furniture. */
    private static void legacy(List<Furniture> all) {
        all.add(piece("sofa_chair", null).paintable().seat(11).shape(
                0, 0, 0, 4, 12, 4,
                12, 0, 0, 16, 12, 4,
                0, 0, 12, 4, 16, 16,
                12, 0, 12, 16, 16, 16,
                4, 4, 0, 12, 12, 16,
                0, 4, 4, 4, 12, 12,
                12, 4, 4, 16, 12, 12,
                0, 12, 8, 16, 16, 12,
                4, 12, 12, 12, 16, 16));
        all.add(piece("iron_table", null).shape(
                0.5754, 0, 0.5754, 15.4246, 9, 15.4246,
                7, 0, 7, 9, 16, 9,
                0, 14, 0, 16, 16, 16));
        all.add(piece("cosmo_rug", null).paintable().defaultPaint(0x547483).noCollision().shape(
                0, 0, 0, 16, 1, 16));
    }
}
