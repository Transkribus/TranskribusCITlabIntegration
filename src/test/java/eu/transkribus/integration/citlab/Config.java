package eu.transkribus.integration.citlab;

import java.io.File;

import eu.transkribus.core.io.DocExporter.ExportOptions;

public class Config {
	public final static ExportOptions HTR_TRAIN_EXPORT_OPTIONS = new ExportOptions();
	private final static String TMP_DIR_PATH = System.getProperty("java.io.tmpdir");
	public final static File TMP_DIR = new File(TMP_DIR_PATH);
	
	public final static int NUM_EPOCHS = 3;
	public final static String LEARN_RATE = "2e-3";
	public final static String NOISE = "both";
	public final static int TRAIN_SIZE = 1000;
	public final static int THREADS = 2;
	
	static {
		HTR_TRAIN_EXPORT_OPTIONS.doWriteImages = true;
		HTR_TRAIN_EXPORT_OPTIONS.exportAltoXml = false;
		HTR_TRAIN_EXPORT_OPTIONS.exportPageXml = true;
		HTR_TRAIN_EXPORT_OPTIONS.pageDirName = "";
		HTR_TRAIN_EXPORT_OPTIONS.useOcrMasterDir = false;
		HTR_TRAIN_EXPORT_OPTIONS.writeMets = false;
	}
}
