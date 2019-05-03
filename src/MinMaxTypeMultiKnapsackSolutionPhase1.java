package src;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.*;

public class MinMaxTypeMultiKnapsackSolutionPhase1 extends MinMaxTypeMultiKnapsackSolution {
    @Override
    public double violations(int b) {
        double sumViolation = 0;

        if (nTypePerBin[b] == 0) {
            return 0;
        }

        sumViolation += Math.max(0, sumW[b] - bins[b].getCapacity());
        sumViolation += Math.max(0, sumP[b] - bins[b].getP());
        sumViolation += Math.max(0, nTypePerBin[b] - bins[b].getT());
        sumViolation += Math.max(0, nClassPerBin[b] - bins[b].getR());
        sumViolation += notInB[b];


        if (bins[b].getMinLoad() - sumW[b] > 0) {
            sumViolation += 0.5;
        }
        //sumViolation += Math.max(0, bins[b].getMinLoad() - sumW[b])/maxLoad;
        return sumViolation;
    }

    @Override
    public double violations(ArrayList<Integer> a, int b) {
        double sumViolation = 0;
        double sumW = 0;
        double sumP = 0;
        int nType = 0;
        int nClass = 0;
        HashSet<Integer> type = new HashSet<Integer>();
        HashSet<Integer> clas = new HashSet<Integer>() ;

        int t = 0, r = 0;

        HashSet<Integer> binIndices;

        for (int i : a) {
            binIndices = newBinIndices[i];
            if (!binIndices.contains(bins[b].getId())) {
                sumViolation += 1;
            }

            sumW += items[i].getW();
            sumP += items[i].getP();
            t = items[i].getT();
            r = items[i].getR();
            if (!type.contains(t)) {
                nType += 1;
                type.add(t);
            }

            if (!clas.contains(r)) {
                nClass += 1;
                clas.add(r);
            }
        }

        sumViolation += Math.max(0, sumW - bins[b].getCapacity());
        sumViolation += Math.max(0, sumP - bins[b].getP());
        sumViolation += Math.max(0, nType - bins[b].getT());
        sumViolation += Math.max(0, nClass - bins[b].getR());

        if (bins[b].getMinLoad() - sumW > 0) {
            sumViolation += 0.5;
        }

        return sumViolation;
    }

    public double getAssignDelta(int newBin, int i) {
        int oldBin = take[i];
        if (oldBin == newBin) {
            return 0;
        }
        double newSumVOfB = 0, oldSumVOfB = 0;
        double newSumVOfA = 0, oldSumVOfA = 0;
        int t = items[i].getT();
        int r = items[i].getR();
        int deltaTypeB = 0, deltaTypeA = 0;
        int deltaClassB = 0, deltaClassA = 0;
        int deltaNotInB = 0, deltaNotInA = 0;
        HashSet<Integer> binIndices = newBinIndices[i];

        if (oldBin != NOT_USE_FOREVER) {
            oldSumVOfB = violations(oldBin);
            sumW[oldBin] -= items[i].getW();
            sumP[oldBin] -= items[i].getP();
            if (!binIndices.contains(bins[oldBin].getId())) {
                notInB[oldBin] -= 1;
                deltaNotInB = 1;
            }
            if (typePerBin[oldBin].get(t) == 1) {
                nTypePerBin[oldBin] -= 1;
                deltaTypeB = 1;
            }
            if (classPerBin[oldBin].get(r) == 1) {
                nClassPerBin[oldBin] -= 1;
                deltaClassB = 1;
            }
            nItemPerBin[oldBin]--;
            newSumVOfB = violations(oldBin);
        }

        if (newBin != NOT_USE_FOREVER) {
            oldSumVOfA = violations(newBin);
            sumW[newBin] += items[i].getW();
            sumP[newBin] += items[i].getP();
            if (!binIndices.contains(bins[newBin].getId())) {
                notInB[newBin] += 1;
                deltaNotInA = -1;
            }
            if (!typePerBin[newBin].containsKey(t)) {
                nTypePerBin[newBin] += 1;
                deltaTypeA = -1;
            }
            if (!classPerBin[newBin].containsKey(r)) {
                nClassPerBin[newBin] += 1;
                deltaClassA = -1;
            }
            nItemPerBin[newBin]++;

            newSumVOfA = violations(newBin);
        }

        if (oldBin != NOT_USE_FOREVER) {
            sumW[oldBin] += items[i].getW();
            sumP[oldBin] += items[i].getP();
            nTypePerBin[oldBin] += deltaTypeB;
            nClassPerBin[oldBin] += deltaClassB;
            notInB[oldBin] += deltaNotInB;
            nItemPerBin[oldBin]++;
        }

        if (newBin != NOT_USE_FOREVER) {
            sumW[newBin] -= items[i].getW();
            sumP[newBin] -= items[i].getP();
            nTypePerBin[newBin] += deltaTypeA;
            nClassPerBin[newBin] += deltaClassA;
            notInB[newBin] += deltaNotInA;
            nItemPerBin[newBin]--;
        }

        return (newSumVOfB + newSumVOfA) - (oldSumVOfB + oldSumVOfA);
    }

