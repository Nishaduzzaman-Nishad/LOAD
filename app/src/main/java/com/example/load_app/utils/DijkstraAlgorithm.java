package com.example.load_app.utils;

import java.util.*;

public class DijkstraAlgorithm {
    // A simple Graph structure
    public static class Edge {
        public final String target;
        public final int weight;
        public Edge(String target, int weight) {
            this.target = target;
            this.weight = weight;
        }
    }

    // Runs Dijkstra and returns the shortest path as a String
    public static String getShortestPath(Map<String, List<Edge>> graph, String start, String end) {
        if (!graph.containsKey(start) || !graph.containsKey(end)) {
            return "Route unavailable (Address not in delivery zone)";
        }

        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previousNodes = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(distances::get));

        for (String node : graph.keySet()) {
            distances.put(node, Integer.MAX_VALUE);
        }
        distances.put(start, 0);
        pq.add(start);

        while (!pq.isEmpty()) {
            String current = pq.poll();
            if (current.equals(end)) break;

            for (Edge edge : graph.get(current)) {
                int newDist = distances.get(current) + edge.weight;
                if (newDist < distances.get(edge.target)) {
                    distances.put(edge.target, newDist);
                    previousNodes.put(edge.target, current);
                    pq.add(edge.target);
                }
            }
        }

        if (distances.get(end) == Integer.MAX_VALUE) return "No route found.";

        // Reconstruct path
        LinkedList<String> path = new LinkedList<>();
        String step = end;
        while (step != null) {
            path.addFirst(step);
            step = previousNodes.get(step);
        }

        return String.join(" -> ", path) + "\nTotal Distance: " + distances.get(end) + " km";
    }

    // Builds a mock graph of Dhaka city zones for the demo
    public static Map<String, List<Edge>> buildDhakaGraph() {
        Map<String, List<Edge>> graph = new HashMap<>();

        graph.put("Uttara", Arrays.asList(new Edge("Mirpur", 12), new Edge("Gulshan", 15)));
        graph.put("Mirpur", Arrays.asList(new Edge("Uttara", 12), new Edge("Dhanmondi", 10), new Edge("Farmgate", 8)));
        graph.put("Gulshan", Arrays.asList(new Edge("Uttara", 15), new Edge("Farmgate", 10), new Edge("Motijheel", 14)));
        graph.put("Farmgate", Arrays.asList(new Edge("Mirpur", 8), new Edge("Gulshan", 10), new Edge("Dhanmondi", 4), new Edge("Motijheel", 5)));
        graph.put("Dhanmondi", Arrays.asList(new Edge("Mirpur", 10), new Edge("Farmgate", 4), new Edge("Motijheel", 7)));
        graph.put("Motijheel", Arrays.asList(new Edge("Gulshan", 14), new Edge("Farmgate", 5), new Edge("Dhanmondi", 7)));

        return graph;
    }
}