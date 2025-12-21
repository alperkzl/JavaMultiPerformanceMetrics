package taskprocessor;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Task Processor - Analyzes multi-objective and single-objective optimization results
 * for cloud VM task scheduling experiments.
 *
 * Usage: java taskprocessor.TaskProcessor <n> <includeSingleObjective> <objective1> <objective2> [options]
 *   n: number of tasks (200-2000, must be 700, 900, or 1200)
 *   includeSingleObjective: true/false - whether to include single-objective algorithms
 *   objective1: Makespan, Energy, or AvgWait
 *   objective2: Makespan, Energy, or AvgWait
 *
 * Options:
 *   --plot              Generate Pareto front plot
 *   --plot-title        Custom plot title
 *   --plot-legend       Show legend (true/false, default: true)
 *   --plot-labels       Show point labels (true/false, default: false)
 *   --plot-marker-size  Marker size (default: 8)
 *   --plot-marker-shape Marker shape (circle/square/triangle/diamond, default: circle)
 *   --plot-output       Custom output filename for plot
 *   --plot-dpi          Plot DPI (default: 150)
 *   --plot-width        Plot width in inches (default: 12)
 *   --plot-height       Plot height in inches (default: 8)
 */
public class TaskProcessor {

    private int numTasks;
    private boolean includeSingleObjective;
    private String objective1;
    private String objective2;
    private String basePath;

    // Plot configuration
    private boolean generatePlot = false;
    private String plotTitle = null;
    private boolean plotLegend = true;
    private boolean plotLabels = false;
    private int plotMarkerSize = 8;
    private String plotMarkerShape = "circle";
    private String plotOutput = null;
    private int plotDpi = 150;
    private double plotWidth = 12;
    private double plotHeight = 8;

    // Objective column mappings
    private static final Map<String, String> OBJECTIVE_COLUMNS = new HashMap<>();
    static {
        OBJECTIVE_COLUMNS.put("Makespan", "Makespan");
        OBJECTIVE_COLUMNS.put("Energy", "Energy Use Wh");
        OBJECTIVE_COLUMNS.put("AvgWait", "Avg Waiting Time");
    }

    // Objective display names for plot
    private static final Map<String, String> OBJECTIVE_DISPLAY_NAMES = new HashMap<>();
    static {
        OBJECTIVE_DISPLAY_NAMES.put("Makespan", "Makespan (s)");
        OBJECTIVE_DISPLAY_NAMES.put("Energy", "Energy Consumption (Wh)");
        OBJECTIVE_DISPLAY_NAMES.put("AvgWait", "Avg. Wait Time (s)");
    }

    // Objective pair to file pattern mapping
    private static final Map<String, String> OBJECTIVE_PAIR_PATTERNS = new HashMap<>();
    static {
        OBJECTIVE_PAIR_PATTERNS.put("Energy_Makespan", "");  // No suffix for Energy vs Makespan
        OBJECTIVE_PAIR_PATTERNS.put("Makespan_Energy", "");  // Same pair, different order
        OBJECTIVE_PAIR_PATTERNS.put("Energy_AvgWait", "_eVSs");
        OBJECTIVE_PAIR_PATTERNS.put("AvgWait_Energy", "_eVSs");
        OBJECTIVE_PAIR_PATTERNS.put("Makespan_AvgWait", "_mVSs");
        OBJECTIVE_PAIR_PATTERNS.put("AvgWait_Makespan", "_mVSs");
    }

    // Multi-objective algorithms
    private static final String[] MO_ALGORITHMS = {
        "MOEA_AMOSA", "MOEA_NSGAII", "MOEA_SPEAII", "MOEA_eNSGAII"
    };

    // Seeds used in experiments
    private static final int[] SEEDS = {1200, 1201, 1202, 1203, 1204, 1205, 1206, 1207, 1208, 1209};

    // Store all solutions by algorithm
    private Map<String, List<double[]>> algorithmSolutions = new LinkedHashMap<>();

    // Store solutions by algorithm and seed
    private Map<String, Map<Integer, List<double[]>>> algorithmSeedSolutions = new LinkedHashMap<>();

    // Store non-dominated solutions by algorithm
    private Map<String, List<double[]>> algorithmNonDominated = new LinkedHashMap<>();

