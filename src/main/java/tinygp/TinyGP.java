/*
 * Program:   tiny_gp.java
 *
 * Author:    Riccardo Poli (email: rpoli@essex.ac.uk)
 *
 */
package tinygp;

import java.util.*;
import java.io.*;
import java.lang.Math;

public class TinyGP {
    // CONST
    final int
            ADD = 110,
            SUB = 111,
            MUL = 112,
            DIV = 113,
            SIN = 114,
            COS = 115,
            OPERATIONS_START = ADD,
            MULTI_ARG_OPERATIONS_END = DIV,
            SINGLE_ARG_OPERATIONS_START = SIN,
            OPERATIONS_END = COS;
    final int
            MAX_LEN = 10000,
            POP_SIZE = 100000,
            DEPTH = 5,
            GENERATIONS = 100,
            T_SIZE = 2;
    public final double
            MUT_PER_NODE = 0.05,
            CROSSOVER_PROB = 0.9,
            EPSILON = -1e-5;

    // VAR
    ExcelManager excelManager = new ExcelManager();
    Optimizer optimizer = new Optimizer();
    static Random rd = new Random();

    char[][] pop;
    double[] fitness;
    double[] x = new double[OPERATIONS_START];
    double minRandom, maxRandom;
    char[] program;
    int PC;
    int varNumber, fitnessCases, randomNumber;
    double fitnessBestPop = 0.0;
    long seed;
    double[][] targets;

    public TinyGP(String fileName, long s ) {
        excelManager.setFilename(fileName);

        fitness =  new double[POP_SIZE];

        seed = s;
        if ( seed >= 0 )
            rd.setSeed(seed);

        setup(fileName);
        for (int i = 0; i < OPERATIONS_START; i++ )
            x[i] = (maxRandom - minRandom) * rd.nextDouble() + minRandom;

        pop = create_random_pop(fitness);
    }

    void setup(String fileName) {
        try {
            BufferedReader in =  new BufferedReader(new FileReader(fileName));
            String line = in.readLine();
            StringTokenizer tokens = new StringTokenizer(line);

            varNumber = Integer.parseInt(tokens.nextToken().trim());
            randomNumber = Integer.parseInt(tokens.nextToken().trim());
            minRandom =	Double.parseDouble(tokens.nextToken().trim());
            maxRandom =  Double.parseDouble(tokens.nextToken().trim());
            fitnessCases = Integer.parseInt(tokens.nextToken().trim());

            if (varNumber + randomNumber >= OPERATIONS_START)
                System.out.println("too many variables and constants");

            targets = new double[fitnessCases][varNumber + 1];

            for (int i = 0; i < fitnessCases; i ++) {
                line = in.readLine();
                tokens = new StringTokenizer(line);
                for (int j = 0; j <= varNumber; j++)
                    targets[i][j] = Double.parseDouble(tokens.nextToken().trim());
            }

            excelManager.setTargets(targets);
            in.close();
        }
        catch(FileNotFoundException e) {
            System.out.println("ERROR: Please provide a data file");
            System.exit(0);
        }
        catch(Exception e) {
            System.out.println("ERROR: Incorrect data format");
            System.exit(0);
        }
    }

    char[][] create_random_pop(double[] fitness) {
        char[][]pop = new char[POP_SIZE][];

        for (int i = 0; i < POP_SIZE; i++) {
            pop[i] = create_random_indiv();
            fitness[i] = fitness_function(pop[i]);
        }
        return pop;
    }

    char [] buffer = new char[MAX_LEN];
    char [] create_random_indiv() {
        int len;

        do{
            len = grow(buffer,0, MAX_LEN, DEPTH);
        } while(len < 0);

        char[] ind = new char[len];
        System.arraycopy(buffer, 0, ind, 0, len );

        return ind;
    }

    int grow(char[] buffer, int pos, int max, int depth) {
        char prim = (char)rd.nextInt(2);

        if (pos >= max) return -1;

        if (pos == 0) prim = 1;

        if (prim == 0 || depth == 0) {
            prim = (char)rd.nextInt(varNumber + randomNumber);
            buffer[pos] = prim;
            return pos + 1;
        }
        else {
            prim = (char)(rd.nextInt(OPERATIONS_END - OPERATIONS_START + 1) + OPERATIONS_START);
            switch (prim) {
                case ADD:
                case SUB:
                case MUL:
                case DIV:
                    buffer[pos] = prim;
                    int one_child = grow(buffer, pos + 1, max, depth - 1);
                    if (one_child < 0)
                        return -1;
                    return grow(buffer, one_child, max, depth - 1);
                case SIN:
                case COS:
                    buffer[pos] = prim;
                    return grow(buffer, pos + 1, max, depth - 1);
                default:
                    return 0; // should never get here
            }
        }
    }

