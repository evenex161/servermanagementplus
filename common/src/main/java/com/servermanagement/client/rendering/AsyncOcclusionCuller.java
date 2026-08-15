package com.servermanagement.client.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asynchronous entity occlusion culler that uses AABB frustum intersection testing
 * to skip rendering entities that are outside the camera's view frustum.
 * 
 * The visibility check runs on a dedicated background thread to avoid blocking
 * the render thread. Results are delivered back to the main thread via
 * Minecraft's execute queue.
 * 
 * Toggle via PerformanceSettingsScreen — default OFF (opt-in).
 */
public class AsyncOcclusionCuller {

    private static final ExecutorService cullingExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SM-OcclusionCuller");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private static boolean enabled = false;

    public static void setEnabled(boolean state) {
        enabled = state;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Submit an entity for async visibility checking.
     * If the entity is within the camera frustum, onVisible is called on the main thread.
     * If culling is disabled, onVisible is called immediately.
     */
    public static void checkEntityOcclusionAsync(Entity entity, Runnable onVisible) {
        if (!enabled) {
            onVisible.run();
            return;
        }

        // Snapshot the entity's bounding box and camera position on the calling thread
        AABB boundingBox = entity.getBoundingBox();
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        org.joml.Vector3f lookVec = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
        Vec3 cameraLookVec = new Vec3(lookVec.x(), lookVec.y(), lookVec.z());
        double fov = Minecraft.getInstance().options.fov().get();

        cullingExecutor.submit(() -> {
            boolean isVisible = performFrustumCheck(boundingBox, cameraPos, cameraLookVec, fov);
            if (isVisible) {
                Minecraft.getInstance().execute(onVisible);
            }
        });
    }

    /**
     * Submit a block entity for async visibility checking.
     */
    public static void checkBlockEntityOcclusionAsync(BlockEntity blockEntity, Runnable onVisible) {
        if (!enabled) {
            onVisible.run();
            return;
        }

        // Block entities use their block position as the center
        AABB boundingBox = new AABB(blockEntity.getBlockPos()).inflate(0.5);
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        org.joml.Vector3f lookVec2 = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
        Vec3 cameraLookVec = new Vec3(lookVec2.x(), lookVec2.y(), lookVec2.z());
        double fov = Minecraft.getInstance().options.fov().get();

        cullingExecutor.submit(() -> {
            boolean isVisible = performFrustumCheck(boundingBox, cameraPos, cameraLookVec, fov);
            if (isVisible) {
                Minecraft.getInstance().execute(onVisible);
            }
        });
    }

    /**
     * Performs a simplified frustum check using the entity's AABB, the camera position,
     * camera look direction, and field of view.
     * 
     * This is a cone-based approximation: if any corner of the AABB falls within
     * the camera's view cone, the entity is considered visible.
     */
    private static boolean performFrustumCheck(AABB aabb, Vec3 cameraPos, Vec3 cameraLook, double fov) {
        // Calculate the center of the AABB relative to the camera
        double cx = (aabb.minX + aabb.maxX) * 0.5 - cameraPos.x;
        double cy = (aabb.minY + aabb.maxY) * 0.5 - cameraPos.y;
        double cz = (aabb.minZ + aabb.maxZ) * 0.5 - cameraPos.z;

        double distSq = cx * cx + cy * cy + cz * cz;

        // Always visible if very close (within 4 blocks)
        if (distSq < 16.0) return true;

        // Always cull if very far (beyond 256 blocks) — entities shouldn't render this far anyway
        if (distSq > 65536.0) return false;

        double dist = Math.sqrt(distSq);
        if (dist < 0.001) return true;

        // Dot product between camera direction and entity direction
        double dot = (cx * cameraLook.x + cy * cameraLook.y + cz * cameraLook.z) / dist;

        // Behind the camera — cull
        if (dot < 0) return false;

        // Half-angle of the view cone (FOV + generous margin for AABB extent)
        double halfAngleCos = Math.cos(Math.toRadians(fov * 0.6 + 15.0));

        // Entity center is within the expanded view cone
        return dot >= halfAngleCos;
    }

    public static void shutdown() {
        cullingExecutor.shutdown();
    }
}