    // Universal Pareto set
    private List<double[]> universalParetoSet = new ArrayList<>();

    public TaskProcessor(int numTasks, boolean includeSingleObjective, String objective1, String objective2, String basePath) {
        this.numTasks = numTasks;
        this.includeSingleObjective = includeSingleObjective;
        this.objective1 = objective1;
        this.objective2 = objective2;
        this.basePath = basePath;
    }

    // Setters for plot configuration
    public void setGeneratePlot(boolean generatePlot) { this.generatePlot = generatePlot; }
    public void setPlotTitle(String plotTitle) { this.plotTitle = plotTitle; }
    public void setPlotLegend(boolean plotLegend) { this.plotLegend = plotLegend; }
    public void setPlotLabels(boolean plotLabels) { this.plotLabels = plotLabels; }
    public void setPlotMarkerSize(int plotMarkerSize) { this.plotMarkerSize = plotMarkerSize; }
    public void setPlotMarkerShape(String plotMarkerShape) { this.plotMarkerShape = plotMarkerShape; }
    public void setPlotOutput(String plotOutput) { this.plotOutput = plotOutput; }
    public void setPlotDpi(int plotDpi) { this.plotDpi = plotDpi; }
    public void setPlotWidth(double plotWidth) { this.plotWidth = plotWidth; }
    public void setPlotHeight(double plotHeight) { this.plotHeight = plotHeight; }

    public void process() throws Exception {
        System.out.println("=== Task Processor ===");
        System.out.println("Number of tasks: " + numTasks);
        System.out.println("Include Single-Objective: " + includeSingleObjective);
        System.out.println("Objective 1: " + objective1 + " (" + OBJECTIVE_COLUMNS.get(objective1) + ")");
        System.out.println("Objective 2: " + objective2 + " (" + OBJECTIVE_COLUMNS.get(objective2) + ")");
        System.out.println();

        // Step 1: Scan and parse files
        scanMultiObjectiveFiles();
        if (includeSingleObjective) {
            scanSingleObjectiveFiles();
        }

        // Step 2: Calculate non-dominated points per algorithm
        calculateNonDominatedPerAlgorithm();

        // Step 3: Calculate universal Pareto set
        calculateUniversalParetoSet();

        // Step 4-6: Calculate performance metrics
        Map<String, double[]> metrics = calculatePerformanceMetrics();

        // Step 7: Generate CSV report
        generateCSVReport(metrics);

        // Step 8: Generate plot if requested
        if (generatePlot) {
            generatePlot();
        }
    }

    private void scanMultiObjectiveFiles() throws Exception {
        String taskFolder = numTasks + " Task";
        String moPath = basePath + "/Multi-Objective Algorithms/" + taskFolder;
        File moDir = new File(moPath);

        if (!moDir.exists() || !moDir.isDirectory()) {
            System.err.println("Multi-Objective directory not found: " + moPath);
            return;
        }

        // Determine the file pattern based on objective pair
        String objPairKey = objective1 + "_" + objective2;
        String objPattern = OBJECTIVE_PAIR_PATTERNS.get(objPairKey);
        if (objPattern == null) {
            // Try reverse order
            objPairKey = objective2 + "_" + objective1;
            objPattern = OBJECTIVE_PAIR_PATTERNS.get(objPairKey);
        }

        if (objPattern == null) {
            System.err.println("Invalid objective pair: " + objective1 + " vs " + objective2);
            return;
        }

        System.out.println("Scanning Multi-Objective files with pattern: " + (objPattern.isEmpty() ? "(no suffix)" : objPattern));

        File[] files = moDir.listFiles((dir, name) -> name.endsWith(".xlsx") && !name.startsWith("~"));
        if (files == null) return;

        ExcelReader reader = new ExcelReader();

        for (String algo : MO_ALGORITHMS) {
            algorithmSolutions.put(algo, new ArrayList<>());
            algorithmSeedSolutions.put(algo, new LinkedHashMap<>());
            for (int seed : SEEDS) {
                algorithmSeedSolutions.get(algo).put(seed, new ArrayList<>());
            }
        }

        // Handle special case for eNSGAII which might be named eNSGA2 for energy vs makespan
        String eNSGAPattern = objPattern.isEmpty() ? "MOEA_eNSGA2" : "MOEA_eNSGAII";

        for (File file : files) {
            String fileName = file.getName();

            for (String algo : MO_ALGORITHMS) {
                String searchPattern = algo;
                if (algo.equals("MOEA_eNSGAII") && objPattern.isEmpty()) {
                    searchPattern = "MOEA_eNSGA2";
                }

                // Check if file matches algorithm and objective pattern
                String fullPattern = searchPattern + objPattern + "_rnd_";
                if (!fileName.startsWith(fullPattern)) continue;

                // Extract seed from filename
                Pattern seedPattern = Pattern.compile("_rnd_(\\d+)_");
                Matcher seedMatcher = seedPattern.matcher(fileName);
                if (!seedMatcher.find()) continue;

                int seed = Integer.parseInt(seedMatcher.group(1));
                if (!containsSeed(seed)) continue;

                // Read the Excel file
                try {
                    Map<String, Double> values = reader.readExcelFile(file.getAbsolutePath());
                    Double obj1Value = values.get(OBJECTIVE_COLUMNS.get(objective1));
                    Double obj2Value = values.get(OBJECTIVE_COLUMNS.get(objective2));

                    if (obj1Value != null && obj2Value != null) {
                        double[] solution = new double[]{obj1Value, obj2Value};
                        algorithmSolutions.get(algo).add(solution);
                        algorithmSeedSolutions.get(algo).get(seed).add(solution);
                    }
                } catch (Exception e) {
                    System.err.println("Error reading file: " + file.getName() + " - " + e.getMessage());
                }
            }
        }

        // Print solution counts
        System.out.println("\nMulti-Objective Solution Counts:");
        for (String algo : MO_ALGORITHMS) {
            System.out.println("  " + algo + ": " + algorithmSolutions.get(algo).size() + " solutions");
        }
    }

