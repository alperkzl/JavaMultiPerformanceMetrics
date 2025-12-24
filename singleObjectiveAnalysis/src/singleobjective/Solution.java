package singleobjective;

/**
 * Represents a single solution from a single-objective optimization algorithm.
 * Contains all three objective values: Makespan, Energy Consumption, and Avg Wait Time.
 */
public class Solution {
    private double makespan;        // Makespan in seconds
    private double energy;          // Energy Consumption in Wh
    private double avgWaitTime;     // Average Wait Time in seconds
    private int seed;               // Random seed used for this run
    private int taskCount;          // Number of tasks in the experiment
    private String algorithmName;   // Name of the algorithm

    public Solution(double makespan, double energy, double avgWaitTime, int seed, int taskCount, String algorithmName) {
        this.makespan = makespan;
        this.energy = energy;
        this.avgWaitTime = avgWaitTime;
        this.seed = seed;
        this.taskCount = taskCount;
        this.algorithmName = algorithmName;
    }

    // Getters
    public double getMakespan() { return makespan; }
    public double getEnergy() { return energy; }
    public double getAvgWaitTime() { return avgWaitTime; }
    public int getSeed() { return seed; }
    public int getTaskCount() { return taskCount; }
    public String getAlgorithmName() { return algorithmName; }

    /**
     * Get objective value by name.
     * @param objective "Makespan", "Energy", or "AvgWait"
     * @return The objective value
     */
    public double getObjective(String objective) {
        switch (objective) {
            case "Makespan": return makespan;
            case "Energy": return energy;
            case "AvgWait": return avgWaitTime;
            default: throw new IllegalArgumentException("Unknown objective: " + objective);
        }
    }

    @Override
    public String toString() {
        return String.format("Solution[%s, %d tasks, seed=%d, makespan=%.2f, energy=%.2f, avgWait=%.2f]",
                algorithmName, taskCount, seed, makespan, energy, avgWaitTime);
    }
}
