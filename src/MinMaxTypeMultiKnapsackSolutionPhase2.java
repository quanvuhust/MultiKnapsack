package src;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.*;

public class MinMaxTypeMultiKnapsackSolutionPhase2 extends MinMaxTypeMultiKnapsackSolution{
	private final int DEBUG = 0;
	
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
		
		
		if(bins[b].getMinLoad() - sumW[b] > 0) {
			sumViolation += ((double)nItemPerBin[b])/n;
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

		for (int i: a) {
			binIndices = items[i].getBinIndices();
			if(!binIndices.contains(bins[b].getId())) {
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
		
		if(bins[b].getMinLoad() - sumW > 0) {
			sumViolation += ((double)a.size())/n;
		}

		return sumViolation;
	}

	public double getAssignDeltaOfBalanceTwoBin(int oldBin, int newBin, StatusOfBin statusX, StatusOfBin statusY, int i, int nItem) {
		double vioOld = violationsOfBalanceTwoBin(statusX, nItem) + violationsOfBalanceTwoBin(statusY, nItem);
		int b_x = statusX.b;
		int b_y = statusY.b;
		if(oldBin == b_x && newBin == b_y) {
			statusY.addItem(i);
			statusX.removeItem(i);
		} else if(oldBin == b_y && newBin == b_x) {
			statusY.removeItem(i);
			statusX.addItem(i);
		} else {
			return 0;
		}
		double vioNew = violationsOfBalanceTwoBin(statusX, nItem) + violationsOfBalanceTwoBin(statusY, nItem);
		if(oldBin == b_x && newBin == b_y) {
			statusY.removeItem(i);
			statusX.addItem(i);
		} else if(oldBin == b_y && newBin == b_x) {
			statusY.addItem(i);
			statusX.removeItem(i);
		}
		return vioNew - vioOld;
	}
	
	private double violationsOfBalanceTwoBin(StatusOfBin status, int numItem) {
		double sumViolation = 0;
		int b = status.b;

		sumViolation += Math.max(0, status.sumW - bins[b].getCapacity());
		//sumViolation += Math.max(0, bins[b].getMinLoad() - status.sumW);
		sumViolation += Math.max(0, status.sumP - bins[b].getP());
		sumViolation += Math.max(0, status.nType - bins[b].getT());
		
		if(bins[b].getMinLoad() - status.sumW > 0) {
			sumViolation += ((double)status.nItem)/numItem;
		}

		return sumViolation;
	}
	
	
	private double violationsOfMaxNumItemInABin(StatusOfBin status, int numItem) {
		double sumViolation = 0;
		int b = status.b;

		sumViolation += Math.max(0, status.sumW - bins[b].getCapacity());
		sumViolation += Math.max(0, bins[b].getMinLoad() - status.sumW);
		sumViolation += Math.max(0, status.sumP - bins[b].getP());
		sumViolation += Math.max(0, status.nType - bins[b].getT());
		
		sumViolation += (double)(numItem - status.nItem)/numItem;
		//System.out.println("Hehe " +  Math.max(0, bins[b].getMinLoad() - status.sumW));

		return sumViolation;
	}	
	
	private void maxNumItemInABin(int tabulen, int maxTime, int maxIter, int maxStable, int b, ArrayList<Integer> itemsUse, ArrayList<Integer> newBin, ArrayList<Integer> otherBin) {
		int numItem = itemsUse.size();
		double sumV = 0;
		double t0 = System.currentTimeMillis();
		
		// System.out.println("n = " + n + ", D = " + D);
		int tabu[][] = new int[numItem][2];
		for (int i = 0; i < numItem; i++) 
			for (int j = 0; j < 2; j++) 
				tabu[i][j] = -1;

		int it = 0;
		maxTime = maxTime * 1000;// convert into milliseconds

		int[] x_best = new int[numItem];
		int[] x_take = new int[numItem];
		
		for (int k = 0; k < numItem; k++) x_take[k] = 0;
		for (int k = 0; k < numItem; k++) x_best[k] = x_take[k];
		StatusOfBin status = new StatusOfBin(b);
		double best = violationsOfMaxNumItemInABin(status, numItem);

		int nic = 0;
		ArrayList<AssignMove> moves = new ArrayList<AssignMove>();
		Random R = new Random();
		
		int flag = 0;
		for (flag = 0; flag < numItem; flag++) {
			int i = itemsUse.get(flag);
			if(items[i].getBinIndices().contains(bins[b].getId())) break;
		}
		
		while (flag < numItem && it < maxIter && System.currentTimeMillis() - t0 < maxTime
				&& (sumV = violationsOfMaxNumItemInABin(status, numItem)) > 0) {
			int sel_i = -1;
			int sel_v = -1;
			double minDelta = Double.MAX_VALUE;
			moves.clear();
			
			for (int k = 0; k < numItem; k++) {
				int i = itemsUse.get(k);
				if(!items[i].getBinIndices().contains(bins[b].getId())) continue;
				for (int choice = 0; choice < 2; choice++) {
					double delta = getAssignDeltaOfMaxNumItemInABin(x_take[k], choice, status, i, numItem);
					if(tabu[k][choice] <= it || sumV + delta < best) {
						if (delta < minDelta) {
							minDelta = delta;
							moves.clear();
							moves.add(new AssignMove(choice, x_take[k], k));
						} else if (delta == minDelta) {
							moves.add(new AssignMove(choice, x_take[k], k));
						}
					}
				}
			}
			
			if (moves.size() <= 0) {
				if(DEBUG == 1) {
					System.out.println("maxNumItemInABin::TabuSearch::restart.....");
				}
				
				restartOfMaxNumItemInABin(tabu, status, itemsUse, x_take);
				nic = 0;
			} else {
				// perform the move
				AssignMove m = moves.get(R.nextInt(moves.size()));
				sel_i = m.i;
				sel_v = m.newBin;
				if(x_take[sel_i] == 0 && sel_v == 1) {
					status.addItem(itemsUse.get(sel_i));
				} else if(x_take[sel_i] == 1 && sel_v == 0) {
					status.removeItem(itemsUse.get(sel_i));
				}
				x_take[sel_i] = sel_v;
				
				tabu[sel_i][sel_v] = it + tabulen;
				sumV = violationsOfMaxNumItemInABin(status, numItem);
				if(DEBUG == 1) {
					System.out.println("maxNumItemInABin::Step " + it + ", "
							+ "S = " + sumV
							+ ", best = " + best + ", delta = " + minDelta
							+ ", nic = " + nic);
				}
				
				// update best
				if (sumV < best) {
					best = sumV;
					for (int k = 0; k < numItem; k++) x_best[k] = x_take[k];
				}

				//if (minDelta >= 0) {
				if(sumV >= best){
					nic++;
					if (nic > maxStable) {
						if(DEBUG == 1) {
							System.out.println("maxNumItemInABin::TabuSearch::restart.....");
						}
						
						restartOfMaxNumItemInABin(tabu, status, itemsUse, x_take);
						nic = 0;
					}
				} else {
					nic = 0;
				}
			}
			it++;
		}
		
		for (int k = 0; k < numItem; k++) {
			if(x_take[k] == 1) {
				newBin.add(itemsUse.get(k));
			} else {
				otherBin.add(itemsUse.get(k));
			}
		}
	}	
	
	private double getAssignDeltaOfMaxNumItemInABin(int oldChoice, int newChoice, StatusOfBin status, int i, int nItem) {
		double vioOld = violationsOfMaxNumItemInABin(status, nItem);
		if(oldChoice == 0 && newChoice == 1) {
			status.addItem(i);
		} else if(oldChoice == 1 && newChoice == 0) {
			status.removeItem(i);
		} else {
			return 0;
		}
		double vioNew = violationsOfMaxNumItemInABin(status, nItem);
		if(oldChoice == 0 && newChoice == 1) {
			status.removeItem(i);
		} else if(oldChoice == 1 && newChoice == 0) {
			status.addItem(i);
		}
		return vioNew - vioOld;
	}
	
	private void balanceTwoBin(int tabulen, int maxTime, int maxIter, int maxStable, int b_x, int b_y, ArrayList<Integer> binXOld, ArrayList<Integer> binYOld, ArrayList<Integer> binXNew, ArrayList<Integer> binYNew) {
		binXNew.clear();
		binYNew.clear();
		binXNew.addAll(binXOld);
		binYNew.addAll(binYOld);
		double sumV = 0;
		double t0 = System.currentTimeMillis();

		ArrayList<Integer> itemsUse = new ArrayList<Integer>(binXOld);
		itemsUse.addAll(binYOld);
		
		int numItem = itemsUse.size();
		
		// System.out.println("n = " + n + ", D = " + D);
		int tabu[][] = new int[numItem][2];
		for (int i = 0; i < numItem; i++) 
			for (int j = 0; j < 2; j++) 
				tabu[i][j] = -1;

		int it = 0;
		maxTime = maxTime * 1000;// convert into milliseconds
		
		int[] x_best = new int[numItem];
		int[] x_take = new int[numItem];
		int[] binsUse = {b_x, b_y};
		
		for(int k = 0; k < binXOld.size(); k++) {
			x_take[k] = b_x;
		}
		
		for(int k = binXOld.size(); k < numItem; k++) {
			x_take[k] = b_y;
		}
			
		for (int k = 0; k < numItem; k++)
			x_best[k] = x_take[k];
		StatusOfBin statusX = new StatusOfBin(binXOld, b_x);
		StatusOfBin statusY = new StatusOfBin(binYOld, b_y);
		double best = violationsOfBalanceTwoBin(statusX, n) +
				violationsOfBalanceTwoBin(statusY, n);
		int nic = 0;
		ArrayList<AssignMove> moves = new ArrayList<AssignMove>();
		Random R = new Random();
		
		while (it < maxIter && System.currentTimeMillis() - t0 < maxTime
				&& (sumV = violationsOfBalanceTwoBin(statusX, n) +
						violationsOfBalanceTwoBin(statusY, n)) > 0) {
			
			int sel_i = -1;
			int sel_v = -1;
			double minDelta = Double.MAX_VALUE;
			moves.clear();
			
			for (int k = 0; k < numItem; k++) {
				int i = itemsUse.get(k);
				
				for (int choice = 0; choice < 2; choice++) {
					int b = binsUse[choice];
					if(!items[i].getBinIndices().contains(bins[b].getId())) {
						continue;
					}
						
					double delta = getAssignDeltaOfBalanceTwoBin(x_take[k], b, statusX, statusY, i, n);
					//System.out.println("Delta = " + delta + " old = " + x_take[k] + " new = " + b + " i = " + i);
					if(tabu[k][choice] <= it || sumV + delta < best) {
						if (delta < minDelta) {
							minDelta = delta;
							moves.clear();
							moves.add(new AssignMove(b, x_take[k], k));
						} else if (delta == minDelta) {
							moves.add(new AssignMove(b, x_take[k], k));
						}
					}
				}
			}
			
			if (moves.size() <= 0) {
				if(DEBUG == 1) {
					System.out.println("Balance::TabuSearch::restart.....");
				}
				
				restartOfBalanceTwoBin(tabu, b_x, b_y, itemsUse, x_take, statusX, statusY);
				nic = 0;
			} else {
				// perform the move
				AssignMove m = moves.get(R.nextInt(moves.size()));
				sel_i = m.i;
				sel_v = m.newBin;
				if(x_take[sel_i] == b_x && sel_v == b_y) {
					statusY.addItem(itemsUse.get(sel_i));
					statusX.removeItem(itemsUse.get(sel_i));
				} else if(x_take[sel_i] == b_y && sel_v == b_x) {
					statusY.removeItem(itemsUse.get(sel_i));
					statusX.addItem(itemsUse.get(sel_i));
				}
				
				x_take[sel_i] = sel_v;
				tabu[sel_i][sel_v == b_x ?0:1] = it + tabulen;
				binXNew.clear();
				binYNew.clear();
				for (int k = 0; k < numItem; k++) {
					int i = itemsUse.get(k);
					if(x_best[k] == 0) {
						binXNew.add(i);
					} else {
						binYNew.add(i);
					}
				}
				sumV = violationsOfBalanceTwoBin(statusX, n) +
						violationsOfBalanceTwoBin(statusY, n);
				
				if(DEBUG == 1) {
					System.out.println("Balance::Step " + it + ", "
							+ "S = " + sumV
							+ ", best = " + best + ", delta = " + minDelta
							+ ", nic = " + nic);
				}
				
				// update best
				if (sumV < best) {
					best = sumV;
					for (int k = 0; k < numItem; k++)
						x_best[k] = x_take[k];
				}

				//if (minDelta >= 0) {
				if(sumV >= best){
					nic++;
					if (nic > maxStable) {
						if(DEBUG == 1) {
							System.out.println("Balance::TabuSearch::restart.....");
						}
						
						restartOfBalanceTwoBin(tabu, b_x, b_y, itemsUse, x_take, statusX, statusY);
						nic = 0;
					}
				} else {
					nic = 0;
				}
			}
			it++;
		}
		
		binXNew.clear();
		binYNew.clear();
		for (int k = 0; k < numItem; k++) {
			int i = itemsUse.get(k);
			if(x_best[k] == 0) {
				binXNew.add(i);
			} else {
				binYNew.add(i);
			}
		}
	}
	
	public double getSwapDelta(int b_x, int b_y, ArrayList<Integer> binXNew, ArrayList<Integer> binYNew) {
		binXNew.clear();
		binYNew.clear();
		
		
		ArrayList<Integer> binXOld = new ArrayList<Integer>();
		ArrayList<Integer> binYOld = new ArrayList<Integer>();
		
		for(int i: itemsUse) {
			if(take[i] == b_x) {
				binXOld.add(i);
			} else if(take[i] == b_y) {
				binYOld.add(i);
			}
		}
		int rBxOld = -1, rByOld = -1;
		if(binXOld.size() > 0) rBxOld = items[binXOld.get(0)].getR();
		if(binYOld.size() > 0) rByOld = items[binYOld.get(0)].getR();
				
		if(rBxOld == -1 && rByOld == -1) {
			return 0;
		}
		double newViolation = 0;
		if(rBxOld != -1 && rByOld != -1 && rByOld != rBxOld) {
			for(int i: binXOld) binYNew.add(i);
			for(int i: binYOld) binXNew.add(i);
			newViolation = violations(binXNew, b_x) + violations(binYNew, b_y);
		} else {
			binXNew.clear();
			binYNew.clear();
			if(rBxOld == -1 && rByOld != -1) {
				//System.out.println("Max number items in bin " + b_x);
				if(bins[b_x].getMinLoad() >= bins[b_y].getMinLoad()) {
                    for(int i: binYOld) binYNew.add(i);
					return 0;
				}
				ArrayList<Integer> allItemTwoBin = new ArrayList<Integer>();
				for(int i: binXOld) allItemTwoBin.add(i);
				for(int i: binYOld) allItemTwoBin.add(i);
				
				maxNumItemInABin(5, 5000, 1000, 100, b_x, allItemTwoBin, binXNew, binYNew);
				newViolation = violations(binXNew, b_x) + violations(binYNew, b_y);
			} else if(rBxOld != -1 && rByOld == -1) {
				//System.out.println("Max number items in bin " + b_y);
				if(bins[b_x].getMinLoad() <= bins[b_y].getMinLoad()) {
                    for(int i: binXOld) binXNew.add(i);
					return 0;
				}
				ArrayList<Integer> allItemTwoBin = new ArrayList<Integer>();
				for(int i: binXOld) allItemTwoBin.add(i);
				for(int i: binYOld) allItemTwoBin.add(i);
				
				maxNumItemInABin(5, 5000, 1000, 100, b_y, allItemTwoBin, binYNew, binXNew);
				newViolation = violations(binXNew, b_x) + violations(binYNew, b_y);
			} else {
				//System.out.println("Balance 2 bin " + b_x + "-" + b_y);
				// Chi su dung 1 bin x
				ArrayList<Integer> allItemTwoBin = new ArrayList<Integer>();
				for(int i: binXOld) allItemTwoBin.add(i);
				for(int i: binYOld) allItemTwoBin.add(i);
				ArrayList<Integer> binXNew1 = new ArrayList<Integer>();
				ArrayList<Integer> binYNew1 = new ArrayList<Integer>();
				maxNumItemInABin(15, 5000, 1000, 100, b_x, allItemTwoBin, binXNew1, binYNew1);
				double newViolation1 = violations(binXNew1, b_x) + violations(binYNew1, b_y);

				// Chi su dung 1 bin y	
				ArrayList<Integer> binXNew2 = new ArrayList<Integer>();
				ArrayList<Integer> binYNew2 = new ArrayList<Integer>();
				maxNumItemInABin(15, 5000, 1000, 100, b_y, allItemTwoBin, binYNew2, binXNew2);
				double newViolation2 = violations(binXNew2, b_x) + violations(binYNew2, b_y);
				
				int use = -1;
				if(newViolation1 < newViolation2) {
					newViolation = newViolation1;
					use = b_x;
				} else {
					newViolation = newViolation2;
					use = b_y;
				}
				

				// Su dung ca 2 bin
				balanceTwoBin(5, 500, 1000,
						100, b_x, b_y, 
						binXOld, binYOld,
						binXNew, binYNew) ;
				
				double newViolation3 = violations(binXNew, b_x) + violations(binYNew, b_y);
				/*
				if(b_y == 514) {
					
					System.out.println(newViolation);
					System.out.println(newViolation3);
				}*/
				if(newViolation3 < newViolation) {
					newViolation = newViolation3;
				} else {
					binXNew.clear();
					binYNew.clear();
					if(use == b_x) {
						for(int i: binXNew1) binXNew.add(i);
						for(int i: binYNew1) binYNew.add(i);
					} else {
						for(int i: binXNew2) binXNew.add(i);
						for(int i: binYNew2) binYNew.add(i);
					}
				}
			}
			
		}
		double oldViolation = violations(binXOld, b_x) + violations(binYOld, b_y);	
		
		return newViolation - oldViolation;
	}
	
	private void restartMaintainConstraint(int[][] tabu) {
		ArrayList<Integer> binXNew = new ArrayList<Integer>();
		ArrayList<Integer> binYNew = new ArrayList<Integer>();
		Collections.shuffle(binsUse);
		int len = binsUse.size();
		for(int k = 0; k < len/2; k++) {
			binXNew.clear();
			binYNew.clear();
			double delta = getSwapDelta(binsUse.get(k), binsUse.get(len - k - 1), binXNew, binYNew);
			if(delta <= 0) {
				for(int i: binXNew) take[i] = binsUse.get(k);
				for(int i: binYNew) take[i] = binsUse.get(len - k - 1);
			}		
		}
		
		for (int i = 0; i < tabu.length; i++) {
			for (int j = 0; j < tabu[i].length; j++)
				tabu[i][j] = -1;
		}
		
	}
	
	private void restartOfBalanceTwoBin(int[][] tabu, int b_x, int b_y, ArrayList<Integer> itemsUse, int x_take[], StatusOfBin statusX, StatusOfBin statusY) {	
		int numItem = itemsUse.size();
		for (int k = 0; k < numItem; k++) {
			int i = itemsUse.get(k);
			java.util.ArrayList<Integer> L = new java.util.ArrayList<Integer>();
			for (int choice = 0; choice < 2; choice++) {
				int b = x_take[choice];
				if(!items[i].getBinIndices().contains(bins[b].getId())) {
					continue;
				}
				if (getAssignDeltaOfBalanceTwoBin(x_take[k], b, statusX, statusY, i, n) <= 0)
					L.add(b);
			}
			
			if(L.size() == 0) {
				continue;
			}
			
			int oldBin = x_take[k];
			int newBin = L.get(rand.nextInt(L.size()));
			if(oldBin == b_x && newBin == b_y) {
				statusY.addItem(i);
				statusX.removeItem(i);
			} else if(oldBin == b_y && newBin == b_x) {
				statusY.removeItem(i);
				statusX.addItem(i);
			}
			x_take[k] = newBin;
		}
		
		for (int i = 0; i < tabu.length; i++) {
			for (int j = 0; j < tabu[i].length; j++)
				tabu[i][j] = -1;
		}
	}

	private void restartOfMaxNumItemInABin(int[][] tabu, StatusOfBin status, ArrayList<Integer> itemsUse, int x_take[]) {
		for (int k = 0; k < itemsUse.size(); k++) {
			int i = itemsUse.get(k);
			if(!items[i].getBinIndices().contains(bins[status.b].getId())) continue;
			java.util.ArrayList<Integer> L = new java.util.ArrayList<Integer>();
			for (int choice = 0; choice < 2; choice++) {
				if (getAssignDeltaOfMaxNumItemInABin(x_take[k], choice, status, i, itemsUse.size()) <= 0) L.add(choice);
			}
			
			if(L.size() == 0) continue;
			int newChoice = L.get(rand.nextInt(L.size()));
			if(x_take[k] == 0 && newChoice == 1) {
				status.addItem(i);
			} else if(x_take[k] == 1 && newChoice == 0) {
				status.removeItem(i);
			}
			x_take[k] = newChoice;
		}
		
		for (int i = 0; i < tabu.length; i++) 
			for (int j = 0; j < tabu[i].length; j++) 
				tabu[i][j] = -1;
	}
	
	public void updateBest(){
		
	}
	
	@Override
	public void tabuSearch(int tabulen, int maxTime, int maxIter, int maxStable, ArrayList<Integer> binsUse, ArrayList<Integer> itemsUse) {
		double sumV = 0;
		double t0 = System.currentTimeMillis();
		int minB = Integer.MAX_VALUE, maxB = Integer.MIN_VALUE;
		int minI = Integer.MAX_VALUE, maxI = Integer.MIN_VALUE;
		ArrayList<Integer> binXNew = new ArrayList<Integer>();
		ArrayList<Integer> binYNew = new ArrayList<Integer>();
		
		binsUse.sort(new Comparator<Integer>() {
			@Override
			public int compare(Integer b1, Integer b2) {
				// TODO Auto-generated method stub
				double vio1 = violations(b1);
				double vio2 = violations(b2);
				double r = vio2 - vio1;
				if (r > 0) {
					return 1;
				} else if (r < 0) {
					return -1;
				} else {
					return 0;
				}
			}
		});
		
		for(int b: binsUse) {
			if(minB > b) {
				minB = b;
			}
			if(maxB < b) {
				maxB = b;
			}
		}
		for(int i: itemsUse) {
			if(minI > i) {
				minI = i;
			}
			if(maxI < i) {
				maxI = i;
			}
		}
		int DB = maxB - minB;
		int DI = maxI - minI;
		// System.out.println("n = " + n + ", D = " + D);
		int tabu[][] = new int[DB + 1][DB + 1];
		for (int i = 0; i <= DB; i++)
			for (int v = 0; v <= DB; v++)
				tabu[i][v] = -1;

		int it = 0;
		maxTime = maxTime * 1000;// convert into milliseconds

		double best = violations();
		int[] x_best = new int[maxI + 1];
		for (int i:itemsUse)
			x_best[i] = take[i];

		System.out.println("TabuSearch, init S = " + violations());
		int nic = 0;
		ArrayList<SwapMove> moves = new ArrayList<SwapMove>();
		Random R = new Random();
		ArrayList<Integer> binsUseVail = new ArrayList<Integer>(binsUse);
		System.out.println("bin use size: " + binsUse.size());
		while (it < maxIter && System.currentTimeMillis() - t0 < maxTime
				&& (sumV = violations()) > 0) {
			double minDelta = Double.MAX_VALUE;
			moves.clear();
			
			ArrayList<Integer> maxVioBin = new ArrayList<Integer>();
			double maxVio = -1;
			if(binsUseVail.size() == 0) {
				binsUseVail.addAll(binsUse);
			}
			for (int b: binsUseVail) {
				double vio = violations(b);
				
				if(maxVio < vio) {
					maxVio = vio;
					maxVioBin.clear();
					maxVioBin.add(b);
				} else if(maxVio == vio) {
					maxVioBin.add(b);
				}
			}
			int b_ucv = maxVioBin.get(R.nextInt(maxVioBin.size()));
			
			binsUseVail.remove(new Integer(b_ucv));
			//b_ucv = 505;
			binXNew.clear();
			binYNew.clear();
			
			for(int b: binsUse) {
				//if(b == b_ucv) continue;
				//System.out.println("Processing bin " + b_ucv + " and " + b);
				
				binXNew.clear();
				binYNew.clear();
				double delta = getSwapDelta(b_ucv, b, binXNew, binYNew);	
				
				/*
				System.out.println("Processing bin " + b_ucv + " and " + b + ": delta = " + delta);
				if(delta < 0) {
					System.out.println("Press Enter to continue");
					try{System.in.read();}
					catch(Exception e){}
				}
				
				*/
				if (tabu[b_ucv - minB][b - minB] <= it || sumV + delta < best) {
					if (delta < minDelta) {
						minDelta = delta;
						moves.clear();
						moves.add(new SwapMove(b_ucv, b, binXNew, binYNew));
					} else if (delta == minDelta) {
						moves.add(new SwapMove(b_ucv, b, binXNew, binYNew));
					}
				}
			}
			

			if (moves.size() <= 0) {
				System.out.println("TabuSearch::restart.....");
				restartMaintainConstraint(tabu);
				nic = 0;
			} else {
				// perform the move
				SwapMove m = moves.get(R.nextInt(moves.size()));
				int b_x = m.b_x;
				int b_y = m.b_y;
				
				for(int i: m.binXNew) take[i] = b_x;
				for(int i: m.binYNew) take[i] = b_y;
				
				tabu[b_x - minB][b_y - minB] = it + tabulen;
				sumV = violations();
				System.out.println("Step " + it + ", S = " + sumV
						+ ", best = " + best + ", delta = " + minDelta
						+ ", nic = " + nic);
				System.out.println("Balance bx = " + b_x + " by = " + b_y);
				// update best
				if (sumV < best) {
					best = sumV;
					for (int i:itemsUse) x_best[i] = take[i];
				}

				//if (minDelta >= 0) {
				if(sumV >= best){
					nic++;
					if (nic > maxStable) {
						System.out.println("TabuSearch::restart.....");
						restartMaintainConstraint(tabu);
						nic = 0;
					}
				} else {
					nic = 0;
				}
			}
			it++;
		}
		System.out.println("Step " + it + ", S = " + violations());
		for (int i:itemsUse) take[i] = x_best[i];
	}
	
	public void loadPretrainedModel(String path, int r) {
		System.out.println(path);
		File file = new File(path);
		
		try {
	        Scanner sc = new Scanner(file);
	        int i = 0;
	        
	        while (sc.hasNextLine()) {
	        	String[] tmp = sc.nextLine().split(" ");
	        	int b = Integer.parseInt(tmp[0]);
	        	for(int k = 1; k < tmp.length; k++) {
	        		i = Integer.parseInt(tmp[k]);
	        		take[i] = b;
	        	}
	        }
	        
	        sc.close();
	    } 
	    catch (FileNotFoundException e) {
	        e.printStackTrace();
	    }
	    itemsUse.clear();
        binsUse.clear();
        for (int i = 0; i < n; i++) {
            if (take[i] == NOT_USE_FOREVER || !availR.contains(items[i].getR())) {
                take[i] = NOT_USE_FOREVER;
            } else {
                itemsUse.add(i);
            }
        }

        for (int b = 0; b < m; b++) {
            if (bins[b].getUse() != NOT_USE_FOREVER) {
                binsUse.add(b);
            }
        }
	}
	
	public double testSwapDelta(int b_x, int b_y) {
		ArrayList<Integer> binXNew = new ArrayList<Integer>();
		ArrayList<Integer> binYNew = new ArrayList<Integer>();
		
		ArrayList<Integer> binXOld = new ArrayList<Integer>();
		ArrayList<Integer> binYOld = new ArrayList<Integer>();
		
		for(int i: itemsUse) {
			if(take[i] == b_x) {
				binXOld.add(i);
			} else if(take[i] == b_y) {
				binYOld.add(i);
			}
		}
		int rBxOld = -1, rByOld = -1;
		if(binXOld.size() > 0) rBxOld = items[binXOld.get(0)].getR();
		if(binYOld.size() > 0) rByOld = items[binYOld.get(0)].getR();
		if(rBxOld == -1 && rByOld == -1) {
			return 0;
		}
		double newViolation = 0;
		
		if(rBxOld != -1 && rByOld != -1 && rByOld != rBxOld) {
			System.out.println("Swap bin: " + rByOld);
			for(int i: binXOld) {
				binYNew.add(i);
			}
			for(int i: binYOld) {
				binXNew.add(i);
			}
			newViolation = violations(binXNew, b_x) + violations(binYNew, b_y);
		} else {
			binXNew.clear();
			binYNew.clear();
			if(rBxOld == -1 && rByOld != -1) {
				if(bins[b_x].getMinLoad() >= bins[b_y].getMinLoad()) {
					return 0;
				}
				System.out.println("Max item:");
				ArrayList<Integer> allItemTwoBin = new ArrayList<Integer>();
				for(int i: binXOld) allItemTwoBin.add(i);
				for(int i: binYOld) allItemTwoBin.add(i);
				
				maxNumItemInABin(5, 5000, 500, 100, b_x, allItemTwoBin, binXNew, binYNew);
				newViolation = violations(binXNew, b_x) + violations(binYNew, b_y);
			} else if(rBxOld != -1 && rByOld == -1) {
				if(bins[b_x].getMinLoad() <= bins[b_y].getMinLoad()) {
					return 0;
				}
				System.out.println("Max item:");
				ArrayList<Integer> allItemTwoBin = new ArrayList<Integer>();
				for(int i: binXOld) allItemTwoBin.add(i);
				for(int i: binYOld) allItemTwoBin.add(i);
				//System.out.println(binXOld);
				maxNumItemInABin(5, 5000, 500, 100, b_y, allItemTwoBin, binYNew, binXNew);
				//System.out.println(binXNew);
				//System.out.println(binYNew);
				//System.out.println(violations(binXNew, b_x));
				//System.out.println(violations(binYNew, b_y));
				newViolation = violations(binXNew, b_x) + violations(binYNew, b_y);
			} else {
				System.out.println("Balance 2 bin " + b_x + "-" + b_y);
				// Chi su dung 1 bin x
				ArrayList<Integer> allItemTwoBin = new ArrayList<Integer>();
				for(int i: binXOld) allItemTwoBin.add(i);
				for(int i: binYOld) allItemTwoBin.add(i);
				ArrayList<Integer> binXNew1 = new ArrayList<Integer>();
				ArrayList<Integer> binYNew1 = new ArrayList<Integer>();
				maxNumItemInABin(5, 5000, 1000, 100, b_x, allItemTwoBin, binXNew1, binYNew1);
				double newViolation1 = violations(binXNew1, b_x) + violations(binYNew1, b_y);

				// Chi su dung 1 bin y	
				ArrayList<Integer> binXNew2 = new ArrayList<Integer>();
				ArrayList<Integer> binYNew2 = new ArrayList<Integer>();
				maxNumItemInABin(5, 5000, 1000, 100, b_y, allItemTwoBin, binYNew2, binXNew2);
				double newViolation2 = violations(binXNew2, b_x) + violations(binYNew2, b_y);
				
				int use = -1;
				if(newViolation1 < newViolation2) {
					newViolation = newViolation1;
					use = b_x;
				} else {
					newViolation = newViolation2;
					use = b_y;
				}
				
				// Su dung ca 2 bin
				balanceTwoBin(5, 5000, 1000,
						100, b_x, b_y, 
						binXOld, binYOld,
						binXNew, binYNew) ;
				
				double newViolation3 = violations(binXNew, b_x) + violations(binYNew, b_y);
				
				System.out.println(newViolation);
				System.out.println(newViolation3);
				if(newViolation3 < newViolation) {
					newViolation = newViolation3;
				} else {
					binXNew.clear();
					binYNew.clear();
					if(use == b_x) {
						for(int i: binXNew1) binXNew.add(i);
						for(int i: binYNew1) binYNew.add(i);
					} else {
						for(int i: binXNew2) binXNew.add(i);
						for(int i: binYNew2) binYNew.add(i);
					}
				}
			}
			
		}
		
		double oldViolation = violations(binXOld, b_x) + violations(binYOld, b_y);
		System.out.println(oldViolation);
		return newViolation - oldViolation;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
        System.out.println(Paths.get(".").toAbsolutePath().normalize().toString());
		MinMaxTypeMultiKnapsackSolutionPhase2  solution = new MinMaxTypeMultiKnapsackSolutionPhase2();

		// solution.loadData("src/khmtk60/miniprojects/multiknapsackminmaxtypeconstraints/MinMaxTypeMultiKnapsackInput.json");
		solution.loadData(
				"./dataset/MinMaxTypeMultiKnapsackInput-3000.json");
		solution.preprocess();
		solution.loadPretrainedModel();
		/*
		for(int b: solution.getBinsUse()) {
			System.out.println(b + " Test result: " + solution.testSwapDelta(1639, b));
		}*/
		//System.out.println(" Test result: " + solution.testSwapDelta(505, 514));
		
		solution.tabuSearch(1, 5000, 20, 10, solution.getBinsUse(), solution.getItemsUse()); // Cho tap du lieu 51004418316727.json
		solution.writeSolution();
		//solution.writeSubmit();
		solution.printSolution();
	}

}