    private void restartMaintainConstraint(int[][] tabu, ArrayList<Integer> binsUse, ArrayList<Integer> itemsUse) {
        ArrayList <Integer> subItemsUse = new ArrayList <Integer>();
        if (itemsUse.size() > 1000) {
            Collections.shuffle(itemsUse);

            for (int k = 0; k < 250; k++) {
                subItemsUse.add(itemsUse.get(k));
            }
        } else {
            subItemsUse.addAll(itemsUse);
        }

        Random rand = new Random();

        for (int i : subItemsUse) {
            java.util.ArrayList<Integer> L = new java.util.ArrayList<Integer>();
            ArrayList<Integer>  binIndices = new ArrayList<Integer>(newBinIndices[i]);
            Collections.shuffle(binIndices, new Random(System.nanoTime()));
            for (int b : binIndices) {
                if (getAssignDelta(b, i) <= 0) L.add(b);
            }

            if (L.size() == 0) continue;

            int idx = rand.nextInt(L.size());
            assignPropagate(i, L.get(idx));
        }

        for (int i = 0; i < tabu.length; i++) {
            for (int j = 0; j < tabu[i].length; j++)
                tabu[i][j] = -1;
        }

    }

    public int useNewBin(int oldBin, HashSet<Integer> binNotUse) {
        ArrayList<Integer> oldBinList = new ArrayList<Integer>();
        double minVio = Double.MAX_VALUE;
        int newBin = -1;
        for (int b : binsUse) {
            if (binNotUse.contains(b)) {
                if (violations(oldBinList, b) < minVio) {
                    newBin = b;
                }
            }
        }
        for (int i : itemsUse) {
            if (take[i] == oldBin) {
                take[i] = newBin;
            }
        }
        return newBin;
    }

    public void updateBest() {

    }

    public void assignPropagate(int i, int newBin) {
        int oldBin = take[i];
        take[i] = newBin;

        int t = items[i].getT();
        int r = items[i].getR();
        HashSet<Integer> binIndices = newBinIndices[i];

        if (oldBin != NOT_USE_FOREVER) {
            sumW[oldBin] -= items[i].getW();
            sumP[oldBin] -= items[i].getP();
            if (!binIndices.contains(bins[oldBin].getId())) {
                notInB[oldBin] -= 1;
            }
            if (typePerBin[oldBin].get(t) == 1) {
                nTypePerBin[oldBin] -= 1;
            }
            if (classPerBin[oldBin].get(r) == 1) {
                nClassPerBin[oldBin] -= 1;
            }
            nItemPerBin[oldBin]--;
        }

        if (newBin != NOT_USE_FOREVER) {
            sumW[newBin] += items[i].getW();
            sumP[newBin] += items[i].getP();
            if (!binIndices.contains(bins[newBin].getId())) {
                notInB[newBin] += 1;
            }
            if (!typePerBin[newBin].containsKey(t)) {
                nTypePerBin[newBin] += 1;
            }
            if (!classPerBin[newBin].containsKey(r)) {
                nClassPerBin[newBin] += 1;
            }
            nItemPerBin[newBin]++;
        }
    }

