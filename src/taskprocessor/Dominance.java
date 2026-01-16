package taskprocessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Dominance comparison for multi-objective optimization.
 * Based on the original PerfMet.Dominance class.
 */
public class Dominance {

    /**
     * Tolerance for floating-point comparison.
     * Matches Python's plot_pareto.py tolerance of 1e-9.
     */
    public static final double EPSILON = 1e-9;

    /**
     * Check if two points are equal within the specified tolerance.
     *
     * @param a First point [obj1, obj2]
     * @param b Second point [obj1, obj2]
     * @param epsilon Tolerance for comparison
     * @return true if points are equal within tolerance
     */
    public static boolean arePointsEqual(double[] a, double[] b, double epsilon) {
        return Math.abs(a[0] - b[0]) < epsilon && Math.abs(a[1] - b[1]) < epsilon;
    }

    /**
     * Check if two points are equal using default EPSILON tolerance.
     *
     * @param a First point [obj1, obj2]
     * @param b Second point [obj1, obj2]
     * @return true if points are equal within EPSILON tolerance
     */
    public static boolean arePointsEqual(double[] a, double[] b) {
        return arePointsEqual(a, b, EPSILON);
    }

    /**
     * Check if two points (ArrayList format) are equal within tolerance.
     *
     * @param a First point [obj1, obj2]
     * @param b Second point [obj1, obj2]
     * @param epsilon Tolerance for comparison
     * @return true if points are equal within tolerance
     */
    public static boolean arePointsEqual(ArrayList<Double> a, ArrayList<Double> b, double epsilon) {
        return Math.abs(a.get(0) - b.get(0)) < epsilon && Math.abs(a.get(1) - b.get(1)) < epsilon;
    }

    /**
     * Check if two points (ArrayList format) are equal using default EPSILON tolerance.
     *
     * @param a First point [obj1, obj2]
     * @param b Second point [obj1, obj2]
     * @return true if points are equal within EPSILON tolerance
     */
    public static boolean arePointsEqual(ArrayList<Double> a, ArrayList<Double> b) {
        return arePointsEqual(a, b, EPSILON);
    }

    /**
     * Compare two solutions for dominance.
     * Assumes minimization for both objectives.
     *
     * @param f1 First solution [obj1, obj2]
     * @param f2 Second solution [obj1, obj2]
     * @return -1 if f1 dominates f2, 1 if f2 dominates f1, 0 if incomparable
     */
    public static int compare(double[] f1, double[] f2) {
        // f1 strictly dominates f2 if f1 is better in all objectives
        if (f1[0] < f2[0] && f1[1] < f2[1]) {
            return -1;
        }
        // f2 strictly dominates f1
        else if (f2[0] < f1[0] && f2[1] < f1[1]) {
            return 1;
        }
        // Incomparable
        else {
            return 0;
        }
    }

    /**
     * Compare two solutions for dominance using ArrayList format.
     * Compatible with PerformanceMetrics class.
     */
    public static int compare(ArrayList<Double> f1, ArrayList<Double> f2) {
        if (f1.get(0) < f2.get(0) && f1.get(1) < f2.get(1)) {
            return -1;
        } else if (f2.get(0) < f1.get(0) && f2.get(1) < f1.get(1)) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Get the non-dominated set from a list of solutions.
     * A solution is non-dominated if no other solution dominates it.
     *
     * @param solutions List of solutions, each solution is [obj1, obj2]
     * @return List of non-dominated solutions
     */
    public static List<double[]> getNonDominatedSet(List<double[]> solutions) {
        if (solutions == null || solutions.isEmpty()) {
            return new ArrayList<>();
        }

        List<double[]> nonDominated = new ArrayList<>();

        for (double[] candidate : solutions) {
            boolean isDominated = false;
            List<double[]> toRemove = new ArrayList<>();

            for (double[] existing : nonDominated) {
                int comparison = compare(candidate, existing);

                if (comparison == 1) {
                    // candidate is dominated by existing
                    isDominated = true;
                    break;
                } else if (comparison == -1) {
                    // candidate dominates existing, mark for removal
                    toRemove.add(existing);
                }
            }

            if (!isDominated) {
                nonDominated.removeAll(toRemove);
                // Check if candidate is not already in the set (avoid duplicates)
                // Uses tolerance-based comparison for floating-point safety
                boolean exists = false;
                for (double[] sol : nonDominated) {
                    if (arePointsEqual(sol, candidate)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    nonDominated.add(candidate);
                }
            }
        }

        return nonDominated;
    }

    /**
     * Get the non-dominated set from ArrayList format.
     */
    public static ArrayList<ArrayList<Double>> getNonDominatedSetArrayList(ArrayList<ArrayList<Double>> solutions) {
        if (solutions == null || solutions.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<ArrayList<Double>> nonDominated = new ArrayList<>();

        for (ArrayList<Double> candidate : solutions) {
            boolean isDominated = false;
            ArrayList<ArrayList<Double>> toRemove = new ArrayList<>();

            for (ArrayList<Double> existing : nonDominated) {
                int comparison = compare(candidate, existing);

                if (comparison == 1) {
                    isDominated = true;
                    break;
                } else if (comparison == -1) {
                    toRemove.add(existing);
                }
            }

            if (!isDominated) {
                nonDominated.removeAll(toRemove);
                // Uses tolerance-based comparison for floating-point safety
                boolean exists = false;
                for (ArrayList<Double> sol : nonDominated) {
                    if (arePointsEqual(sol, candidate)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    nonDominated.add(candidate);
                }
            }
        }

        return nonDominated;
    }
}
