package khmtk60.miniprojects.multiknapsackminmaxtypeconstraints.model;

import java.util.*;

import localsearch.model.IConstraint;
import localsearch.model.VarIntLS;
import localsearch.search.MoveType;
import localsearch.search.OneVariableValueMove;


public class MinMaxTypeMultiKnapsackSolution {
	private int NOT_USE = -1;
	private int take[];
	private int n;
	private int m;
	private MinMaxTypeMultiKnapsackInputItem items[];
	private MinMaxTypeMultiKnapsackInputBin bins[];
	private double sumW[];
	private double sumP[];
	private HashMap<Integer, Integer> typePerBin[];
	private HashMap<Integer, Integer> classPerBin[];
	private int nTypePerBin[];
	private int nClassPerBin[];
	private int notInB[];
	private double sumV = 0;
	private java.util.Random rand = null;
	private HashMap<Integer, Double> sumWPerR = new HashMap<Integer, Double>();
	private HashMap<Integer, ArrayList<Integer>> itemPerR = new HashMap<Integer, ArrayList<Integer>>();
	private HashMap<Integer, HashSet<Integer>> intersection = new HashMap<Integer, HashSet<Integer>>();
	
	private ArrayList<Integer> availR = new ArrayList<Integer>();
	private double maxWR = -1;
	private double maxLoad = -1;
	
	private String inputPath;
	private String outputPath;
	
	public void info() {
		double tmp = 0;
		int maxR = -1;
        int r = -1;
        double w = -1;
        HashSet<Integer> binIndices;
		
		double minLoad = Double.MAX_VALUE;
        

		for(int b = 0; b < m; b++) {
			if(maxR < bins[b].getR()) {
				maxR = bins[b].getR();
			}
			if(minLoad > bins[b].getMinLoad()) {
				minLoad = bins[b].getMinLoad();
			}
			if(maxLoad < bins[b].getMinLoad()) {
				maxLoad = bins[b].getMinLoad();
			}
		}
		sumWPerR.clear();
        itemPerR.clear();
        intersection.clear();

		for (int i = 0; i < n; i++) {
			w = items[i].getW();
			r = items[i].getR();
			binIndices = items[i].getBinIndices();
			tmp += w;
			if(!sumWPerR.containsKey(r)) {
				sumWPerR.put(r, w);
				itemPerR.put(r, new ArrayList<Integer>());
				intersection.put(r, new HashSet<Integer>(binIndices));
			} else {
				sumWPerR.replace(r, sumWPerR.get(r) + w);
				intersection.get(r).retainAll(binIndices);
			}
			itemPerR.get(r).add(i);
		}
			
		for (Map.Entry<Integer, HashSet<Integer>> entry : intersection.entrySet()) {
			r = entry.getKey();
			double minLoadR = Double.MAX_VALUE; 
			for (int i = 0; i < n; i++) {
				if(items[i].getR() == r) {
					for(int x: items[i].getBinIndices()) {
						if(minLoadR > bins[x].getMinLoad()) {
							minLoadR = bins[x].getMinLoad();
						}
					}
				}
			}
		    System.out.print("R = " + r + ", W = " + sumWPerR.get(r) + ", minLoadR = " + minLoadR + ", Indices = ");
		    for(int b: entry.getValue()) {
		    	System.out.print(String.format("%.1f", bins[b].getMinLoad()) + " ");
		    }
		    System.out.println();
		    
		}
		System.out.println("Number of items = " + n);
		System.out.println("Number of bins = " + m);
		System.out.println("Sum W (all Bins) = " + tmp);
		System.out.println("Max R (all Bins) = " + maxR);
		System.out.println("Min minload (all Bins) = " + minLoad);
	}
	