    @Override
    public void tabuSearch(int tabulen, int maxTime, int maxIter, int maxStable, ArrayList<Integer> binsUse, ArrayList<Integer> itemsUse) {
        double t0 = System.currentTimeMillis();
        int minB = Integer.MAX_VALUE, maxB = Integer.MIN_VALUE;
        int minI = Integer.MAX_VALUE, maxI = Integer.MIN_VALUE;

        for (int b : binsUse) {
            if (minB > b) minB = b;
            if (maxB < b) maxB = b;
        }
        for (int i : itemsUse) {
            if (minI > i) minI = i;
            if (maxI < i) maxI = i;
        }
        int DB = maxB - minB;
        int DI = maxI - minI;
        // System.out.println("n = " + n + ", D = " + D);
        int tabu[][] = new int[DI + 1][DB + 1];
        for (int i = 0; i <= DI; i++)
            for (int v = 0; v <= DB; v++)
                tabu[i][v] = -1;

        int it = 0;
        maxTime = maxTime * 1000;// convert into milliseconds

        double best = violations();
        double sumV = best;
        int[] x_best = new int[maxI + 1];

        for (int i : itemsUse) x_best[i] = take[i];

        System.out.println("TabuSearch, init S = " + violations());
        int nic = 0;
        ArrayList<AssignMove> moves = new ArrayList<AssignMove>();
        Random R = new Random();
        while (it < maxIter && System.currentTimeMillis() - t0 < maxTime
                && sumV > 0) {
            int sel_i = -1;
            int sel_v = -1;
            double minDelta = Double.MAX_VALUE;
            moves.clear();

            ArrayList<Integer> maxVioBin = new ArrayList<Integer>();
            double maxVio = -1;
            for (int b : binsUse) {
                double vio = violations(b);

                if (maxVio < vio) {
                    maxVio = vio;
                    maxVioBin.clear();
                    maxVioBin.add(b);
                } else if (maxVio == vio) {
                    maxVioBin.add(b);
                }
            }
            int b_ucv = maxVioBin.get(R.nextInt(maxVioBin.size()));

            ArrayList<Integer> itemUcvs = new ArrayList<Integer>();
            for (int i : itemsUse) {
                if (take[i] == b_ucv) {
                    itemUcvs.add(i);
                }
            }
            //System.out.println(itemUcvs);
            int item_ucv = itemUcvs.get(R.nextInt(itemUcvs.size()));
            for (int b : newBinIndices[item_ucv]) {
                double delta = getAssignDelta(b, item_ucv);

                if (tabu[item_ucv - minI][b - minB] <= it || sumV + delta < best) {
                    if (delta < minDelta) {
                        minDelta = delta;
                        moves.clear();
                        moves.add(new AssignMove(b, take[item_ucv], item_ucv));
                    } else if (delta == minDelta) {
                        moves.add(new AssignMove(b, take[item_ucv], item_ucv));
                    }
                }
            }

            if (moves.size() <= 0) {
                restartMaintainConstraint(tabu, binsUse, itemsUse);
                System.out.println("TabuSearch::restart..... S = " + violations());
                nic = 0;
            } else {
                // perform the move
                AssignMove m = moves.get(R.nextInt(moves.size()));
                sel_i = m.i;
                sel_v = m.newBin;
                assignPropagate(sel_i, sel_v);
                tabu[sel_i - minI][sel_v - minB] = it + tabulen;
                
                if(Math.abs(minDelta) < 1e-6) minDelta = 0;

                sumV += minDelta;
                if (it % 100 == 0) {
                    System.out.println("Step " + it + ", S = " + sumV
                                       + ", best = " + best + ", delta = " + minDelta
                                       + ", nic = " + nic);
                }

                // update best
                if (sumV < best) {
                    best = sumV;
                    for (int i : itemsUse) x_best[i] = take[i];
                }

                //if (minDelta >= 0) {
                if (sumV >= best) {
                    nic++;
                    if (nic > maxStable) {
                        restartMaintainConstraint(tabu, binsUse, itemsUse);
                        System.out.println("TabuSearch::restart..... S = " + violations());
                        nic = 0;
                    }
                } else {
                    nic = 0;
                }
            }
            it++;
        }
        System.out.println("Step " + it + ", S = " + violations());
        for (int i : itemsUse) take[i] = x_best[i];
    }

