package AdhocCollaboration;

import java.util.HashMap;
import java.util.Random;

public class SubtaskLearningType {
	/*
	 * alphaS is the selflearning coefficient
	 */
	HashMap<Integer, Double> alphaS = new HashMap<Integer, Double>();
	// value=[0.1,0.2,0.3,0.4]
	double alphaSvalue[] = { 0.2, 0.2, 0.4, 0.3, 0.3, 0.3, 0.1, 0.4, 0.3, 0.3,
			0.4, 0.2, 0.1, 0.1, 0.1, 0.2, 0.4, 0.3, 0.3, 0.4 };
	/*
	 * alphaO is the ObervitionalLearning coefficient
	 */
	HashMap<Integer, Integer> alphaO = new HashMap<Integer, Integer>();
	// value=[1,2,3,4]
	int alphaOvalue[] = { 2, 4, 1, 2, 2, 1, 4, 3, 1, 3, 3, 4, 2, 3, 2, 3, 4, 2,
			4, 1 };

	double delta = 0.2;// if cap diff < delta, then does not get observe gain

	public SubtaskLearningType() {
		/**
		 * For the sake of consistency, we randonly assigned the numbers and
		 * then hard code it to the map
		 */

		// {1=2, 2=4, 3=1, 4=2, 5=2, 6=1, 7=4, 8=3, 9=1, 10=3, 11=3, 12=4, 13=2,14=3, 15=2, 17=3, 16=4, 19=2, 18=4, 20=1}
		// {1=0.2, 2=0.2, 3=0.4, 4=0.3, 5=0.3, 6=0.3, 7=0.1, 8=0.4, 9=0.3,
		// 10=0.3, 11=0.4, 12=0.2, 13=0.1, 14=0.1, 15=0.1, 17=0.2, 16=0.4,
		// 19=0.3, 18=0.3, 20=0.4}

		int multiplyer1=3;
		int multiplyer2=15;
		
		for (int i = 1; i <= 20; i++) {
			alphaS.put(i, alphaSvalue[i-1]);
		}

		for (int i = 1; i <= 20; i++) {
			alphaO.put(i, alphaOvalue[i-1]);
		}

	}

}

// Random rand=new Random() ;
// //int randomNum = rand.nextInt((max - min) + 1) + min;
//
// /**
// * initalize the alphaS map
// * value=[0.1,0.2,0.3,0.4]
// */
// for (int i=1;i<=20;i++){
// int index=rand.nextInt(4); //[0,3]
// alphaS.put(i, alphaSvalue[index]);
// }
//
//
// /**
// * initalize the alphaO map
// * value=[1,2,3,4]
// */
// for (int i=1;i<=20;i++){
// int index=rand.nextInt(4);
// alphaO.put(i, alphaOvalue[index]);
// }
//
// }

