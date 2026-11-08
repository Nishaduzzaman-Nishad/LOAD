package com.example.load_app.utils;

import com.example.load_app.models.Package;
import java.util.Comparator;
import java.util.PriorityQueue;

public class PriorityQueueAlgorithm {

    // Custom Comparator: Urgent first, then oldest first (FIFO)
    public static PriorityQueue<Package> createDeliveryQueue() {
        return new PriorityQueue<>(new Comparator<Package>() {
            @Override
            public int compare(Package p1, Package p2) {
                // 1. Check Priority
                boolean p1IsUrgent = "URGENT".equalsIgnoreCase(p1.getPriority()) || "HIGH".equalsIgnoreCase(p1.getPriority());
                boolean p2IsUrgent = "URGENT".equalsIgnoreCase(p2.getPriority()) || "HIGH".equalsIgnoreCase(p2.getPriority());

                if (p1IsUrgent && !p2IsUrgent) return -1; // p1 comes first
                if (!p1IsUrgent && p2IsUrgent) return 1;  // p2 comes first

                // 2. If both have same priority, sort by creation time (oldest first)
                return Long.compare(p1.getCreatedAt(), p2.getCreatedAt());
            }
        });
    }
}