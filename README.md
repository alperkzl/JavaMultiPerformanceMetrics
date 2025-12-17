# Java Performance Metrics for Multi-Objective Evolutionary Algorithms

A Java library for calculating performance metrics used to evaluate Pareto fronts in multi-objective evolutionary algorithms (MOEAs). This library provides implementations of standard quality indicators including Hypervolume, Inverse Generational Distance, Spacing, and Coverage Metric.

## Overview

Multi-objective optimization algorithms produce a set of trade-off solutions known as a Pareto front. Evaluating the quality of these fronts requires specialized metrics that assess both **convergence** (how close solutions are to the true optimal front) and **diversity** (how well-spread solutions are across the objective space).

This library implements the most commonly used performance metrics for 2-objective optimization problems.

## Project Structure

```
Java-Performance-Metrics/
├── PerfMet/                          # Source code package
│   ├── PM_Main.java                  # Entry point and demonstration
│   ├── PerformanceMetrics.java       # Core metrics calculations
│   ├── Dominance.java                # Dominance comparison logic
│   ├── FitnessComparator.java        # Fitness value sorting
│   └── MyFileReader.java             # Pareto front file I/O
└── paretoFiles/                      # Sample data files
    ├── A.txt                         # Sample Pareto front A
    ├── B.txt                         # Sample Pareto front B
    └── True_Pareto.txt               # Reference/true Pareto front
```

## Implemented Metrics

### 1. Hypervolume (HV)

**Purpose:** Measures the volume of objective space covered by the Pareto front with respect to a reference point.

**How it works:**
- Solutions are normalized to [0, 1] range
- Reference point is set to (1.0, 1.0)
- Calculates the area dominated by the Pareto front
- Uses a linear sweep algorithm on sorted solutions

**Interpretation:** Higher is better - larger covered area indicates a better solution set.

```java
double hv = pm.HV(paretoIndex);
```

### 2. Inverse Generational Distance (IGD)

**Purpose:** Measures convergence by calculating the average distance from each point in the true Pareto front to its nearest point in the approximation.

**How it works:**
- For each solution in the true Pareto front, find the minimum Euclidean distance to any solution in the approximation
- Average all minimum distances

**Formula:**
```
IGD = (1/|True_Pareto|) * Σ min(euclidean_distance(true_solution, approx_solution))
```

**Interpretation:** Lower is better - solutions closer to the true front indicate better convergence.

```java
double igd = pm.IGD(paretoIndex);
```

### 3. Spacing Metric

**Purpose:** Measures the uniformity of solution distribution along the Pareto front.

**How it works:**
- Calculates Manhattan distances between consecutive solutions
- Computes the standard deviation of these distances
- Uniform spacing results in lower values

**Interpretation:** Lower is better - uniform spacing indicates good diversity.

```java
double spacing = pm.Spacing(paretoIndex);
```

### 4. Coverage Metric (C-Metric)

**Purpose:** Binary comparison metric that measures what fraction of one Pareto front is dominated by another.

**How it works:**
- For each solution in front B, check if any solution in front A dominates it
- A solution dominates another if it is better in all objectives

**Formula:**
```
C(A,B) = |{solutions in B dominated by A}| / |B|
```

**Interpretation:**
- Range: [0, 1]
- C(A,B) = 1 means A completely dominates B
- Note: C(A,B) ≠ C(B,A) (asymmetric)

```java
double coverage = pm.C_Metric(firstIndex, secondIndex);
```

## Usage

### Basic Example

