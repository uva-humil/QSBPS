import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class QSBPS {

	static ArrayList<String> enList = new ArrayList<String>();
	static ArrayList<String> docList = new ArrayList<String>();
	static ArrayList<String> doc_qrel_relevance = new ArrayList<String>();
	static ArrayList<Integer> questionsIndex = new ArrayList<Integer>();
	static int lastmindex = 0;
	static int countthrow = 0;
	static double rewardParameter = 0.0;

	public static void main(String[] args) throws Exception {
		// parameters setting
		String trainDir = "./home_and_kitchen/qrelDirectoryTrain/";//training set 
		String testingDir = "./home_and_kitchen/qrelDirectoryTesting/";//testing set
		String pathin = "./home_and_kitchen/doc-entity-home_and_kitchen.txt";//entity file from TagMe
		String resultDir = "./home_and_kitchen/results/";//ranking results
		int iteration1 = 5;//number of questions 
		rewardParameter = 1;//weight of question reward

		if (args.length != 0) {
			trainDir = args[0];
			testingDir = args[1];
			pathin = args[2];
			resultDir = args[3];
			iteration1 = Integer.parseInt(args[4]);
			rewardParameter = Double.parseDouble(args[5]);
		}

		try {
			File dir = new File(testingDir);
			File[] files = dir.listFiles();

			enList = new ArrayList<String>();
			docList = new ArrayList<String>();

			FileReader reader1 = new FileReader(pathin);
			BufferedReader br1 = new BufferedReader(reader1);
			String str1 = "";
			int count = 0;
			br1.readLine();// remove the first empty line
			while ((str1 = br1.readLine()) != null) {
				count++;
				String entities[] = str1.split(",");
				docList.add(entities[0].trim());// example: b00000jb91txt
				for (int i = 0; i < entities.length; i++) {
					if (!entities[i].trim().equals("") && i > 0) {
						if (!enList.contains(entities[i].trim())) {
							enList.add(entities[i].trim());
						}
					}
				}

			}
			br1.close();

			int[][] entityDocMatrixArray = new int[enList.size()][count];
			FileReader reader2 = new FileReader(pathin);
			BufferedReader br2 = new BufferedReader(reader2);
			String str2 = "";
			int column1 = 0;
			br2.readLine();
			while ((str2 = br2.readLine()) != null) {
				String entities2[] = str2.split(",");
				for (int i = 0; i < entities2.length; i++) {
					if (!entities2[i].trim().equals("") && i > 0) {
						entityDocMatrixArray[enList.indexOf(entities2[i].trim())][column1] = 1;
					}
				}
				column1++;
			}
			br2.close();

			for (int ai = 0; ai < files.length; ai++) {
				String topiccount = files[ai].getName();
				if (topiccount.contains(".DS_Store"))
					continue;
				System.out.println(topiccount);
				String doc_qrel_train = trainDir + topiccount;
				String doc_qrel_relePath = testingDir + topiccount;

				String resultFile = resultDir + "questions" + iteration1 + "/rewardParameter" + rewardParameter
						+ "/finalresult" + topiccount;
				File fresultFile = new File(
						resultDir + "questions" + iteration1 + "/rewardParameter" + rewardParameter);
				if (!fresultFile.exists())
					fresultFile.mkdirs();

				double[] Alpha = new double[count];
				double[] PrefPi = new double[Alpha.length];

				int[] discardfile = new int[count];
				// initializing Alpha to all 1 vector
				for (int d = 0; d < docList.size(); d++) {
					Alpha[d] = 1;
				}
				double updateAlpha = 1;

				Map<String, Double> questionmap = new HashMap<String, Double>();
				FileReader readerTrain = new FileReader(doc_qrel_train);
				BufferedReader brTrain = new BufferedReader(readerTrain);
				String strTrain = "";
				int counttrain = 0;
				while ((strTrain = brTrain.readLine()) != null) {
					counttrain++;
					doc_qrel_relevance = new ArrayList<String>();
					doc_qrel_relevance.add(strTrain.split(",")[0].trim().toLowerCase());
					double[] Alpha1 = new double[count];
					for (int w = 0; w < enList.size(); w++) {
						discardfile = new int[count];
						Alpha1 = SequentialBayesianSearchTrain(entityDocMatrixArray, Alpha, updateAlpha, discardfile,
								w);
						double reward = ((double) countthrow / (double) count);
						if (questionmap.containsKey(enList.get(lastmindex)))
							questionmap.put(enList.get(lastmindex), (questionmap.get(enList.get(lastmindex)) + reward));
						else
							questionmap.put(enList.get(lastmindex), reward);
					}

					updateAlpha = 0;
					for (int f = 0; f < Alpha.length; f++) {
						if (Alpha[f] > updateAlpha)
							updateAlpha = Alpha[f];
					}
				}

				for (Map.Entry<String, Double> entry : questionmap.entrySet()) {
					double avgValue = entry.getValue() / counttrain;
					questionmap.put(entry.getKey(), avgValue);
				}

				brTrain.close();

				// test
				FileReader reader4 = new FileReader(doc_qrel_relePath);
				BufferedReader br4 = new BufferedReader(reader4);
				String str4 = "";
				while ((str4 = br4.readLine()) != null) {
					doc_qrel_relevance = new ArrayList<String>();
					questionsIndex = new ArrayList<Integer>();
					String docqrelTemp = str4.split(",")[0];
					doc_qrel_relevance.add(docqrelTemp.trim().toLowerCase());
					discardfile = new int[count];
					PrefPi = new double[Alpha.length];
					double[] Alphamp=new double[Alpha.length];
					for (int f = 0; f < Alpha.length; f++) {
							Alphamp[f]=Alpha[f];
					}

					double updateAlphamp=updateAlpha;
					for (int w = 0; w <= iteration1; w++) { // PrefPi is not updated in last iteration
						PrefPi = SequentialBayesianSearchTest(entityDocMatrixArray, Alphamp, updateAlphamp, discardfile,
								questionmap);
					}

					// sorting
					Map<String, Double> map = new HashMap<String, Double>();
					for (int mm = 0; mm < PrefPi.length; mm++) {
						map.put(docList.get(mm), PrefPi[mm]);
					}
					List<Map.Entry<String, Double>> mappingList = new ArrayList<Map.Entry<String, Double>>(
							map.entrySet());
					Collections.sort(mappingList, new Comparator<Map.Entry<String, Double>>() {
						public int compare(Map.Entry<String, Double> mapping1, Map.Entry<String, Double> mapping2) {
							return (mapping2.getValue()).compareTo(mapping1.getValue());
						}
					});

					FileWriter writerresult = new FileWriter(resultFile + " " + doc_qrel_relevance.get(0));
					BufferedWriter bwresult = new BufferedWriter(writerresult);
					for (Map.Entry<String, Double> mapping : mappingList) {
//						System.out.println(mapping.getValue().toString() + "=" + mapping.getKey().toString() + "\n");
						bwresult.write(mapping.getValue().toString() + "=" + mapping.getKey().toString() + "\n");
					}
					bwresult.flush();
					bwresult.close();

				}
				br4.close();

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static double[] SequentialBayesianSearchTrain(int[][] entityDocMatrixArray, double[] Alpha,
			double updateAlpha, int[] discardfile, int w) {
		countthrow = 0;
		int minIndex = w;
		lastmindex = minIndex;
		int indexM = docList.indexOf(doc_qrel_relevance.get(0) + "txt");
		if (indexM != -1) {
			if (entityDocMatrixArray[minIndex][indexM] == 0) {
				for (int m = 0; m < entityDocMatrixArray[minIndex].length; m++) {
					if (entityDocMatrixArray[minIndex][m] == 0) {
						Alpha[m] = Alpha[m] + updateAlpha;
					} else {
						discardfile[m] = 1;
						countthrow++;
					}
				}
			}

			else if (entityDocMatrixArray[minIndex][indexM] == 1) {
				for (int m1 = 0; m1 < entityDocMatrixArray[minIndex].length; m1++) {
					if (entityDocMatrixArray[minIndex][m1] == 1) {
						Alpha[m1] = Alpha[m1] + updateAlpha;
					} else {
						discardfile[m1] = 1;
						countthrow++;
					}
				}
			}
		}

		return Alpha;
	}

	public static double[] SequentialBayesianSearchTest(int[][] entityDocMatrixArray, double[] Alpha,
			double updateAlpha, int[] discardfile, Map<String, Double> questionmap) {
		double[] PreferencePi = new double[Alpha.length];
		double countAlpha = 0.0;
		for (int f = 0; f < Alpha.length; f++) {
			countAlpha += Alpha[f];
		}
		for (int q = 0; q < Alpha.length; q++) {
			PreferencePi[q] = Alpha[q] / countAlpha;
		}
		double min = 1000000000.0;
		int minIndex = 0;
		int count2 = 0;
		for (int z = 0; z < entityDocMatrixArray.length; z++) {

			double argMin = 0.0;
			if (!questionsIndex.contains(z)) {// exclude the already asked questions
				for (int i = 0; i < entityDocMatrixArray[z].length; i++) {
					if (discardfile[i] != 1) {
						if (entityDocMatrixArray[z][i] == 1)
							argMin = argMin + PreferencePi[i];
						if (entityDocMatrixArray[z][i] == 0)
							argMin = argMin - PreferencePi[i];
					}
				}

				double judge = Math.abs(argMin);
				if (questionmap.containsKey(enList.get(count2)))
					judge = Math.abs(argMin) - rewardParameter * questionmap.get(enList.get(count2));// judge if it is minimal	//no noise version																
				if (judge < min) {
					min = judge;
					minIndex = count2;
				}
			}
			count2++;
		}

		questionsIndex.add(minIndex);

		for (int k = 0; k < doc_qrel_relevance.size(); k++) {
			int indexM = docList.indexOf(doc_qrel_relevance.get(k) + "txt");
			if (indexM != -1) {
				if (entityDocMatrixArray[minIndex][indexM] == 0) {
					for (int m = 0; m < entityDocMatrixArray[minIndex].length; m++) {
						if (entityDocMatrixArray[minIndex][m] == 0) {
							Alpha[m] = Alpha[m] + updateAlpha;
						} else {
							discardfile[m] = 1;
						}
					}
				}

				else if (entityDocMatrixArray[minIndex][indexM] == 1) {
					for (int m1 = 0; m1 < entityDocMatrixArray[minIndex].length; m1++) {
						if (entityDocMatrixArray[minIndex][m1] == 1) {
							Alpha[m1] = Alpha[m1] + updateAlpha; 
						} else {
							discardfile[m1] = 1;
						}
					}
				}

			}
		}

		return PreferencePi;
	}

}