    private void scanSingleObjectiveFiles() throws Exception {
        String taskFolder = numTasks + " Tasks";
        String soPath = basePath + "/Single - Objective Algorithms/" + taskFolder;
        File soDir = new File(soPath);

        if (!soDir.exists() || !soDir.isDirectory()) {
            System.err.println("Single-Objective directory not found: " + soPath);
            return;
        }

        System.out.println("\nScanning Single-Objective files...");

        ExcelReader reader = new ExcelReader();

        // Map objectives to folder names
        Map<String, List<String>> objToFolders = new HashMap<>();
        objToFolders.put("Makespan", Arrays.asList("GA_MAKESPAN", "GA_ISL_Makespan", "SA_Makespan", "SJF_BEST", "SJF_WORST", "LJF_BEST", "LJF_WORST"));
        objToFolders.put("Energy", Arrays.asList("GA_Energy", "GA_ISL_Energy", "SA_Energy"));
        objToFolders.put("AvgWait", Arrays.asList("GA_AvgWait", "GA_ISL_AvgWait", "SA_AvgWait"));

        // Determine which single-objective algorithms to include
        Set<String> relevantFolders = new HashSet<>();

        // Include algorithms optimizing either objective1 or objective2
        if (objToFolders.containsKey(objective1)) {
            relevantFolders.addAll(objToFolders.get(objective1));
        }
        if (objToFolders.containsKey(objective2)) {
            relevantFolders.addAll(objToFolders.get(objective2));
        }

        // Also include heuristics which may optimize multiple objectives
        relevantFolders.add("SJF_BEST");
        relevantFolders.add("SJF_WORST");
        relevantFolders.add("LJF_BEST");
        relevantFolders.add("LJF_WORST");

        for (String folder : relevantFolders) {
            File algoDir = new File(soPath + "/" + folder);
            if (!algoDir.exists() || !algoDir.isDirectory()) continue;

            String algoName = "SO_" + folder;
            algorithmSolutions.put(algoName, new ArrayList<>());
            algorithmSeedSolutions.put(algoName, new LinkedHashMap<>());
            for (int seed : SEEDS) {
                algorithmSeedSolutions.get(algoName).put(seed, new ArrayList<>());
            }

            File[] files = algoDir.listFiles((dir, name) -> name.endsWith(".xlsx") && !name.startsWith("~") && !name.contains("results"));
            if (files == null) continue;

            for (File file : files) {
                String fileName = file.getName();

                // Extract seed from filename
                Pattern seedPattern = Pattern.compile("_rnd_(\\d+)_");
                Matcher seedMatcher = seedPattern.matcher(fileName);
                if (!seedMatcher.find()) continue;

                int seed = Integer.parseInt(seedMatcher.group(1));
                if (!containsSeed(seed)) continue;

                // Read the Excel file
                try {
                    Map<String, Double> values = reader.readExcelFile(file.getAbsolutePath());
                    Double obj1Value = values.get(OBJECTIVE_COLUMNS.get(objective1));
                    Double obj2Value = values.get(OBJECTIVE_COLUMNS.get(objective2));

                    if (obj1Value != null && obj2Value != null) {
                        double[] solution = new double[]{obj1Value, obj2Value};
                        algorithmSolutions.get(algoName).add(solution);
                        algorithmSeedSolutions.get(algoName).get(seed).add(solution);
                    }
                } catch (Exception e) {
                    System.err.println("Error reading file: " + file.getName() + " - " + e.getMessage());
                }
            }
        }

        // Print solution counts for single-objective
        System.out.println("\nSingle-Objective Solution Counts:");
        for (String algo : algorithmSolutions.keySet()) {
            if (algo.startsWith("SO_")) {
                System.out.println("  " + algo + ": " + algorithmSolutions.get(algo).size() + " solutions");
            }
        }
    }

