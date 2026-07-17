package com.servermanagement.network.prioritization;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.PriorityQueue;

public class SmartPacketQueue {

    public static class PrioritizedPacket {
        public final Packet<?> packet;
        public final int priority;

        public PrioritizedPacket(Packet<?> packet, int priority) {
            this.packet = packet;
            this.priority = priority;
        }
    }

    private final PriorityQueue<PrioritizedPacket> queue = new PriorityQueue<>(
            Comparator.comparingInt(p -> p.priority)
    );

    public void enqueue(ServerPlayer player, Packet<?> packet, double targetX, double targetY, double targetZ) {
        // Calculate dot product of look vector to determine if target is in front of the player
        Vec3 lookVec = player.getLookAngle();
        Vec3 toTarget = new Vec3(targetX - player.getX(), targetY - player.getY(), targetZ - player.getZ()).normalize();
        
        double dot = lookVec.dot(toTarget);
        
        // Priority 0 = highest priority.
        // If dot > 0.5 (roughly in front), high priority.
        // If dot < 0 (behind), low priority.
        int priority;
        if (dot > 0.7) priority = 0;
        else if (dot > 0.3) priority = 1;
        else if (dot > 0.0) priority = 2;
        else priority = 3;

        queue.add(new PrioritizedPacket(packet, priority));
    }

    public void sendNext(ServerPlayer player) {
        if (!queue.isEmpty()) {
            player.connection.send(queue.poll().packet);
        }
    }
}
