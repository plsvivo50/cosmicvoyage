package com.Ray1101.cosmicvoyage.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class SpaceData extends SavedData {

    private double anchorX;
    private double anchorY;
    private double anchorZ;

    private boolean hasAnchor = false;

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

        data.anchorX = tag.getDouble("AnchorX");
        data.anchorY = tag.getDouble("AnchorY");
        data.anchorZ = tag.getDouble("AnchorZ");
        data.hasAnchor = tag.getBoolean("HasAnchor");

        return data;
    }

    // ======================
    // 保存
    // ======================
    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putDouble("AnchorX", anchorX);
        tag.putDouble("AnchorY", anchorY);
        tag.putDouble("AnchorZ", anchorZ);
        tag.putBoolean("HasAnchor", hasAnchor);
        return tag;
    }

    // ======================
    // API：设置锚点
    // ======================
    public void setAnchor(double x, double y, double z) {
        this.anchorX = x;
        this.anchorY = y;
        this.anchorZ = z;
        this.hasAnchor = true;
        setDirty();
    }

    // ======================
    // API：读取锚点
    // ======================
    public boolean hasAnchor() {
        return hasAnchor;
    }

    public double getX() { return anchorX; }
    public double getY() { return anchorY; }
    public double getZ() { return anchorZ; }
}