    double fitness_function(char[] p) {
        double result, fit = 0.0;

        for (int i = 0; i < fitnessCases; i ++ ) {
            if (varNumber >= 0)
                System.arraycopy(targets[i], 0, x, 0, varNumber);

            program = p;
            PC = 0;

            result = run();
            fit += Math.abs(result - targets[i][varNumber]);
        }

        return -fit;
    }

    double run() { /* Interpreter */
        char primitive = program[PC++];
        if (primitive < OPERATIONS_START)
            return x[primitive];

        switch (primitive) {
            case ADD : return run() + run();
            case SUB : return run() - run();
            case MUL : return run() * run();
            case SIN : return Math.sin(run());
            case COS : return Math.cos(run());
            case DIV : {
                double num = run(), den = run();
                if (Math.abs(den) <= 0.001)
                    return num;
                else
                    return num / den;
            }
            default:
                return 0.0; // should never get here
        }
    }

    void evolve() {
        char[] newInd;

        printParameters();
        stats(fitness, pop, 0);
        for (int gen = 1; gen < GENERATIONS; gen++) {
            if (fitnessBestPop > EPSILON) {
                excelManager.writeToExcel();

                System.out.print("PROBLEM SOLVED\n");
                System.exit( 0 );
            }
            for (int indivs = 0; indivs < POP_SIZE; indivs ++) {
                if (rd.nextDouble() < CROSSOVER_PROB) {
                    int parent1 = tournament(fitness);
                    int parent2 = tournament(fitness);
                    newInd = crossover(pop[parent1],pop[parent2]);
                }
                else {
                    int parent = tournament(fitness);
                    newInd = mutation(pop[parent]);
                }

                double newFit = fitness_function(newInd);
                int offspring = negativeTournament(fitness);
                pop[offspring] = newInd;
                fitness[offspring] = newFit;
            }

            stats( fitness, pop, gen);
        }

        excelManager.writeToExcel();

        System.out.print("PROBLEM *NOT* SOLVED\n");
        System.exit(1);
    }

    void printParameters() {
        System.out.print("-- TINY GP (Java version) --\n");
        System.out.print("SEED=" + seed + "\nMAX_LEN=" + MAX_LEN +
                "\nPOPSIZE=" + POP_SIZE + "\nDEPTH=" + DEPTH +
                "\nCROSSOVER_PROB=" + CROSSOVER_PROB +
                "\nMUT_PER_NODE=" + MUT_PER_NODE +
                "\nMIN_RANDOM=" + minRandom +
                "\nMAX_RANDOM=" + maxRandom +
                "\nGENERATIONS=" + GENERATIONS +
                "\nTSIZE=" + T_SIZE +
                "\n----------------------------------\n");
    }

    void stats(double[] fitness, char[][] pop, int gen) {
        int best = rd.nextInt(POP_SIZE);
        int nodeCount = 0;
        fitnessBestPop = fitness[best];
        double fitnessAvgPop = 0.0;

        for (int i = 0; i < POP_SIZE; i++) {
            nodeCount += traverse(pop[i],0);
            fitnessAvgPop += fitness[i];
            if (fitness[i] > fitnessBestPop) {
                best = i;
                fitnessBestPop = fitness[i];
            }
        }

        double avgLength = (double)nodeCount / POP_SIZE;
        fitnessAvgPop /= POP_SIZE;

        StringBuilder equation = new StringBuilder();
        System.out.print("Generation=" + gen + " Avg Fitness=" + (-fitnessAvgPop) +
                " Best Fitness=" + (-fitnessBestPop) + " Avg Size=" + avgLength +
                "\nBest Individual: ");
        print_indiv(pop[best], 0, equation);

        String optEquation = optimizer.optimize(equation.toString());

        excelManager.addBestIndividual(optEquation);
        System.out.print(optEquation + "\n");
        System.out.flush();
    }

