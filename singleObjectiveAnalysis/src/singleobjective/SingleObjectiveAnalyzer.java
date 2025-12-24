package singleobjective;

import java.io.*;
import java.util.*;

/**
 * Single Objective Analyzer - Main entry point for analyzing single-objective
 * optimization algorithm results.
 *
 * Features:
 * - Parses Excel files from Single - Objective Algorithms folder
 * - Reports solution counts per algorithm, task count, and seed
 * - Stores objective values for all solutions
 * - Computes average points (X, Y, Z) for each algorithm per task count
 * - Generates 2D Pareto-style plots (X-Y)
 * - Generates 3D plots (static and interactive)
 *
 * Usage:
 *   java singleobjective.SingleObjectiveAnalyzer [options]
 *
 * Options:
 *   --plot2d <obj1> <obj2>     Generate 2D plot with specified objectives
 *   --plot3d                   Generate 3D plots (both static and interactive)
 *   --tasks <n1,n2,...>        Filter by specific task counts (comma-separated)
 *   --legend <true|false>      Show legend (default: true)
 *   --labels <true|false>      Show point labels (default: false)
 *   --title <title>            Custom plot title
 *   --marker-size <n>          Marker size (default: 8)
 *   --marker-shape <shape>     Marker shape: circle, square, triangle, diamond
 *   --output-dir <dir>         Output directory (default: singleObjectiveAnalysis/output)
 */
public class SingleObjectiveAnalyzer {

    private String basePath;
    private String outputDir;
    private String scriptsDir;

    // Plot options
    private boolean generatePlot2D = false;
    private String objective1 = "Makespan";
    private String objective2 = "Energy";
    private boolean generatePlot3D = false;
    private int[] taskCountFilter = null;

    // Plot configuration
    private boolean showLegend = true;
    private boolean showLabels = false;
    private String plotTitle = null;
    private int markerSize = 8;
    private String markerShape = "circle";
    private int dpi = 150;
    private double plotWidth = 12;
    private double plotHeight = 8;

    public SingleObjectiveAnalyzer(String basePath) {
        this.basePath = basePath;
        this.outputDir = basePath + "/singleObjectiveAnalysis/output";
        this.scriptsDir = basePath + "/singleObjectiveAnalysis/scripts";
    }

    // Setters
    public void setGeneratePlot2D(boolean generate, String obj1, String obj2) {
        this.generatePlot2D = generate;
        if (obj1 != null) this.objective1 = obj1;
        if (obj2 != null) this.objective2 = obj2;
    }

    public void setGeneratePlot3D(boolean generate) {
        this.generatePlot3D = generate;
    }

    public void setTaskCountFilter(int[] filter) {
        this.taskCountFilter = filter;
    }

    public void setShowLegend(boolean show) { this.showLegend = show; }
    public void setShowLabels(boolean show) { this.showLabels = show; }
    public void setPlotTitle(String title) { this.plotTitle = title; }
    public void setMarkerSize(int size) { this.markerSize = size; }
    public void setMarkerShape(String shape) { this.markerShape = shape; }
    public void setDpi(int dpi) { this.dpi = dpi; }
    public void setPlotWidth(double width) { this.plotWidth = width; }
    public void setPlotHeight(double height) { this.plotHeight = height; }
    public void setOutputDir(String dir) { this.outputDir = dir; }

    /**
     * Run the analysis.
     */
    public void run() throws Exception {
        System.out.println("==============================================");
        System.out.println("   Single Objective Algorithm Analyzer");
        System.out.println("==============================================");
        System.out.println("Base path: " + basePath);
        System.out.println("Output directory: " + outputDir);
        System.out.println();

        // Create output directory
        new File(outputDir).mkdirs();

        // Step 1: Parse all data
        DataParser parser = new DataParser(basePath);
        parser.parseAll();
        parser.printSummary();

        // Step 2: Generate CSV reports
        ReportGenerator reportGen = new ReportGenerator(parser, outputDir);
        reportGen.generateAllReports();

        // Step 3: Generate plots if requested
        if (generatePlot2D || generatePlot3D) {
            PythonPlotCaller plotter = new PythonPlotCaller(scriptsDir, outputDir);
            plotter.setShowLegend(showLegend);
            plotter.setShowLabels(showLabels);
            plotter.setPlotTitle(plotTitle);
            plotter.setMarkerSize(markerSize);
            plotter.setMarkerShape(markerShape);
            plotter.setDpi(dpi);
            plotter.setWidth(plotWidth);
            plotter.setHeight(plotHeight);

            if (generatePlot2D) {
                String dataFile = reportGen.generatePlotDataJson(objective1, objective2, taskCountFilter);
                String outputFile = plotter.generate2DPlot(dataFile, objective1, objective2, null);
                System.out.println("2D Plot saved to: " + outputFile);
            }

            if (generatePlot3D) {
                String dataFile = reportGen.generate3DPlotDataJson(taskCountFilter);

                // Generate static 3D plot
                String staticOutput = plotter.generate3DStaticPlot(dataFile, null);
                System.out.println("3D Static Plot saved to: " + staticOutput);

                // Generate interactive 3D plot
                String interactiveOutput = plotter.generate3DInteractivePlot(dataFile, null);
                System.out.println("3D Interactive Plot saved to: " + interactiveOutput);
            }
        }

        System.out.println("\n=== Analysis Complete ===");
    }

