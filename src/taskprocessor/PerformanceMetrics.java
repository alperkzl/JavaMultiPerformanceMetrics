package taskprocessor;

import java.util.ArrayList;

/**
 * Performance metrics for evaluating Pareto fronts.
 * Based on the original PerfMet.PerformanceMetrics class.
 *
 * Metrics implemented:
 * - Hypervolume (HV): Measures the volume of objective space covered
 * - Inverse Generational Distance (IGD): Measures convergence to reference front
 * - Generational Distance (GD): Average distance from approximation to reference
 * - Spacing: Measures uniformity of solution distribution
 * - C-Metric: Coverage metric comparing two fronts
 */
public class PerformanceMetrics {

    double f1min, f1max, f2min, f2max;
    ArrayList<ArrayList<ArrayList<Double>>> allParetos;
    ArrayList<ArrayList<ArrayList<Double>>> normalizedParetos;

    /**
     * Constructor - initializes with all Pareto fronts.
     * Index 2 is expected to be the reference/true Pareto front for IGD calculation.
     *
     * @param allParetos List of Pareto fronts
     */
    public PerformanceMetrics(ArrayList<ArrayList<ArrayList<Double>>> allParetos) {
        for (int i = 0; i < allParetos.size(); i++) {
            allParetos.get(i).sort(new FitnessComparator());
        }
        this.allParetos = allParetos;
        findMinMax();
        normalize();
    }

    private void findMinMax() {
        if (allParetos.isEmpty() || allParetos.get(0).isEmpty()) {
            f1min = f1max = f2min = f2max = 0;
            return;
        }

        f1min = f1max = allParetos.get(0).get(0).get(0);
        f2min = f2max = allParetos.get(0).get(0).get(1);

        for (int i = 0; i < allParetos.size(); i++) {
            if (allParetos.get(i).isEmpty()) continue;

            int first = 0, last = allParetos.get(i).size() - 1;

            // f1 - first - min
            if (allParetos.get(i).get(first).get(0) < f1min) {
                f1min = allParetos.get(i).get(first).get(0);
            }
            // f1 - last - max
            if (allParetos.get(i).get(last).get(0) > f1max) {
                f1max = allParetos.get(i).get(last).get(0);
            }
            // f2 - first - min
            if (allParetos.get(i).get(last).get(1) < f2min) {
                f2min = allParetos.get(i).get(last).get(1);
            }
            // f2 - last - max
            if (allParetos.get(i).get(first).get(1) > f2max) {
                f2max = allParetos.get(i).get(first).get(1);
            }
        }
    }

    private void normalize() {
        normalizedParetos = new ArrayList<>();

        double f1Range = f1max - f1min;
        double f2Range = f2max - f2min;

        // Avoid division by zero
        if (f1Range == 0) f1Range = 1;
        if (f2Range == 0) f2Range = 1;

        for (int i = 0; i < allParetos.size(); i++) {
            ArrayList<ArrayList<Double>> normalizedP = new ArrayList<>();
            for (ArrayList<Double> solution : allParetos.get(i)) {
                double nf1 = (solution.get(0) - f1min) / f1Range;
                double nf2 = (solution.get(1) - f2min) / f2Range;
                ArrayList<Double> nSolution = new ArrayList<>();
                nSolution.add(nf1);
                nSolution.add(nf2);
                normalizedP.add(nSolution);
            }
            normalizedParetos.add(normalizedP);
        }
    }

    /**
     * Calculate Hypervolume for a Pareto front.
     * Higher is better - larger covered area indicates a better solution set.
     *
     * @param paretoIndex Index of the Pareto front
     * @return Hypervolume value
     */
    public double HV(int paretoIndex) {
        if (normalizedParetos.get(paretoIndex).isEmpty()) {
            return 0.0;
        }

        ArrayList<ArrayList<Double>> p = normalizedParetos.get(paretoIndex);

        // Reference point is (1.0, 1.0) for normalized space
        double volume = Math.abs(1.0 - p.get(0).get(0)) * Math.abs(1.0 - p.get(0).get(1));

        for (int i = 1; i < p.size(); i++) {
            volume += Math.abs(1.0 - p.get(i).get(0)) * Math.abs(p.get(i - 1).get(1) - p.get(i).get(1));
        }

        return volume;
    }

    private double euclid(ArrayList<Double> x, ArrayList<Double> y) {
        return Math.sqrt(Math.pow(x.get(0) - y.get(0), 2) + Math.pow(x.get(1) - y.get(1), 2));
    }

