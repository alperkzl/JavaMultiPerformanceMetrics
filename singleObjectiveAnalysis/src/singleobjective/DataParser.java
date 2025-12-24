package singleobjective;

import taskprocessor.ExcelReader;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Parses Single-Objective Algorithms folder structure and Excel files.
 */
public class DataParser {

    // Algorithms of interest
    public static final String[] TARGET_ALGORITHMS = {
        "GA_AvgWait",
        "GA_Energy",
        "GA_MAKESPAN",
        "SA_AvgWait",
        "SA_Energy",
        "SA_Makespan",
        "GA_ISL_AvgWait",
        "GA_ISL_Energy",
        "GA_ISL_Makespan"
    };

    // Task counts to process
    public static final int[] TASK_COUNTS = {200, 300, 500, 700, 900, 1200, 1500, 1800};

    // Seeds used in experiments
    public static final int[] SEEDS = {1200, 1201, 1202, 1203, 1204, 1205, 1206, 1207, 1208, 1209};

    // Column name mappings
    private static final String COL_MAKESPAN = "Makespan";
    private static final String COL_ENERGY = "Energy Use Wh";
    private static final String COL_AVG_WAIT = "Avg Waiting Time";

    private String basePath;
    private ExcelReader excelReader;
    private Map<String, AlgorithmData> algorithmDataMap;

    public DataParser(String basePath) {
        this.basePath = basePath;
        this.excelReader = new ExcelReader();
        this.algorithmDataMap = new LinkedHashMap<>();

        // Initialize algorithm data containers
        for (String algo : TARGET_ALGORITHMS) {
            algorithmDataMap.put(algo, new AlgorithmData(algo));
        }
    }

    /**
     * Parse all data from the Single-Objective Algorithms folder.
     */
    public void parseAll() throws Exception {
        System.out.println("=== Parsing Single-Objective Algorithm Data ===");
        System.out.println("Base path: " + basePath);
        System.out.println();

        String soBasePath = basePath + "/Single - Objective Algorithms";
        File soDir = new File(soBasePath);

        if (!soDir.exists() || !soDir.isDirectory()) {
            throw new Exception("Single-Objective Algorithms directory not found: " + soBasePath);
        }

        for (int taskCount : TASK_COUNTS) {
            parseTaskCount(soBasePath, taskCount);
        }

        // Compute averages for all algorithms
        System.out.println("\n=== Computing Average Points ===");
        for (AlgorithmData algoData : algorithmDataMap.values()) {
            algoData.computeAverages();
            if (!algoData.getAllAveragePoints().isEmpty()) {
                System.out.println("  " + algoData.getName() + ": " + algoData.getAllAveragePoints().size() + " average points computed");
            }
        }
    }

    /**
     * Find the task folder handling both "Task" and "Tasks" naming.
     */
    private String findTaskFolder(String soBasePath, int taskCount) {
        // Try "XX Task" first
        File folder1 = new File(soBasePath, taskCount + " Task");
        if (folder1.exists() && folder1.isDirectory()) {
            return folder1.getAbsolutePath();
        }

        // Try "XX Tasks"
        File folder2 = new File(soBasePath, taskCount + " Tasks");
        if (folder2.exists() && folder2.isDirectory()) {
            return folder2.getAbsolutePath();
        }

        return null;
    }

    /**
     * Parse all algorithms for a specific task count.
     */
    private void parseTaskCount(String soBasePath, int taskCount) throws Exception {
        String taskFolderPath = findTaskFolder(soBasePath, taskCount);

        if (taskFolderPath == null) {
            System.out.println("Task folder not found for " + taskCount + " tasks - skipping");
            return;
        }

        System.out.println("Parsing " + taskCount + " tasks from: " + taskFolderPath);

        for (String algoName : TARGET_ALGORITHMS) {
            parseAlgorithm(taskFolderPath, taskCount, algoName);
        }
    }