	public void preprocess() {
        NOT_USE = m;

        double minLoad = Double.MAX_VALUE;
		for(int i = 0; i < m; i++) {
			if(minLoad > bins[i].getMinLoad()) {
				minLoad = bins[i].getMinLoad();
			}
		}
		int r;
		double w;
		sumWPerR.clear();
		availR.clear();
		for (int i = 0; i < n; i++) {
			r = items[i].getR();
			w = items[i].getW();
			if(!sumWPerR.containsKey(r)) {
				sumWPerR.put(r, w);
			} else {
				sumWPerR.replace(r, sumWPerR.get(r) + w);
			}
		}
		for (Map.Entry<Integer, Double> entry : sumWPerR.entrySet()) {
			r = entry.getKey();
			double minLoadR = Double.MAX_VALUE; 
			for (int i = 0; i < n; i++) {
				if(items[i].getR() == r) {
					for(int x: items[i].getBinIndices()) {
						if(minLoadR > bins[x].getMinLoad()) {
							minLoadR = bins[x].getMinLoad();
						}
					}
				}
			}
			if(entry.getValue() >= minLoadR) {
				availR.add(r);
			}
		}
		System.out.println(availR);
	}
	
	
	public void initModel() {
		preprocess();
		info();
		
		rand = new Random();
		take = new int[n];
		sumW = new double[m];
		sumP = new double[m];
		typePerBin = new HashMap[m];
		classPerBin = new HashMap[m];
		nTypePerBin = new int[m];
		nClassPerBin = new int[m];
		notInB = new int[m];
		

		for (int b = 0; b < m; b++) {
			typePerBin[b] = new HashMap<Integer, Integer>();
			classPerBin[b] = new HashMap<Integer, Integer>();
		}
		
		Arrays.sort(bins, new Comparator<MinMaxTypeMultiKnapsackInputBin>() {

			@Override
			public int compare(MinMaxTypeMultiKnapsackInputBin o1, MinMaxTypeMultiKnapsackInputBin o2) {
				// TODO Auto-generated method stub
				double r = o2.getMinLoad() - o1.getMinLoad();
				if (r > 0) {
					return 1;
				} else if (r < 0) {
					return -1;
				} else {
					return 0;
				}
			}
		});
		
		HashMap<Integer, Double> sumWPerBin = new HashMap<Integer, Double>();
		
		for (int i = 0; i < n; i++) {
            if(!availR.contains(items[i].getR())) {
                take[i] = NOT_USE;
                continue;
            }
            
            double w = items[i].getW();
            
            for(int b = m-1; b >= 0; b--) {
                if(items[i].getBinIndices().contains(bins[b].getId())) {
                	if(!sumWPerBin.containsKey(b)) {
                		sumWPerBin.put(b, w);
                	} else {
                		if(sumWPerBin.get(b) + w > bins[b].getCapacity()) {
                    		continue;
                    	}
                		sumWPerBin.replace(b, sumWPerBin.get(b) + w);
                	}
                    take[i] = b;
                    //System.out.println("R = " + items[i].getR());
                    //System.out.println("B = " + b + ", min load = " + bins[b].getMinLoad());
                    break;
                }
            }
		}
		
		printSolution();
		System.out.println("Init S = " + violations());
	}

	public void resetArray(double a[]) {
		for (int i = 0; i < a.length; i++) {
			a[i] = 0;
		}
	}

	public void resetArray(int a[]) {
		for (int i = 0; i < a.length; i++) {
			a[i] = 0;
		}
	}

	public double violations() {
		double sumViolation = 0;
		resetArray(sumW);
		resetArray(sumP);
		resetArray(notInB);
		resetArray(nTypePerBin);
		resetArray(nClassPerBin);

		int t = 0, r = 0, b = 0;
		for (b = 0; b < m; b++) {
			typePerBin[b].clear();
			classPerBin[b].clear();
		}
		HashSet<Integer> binIndices; 

		for (int i = 0; i < n; i++) {
			b = take[i];
			if(b == NOT_USE) {
				continue;
			}
			binIndices = items[i].getBinIndices();
			if(!binIndices.contains(bins[b].getId())) {
				notInB[b] += 1;
			}
			
			sumW[b] += items[i].getW();
			sumP[b] += items[i].getP();
			t = items[i].getT();
			r = items[i].getR();
			if (!typePerBin[b].containsKey(t)) {
				nTypePerBin[b] += 1;
				typePerBin[b].put(t, 1);
			} else {
				typePerBin[b].replace(t, typePerBin[b].get(t) + 1);
			}
			if (!classPerBin[b].containsKey(r)) {
				nClassPerBin[b] += 1;
				classPerBin[b].put(r, 1);
			} else {
				classPerBin[b].replace(r, classPerBin[b].get(r) + 1);
			}
		}

		for (b = 0; b < m; b++) {
			sumViolation += violations(b);
		}

		return sumViolation;
	}
	
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
		