    /**
     * Calculate Inverse Generational Distance.
     * Measures convergence by calculating average distance from true front to approximation.
     * Lower is better.
     *
     * @param paretoIndex Index of the approximation Pareto front
     * @return IGD value
     */
    public double IGD(int paretoIndex) {
        if (normalizedParetos.size() < 3 || normalizedParetos.get(2).isEmpty()) {
            return Double.MAX_VALUE;
        }
        if (normalizedParetos.get(paretoIndex).isEmpty()) {
            return Double.MAX_VALUE;
        }

        double distance = 0.0;
        ArrayList<ArrayList<Double>> pareto = normalizedParetos.get(paretoIndex);

        for (ArrayList<Double> trueParetoSolution : normalizedParetos.get(2)) {
            double minDistance = Double.MAX_VALUE;
            for (ArrayList<Double> paretoSolution : pareto) {
                double d = euclid(trueParetoSolution, paretoSolution);
                if (d < minDistance) {
                    minDistance = d;
                }
            }
            distance += minDistance;
        }

        return distance / normalizedParetos.get(2).size();
    }

    /**
     * Calculate Generational Distance.
     * Measures convergence by calculating average distance from approximation to true front.
     * Lower is better.
     *
     * @param paretoIndex Index of the approximation Pareto front
     * @return GD value
     */
    public double GD(int paretoIndex) {
        if (normalizedParetos.size() < 3 || normalizedParetos.get(2).isEmpty()) {
            return Double.MAX_VALUE;
        }
        if (normalizedParetos.get(paretoIndex).isEmpty()) {
            return Double.MAX_VALUE;
        }

        double distance = 0.0;
        ArrayList<ArrayList<Double>> pareto = normalizedParetos.get(paretoIndex);
        ArrayList<ArrayList<Double>> reference = normalizedParetos.get(2);

        for (ArrayList<Double> approxSolution : pareto) {
            double minDistance = Double.MAX_VALUE;
            for (ArrayList<Double> refSolution : reference) {
                double d = euclid(approxSolution, refSolution);
                if (d < minDistance) {
                    minDistance = d;
                }
            }
            distance += minDistance;
        }

        return distance / pareto.size();
    }

    /**
     * Calculate Coverage Metric (C-Metric).
     * Measures what fraction of the second front is dominated by the first.
     *
     * @param first Index of first Pareto front
     * @param second Index of second Pareto front
     * @return C-Metric value [0, 1]
     */
    public double C_Metric(int first, int second) {
        if (normalizedParetos.get(first).isEmpty() || normalizedParetos.get(second).isEmpty()) {
            return 0.0;
        }

        int count = 0;
        for (ArrayList<Double> secondParetoSolution : normalizedParetos.get(second)) {
            for (ArrayList<Double> firstParetoSolution : normalizedParetos.get(first)) {
                if (Dominance.compare(firstParetoSolution, secondParetoSolution) == -1) {
                    count++;
                    break;
                }
            }
        }
        return (double) count / normalizedParetos.get(second).size();
    }

    /**
     * Calculate Spacing metric.
     * Measures uniformity of solution distribution along the Pareto front.
     * Lower is better - uniform spacing indicates good diversity.
     *
     * @param paretoIndex Index of the Pareto front
     * @return Spacing value
     */
    public double Spacing(int paretoIndex) {
        ArrayList<ArrayList<Double>> p = normalizedParetos.get(paretoIndex);

        if (p.size() < 2) {
            return 0.0;
        }

        ArrayList<Double> distance = new ArrayList<>();

        // Finding distances of each solution
        distance.add(Math.abs(p.get(1).get(0) - p.get(0).get(0))
                + Math.abs(p.get(0).get(1) - p.get(1).get(1)));
        double total = distance.get(0);
        double prevDist = distance.get(0);

        for (int i = 1; i < p.size() - 1; i++) {
            double nextDist = Math.abs(p.get(i + 1).get(0) - p.get(i).get(0))
                    + Math.abs(p.get(i).get(1) - p.get(i + 1).get(1));
            distance.add(Math.min(prevDist, nextDist));
            total += distance.get(i);
            prevDist = nextDist;
        }

        int last = p.size() - 1;
        distance.add(p.get(last).get(0) - p.get(last - 1).get(0)
                + p.get(last - 1).get(1) - p.get(last).get(1));
        total += distance.get(last);
        double dAvg = total / distance.size();

        double space = 0;
        for (Double d : distance) {
            space += Math.pow(dAvg - d, 2);
        }
        space = Math.sqrt((1.0 / (p.size() - 1)) * space);

        return space;
    }
}
