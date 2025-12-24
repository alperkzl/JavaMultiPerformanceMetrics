package singleobjective;

import java.io.*;
import java.util.*;

/**
 * Invokes Python scripts for generating plots from Java.
 */
public class PythonPlotCaller {

    private String scriptsDir;
    private String outputDir;

    // Plot configuration
    private boolean showLegend = true;
    private boolean showLabels = false;
    private String plotTitle = null;
    private int markerSize = 8;
    private String markerShape = "circle";
    private int dpi = 150;
    private double width = 12;
    private double height = 8;

    public PythonPlotCaller(String scriptsDir, String outputDir) {
        this.scriptsDir = scriptsDir;
        this.outputDir = outputDir;
    }

    // Setters for plot configuration
    public void setShowLegend(boolean showLegend) { this.showLegend = showLegend; }
    public void setShowLabels(boolean showLabels) { this.showLabels = showLabels; }
    public void setPlotTitle(String plotTitle) { this.plotTitle = plotTitle; }
    public void setMarkerSize(int markerSize) { this.markerSize = markerSize; }
    public void setMarkerShape(String markerShape) { this.markerShape = markerShape; }
    public void setDpi(int dpi) { this.dpi = dpi; }
    public void setWidth(double width) { this.width = width; }
    public void setHeight(double height) { this.height = height; }

    /**
     * Generate a 2D plot using the Python script.
     *
     * @param dataFile Path to the JSON data file
     * @param objective1 X-axis objective (Makespan, Energy, or AvgWait)
     * @param objective2 Y-axis objective (Makespan, Energy, or AvgWait)
     * @param outputFile Output image file path (or null for default)
     * @return The output file path
     */
    public String generate2DPlot(String dataFile, String objective1, String objective2, String outputFile) throws Exception {
        System.out.println("\n=== Generating 2D Plot ===");

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String pythonCmd = isWindows ? "python" : "python3";

        String scriptPath = scriptsDir + "/plot_2d.py";

        if (outputFile == null) {
            outputFile = outputDir + "/plot_" + objective1 + "_vs_" + objective2 + ".png";
        }

        List<String> command = new ArrayList<>();
        command.add(pythonCmd);
        command.add(scriptPath);
        command.add("--data");
        command.add(dataFile);
        command.add("--output");
        command.add(outputFile);

        if (plotTitle != null) {
            command.add("--title");
            command.add(plotTitle);
        }

        command.add("--legend");
        command.add(String.valueOf(showLegend));

        command.add("--labels");
        command.add(String.valueOf(showLabels));

        command.add("--marker-size");
        command.add(String.valueOf(markerSize));

        command.add("--marker-shape");
        command.add(markerShape);

        command.add("--dpi");
        command.add(String.valueOf(dpi));

        command.add("--width");
        command.add(String.valueOf(width));

        command.add("--height");
        command.add(String.valueOf(height));

        executeCommand(command);
        return outputFile;
    }

    /**
     * Generate a static 3D plot using matplotlib.
     *
     * @param dataFile Path to the JSON data file
     * @param outputFile Output image file path (or null for default)
     * @return The output file path
     */
    public String generate3DStaticPlot(String dataFile, String outputFile) throws Exception {
        System.out.println("\n=== Generating 3D Static Plot ===");

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String pythonCmd = isWindows ? "python" : "python3";

        String scriptPath = scriptsDir + "/plot_3d_static.py";

        if (outputFile == null) {
            outputFile = outputDir + "/plot_3d_static.png";
        }

        List<String> command = new ArrayList<>();
        command.add(pythonCmd);
        command.add(scriptPath);
        command.add("--data");
        command.add(dataFile);
        command.add("--output");
        command.add(outputFile);

        if (plotTitle != null) {
            command.add("--title");
            command.add(plotTitle);
        }

        command.add("--legend");
        command.add(String.valueOf(showLegend));

        command.add("--labels");
        command.add(String.valueOf(showLabels));

        command.add("--marker-size");
        command.add(String.valueOf(markerSize));

        command.add("--dpi");
        command.add(String.valueOf(dpi));

        command.add("--width");
        command.add(String.valueOf(width));

        command.add("--height");
        command.add(String.valueOf(height));

        executeCommand(command);
        return outputFile;
    }

    /**
     * Generate an interactive 3D plot using Plotly.
     *
     * @param dataFile Path to the JSON data file
     * @param outputFile Output HTML file path (or null for default)
     * @return The output file path
     */
    public String generate3DInteractivePlot(String dataFile, String outputFile) throws Exception {
        System.out.println("\n=== Generating 3D Interactive Plot ===");

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String pythonCmd = isWindows ? "python" : "python3";

        String scriptPath = scriptsDir + "/plot_3d_interactive.py";

        if (outputFile == null) {
            outputFile = outputDir + "/plot_3d_interactive.html";
        }

        List<String> command = new ArrayList<>();
        command.add(pythonCmd);
        command.add(scriptPath);
        command.add("--data");
        command.add(dataFile);
        command.add("--output");
        command.add(outputFile);

        if (plotTitle != null) {
            command.add("--title");
            command.add(plotTitle);
        }

        command.add("--legend");
        command.add(String.valueOf(showLegend));

        command.add("--marker-size");
        command.add(String.valueOf(markerSize));

        executeCommand(command);
        return outputFile;
    }

    /**
     * Execute a command and print output.
     */
    private void executeCommand(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        System.out.println("Executing: " + String.join(" ", command));

        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Python script failed with exit code: " + exitCode);
        }
    }
}
