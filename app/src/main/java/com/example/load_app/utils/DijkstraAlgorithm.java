package com.example.load_app.utils;

import java.util.*;

public class DijkstraAlgorithm {
    public static class Edge {
        public final String target;
        public final int weight;
        public Edge(String target, int weight) {
            this.target = target;
            this.weight = weight;
        }
    }

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

        LinkedList<String> path = new LinkedList<>();
        String step = end;
        while (step != null) {
            path.addFirst(step);
            step = previousNodes.get(step);
        }

        return String.join(" -> ", path) + "\nTotal Distance: " + distances.get(end) + " km";
    }

    public static Map<String, List<Edge>> buildDhakaGraph() {
        Map<String, List<Edge>> graph = new HashMap<>();

        // Helper method to add undirected edges
        addEdge(graph, "Uttara", "Airport", 3);
        addEdge(graph, "Uttara", "Khilkhet", 5);
        addEdge(graph, "Uttara", "Mirpur", 12);
        addEdge(graph, "Uttara", "Gulshan", 15);

        addEdge(graph, "Airport", "Nikunja", 2);
        addEdge(graph, "Airport", "Khilkhet", 3);

        addEdge(graph, "Khilkhet", "Kuril", 2);
        addEdge(graph, "Khilkhet", "Bashundhara", 3);

        addEdge(graph, "Kuril", "Notun Bazar", 2);
        addEdge(graph, "Kuril", "Badda", 3);

        addEdge(graph, "Notun Bazar", "Gulshan", 3);
        addEdge(graph, "Notun Bazar", "Baridhara", 2);

        addEdge(graph, "Gulshan", "Banani", 2);
        addEdge(graph, "Gulshan", "Mohakhali", 3);
        addEdge(graph, "Gulshan", "Baridhara", 3);

        addEdge(graph, "Banani", "Mohakhali", 2);
        addEdge(graph, "Banani", "Tejgaon", 5);

        addEdge(graph, "Mohakhali", "Tejgaon", 4);
        addEdge(graph, "Mohakhali", "Kawran Bazar", 4);

        addEdge(graph, "Tejgaon", "Farmgate", 2);
        addEdge(graph, "Tejgaon", "Kawran Bazar", 2);

        addEdge(graph, "Farmgate", "Dhanmondi", 3);
        addEdge(graph, "Farmgate", "Mohammadpur", 4);
        addEdge(graph, "Farmgate", "Shahbagh", 2);
        addEdge(graph, "Farmgate", "Agargaon", 3);

        addEdge(graph, "Dhanmondi", "Jhigatola", 1);
        addEdge(graph, "Dhanmondi", "Shahbagh", 3);
        addEdge(graph, "Dhanmondi", "Mohammadpur", 2);

        addEdge(graph, "Mohammadpur", "Shyamoli", 2);
        addEdge(graph, "Mohammadpur", "Dhanmondi", 2);

        addEdge(graph, "Shyamoli", "Mirpur", 4);
        addEdge(graph, "Shyamoli", "Agargaon", 3);

        addEdge(graph, "Mirpur", "Kazipara", 2);
        addEdge(graph, "Mirpur", "Agargaon", 4);

        addEdge(graph, "Agargaon", "Kawran Bazar", 3);

        addEdge(graph, "Shahbagh", "Motijheel", 4);
        addEdge(graph, "Shahbagh", "Paltan", 3);
        addEdge(graph, "Shahbagh", "Kawran Bazar", 2);

        addEdge(graph, "Motijheel", "Paltan", 2);
        addEdge(graph, "Motijheel", "Jatrabari", 5);
        addEdge(graph, "Motijheel", "Khilgaon", 4);

        addEdge(graph, "Paltan", "Moghbazar", 3);

        addEdge(graph, "Moghbazar", "Malibagh", 2);
        addEdge(graph, "Moghbazar", "Rampura", 3);

        addEdge(graph, "Malibagh", "Khilgaon", 2);

        addEdge(graph, "Khilgaon", "Rampura", 2);

        addEdge(graph, "Rampura", "Badda", 3);

        addEdge(graph, "Badda", "Merul Badda", 1);
        addEdge(graph, "Badda", "Rampura", 3);

        addEdge(graph, "Jatrabari", "Sayedabad", 2);
        addEdge(graph, "Jatrabari", "Demra", 6);

        addEdge(graph, "Sayedabad", "Motijheel", 4);
        addEdge(graph, "Sayedabad", "Gendaria", 2);

        addEdge(graph, "Gendaria", "Kotwali", 2);

        addEdge(graph, "Kotwali", "Lalbagh", 2);
        addEdge(graph, "Kotwali", "Azimpur", 3);

        addEdge(graph, "Lalbagh", "Azimpur", 2);

        addEdge(graph, "Azimpur", "Newmarket", 2);
        addEdge(graph, "Azimpur", "Dhanmondi", 3);

        addEdge(graph, "Newmarket", "Dhanmondi", 2);

        addEdge(graph, "Kawran Bazar", "Panthapath", 2);

        addEdge(graph, "Panthapath", "Green Road", 2);
        addEdge(graph, "Panthapath", "Farmgate", 2);

        addEdge(graph, "Green Road", "Kalabagan", 1);

        addEdge(graph, "Kalabagan", "Dhanmondi", 2);

        addEdge(graph, "Mirpur", "Gabtoli", 4);

        addEdge(graph, "Gabtoli", "Kallyanpur", 3);

        addEdge(graph, "Kallyanpur", "Shyamoli", 2);

        return graph;
    }

    private static void addEdge(Map<String, List<Edge>> graph, String source, String target, int weight) {
        graph.computeIfAbsent(source, k -> new ArrayList<>()).add(new Edge(target, weight));
        graph.computeIfAbsent(target, k -> new ArrayList<>()).add(new Edge(source, weight));
    }
}