    private boolean containsSeed(int seed) {
        for (int s : SEEDS) {
            if (s == seed) return true;
        }
        return false;
    }

    private void calculateNonDominatedPerAlgorithm() {
        System.out.println("\n=== Calculating Non-Dominated Points Per Algorithm ===");

        for (String algo : algorithmSolutions.keySet()) {
            List<double[]> solutions = algorithmSolutions.get(algo);
            List<double[]> nonDominated = Dominance.getNonDominatedSet(solutions);
            algorithmNonDominated.put(algo, nonDominated);
            System.out.println("  " + algo + ": " + nonDominated.size() + " non-dominated / " + solutions.size() + " total");
        }
    }

    private void calculateUniversalParetoSet() {
        System.out.println("\n=== Calculating Universal Pareto Set ===");

        // Combine all solutions
        List<double[]> allSolutions = new ArrayList<>();
        for (List<double[]> solutions : algorithmSolutions.values()) {
            allSolutions.addAll(solutions);
        }

        // Find universal non-dominated set
        universalParetoSet = Dominance.getNonDominatedSet(allSolutions);
        System.out.println("Universal Pareto Set size: " + universalParetoSet.size() + " from " + allSolutions.size() + " total solutions");
    }

    private Map<String, double[]> calculatePerformanceMetrics() {
        System.out.println("\n=== Calculating Performance Metrics ===");

        Map<String, double[]> metrics = new LinkedHashMap<>();

        // Create PerformanceMetrics instance
        // We need: [algorithm pareto, universal pareto]
        for (String algo : algorithmSolutions.keySet()) {
            List<double[]> algoPareto = algorithmNonDominated.get(algo);

            if (algoPareto.isEmpty()) {
                System.out.println("  " + algo + ": No solutions, skipping metrics");
                metrics.put(algo, new double[]{0, Double.MAX_VALUE, Double.MAX_VALUE});
                continue;
            }

            // Convert to ArrayList<ArrayList<Double>> format for PerformanceMetrics
            ArrayList<ArrayList<Double>> algoParetoList = convertToArrayList(algoPareto);
            ArrayList<ArrayList<Double>> universalParetoList = convertToArrayList(universalParetoSet);

            ArrayList<ArrayList<ArrayList<Double>>> allParetos = new ArrayList<>();
            allParetos.add(algoParetoList);    // index 0: algorithm pareto
            allParetos.add(universalParetoList); // index 1: not used directly
            allParetos.add(universalParetoList); // index 2: reference front for IGD

            PerformanceMetrics pm = new PerformanceMetrics(allParetos);

            double hv = pm.HV(0);
            double igd = pm.IGD(0);
            double gd = calculateGD(algoPareto, universalParetoSet);

            metrics.put(algo, new double[]{hv, gd, igd});
            System.out.println("  " + algo + ": HV=" + String.format("%.6f", hv) +
                             ", GD=" + String.format("%.6f", gd) +
                             ", IGD=" + String.format("%.6f", igd));
        }

        return metrics;
    }

