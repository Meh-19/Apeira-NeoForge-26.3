package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.world.feature.BirchTreeFeature;
import com.apeira.main.world.feature.ColdLeafLitterFeature;
import com.apeira.main.world.feature.DampMossPatchFeature;
import com.apeira.main.world.feature.FallenBirchLogFeature;
import com.apeira.main.world.feature.FallenLogFeature;
import com.apeira.main.world.feature.FirBushFeature;
import com.apeira.main.world.feature.FirTreeFeature;
import com.apeira.main.world.feature.FoothillsPondFeature;
import com.apeira.main.world.feature.LeafLitterFeature;
import com.apeira.main.world.feature.MapleBranchingTrunkPlacer;
import com.apeira.main.world.feature.MapleBushFeature;
import com.apeira.main.world.feature.MapleTreeFeature;
import com.apeira.main.world.feature.MycenaPatchFeature;
import com.apeira.main.world.feature.OakBushFeature;
import com.apeira.main.world.feature.OrangeMycenaFeature;
import com.apeira.main.world.feature.PodzolPatchFeature;
import com.apeira.main.world.feature.ShelfFungusClusterFeature;
import com.apeira.main.world.feature.SlateBoulderFeature;
import com.apeira.main.world.feature.StickPatchFeature;
import com.apeira.main.world.feature.StoneMoundFeature;
import com.apeira.main.world.feature.StoneShoreFeature;
import com.apeira.main.world.feature.SurfacePatchFeature;
import com.apeira.main.world.feature.UnderwaterSiltPatchFeature;
import com.apeira.main.world.feature.WaterEdgeCattailFeature;
import com.apeira.main.world.feature.WaterEdgeDiskFeature;
import com.apeira.main.world.feature.YellowBirchTreeFeature;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Xenon's worldgen feature types and trunk placer, with Xenon's ids (classes in {@code world.feature}). In 26.x a
 * feature type is the codec of its {@link Feature} records; the configured features using them are datapack entries
 * written by {@code datagen/MainWorldgen}.
 */
public final class MainFeatures {
    private static final DeferredRegister<MapCodec<? extends Feature>> FEATURES = Apeira.REGISTRAR.forRegistry(Registries.FEATURE_TYPE);

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<OrangeMycenaFeature>> ORANGE_MYCENA =
            FEATURES.register("orange_mycena_feature", () -> OrangeMycenaFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<MycenaPatchFeature>> MYCENA_PATCH =
            FEATURES.register("mycena_patch", () -> MycenaPatchFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<MapleTreeFeature>> MAPLE_TREE =
            FEATURES.register("maple_tree", () -> MapleTreeFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<FirTreeFeature>> FIR_TREE =
            FEATURES.register("fir_tree", () -> FirTreeFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<FirBushFeature>> FIR_BUSH =
            FEATURES.register("fir_bush", () -> FirBushFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<MapleBushFeature>> MAPLE_BUSH =
            FEATURES.register("maple_bush", () -> MapleBushFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<StoneMoundFeature>> STONE_MOUND =
            FEATURES.register("stone_mound", () -> StoneMoundFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<SlateBoulderFeature>> SLATE_BOULDER =
            FEATURES.register("slate_boulder", () -> SlateBoulderFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<FallenLogFeature>> FALLEN_OAK_LOG =
            FEATURES.register("fallen_oak_log", () -> FallenLogFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<WaterEdgeCattailFeature>> WATER_EDGE_CATTAIL =
            FEATURES.register("water_edge_cattail", () -> WaterEdgeCattailFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<WaterEdgeDiskFeature>> WATER_EDGE_SILT =
            FEATURES.register("water_edge_silt", () -> WaterEdgeDiskFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<UnderwaterSiltPatchFeature>> UNDERWATER_SILT_PATCH =
            FEATURES.register("underwater_silt_patch", () -> UnderwaterSiltPatchFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<LeafLitterFeature>> LEAF_LITTER =
            FEATURES.register("leaf_litter", () -> LeafLitterFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<SurfacePatchFeature>> SURFACE_PATCH =
            FEATURES.register("surface_patch", () -> SurfacePatchFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<ShelfFungusClusterFeature>> SHELF_FUNGUS_CLUSTER =
            FEATURES.register("shelf_fungus_cluster", () -> ShelfFungusClusterFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<DampMossPatchFeature>> DAMP_MOSS_PATCH =
            FEATURES.register("damp_moss_patch", () -> DampMossPatchFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<StoneShoreFeature>> STONE_SHORE =
            FEATURES.register("stone_shore", () -> StoneShoreFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<PodzolPatchFeature>> PODZOL_PATCH =
            FEATURES.register("podzol_patch", () -> PodzolPatchFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<BirchTreeFeature>> BIRCH_TREE =
            FEATURES.register("birch_tree", () -> BirchTreeFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<YellowBirchTreeFeature>> YELLOW_BIRCH_TREE =
            FEATURES.register("yellow_birch_tree", () -> YellowBirchTreeFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<FallenBirchLogFeature>> FALLEN_BIRCH_LOG =
            FEATURES.register("fallen_birch_log", () -> FallenBirchLogFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<StickPatchFeature>> STICK_PATCH =
            FEATURES.register("stick_patch", () -> StickPatchFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<OakBushFeature>> OAK_BUSH =
            FEATURES.register("oak_bush", () -> OakBushFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<ColdLeafLitterFeature>> COLD_LEAF_LITTER =
            FEATURES.register("cold_leaf_litter", () -> ColdLeafLitterFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<FoothillsPondFeature>> FOOTHILLS_POND =
            FEATURES.register("foothills_pond", () -> FoothillsPondFeature.CODEC);

    public static final DeferredHolder<TrunkPlacerType<?>, TrunkPlacerType<MapleBranchingTrunkPlacer>> MAPLE_BRANCHING_TRUNK =
            Apeira.REGISTRAR.forRegistry(Registries.TRUNK_PLACER_TYPE).register("maple_branching_trunk",
                    () -> new TrunkPlacerType<>(MapleBranchingTrunkPlacer.CODEC));

    private MainFeatures() {}

    public static void init(ApeiraRegistrar registrar) {}
}
