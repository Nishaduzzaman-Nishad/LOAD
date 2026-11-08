package com.example.load_app;

import org.junit.Test;
import static org.junit.Assert.*;

import com.example.load_app.models.Package;
import com.example.load_app.utils.KnapsackAlgorithm;
import com.example.load_app.utils.PriorityQueueAlgorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class ExampleUnitTest {

    @Test
    public void knapsack_prioritizesUrgentPackages() {
        List<Package> packages = new ArrayList<>();
        // Package ID, pickup, drop, phone, weight, type, priority, customerId
        Package normalPkg = new Package("1", "A", "B", "123", 10.0, "Box", "NORMAL", "cust1");
        Package urgentPkg = new Package("2", "A", "B", "123", 10.0, "Box", "URGENT", "cust1");
        packages.add(normalPkg);
        packages.add(urgentPkg);

        // Capacity is 10kg, can only fit ONE package.
        // Knapsack should choose the urgent one because its value is higher.
        List<Package> result = KnapsackAlgorithm.getOptimalPackages(packages, 10.0);

        assertEquals(1, result.size());
        assertEquals("2", result.get(0).getPackageId()); // Urgent package selected
    }

    @Test
    public void priorityQueue_sortsUrgentAndOldestFirst() throws InterruptedException {
        PriorityQueue<Package> queue = PriorityQueueAlgorithm.createDeliveryQueue();

        Package normalOld = new Package("1", "A", "B", "123", 5.0, "Box", "NORMAL", "cust1");
        normalOld.setCreatedAt(1000);

        Thread.sleep(5); // ensure different timestamp
        Package urgentNew = new Package("2", "A", "B", "123", 5.0, "Box", "URGENT", "cust1");

        Thread.sleep(5);
        Package normalNew = new Package("3", "A", "B", "123", 5.0, "Box", "NORMAL", "cust1");

        queue.add(normalOld);
        queue.add(urgentNew);
        queue.add(normalNew);

        // Order should be: Urgent New -> Normal Old -> Normal New
        assertEquals("2", queue.poll().getPackageId());
        assertEquals("1", queue.poll().getPackageId());
        assertEquals("3", queue.poll().getPackageId());
    }
}