    private ArrayList<ArrayList<Double>> convertToArrayList(List<double[]> solutions) {
        ArrayList<ArrayList<Double>> result = new ArrayList<>();
        for (double[] sol : solutions) {
            ArrayList<Double> row = new ArrayList<>();
            row.add(sol[0]);
            row.add(sol[1]);
            result.add(row);
        }
        return result;
    }

    private double calculateGD(List<double[]> approximation, List<double[]> reference) {
        if (approximation.isEmpty() || reference.isEmpty()) return Double.MAX_VALUE;

        // Normalize solutions
        double f1min = Double.MAX_VALUE, f1max = Double.MIN_VALUE;
        double f2min = Double.MAX_VALUE, f2max = Double.MIN_VALUE;

        for (double[] sol : approximation) {
            f1min = Math.min(f1min, sol[0]);
            f1max = Math.max(f1max, sol[0]);
            f2min = Math.min(f2min, sol[1]);
            f2max = Math.max(f2max, sol[1]);
        }
        for (double[] sol : reference) {
            f1min = Math.min(f1min, sol[0]);
            f1max = Math.max(f1max, sol[0]);
            f2min = Math.min(f2min, sol[1]);
            f2max = Math.max(f2max, sol[1]);
        }

        if (f1max == f1min) f1max = f1min + 1;
        if (f2max == f2min) f2max = f2min + 1;

        // Calculate GD: average distance from approximation to reference
        double totalDistance = 0.0;
        for (double[] approxSol : approximation) {
            double minDist = Double.MAX_VALUE;
            double nax1 = (approxSol[0] - f1min) / (f1max - f1min);
            double nax2 = (approxSol[1] - f2min) / (f2max - f2min);

            for (double[] refSol : reference) {
                double nrx1 = (refSol[0] - f1min) / (f1max - f1min);
                double nrx2 = (refSol[1] - f2min) / (f2max - f2min);
                double dist = Math.sqrt(Math.pow(nax1 - nrx1, 2) + Math.pow(nax2 - nrx2, 2));
                minDist = Math.min(minDist, dist);
            }
            totalDistance += minDist;
        }

        return totalDistance / approximation.size();
    }

    private void generateCSVReport(Map<String, double[]> metrics) throws IOException {
        String outputFile = basePath + "/results_" + numTasks + "_" + objective1 + "_vs_" + objective2 + ".csv";

        System.out.println("\n=== Generating CSV Report ===");
        System.out.println("Output file: " + outputFile);

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // Header
            writer.println("Algorithm,Type,Total_Solutions,Non_Dominated_Solutions," +
                          "Seed_1200,Seed_1201,Seed_1202,Seed_1203,Seed_1204," +
                          "Seed_1205,Seed_1206,Seed_1207,Seed_1208,Seed_1209," +
                          "HV,GD,IGD");

            // Data rows
            for (String algo : algorithmSolutions.keySet()) {
                StringBuilder sb = new StringBuilder();
                sb.append(algo).append(",");
                sb.append(algo.startsWith("SO_") ? "Single-Objective" : "Multi-Objective").append(",");
                sb.append(algorithmSolutions.get(algo).size()).append(",");
                sb.append(algorithmNonDominated.get(algo).size()).append(",");

                // Solutions per seed
                for (int seed : SEEDS) {
                    int count = algorithmSeedSolutions.get(algo).get(seed).size();
                    sb.append(count).append(",");
                }

                // Metrics
                double[] m = metrics.get(algo);
                sb.append(String.format("%.6f", m[0])).append(",");
                sb.append(String.format("%.6f", m[1])).append(",");
                sb.append(String.format("%.6f", m[2]));

                writer.println(sb.toString());
            }

            // Add universal Pareto set info
            writer.println();
            writer.println("Universal_Pareto_Set_Size," + universalParetoSet.size());
        }