		if(bins[b].getMinLoad() - sumW[b] > 0) {
			sumViolation += 0.5;
		}
		//sumViolation += Math.max(0, bins[b].getMinLoad() - sumW[b])/maxLoad;
		return sumViolation;
	}

	public void loadData(String src) {
		inputPath = src;
		MinMaxTypeMultiKnapsackInput input = MinMaxTypeMultiKnapsackInput.loadFromFile(src);
		items = input.getItems();
		bins = input.getBins();
		
		n = items.length;
		m = bins.length;
		for(int b = 0; b < m; b++) {
			bins[b].setId(b);
		}
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
		HashSet<Integer> binIndices = items[i].getBinIndices(); 
		
		if(oldBin != NOT_USE) {
			oldSumVOfB = violations(oldBin);
			sumW[oldBin] -= items[i].getW();
			sumP[oldBin] -= items[i].getP();
			if(!binIndices.contains(bins[oldBin].getId())) {
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
			newSumVOfB = violations(oldBin);
		}
		
		if(newBin != NOT_USE) {
			oldSumVOfA = violations(newBin);
			sumW[newBin] += items[i].getW();
			sumP[newBin] += items[i].getP();		
			if(!binIndices.contains(bins[newBin].getId())) {
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
			newSumVOfA = violations(newBin);
		}

		if(oldBin != NOT_USE) {
			sumW[oldBin] += items[i].getW();
			sumP[oldBin] += items[i].getP();
			nTypePerBin[oldBin] += deltaTypeB;
			nClassPerBin[oldBin] += deltaClassB;
			notInB[oldBin] += deltaNotInB;
		}
		
		if(newBin != NOT_USE) {
			sumW[newBin] -= items[i].getW();
			sumP[newBin] -= items[i].getP();
			nTypePerBin[newBin] += deltaTypeA;
			nClassPerBin[newBin] += deltaClassA;
			notInB[newBin] += deltaNotInA;
		}
		
		return (newSumVOfB + newSumVOfA) - (oldSumVOfB + oldSumVOfA);
	}

	class AssignMove {
		int newBin;
		int oldBin;
		int i;

		public AssignMove(int newBin, int oldBin, int i) {
			this.newBin = newBin;
			this.oldBin = oldBin;
			this.i = i;
		}
	}
	
	private void restartMaintainConstraint(int[][] tabu, int minV, int maxV) {

		for (int i = 0; i < n; i++) {
			java.util.ArrayList<Integer> L = new java.util.ArrayList<Integer>();
			for (int b = minV; b <= maxV; b++) {
				if(b != maxV && !items[i].getBinIndices().contains(bins[b].getId())) {
					continue;
				}
				if (getAssignDelta(b, i) <= rand.nextInt(1))
					L.add(b);
			}
			/*
			if(L.size() == 0) {
				for (int b = minV; b <= maxV; b++) {
					if(b != maxV && !items[i].getBinIndices().contains(bins[b].getId())) {
						continue;
					}
					if (getAssignDelta(b, i) <= 3)
						L.add(b);
				}
			}*/
			if(L.size() == 0) {
				continue;
			}
			int idx = rand.nextInt(L.size());
			take[i] = L.get(idx);
		}
		for (int i = 0; i < tabu.length; i++) {
			for (int j = 0; j < tabu[i].length; j++)
				tabu[i][j] = -1;
		}
		
	}
	
	public void updateBest(){
		
	}
	
	public void tabuSearch(int tabulen, int maxTime, int maxIter,
			int maxStable) {
		double t0 = System.currentTimeMillis();
		int minV = 0, maxV = m-1;
		int D = maxV - minV;
		// System.out.println("n = " + n + ", D = " + D);
		int tabu[][] = new int[n][D + 1];
		for (int i = 0; i < n; i++)
			for (int v = 0; v <= D; v++)
				tabu[i][v] = -1;

		int it = 0;
		maxTime = maxTime * 1000;// convert into milliseconds

		double best = violations();
		int[] x_best = new int[n];
		for (int i = 0; i < n; i++)
			x_best[i] = take[i];

		System.out.println("TabuSearch, init S = " + violations());
		int nic = 0;
		ArrayList<AssignMove> moves = new ArrayList<AssignMove>();
		Random R = new Random();
		while (it < maxIter && System.currentTimeMillis() - t0 < maxTime
				&& (sumV = violations()) > 0) {
			int sel_i = -1;
			int sel_v = -1;
			double minDelta = Double.MAX_VALUE;
			moves.clear();
			
			for (int i = 0; i < n; i++) {
				if(!availR.contains(items[i].getR())) {
					continue;
				}
				for (int b = minV; b <= maxV; b++) {
					if(b != maxV &&!items[i].getBinIndices().contains(bins[b].getId())) {
						continue;
					}
					double delta = getAssignDelta(b, i);
							
					// System.out.println("min  =   "+x[i].getMinValue()+"   max =     "+x[i].getMaxValue());
					/*
					 * Accept moves that are not tabu or they are better than
					 * the best solution found so far (best)
					 */
					if (tabu[i][b - minV] <= it
							|| sumV + delta < best) {
						if (delta < minDelta) {
							minDelta = delta;
							sel_i = i;
							sel_v = b;
							moves.clear();
							moves.add(new AssignMove(b, take[i], i));
						} else if (delta == minDelta) {
							moves.add(new AssignMove(b, take[i], i));
						}
					}
				}
			}

			if (moves.size() <= 0) {
				System.out.println("TabuSearch::restart.....");
				restartMaintainConstraint(tabu, minV, maxV);
				// restart(x,tabu);
				nic = 0;
			} else {
				// perform the move
				AssignMove m = moves.get(R.nextInt(moves.size()));
				sel_i = m.i;
				sel_v = m.newBin;
				take[sel_i] = sel_v;
				tabu[sel_i][sel_v - minV] = it + tabulen;

				System.out.println("Step " + it + ", S = " + violations()
						+ ", best = " + best + ", delta = " + minDelta
						+ ", nic = " + nic);
				// update best
				if ((sumV = violations()) < best) {
					best = sumV;
					for (int i = 0; i < n; i++)
						x_best[i] = take[i];
					updateBest();
				}

				//if (minDelta >= 0) {
				if(violations() >= best){
					nic++;
					if (nic > maxStable) {
						System.out.println("TabuSearch::restart.....");
						restartMaintainConstraint(tabu, minV, maxV);
						nic = 0;
					}
				} else {
					nic = 0;
				}
			}
			it++;
		}
		System.out.println("Step " + it + ", S = " + violations());
		for (int i = 0; i < n; i++)
			take[i] = x_best[i];
		printSolution();
	}
	
	public void writeSolution() {
		String[] tmp = inputPath.split("/");
		String fileName = tmp[tmp.length - 1].split("\\.")[0] + ".out";
		outputPath = inputPath.substring(0, inputPath.lastIndexOf('/')) + "result/" + fileName;
		for(int i = 0; i < n; i++) {
			System.out.println(take[i]);
		}
	}

	public void printSolution() {
		int sum_not_use = 0;
		System.out.println("n = " + n );
		HashMap<Integer, ArrayList<Integer>> result = new HashMap<Integer, ArrayList<Integer>>();
		
		for (int i = 0; i < n; i++) {
			if(take[i] == NOT_USE) {
				sum_not_use += 1;
			} else {
				int b = take[i];
				if(!result.containsKey(b)) {
					result.put(b, new ArrayList<Integer>());
				} 
				result.get(b).add(i);
			}
			
		}
		
		System.out.println("Not use " + sum_not_use + " items");
		
		for (Map.Entry<Integer, ArrayList<Integer>> entry : result.entrySet()) {
			int b = entry.getKey();
			System.out.print("B = " + bins[b].getId() + ": ");
			System.out.println("");
			System.out.print(entry.getValue().size() + ": ");
			for(int item: entry.getValue()) {
				System.out.print(item + " ");
			}
			System.out.println("");
			System.out.print("W: ");
			for(int item: entry.getValue()) {
				System.out.print(items[item].getW() + " ");
			}
			System.out.println("");
			System.out.print("R: ");
			for(int item: entry.getValue()) {
				System.out.print(items[item].getR() + " ");
			}
			System.out.println("");
			System.out.print("T: ");
			for(int item: entry.getValue()) {
				System.out.print(items[item].getT() + " ");
			}
			System.out.println("");
			violations();
			System.out.println("Min load: " + bins[b].getMinLoad());
			System.out.println("R: " + bins[b].getR());
			System.out.println("T: " + bins[b].getT());
			System.out.println("- Capacity violation = " + Math.max(0, sumW[b] - bins[b].getCapacity()));
			System.out.println("- Min load violation = " + Math.max(0, bins[b].getMinLoad() - sumW[b]));
			System.out.println("- P violation = " + Math.max(0, sumP[b] - bins[b].getP()));
			System.out.println("- Type violation = " + Math.max(0, nTypePerBin[b] - bins[b].getT()));
			System.out.println("- Class violation = " + Math.max(0, nClassPerBin[b] - bins[b].getR()));
			System.out.println("- Indies violation = " + notInB[b]);
			
			System.out.println("\n*****************************************************\n");
		}
		System.out.println(violations());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MinMaxTypeMultiKnapsackSolution solution = new MinMaxTypeMultiKnapsackSolution();

		// solution.loadData("src/khmtk60/miniprojects/multiknapsackminmaxtypeconstraints/MinMaxTypeMultiKnapsackInput.json");
		solution.loadData(
				"src/khmtk60/miniprojects/multiknapsackminmaxtypeconstraints/51004418316727.json");
		solution.initModel();
		solution.tabuSearch(10, 500, 150000, 1000); // Cho tap du lieu 41525782483156.json
	}

}