    int traverse(char[] buffer, int bufferCount) {
        if (buffer[bufferCount] < OPERATIONS_START)
            return ++bufferCount;

        switch (buffer[bufferCount]) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
                return traverse(buffer, traverse(buffer, ++bufferCount));
            case SIN:
            case COS:
                return traverse(buffer, ++bufferCount);
            default:
                return 0; // should never get here
        }
    }

    int print_indiv(char[] buffer, int bufferCounter, StringBuilder eq) {
        int a1 = 0;
        boolean one_arg_function = false;
        if (buffer[bufferCounter] < OPERATIONS_START) {
            if (buffer[bufferCounter] < varNumber)
                eq.append("X").append(buffer[bufferCounter] + 1);
            else
                eq.append(x[buffer[bufferCounter]]);

            return ++bufferCounter;
        }

        switch (buffer[bufferCounter]) {
            case ADD:
                eq.append("(");
                a1 = print_indiv(buffer, ++bufferCounter, eq);
                eq.append(" + ");
                break;
            case SUB:
                eq.append("(");
                a1 = print_indiv(buffer, ++bufferCounter, eq);
                eq.append(" - ");
                break;
            case MUL:
                eq.append("(");
                a1 = print_indiv(buffer, ++bufferCounter, eq);
                eq.append(" * ");
                break;
            case DIV:
                eq.append("(");
                a1 = print_indiv(buffer, ++bufferCounter, eq);
                eq.append(" / ");
                break;
            case SIN:
                eq.append("(sin(");
                a1 = print_indiv(buffer, ++bufferCounter, eq);
                one_arg_function = true;
                eq.append(")");
                break;
            case COS:
                eq.append("(cos(");
                a1 = print_indiv(buffer, ++bufferCounter, eq);
                one_arg_function = true;
                eq.append(")");
                break;
        }

        if(!one_arg_function) {
            int a2 = print_indiv(buffer, a1, eq);
            eq.append(")");
            return a2;
        }

        eq.append(")");
        return a1;
    }

    int tournament(double[] fitness) {
        int best = rd.nextInt(POP_SIZE), competitor;
        double fitnessBest = -1.0e34;

        for (int i = 0; i < T_SIZE; i++) {
            competitor = rd.nextInt(POP_SIZE);
            if (fitness[competitor] > fitnessBest) {
                fitnessBest = fitness[competitor];
                best = competitor;
            }
        }
        return best;
    }

    int negativeTournament(double[] fitness) {
        int worst = rd.nextInt(POP_SIZE), competitor;
        double fitnessWorst = 1e34;

        for (int i = 0; i < T_SIZE; i++) {
            competitor = rd.nextInt(POP_SIZE);
            if (fitness[competitor] < fitnessWorst) {
                fitnessWorst = fitness[competitor];
                worst = competitor;
            }
        }

        return worst;
    }

    char[] crossover(char[] parent1, char[] parent2) {
        int xo1start, xo1end, xo2start, xo2end;
        char[] offspring;
        int len1 = traverse(parent1, 0);
        int len2 = traverse(parent2, 0);

        xo1start = rd.nextInt(len1);
        xo1end = traverse( parent1, xo1start);

        xo2start = rd.nextInt(len2);
        xo2end = traverse(parent2, xo2start);

        int lenoff = xo1start + (xo2end - xo2start) + (len1-xo1end);

        offspring = new char[lenoff];

        System.arraycopy(parent1, 0, offspring, 0, xo1start);
        System.arraycopy(parent2, xo2start, offspring, xo1start, (xo2end - xo2start));
        System.arraycopy(parent1, xo1end, offspring,xo1start + (xo2end - xo2start), (len1 - xo1end));

        return offspring;
    }

    char[] mutation(char[] parent) {
        int len = traverse(parent, 0 );
        char[] parentCopy = new char[len];

        System.arraycopy(parent, 0, parentCopy, 0, len);
        for (int i = 0; i < len; i ++ ) {
            if (rd.nextDouble() < MUT_PER_NODE) {
                if (parentCopy[i] < OPERATIONS_START)
                    parentCopy[i] = (char)rd.nextInt(varNumber + randomNumber);
                else
                    switch (parentCopy[i]) {
                        case ADD:
                        case SUB:
                        case MUL:
                        case DIV:
                            parentCopy[i] = (char)(rd.nextInt(MULTI_ARG_OPERATIONS_END - OPERATIONS_START + 1) + OPERATIONS_START);
                        case SIN:
                        case COS:
                            parentCopy[i] = (char)(rd.nextInt(OPERATIONS_END - SINGLE_ARG_OPERATIONS_START + 1) + SINGLE_ARG_OPERATIONS_START);
                    }
            }
        }

        return parentCopy;
    }
}