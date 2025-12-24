package singleobjective;

import java.util.*;

/**
 * Stores all data for a single-objective algorithm across all task counts and seeds.
 */
public class AlgorithmData {

    public enum AlgorithmType {
        GA,      // Standard Genetic Algorithm
        GA_ISL,  // Island Model Genetic Algorithm
        SA       // Simulated Annealing
    }

    private String name;
    private AlgorithmType type;

    // Solutions organized by task count and seed
    // Map<TaskCount, Map<Seed, List<Solution>>>
    private Map<Integer, Map<Integer, List<Solution>>> solutions;

    // Computed average points per task count
    private Map<Integer, AveragePoint> averagePoints;

    public AlgorithmData(String name) {
        this.name = name;
        this.type = determineType(name);
        this.solutions = new LinkedHashMap<>();
        this.averagePoints = new LinkedHashMap<>();
    }

    private AlgorithmType determineType(String name) {
        if (name.startsWith("GA_ISL")) {
            return AlgorithmType.GA_ISL;
        } else if (name.startsWith("SA")) {
            return AlgorithmType.SA;
        } else if (name.startsWith("GA")) {
            return AlgorithmType.GA;
        }
        return AlgorithmType.GA; // Default
    }

    public String getName() { return name; }
    public AlgorithmType getType() { return type; }

    /**
     * Get the color associated with this specific algorithm.
     * Colors are assigned per algorithm, not per type.
     */
    public String getColor() {
        // SA variants - Red tones
        if (name.equals("SA_Makespan")) return "#FF0000";      // Red
        if (name.equals("SA_Energy")) return "#FF8C00";        // Orange
        if (name.equals("SA_AvgWait")) return "#8B0000";       // Dark Red

        // GA variants - Blue tones
        if (name.equals("GA_MAKESPAN")) return "#0000FF";      // Blue
        if (name.equals("GA_Energy")) return "#87CEEB";        // Light Blue (Sky Blue)
        if (name.equals("GA_AvgWait")) return "#00008B";       // Dark Blue

        // GA_ISL variants - Green tones
        if (name.equals("GA_ISL_Makespan")) return "#228B22";  // Green (Forest Green)
        if (name.equals("GA_ISL_Energy")) return "#90EE90";    // Light Green
        if (name.equals("GA_ISL_AvgWait")) return "#006400";   // Dark Green

        // Default fallback by type
        switch (type) {
            case GA: return "#0000FF";      // Blue
            case GA_ISL: return "#228B22";  // Green
            case SA: return "#FF0000";      // Red
            default: return "#000000";      // Black
        }
    }

    /**
     * Get color name for display.
     */
    public String getColorName() {
        // SA variants
        if (name.equals("SA_Makespan")) return "Red";
        if (name.equals("SA_Energy")) return "Orange";
        if (name.equals("SA_AvgWait")) return "DarkRed";

        // GA variants
        if (name.equals("GA_MAKESPAN")) return "Blue";
        if (name.equals("GA_Energy")) return "LightBlue";
        if (name.equals("GA_AvgWait")) return "DarkBlue";

        // GA_ISL variants
        if (name.equals("GA_ISL_Makespan")) return "Green";
        if (name.equals("GA_ISL_Energy")) return "LightGreen";
        if (name.equals("GA_ISL_AvgWait")) return "DarkGreen";

        // Default fallback by type
        switch (type) {
            case GA: return "Blue";
            case GA_ISL: return "Green";
            case SA: return "Red";
            default: return "Black";
        }
    }

    /**
     * Add a solution for this algorithm.
     */
    public void addSolution(Solution solution) {
        int taskCount = solution.getTaskCount();
        int seed = solution.getSeed();

        solutions.computeIfAbsent(taskCount, k -> new LinkedHashMap<>());
        solutions.get(taskCount).computeIfAbsent(seed, k -> new ArrayList<>());
        solutions.get(taskCount).get(seed).add(solution);
    }

    /**
     * Get all solutions for a specific task count.
     */
    public List<Solution> getSolutionsForTaskCount(int taskCount) {
        List<Solution> result = new ArrayList<>();
        Map<Integer, List<Solution>> seedMap = solutions.get(taskCount);
        if (seedMap != null) {
            for (List<Solution> sols : seedMap.values()) {
                result.addAll(sols);
            }
        }
        return result;
    }

    /**
     * Get solutions for a specific task count and seed.
     */
    public List<Solution> getSolutions(int taskCount, int seed) {
        Map<Integer, List<Solution>> seedMap = solutions.get(taskCount);
        if (seedMap != null && seedMap.containsKey(seed)) {
            return seedMap.get(seed);
        }
        return Collections.emptyList();
    }

    /**
     * Get count of solutions for a specific task count and seed.
     */
    public int getSolutionCount(int taskCount, int seed) {
        return getSolutions(taskCount, seed).size();
    }

    /**
     * Get total solution count for a specific task count.
     */
    public int getTotalSolutionCount(int taskCount) {
        return getSolutionsForTaskCount(taskCount).size();
    }

    /**
     * Get all task counts that have solutions.
     */
    public Set<Integer> getTaskCounts() {
        return solutions.keySet();
    }

    /**
     * Get all seeds that have solutions for a specific task count.
     */
    public Set<Integer> getSeeds(int taskCount) {
        Map<Integer, List<Solution>> seedMap = solutions.get(taskCount);
        if (seedMap != null) {
            return seedMap.keySet();
        }
        return Collections.emptySet();
    }

    /**
     * Compute and store average points for all task counts.
     */
    public void computeAverages() {
        averagePoints.clear();

        for (int taskCount : solutions.keySet()) {
            List<Solution> taskSolutions = getSolutionsForTaskCount(taskCount);
            if (taskSolutions.isEmpty()) continue;

            double sumMakespan = 0, sumEnergy = 0, sumAvgWait = 0;
            for (Solution sol : taskSolutions) {
                sumMakespan += sol.getMakespan();
                sumEnergy += sol.getEnergy();
                sumAvgWait += sol.getAvgWaitTime();
            }

            int count = taskSolutions.size();
            AveragePoint avg = new AveragePoint(
                name,
                taskCount,
                sumMakespan / count,
                sumEnergy / count,
                sumAvgWait / count,
                count
            );
            averagePoints.put(taskCount, avg);
        }
    }

    /**
     * Get average point for a specific task count.
     */
    public AveragePoint getAveragePoint(int taskCount) {
        return averagePoints.get(taskCount);
    }

    /**
     * Get all average points.
     */
    public Collection<AveragePoint> getAllAveragePoints() {
        return averagePoints.values();
    }

    /**
     * Get solution count map for reporting: Map<Seed, Count> for a task count.
     */
    public Map<Integer, Integer> getSolutionCountsBySeed(int taskCount) {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        Map<Integer, List<Solution>> seedMap = solutions.get(taskCount);
        if (seedMap != null) {
            for (Map.Entry<Integer, List<Solution>> entry : seedMap.entrySet()) {
                counts.put(entry.getKey(), entry.getValue().size());
            }
        }
        return counts;
    }

    @Override
    public String toString() {
        int totalSolutions = 0;
        for (int tc : solutions.keySet()) {
            totalSolutions += getTotalSolutionCount(tc);
        }
        return String.format("AlgorithmData[%s, type=%s, taskCounts=%d, totalSolutions=%d]",
                name, type, solutions.size(), totalSolutions);
    }
}
