package eu.transkribus.integration.citlab;

import java.io.File;

public class Config {
	private final static String TMP_DIR_PATH = System.getProperty("java.io.tmpdir");
	public final static File TMP_DIR = new File(TMP_DIR_PATH);
	

	/**
	 * A param set for executing a training with 3 epochs very quickly
	 */
	public final static CitLabHtrTrainTestParams DEFAULT_TEST_TRAINING = new CitLabHtrTrainTestParams(
			3, //just very few epochs 
			"2e-3", //standard learn rate
			"both", //standard noise
			20, //reduced train size for quick training
			2 //nr of threads
			);
	
	
	/**
	 * Default param set used in Transkribus production
	 */
	public final static CitLabHtrTrainTestParams DEFAULT_TRANSKRIBUS_TRAINING = new CitLabHtrTrainTestParams(
			200,
			"2e-3",
			"both",
			1000,
			8
			);
	
	/**
	 * Immutable object that holds a configuration set for training the CITlab HTR
	 *
	 */
	public static class CitLabHtrTrainTestParams {
		private final int numEpochs;
		private final String learnRate;
		private final String noise;
		private final int trainSize;
		private final int threads;
		private CitLabHtrTrainTestParams (final int numEpochs, final String learnRate, 
				final String noise, final int trainSize, final int threads) {
			this.numEpochs = numEpochs;
			this.learnRate = learnRate;
			this.noise = noise;
			this.trainSize = trainSize;
			this.threads = threads;
		}
		public int getNumEpochs() {
			return numEpochs;
		}
		public String getLearnRate() {
			return learnRate;
		}
		public String getNoise() {
			return noise;
		}
		public int getTrainSize() {
			return trainSize;
		}
		public int getThreads() {
			return threads;
		}
		@Override
		public String toString() {
			return "TrainConfig [numEpochs=" + numEpochs + ", learnRate=" + learnRate + ", noise=" + noise
					+ ", trainSize=" + trainSize + ", threads=" + threads + "]";
		}
	}
}