```java
import PerfMet.*;

public class Example {
    public static void main(String[] args) {
        // Step 1: Load Pareto fronts from files
        ArrayList<ArrayList<Double>> A = MyFileReader.getPareto("paretoFiles/A.txt");
        ArrayList<ArrayList<Double>> B = MyFileReader.getPareto("paretoFiles/B.txt");
        ArrayList<ArrayList<Double>> trueFront = MyFileReader.getPareto("paretoFiles/True_Pareto.txt");

        // Step 2: Create container for all fronts
        ArrayList<ArrayList<ArrayList<Double>>> allParetos = new ArrayList<>();
        allParetos.add(A);          // Index 0
        allParetos.add(B);          // Index 1
        allParetos.add(trueFront);  // Index 2 (reference front)

        // Step 3: Initialize the metrics engine
        PerformanceMetrics pm = new PerformanceMetrics(allParetos);

        // Step 4: Calculate and display metrics
        System.out.println("Hypervolume of A: " + pm.HV(0));
        System.out.println("Hypervolume of B: " + pm.HV(1));
        System.out.println("IGD of A: " + pm.IGD(0));
        System.out.println("IGD of B: " + pm.IGD(1));
        System.out.println("Spacing of A: " + pm.Spacing(0));
        System.out.println("Spacing of B: " + pm.Spacing(1));
        System.out.println("C-Metric(A,B): " + pm.C_Metric(0, 1));
        System.out.println("C-Metric(B,A): " + pm.C_Metric(1, 0));
    }
}
```

### Input File Format

Pareto front files should be semicolon-delimited text files with the following format:

```
Pareto Front: (f1; f2)
7544.37 ; 23.05
7807.11 ; 20.55
26584.23 ; 2.62
...
```

- First line is a header (skipped during parsing)
- Each subsequent line contains two objective values separated by `;`
- Values can have decimal points

## Architecture

```
PM_Main (Entry Point)
    │
    ├──→ MyFileReader.getPareto()     ← Load Pareto fronts from files
    │
    ├──→ PerformanceMetrics Constructor
    │    ├──→ FitnessComparator       ← Sort solutions by f1
    │    ├──→ findMinMax()            ← Determine normalization bounds
    │    └──→ normalize()             ← Scale to [0,1] range
    │
    └──→ Calculate Metrics
         ├──→ HV()        → Hypervolume calculation
         ├──→ IGD()       → Inverse Generational Distance
         ├──→ Spacing()   → Distribution uniformity
         └──→ C_Metric()  → Coverage comparison
              └──→ Dominance.compare()
```

## Data Structures

| Structure | Description |
|-----------|-------------|
| `ArrayList<Double>` | Single solution with 2 objective values [f1, f2] |
| `ArrayList<ArrayList<Double>>` | One Pareto front (collection of solutions) |
| `ArrayList<ArrayList<ArrayList<Double>>>` | All Pareto fronts |

## Key Classes

### PerformanceMetrics
The core class that handles:
- Sorting Pareto fronts by first objective
- Min-max normalization to [0, 1] range
- All metric calculations

### Dominance
Implements dominance comparison:
- Returns `-1` if first solution dominates second
- Returns `1` if second solution dominates first
- Returns `0` if incomparable

### FitnessComparator
Java Comparator for sorting solutions by first objective value in ascending order.

### MyFileReader
Handles file I/O operations for reading Pareto front data from text files.

## Technical Notes

- **Objective Space:** Designed for 2-objective optimization problems
- **Optimization Direction:** Assumes minimization for all objectives
- **Normalization:** All solutions are normalized to [0, 1] range before metric calculation
- **Reference Front:** IGD calculation expects the true Pareto front at index 2

## Metric Interpretation Summary

| Metric | Measures | Better Value |
|--------|----------|--------------|
| Hypervolume | Convergence + Diversity | Higher |
| IGD | Convergence | Lower |
| Spacing | Diversity/Uniformity | Lower |
| C-Metric | Relative Dominance | Higher (for first front) |

## References

- Zitzler, E., & Thiele, L. (1999). Multiobjective evolutionary algorithms: a comparative case study and the strength Pareto approach. IEEE Transactions on Evolutionary Computation.
- Deb, K. (2001). Multi-objective optimization using evolutionary algorithms. John Wiley & Sons.