    /**
     * Print usage information.
     */
    public static void printUsage() {
        System.out.println("Single Objective Algorithm Analyzer");
        System.out.println();
        System.out.println("Usage: java singleobjective.SingleObjectiveAnalyzer [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --plot2d <obj1> <obj2>     Generate 2D plot with specified objectives");
        System.out.println("                             Objectives: Makespan, Energy, AvgWait");
        System.out.println("  --plot3d                   Generate 3D plots (static + interactive)");
        System.out.println("  --tasks <n1,n2,...>        Filter by task counts (e.g., 200,500,1000)");
        System.out.println("  --legend <true|false>      Show legend (default: true)");
        System.out.println("  --labels <true|false>      Show point labels (default: false)");
        System.out.println("  --title <title>            Custom plot title");
        System.out.println("  --marker-size <n>          Marker size (default: 8)");
        System.out.println("  --marker-shape <shape>     Shape: circle, square, triangle, diamond");
        System.out.println("  --dpi <n>                  Plot DPI (default: 150)");
        System.out.println("  --width <n>                Plot width in inches (default: 12)");
        System.out.println("  --height <n>               Plot height in inches (default: 8)");
        System.out.println("  --output-dir <dir>         Output directory");
        System.out.println("  --help                     Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Generate CSV reports only");
        System.out.println("  java singleobjective.SingleObjectiveAnalyzer");
        System.out.println();
        System.out.println("  # Generate 2D plot: Makespan vs Energy");
        System.out.println("  java singleobjective.SingleObjectiveAnalyzer --plot2d Makespan Energy");
        System.out.println();
        System.out.println("  # Generate all plots with labels");
        System.out.println("  java singleobjective.SingleObjectiveAnalyzer --plot2d Energy AvgWait --plot3d --labels true");
        System.out.println();
        System.out.println("  # Filter specific task counts");
        System.out.println("  java singleobjective.SingleObjectiveAnalyzer --plot2d Makespan Energy --tasks 200,500,1000");
        System.out.println();
        System.out.println("Algorithms analyzed:");
        System.out.println("  GA variants (Green):     GA_AvgWait, GA_Energy, GA_MAKESPAN");
        System.out.println("  GA_ISL variants (Blue):  GA_ISL_AvgWait, GA_ISL_Energy, GA_ISL_Makespan");
        System.out.println("  SA variants (Red):       SA_AvgWait, SA_Energy, SA_Makespan");
        System.out.println();
        System.out.println("Task counts: 200, 300, 500, 700, 900, 1200, 1500, 1800");
    }

    /**
     * Parse command line arguments.
     */
    public static SingleObjectiveAnalyzer parseArgs(String[] args) {
        String basePath = System.getProperty("user.dir");
        SingleObjectiveAnalyzer analyzer = new SingleObjectiveAnalyzer(basePath);

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "--help":
                case "-h":
                    printUsage();
                    return null;

                case "--plot2d":
                    if (i + 2 < args.length) {
                        String obj1 = args[++i];
                        String obj2 = args[++i];
                        if (!isValidObjective(obj1) || !isValidObjective(obj2)) {
                            System.err.println("Error: Invalid objectives. Use Makespan, Energy, or AvgWait");
                            return null;
                        }
                        analyzer.setGeneratePlot2D(true, obj1, obj2);
                    } else {
                        System.err.println("Error: --plot2d requires two objectives");
                        return null;
                    }
                    break;

                case "--plot3d":
                    analyzer.setGeneratePlot3D(true);
                    break;

                case "--tasks":
                    if (i + 1 < args.length) {
                        String[] parts = args[++i].split(",");
                        int[] tasks = new int[parts.length];
                        try {
                            for (int j = 0; j < parts.length; j++) {
                                tasks[j] = Integer.parseInt(parts[j].trim());
                            }
                            analyzer.setTaskCountFilter(tasks);
                        } catch (NumberFormatException e) {
                            System.err.println("Error: Invalid task count format");
                            return null;
                        }
                    }
                    break;

                case "--legend":
                    if (i + 1 < args.length) {
                        analyzer.setShowLegend(Boolean.parseBoolean(args[++i]));
                    }
                    break;

                case "--labels":
                    if (i + 1 < args.length) {
                        analyzer.setShowLabels(Boolean.parseBoolean(args[++i]));
                    }
                    break;

                case "--title":
                    if (i + 1 < args.length) {
                        analyzer.setPlotTitle(args[++i]);
                    }
                    break;

                case "--marker-size":
                    if (i + 1 < args.length) {
                        analyzer.setMarkerSize(Integer.parseInt(args[++i]));
                    }
                    break;

                case "--marker-shape":
                    if (i + 1 < args.length) {
                        analyzer.setMarkerShape(args[++i]);
                    }
                    break;

                case "--dpi":
                    if (i + 1 < args.length) {
                        analyzer.setDpi(Integer.parseInt(args[++i]));
                    }
                    break;

                case "--width":
                    if (i + 1 < args.length) {
                        analyzer.setPlotWidth(Double.parseDouble(args[++i]));
                    }
                    break;

                case "--height":
                    if (i + 1 < args.length) {
                        analyzer.setPlotHeight(Double.parseDouble(args[++i]));
                    }
                    break;

                case "--output-dir":
                    if (i + 1 < args.length) {
                        analyzer.setOutputDir(args[++i]);
                    }
                    break;

                default:
                    if (!arg.startsWith("--")) {
                        // Could be base path
                        basePath = arg;
                        analyzer = new SingleObjectiveAnalyzer(basePath);
                    }
            }
        }

        return analyzer;
    }

    private static boolean isValidObjective(String obj) {
        return obj.equals("Makespan") || obj.equals("Energy") || obj.equals("AvgWait");
    }

    public static void main(String[] args) {
        try {
            SingleObjectiveAnalyzer analyzer = parseArgs(args);

            if (analyzer == null) {
                return; // Help was printed or error occurred
            }

            analyzer.run();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
