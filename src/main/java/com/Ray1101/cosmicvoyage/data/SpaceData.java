package com.Ray1101.cosmicvoyage.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 太空数据持久化 — 双锚点系统（地球 + 月球）
 *
 * <p>职责边界：
 *   - 只负责锚点数据的序列化/反序列化
 *   - 不处理维度切换逻辑、不处理网络通信
 *   - NBT 结构版本化管理，支持未来扩展
 *
 * <p>NBT 版本演进：
 *   - v1（旧）: anchorX/Y/Z + hasAnchor（单锚点，默认地球）
 *   - v2（当前）: earthAnchor + moonAnchor 双锚点，DATA_VERSION 字段
 */
public class SpaceData extends SavedData {

    private static final int DATA_VERSION = 2;
    private static final String KEY_VERSION = "DataVersion";

    // ===== 地球锚点 =====
    private static final String KEY_HAS_EARTH_ANCHOR = "HasEarthAnchor";
    private static final String KEY_EARTH_ANCHOR_X = "EarthAnchorX";
    private static final String KEY_EARTH_ANCHOR_Y = "EarthAnchorY";
    private static final String KEY_EARTH_ANCHOR_Z = "EarthAnchorZ";

    private double earthAnchorX;
    private double earthAnchorY;
    private double earthAnchorZ;
    private boolean hasEarthAnchor = false;

    // ===== 月球锚点 =====
    private static final String KEY_HAS_MOON_ANCHOR = "HasMoonAnchor";
    private static final String KEY_MOON_ANCHOR_X = "MoonAnchorX";
    private static final String KEY_MOON_ANCHOR_Y = "MoonAnchorY";
    private static final String KEY_MOON_ANCHOR_Z = "MoonAnchorZ";

    private double moonAnchorX;
    private double moonAnchorY;
    private double moonAnchorZ;
    private boolean hasMoonAnchor = false;

    // ======================
    // 创建 / 加载实例
    // ======================

    public static SpaceData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                SpaceData::load,
                SpaceData::new,
                "cosmicvoyage_space"
        );
    }

    public static SpaceData load(CompoundTag tag) {
        SpaceData data = new SpaceData();

        int version = tag.contains(KEY_VERSION) ? tag.getInt(KEY_VERSION) : 1;

        if (version == 1) {
            // v1 → v2 自动迁移：旧 anchorX/Y/Z 迁移到 earthAnchor
            if (tag.contains("HasAnchor") && tag.getBoolean("HasAnchor")) {
                data.earthAnchorX = tag.getDouble("AnchorX");
                data.earthAnchorY = tag.getDouble("AnchorY");
                data.earthAnchorZ = tag.getDouble("AnchorZ");
                data.hasEarthAnchor = true;
            }
        } else {
            // v2 正常加载
            data.hasEarthAnchor = tag.getBoolean(KEY_HAS_EARTH_ANCHOR);
            if (data.hasEarthAnchor) {
                data.earthAnchorX = tag.getDouble(KEY_EARTH_ANCHOR_X);
                data.earthAnchorY = tag.getDouble(KEY_EARTH_ANCHOR_Y);
                data.earthAnchorZ = tag.getDouble(KEY_EARTH_ANCHOR_Z);
            }

            data.hasMoonAnchor = tag.getBoolean(KEY_HAS_MOON_ANCHOR);
            if (data.hasMoonAnchor) {
                data.moonAnchorX = tag.getDouble(KEY_MOON_ANCHOR_X);
                data.moonAnchorY = tag.getDouble(KEY_MOON_ANCHOR_Y);
                data.moonAnchorZ = tag.getDouble(KEY_MOON_ANCHOR_Z);
            }
        }

        return data;
    }

    // ======================
    // 保存
    // ======================

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(KEY_VERSION, DATA_VERSION);

        tag.putBoolean(KEY_HAS_EARTH_ANCHOR, hasEarthAnchor);
        if (hasEarthAnchor) {
            tag.putDouble(KEY_EARTH_ANCHOR_X, earthAnchorX);
            tag.putDouble(KEY_EARTH_ANCHOR_Y, earthAnchorY);
            tag.putDouble(KEY_EARTH_ANCHOR_Z, earthAnchorZ);
        }

        tag.putBoolean(KEY_HAS_MOON_ANCHOR, hasMoonAnchor);
        if (hasMoonAnchor) {
            tag.putDouble(KEY_MOON_ANCHOR_X, moonAnchorX);
            tag.putDouble(KEY_MOON_ANCHOR_Y, moonAnchorY);
            tag.putDouble(KEY_MOON_ANCHOR_Z, moonAnchorZ);
        }

        return tag;
    }

    // ======================
    // 地球锚点 API
    // ======================

    public void setEarthAnchor(double x, double y, double z) {
        this.earthAnchorX = x;
        this.earthAnchorY = y;
        this.earthAnchorZ = z;
        this.hasEarthAnchor = true;
        setDirty();
    }

    public boolean hasEarthAnchor() {
        return hasEarthAnchor;
    }

    public double getEarthX() { return earthAnchorX; }
    public double getEarthY() { return earthAnchorY; }
    public double getEarthZ() { return earthAnchorZ; }

    public BlockPos getEarthAnchor() {
        return hasEarthAnchor ? new BlockPos((int) earthAnchorX, (int) earthAnchorY, (int) earthAnchorZ) : null;
    }

    public void clearEarthAnchor() {
        this.hasEarthAnchor = false;
        setDirty();
    }

    // ======================
    // 月球锚点 API
    // ======================

    public void setMoonAnchor(double x, double y, double z) {
        this.moonAnchorX = x;
        this.moonAnchorY = y;
        this.moonAnchorZ = z;
        this.hasMoonAnchor = true;
        setDirty();
    }

    public boolean hasMoonAnchor() {
        return hasMoonAnchor;
    }

    public double getMoonX() { return moonAnchorX; }
    public double getMoonY() { return moonAnchorY; }
    public double getMoonZ() { return moonAnchorZ; }

    public BlockPos getMoonAnchor() {
        return hasMoonAnchor ? new BlockPos((int) moonAnchorX, (int) moonAnchorY, (int) moonAnchorZ) : null;
    }

    public void clearMoonAnchor() {
        this.hasMoonAnchor = false;
        setDirty();
    }

    // ======================
    // 通用查询
    // ======================

    /**
     * 根据维度返回对应锚点（目标锚点，非当前所在维度锚点）。
     *
     * <p>逻辑：若在主世界 → 返回月球锚点（目的地）；
     *        若在月球   → 返回地球锚点（目的地）。
     *
     * @param dimension 玩家当前所在维度
     * @return 目标维度对应的锚点 BlockPos，若未设置则返回 null
     */
    public BlockPos getAnchorFor(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.OVERWORLD)) {
            return getMoonAnchor();   // 从地球出发 → 目标是月球
        }
        // 月球或其他维度
        return getEarthAnchor();      // 从月球出发 → 目标是地球
    }
}