    public void writeSolution(int take[]) {
        String[] tmp = inputPath.split("/");
        String fileName = tmp[tmp.length - 1].split("\\.")[0] + ".out";
        outputPath = inputPath.substring(0, inputPath.lastIndexOf('/')) + "/pretrain/" + fileName;
        System.out.println(outputPath);
        try (FileWriter fileWriter = new FileWriter(outputPath)) {
            for (int i = 0; i < n; i++) {
                if (take[i] == NOT_USE_FOREVER) {
                    fileWriter.write(take[i] + " ");
                } else {
                    fileWriter.write(bins[take[i]].getId() + " ");
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public HashSet<Integer> getBinsNotUse() {
        HashSet<Integer> result = new HashSet<Integer>();

        for (int i = 0; i < n; i++) {
            if (take[i] != NOT_USE_FOREVER) {
                result.add(take[i]);
            }
        }

        HashSet<Integer> binNotUse = new HashSet<Integer>();

        for (int b : binsUse) {
            if (!result.contains(b)) {
                binNotUse.add(b);
            }
        }
        return (HashSet<Integer>)binNotUse.clone();
    }

    public HashSet<Integer> getSolutionBins() {
        violations();
        HashSet<Integer> result = new HashSet<Integer>();

        for (int i = 0; i < n; i++) {
            if (take[i] != NOT_USE_FOREVER) {
                result.add(take[i]);
            }
        }
        HashSet<Integer> solutionBins = new HashSet<Integer>();
        for (int b : result) {
            if (violations(b) == 0) {
                solutionBins.add(b);
            }
        }
        return (HashSet<Integer>)solutionBins.clone();
    }

    public HashSet<Integer> getClassNotArrange() {
        violations();
        HashSet<Integer> classNotArrange = new HashSet<Integer>();
        for (int b : binsUse) {
            if (violations(b) > 0) {
                for (int r : classPerBin[b].keySet() ) {
                    classNotArrange.add(r);
                }
            }
        }
        return classNotArrange;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // solution.loadData("src/khmtk60/miniprojects/multiknapsackminmaxtypeconstraints/MinMaxTypeMultiKnapsackInput.json");
        String dataset_path = "dataset/MinMaxTypeMultiKnapsackInput-1000.json";
        int tabulen = 10;
        MinMaxTypeMultiKnapsackSolutionPhase1 solution = new MinMaxTypeMultiKnapsackSolutionPhase1();
        solution.loadData(
            dataset_path);
        solution.preprocess();
        solution.info();
        //solution.initModel();
        solution.loadPretrainedModel();
        //solution.tabuSearch(10, 5000, 50000, 1000, solution.getBinsUse(), solution.getItemsUse()); // Cho tap du lieu 51004418316727.json
        //solution.writeSolution();
        HashSet<Integer> classNotArrage = solution.getClassNotArrange();
        HashSet<Integer> tabu = solution.getSolutionBins();
        HashSet<Integer> binNotUse = solution.getBinsNotUse();
        solution.printSolution();
        System.out.println(classNotArrage);
        //classNotArrage.clear();
        //classNotArrage.add(16);

        HashMap<Integer, HashSet<Integer>> tempBinUse = new HashMap<Integer, HashSet<Integer>>();
        for (int r : classNotArrage) {
            for (int i = 0; i < solution.getN(); i++) {
                if (solution.getItems()[i].getR() == r) {
                    if (!tempBinUse.containsKey(r)) {
                        tempBinUse.put(r, new HashSet<Integer>());
                    }
                    int b = solution.getTake()[i];
                    if (b != solution.NOT_USE_FOREVER) {
                        tempBinUse.get(r).add(b);
                        tabu.remove(b);
                    }
                }
            }
        }
        System.out.println(tabu);
        Scanner s = new Scanner(System.in);
        System.out.println("Press enter to continue.....");
        s.nextLine();

        for (int r : classNotArrage) {
            System.out.println("Processing r = " + r);
            for (int k = 0; k < 5; k++) {
                solution = new MinMaxTypeMultiKnapsackSolutionPhase1();
                solution.loadData(dataset_path);
                solution.preprocess();
                solution.loadPretrainedModel();
                int oldTake[] = solution.getTake().clone();

                solution.setAvailR(r);
                solution.loadPretrainedModel();

                //solution.tabuSearch(5, 500, 150000, 1000); // Cho tap du lieu 41525782483156.json
                solution.tabuSearch(10, 5000, 20000, 1000, solution.getBinsUse(), solution.getItemsUse()); // Cho tap du lieu 51004418316727.json

                if (solution.violations() == 0) {
                    HashSet<Integer> solutionBins = solution.getSolutionBins();
                    HashSet<Integer> intersect = new HashSet<Integer>(solutionBins);
                    intersect.retainAll(tabu);
                    if (intersect.isEmpty()) {
                        for (Map.Entry<Integer, HashSet<Integer>> entry : tempBinUse.entrySet()) {
                            if (entry.getKey() == r) continue;
                            for (int b : entry.getValue()) {
                                if (solutionBins.contains(b)) {
                                    int newBin = solution.useNewBin(b, binNotUse);
                                    binNotUse.remove(b);
                                    binNotUse.add(newBin);
                                    tempBinUse.get(entry.getKey()).remove(b);
                                    tempBinUse.get(entry.getKey()).add(newBin);
                                }
                            }
                        }
                        int newTake[] = solution.getTake().clone();
                        for (int i = 0; i < newTake.length; i++) {
                            if (newTake[i] != -1) {
                                oldTake[i] = newTake[i];
                            }
                        }
                        tempBinUse.remove(r);
                        tabu.addAll(solutionBins);
                        solution.writeSolution(oldTake);
                        binNotUse.removeAll(solutionBins);
                        break;
                    } else {

                    }
                }
            }
        }

        //solution.printSolution();
    }
}
