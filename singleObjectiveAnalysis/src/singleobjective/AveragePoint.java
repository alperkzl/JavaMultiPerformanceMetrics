package singleobjective;

/**
 * Represents an average point with X, Y, Z coordinates corresponding to
 * Makespan, Energy, and Avg Wait Time averages for an algorithm at a specific task count.
 */
public class AveragePoint {
    private String algorithmName;
    private int taskCount;
    private double avgMakespan;     // X coordinate
    private double avgEnergy;       // Y coordinate
    private double avgWaitTime;     // Z coordinate
    private int solutionCount;      // Number of solutions used to compute averages

    public AveragePoint(String algorithmName, int taskCount, double avgMakespan, double avgEnergy, double avgWaitTime, int solutionCount) {
        this.algorithmName = algorithmName;
        this.taskCount = taskCount;
        this.avgMakespan = avgMakespan;
        this.avgEnergy = avgEnergy;
        this.avgWaitTime = avgWaitTime;
        this.solutionCount = solutionCount;
    }

    // Getters
    public String getAlgorithmName() { return algorithmName; }
    public int getTaskCount() { return taskCount; }
    public double getAvgMakespan() { return avgMakespan; }
    public double getAvgEnergy() { return avgEnergy; }
    public double getAvgWaitTime() { return avgWaitTime; }
    public int getSolutionCount() { return solutionCount; }

    // Alias getters for X, Y, Z coordinates
    public double getX() { return avgMakespan; }
    public double getY() { return avgEnergy; }
    public double getZ() { return avgWaitTime; }

    /**
     * Get objective value by name.
     * @param objective "Makespan", "Energy", or "AvgWait"
     * @return The average objective value
     */
    public double getObjective(String objective) {
        switch (objective) {
            case "Makespan": return avgMakespan;
            case "Energy": return avgEnergy;
            case "AvgWait": return avgWaitTime;
            default: throw new IllegalArgumentException("Unknown objective: " + objective);
        }
    }

    /**
     * Get the label for this point in format "Algorithm - TaskCount Tasks"
     */
    public String getLabel() {
        return algorithmName + " - " + taskCount + " Tasks";
    }

    @Override
    public String toString() {
        return String.format("AveragePoint[%s, %d tasks, makespan=%.2f, energy=%.2f, avgWait=%.2f, n=%d]",
                algorithmName, taskCount, avgMakespan, avgEnergy, avgWaitTime, solutionCount);
    }
}