        System.out.println("CSV report generated successfully!");
    }

    private void generatePlot() throws Exception {
        System.out.println("\n=== Generating Pareto Front Plot ===");

        // Detect operating system
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String fileSeparator = File.separator;

        // Generate JSON data file (use File to handle path separators correctly)
        String jsonFile = new File(basePath, "plot_data_" + numTasks + "_" + objective1 + "_vs_" + objective2 + ".json").getAbsolutePath();
        generatePlotDataJson(jsonFile);

        // Determine output filename
        String outputFile = plotOutput;
        if (outputFile == null) {
            outputFile = new File(basePath, "pareto_" + numTasks + "_" + objective1 + "_vs_" + objective2 + ".png").getAbsolutePath();
        }

        // Build Python command - use "python" on Windows, "python3" on Unix/Linux/Mac
        List<String> command = new ArrayList<>();
        command.add(isWindows ? "python" : "python3");
        command.add(new File(basePath, "scripts" + fileSeparator + "plot_pareto.py").getAbsolutePath());
        command.add("--data");
        command.add(jsonFile);
        command.add("--output");
        command.add(outputFile);

        if (plotTitle != null) {
            command.add("--title");
            command.add(plotTitle);
        }

        command.add("--legend");
        command.add(String.valueOf(plotLegend));

        command.add("--labels");
        command.add(String.valueOf(plotLabels));

        command.add("--marker-size");
        command.add(String.valueOf(plotMarkerSize));

        command.add("--marker-shape");
        command.add(plotMarkerShape);

        command.add("--dpi");
        command.add(String.valueOf(plotDpi));

        command.add("--width");
        command.add(String.valueOf(plotWidth));

        command.add("--height");
        command.add(String.valueOf(plotHeight));

        // Execute Python script
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.directory(new File(basePath));

        System.out.println("Executing: " + String.join(" ", command));

        Process process = pb.start();

        // Read output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Plot generation failed with exit code: " + exitCode);
        } else {
            System.out.println("Plot generated: " + outputFile);
        }
    }

    private void generatePlotDataJson(String jsonFile) throws IOException {
        System.out.println("Generating plot data JSON: " + jsonFile);

        try (PrintWriter writer = new PrintWriter(new FileWriter(jsonFile))) {
            writer.println("{");

            // Metadata
            writer.println("  \"num_tasks\": " + numTasks + ",");
            writer.println("  \"objective1\": \"" + OBJECTIVE_DISPLAY_NAMES.get(objective1) + "\",");
            writer.println("  \"objective2\": \"" + OBJECTIVE_DISPLAY_NAMES.get(objective2) + "\",");

            // Algorithms
            writer.println("  \"algorithms\": {");

            int algoCount = 0;
            int totalAlgos = algorithmNonDominated.size();

            for (String algo : algorithmNonDominated.keySet()) {
                List<double[]> nonDom = algorithmNonDominated.get(algo);
                List<double[]> allSols = algorithmSolutions.get(algo);

                writer.println("    \"" + algo + "\": {");
                writer.println("      \"total_solutions\": " + allSols.size() + ",");

                // Non-dominated points
                writer.println("      \"non_dominated\": [");
                for (int i = 0; i < nonDom.size(); i++) {
                    double[] sol = nonDom.get(i);
                    writer.print("        [" + sol[0] + ", " + sol[1] + "]");
                    if (i < nonDom.size() - 1) writer.print(",");
                    writer.println();
                }
                writer.println("      ],");

                // All points (for optional full plotting)
                writer.println("      \"all_solutions\": [");
                for (int i = 0; i < allSols.size(); i++) {
                    double[] sol = allSols.get(i);
                    writer.print("        [" + sol[0] + ", " + sol[1] + "]");
                    if (i < allSols.size() - 1) writer.print(",");
                    writer.println();
                }
                writer.println("      ]");

                algoCount++;
                writer.print("    }");
                if (algoCount < totalAlgos) writer.print(",");
                writer.println();
            }

            writer.println("  },");

            // Universal Pareto set
            writer.println("  \"universal_pareto\": [");
            for (int i = 0; i < universalParetoSet.size(); i++) {
                double[] sol = universalParetoSet.get(i);
                writer.print("    [" + sol[0] + ", " + sol[1] + "]");
                if (i < universalParetoSet.size() - 1) writer.print(",");
                writer.println();
            }
            writer.println("  ]");

            writer.println("}");
        }
    }

    public static void printUsage() {
        System.out.println("Usage: java taskprocessor.TaskProcessor <n> <includeSingleObjective> <objective1> <objective2> [options]");
        System.out.println();
        System.out.println("Required arguments:");
        System.out.println("  n                      Number of tasks (700, 900, or 1200)");
        System.out.println("  includeSingleObjective Include single-objective algorithms (true/false)");
        System.out.println("  objective1             First objective (Makespan, Energy, or AvgWait)");
        System.out.println("  objective2             Second objective (Makespan, Energy, or AvgWait)");
        System.out.println();
        System.out.println("Plot options:");
        System.out.println("  --plot                 Generate Pareto front plot");
        System.out.println("  --plot-title <title>   Custom plot title");
        System.out.println("  --plot-legend <bool>   Show legend (true/false, default: true)");
        System.out.println("  --plot-labels <bool>   Show point labels (true/false, default: false)");
        System.out.println("  --plot-marker-size <n> Marker size (default: 8)");
        System.out.println("  --plot-marker-shape <s> Marker shape (circle/square/triangle/diamond, default: circle)");
        System.out.println("  --plot-output <file>   Custom output filename for plot");
        System.out.println("  --plot-dpi <n>         Plot DPI (default: 150)");
        System.out.println("  --plot-width <n>       Plot width in inches (default: 12)");
        System.out.println("  --plot-height <n>      Plot height in inches (default: 8)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java taskprocessor.TaskProcessor 700 true Energy Makespan");
        System.out.println("  java taskprocessor.TaskProcessor 700 false Energy Makespan --plot");
        System.out.println("  java taskprocessor.TaskProcessor 1200 true Makespan AvgWait --plot --plot-title \"Custom Title\" --plot-legend false");
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            printUsage();
            return;
        }

        try {
            int numTasks = Integer.parseInt(args[0]);
            if (numTasks != 700 && numTasks != 900 && numTasks != 1200) {
                System.err.println("Error: n must be 700, 900, or 1200");
                return;
            }

            boolean includeSingleObjective = Boolean.parseBoolean(args[1]);
            String objective1 = args[2];
            String objective2 = args[3];

            // Validate objectives
            if (!OBJECTIVE_COLUMNS.containsKey(objective1)) {
                System.err.println("Error: Invalid objective1. Must be Makespan, Energy, or AvgWait");
                return;
            }
            if (!OBJECTIVE_COLUMNS.containsKey(objective2)) {
                System.err.println("Error: Invalid objective2. Must be Makespan, Energy, or AvgWait");
                return;
            }
            if (objective1.equals(objective2)) {
                System.err.println("Error: objective1 and objective2 must be different");
                return;
            }

            // Determine base path (look for it in the last non-option argument or use current dir)
            String basePath = System.getProperty("user.dir");

            // Parse optional arguments
            TaskProcessor processor = new TaskProcessor(numTasks, includeSingleObjective, objective1, objective2, basePath);

            for (int i = 4; i < args.length; i++) {
                String arg = args[i];

                if (arg.equals("--plot")) {
                    processor.setGeneratePlot(true);
                } else if (arg.equals("--plot-title") && i + 1 < args.length) {
                    processor.setPlotTitle(args[++i]);
                } else if (arg.equals("--plot-legend") && i + 1 < args.length) {
                    processor.setPlotLegend(Boolean.parseBoolean(args[++i]));
                } else if (arg.equals("--plot-labels") && i + 1 < args.length) {
                    processor.setPlotLabels(Boolean.parseBoolean(args[++i]));
                } else if (arg.equals("--plot-marker-size") && i + 1 < args.length) {
                    processor.setPlotMarkerSize(Integer.parseInt(args[++i]));
                } else if (arg.equals("--plot-marker-shape") && i + 1 < args.length) {
                    processor.setPlotMarkerShape(args[++i]);
                } else if (arg.equals("--plot-output") && i + 1 < args.length) {
                    processor.setPlotOutput(args[++i]);
                } else if (arg.equals("--plot-dpi") && i + 1 < args.length) {
                    processor.setPlotDpi(Integer.parseInt(args[++i]));
                } else if (arg.equals("--plot-width") && i + 1 < args.length) {
                    processor.setPlotWidth(Double.parseDouble(args[++i]));
                } else if (arg.equals("--plot-height") && i + 1 < args.length) {
                    processor.setPlotHeight(Double.parseDouble(args[++i]));
                } else if (!arg.startsWith("--")) {
                    // Could be base path
                    basePath = arg;
                    processor = new TaskProcessor(numTasks, includeSingleObjective, objective1, objective2, basePath);
                }
            }

            processor.process();

        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid number format - " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