    /**
     * Parse a specific algorithm folder.
     */
    private void parseAlgorithm(String taskFolderPath, int taskCount, String algoName) throws Exception {
        File algoDir = new File(taskFolderPath, algoName);

        if (!algoDir.exists() || !algoDir.isDirectory()) {
            System.out.println("  Warning: Algorithm folder not found: " + algoName);
            return;
        }

        File[] files = algoDir.listFiles((dir, name) ->
            name.endsWith(".xlsx") &&
            !name.startsWith("~") &&
            !name.toLowerCase().contains("results") &&
            name.contains("_rnd_")
        );

        if (files == null || files.length == 0) {
            System.out.println("  Warning: No data files found for " + algoName);
            return;
        }

        int parsedCount = 0;
        for (File file : files) {
            try {
                Solution solution = parseExcelFile(file, taskCount, algoName);
                if (solution != null) {
                    algorithmDataMap.get(algoName).addSolution(solution);
                    parsedCount++;
                }
            } catch (Exception e) {
                System.err.println("  Error parsing file: " + file.getName() + " - " + e.getMessage());
            }
        }

        if (parsedCount > 0) {
            System.out.println("  " + algoName + ": " + parsedCount + " solutions parsed");
        }
    }

    /**
     * Parse a single Excel file and extract solution data.
     */
    private Solution parseExcelFile(File file, int taskCount, String algoName) throws Exception {
        String fileName = file.getName();

        // Extract seed from filename using pattern "_rnd_XXXX_"
        Pattern seedPattern = Pattern.compile("_rnd_(\\d+)_");
        Matcher seedMatcher = seedPattern.matcher(fileName);

        if (!seedMatcher.find()) {
            return null; // Skip files without seed
        }

        int seed = Integer.parseInt(seedMatcher.group(1));

        // Check if this is one of our target seeds
        if (!isTargetSeed(seed)) {
            return null;
        }

        // Read Excel file
        Map<String, Double> values = excelReader.readExcelFile(file.getAbsolutePath());

        Double makespan = values.get(COL_MAKESPAN);
        Double energy = values.get(COL_ENERGY);
        Double avgWait = values.get(COL_AVG_WAIT);

        if (makespan == null || energy == null || avgWait == null) {
            System.err.println("  Warning: Missing values in " + fileName);
            return null;
        }

        return new Solution(makespan, energy, avgWait, seed, taskCount, algoName);
    }

    private boolean isTargetSeed(int seed) {
        for (int s : SEEDS) {
            if (s == seed) return true;
        }
        return false;
    }

    /**
     * Get parsed algorithm data.
     */
    public Map<String, AlgorithmData> getAlgorithmData() {
        return algorithmDataMap;
    }

    /**
     * Get algorithm data for a specific algorithm.
     */
    public AlgorithmData getAlgorithmData(String algoName) {
        return algorithmDataMap.get(algoName);
    }

    /**
     * Get all algorithms.
     */
    public String[] getTargetAlgorithms() {
        return TARGET_ALGORITHMS;
    }

    /**
     * Get all task counts.
     */
    public int[] getTaskCounts() {
        return TASK_COUNTS;
    }

    /**
     * Get all seeds.
     */
    public int[] getSeeds() {
        return SEEDS;
    }

    /**
     * Print summary of parsed data.
     */
    public void printSummary() {
        System.out.println("\n=== Data Summary ===");
        System.out.println(String.format("%-20s %-10s %s", "Algorithm", "Type", "Solutions per Task Count"));
        System.out.println("-".repeat(80));

        for (String algoName : TARGET_ALGORITHMS) {
            AlgorithmData data = algorithmDataMap.get(algoName);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-20s %-10s ", algoName, data.getType()));

            for (int tc : TASK_COUNTS) {
                int count = data.getTotalSolutionCount(tc);
                sb.append(String.format("%d:%d ", tc, count));
            }
            System.out.println(sb.toString());
        }
    }
}
