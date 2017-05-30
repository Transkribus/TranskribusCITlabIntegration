package eu.transkribus.integration.citlab;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uro.citlab.module.train.TrainHtr;
import de.uro.citlab.module.types.Key;
import de.uro.citlab.module.util.PropertyUtil;
import eu.transkribus.appserver.logic.TrpCITlabHtrTrainer;
import eu.transkribus.appserver.logic.util.B2PUtils;
import eu.transkribus.core.io.DocExporter;
import eu.transkribus.core.io.LocalDocReader;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.builder.CommonExportPars;
import eu.transkribus.core.util.HtrUtils;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.integration.citlab.Config.CitLabHtrTrainTestParams;
import eu.transkribus.interfaces.IBaseline2Polygon;

public class CITlabTranskribusHtrTrainTest extends ACITlabTranskribusIntegrationTest {
	private static final Logger logger = LoggerFactory.getLogger(CITlabTranskribusHtrTrainTest.class);

	/**
	 * Load all documents in TEST_DOC_PATHS to ensure they are valid Transkribus
	 * documents Then try HTR Training workflow
	 * 
	 * @throws IOException
	 */
//	@Test
	public void testHtrTrainWorkflow() throws IOException {
		IBaseline2Polygon laParser = B2PUtils.getDefaultCITlabB2P();
		for (String path : TestFiles.TRAIN_TEST_DOC_PATHS) {
			// try to load the document
			TrpDoc doc = null;
			try {
				doc = LocalDocReader.load(path);
				Assert.assertNotNull(doc);
			} catch (IOException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
			// check all page XMLs
			for (TrpPage page : doc.getPages()) {
				URL xmlUrl = page.getCurrentTranscript().getUrl();
				try {
					PcGtsType pageXml = PageXmlUtils.unmarshal(xmlUrl);
					Assert.assertNotNull(pageXml);
				} catch (JAXBException e) {
					e.printStackTrace();
					Assert.fail("Page XML load failed for file: " + xmlUrl + " | " + e.getMessage());
				}
			}

			File tmpDir = new File(Config.TMP_DIR.getAbsolutePath() + File.separator + UUID.randomUUID());
			if (!tmpDir.mkdir()) {
				throw new IOException("Could not create tmpDir at: " + tmpDir.getAbsolutePath());
			}
			logger.info("Created tmp dir at: " + tmpDir.getAbsolutePath());
			tmpDirs.add(tmpDir);

			// init trainer and properties
			TrainHtr trainer = new TrainHtr();
			String[] createTrainDataProps = PropertyUtil.setProperty(null, "dict", "true");
			createTrainDataProps = PropertyUtil.setProperty(createTrainDataProps, "stat", "true");

			final String basePath = tmpDir.getAbsolutePath() + File.separator;

			// init train input path
			final String trainInputPath = basePath + "trainInput";
			File trainInputDir = new File(trainInputPath);

			// init traindata Path
			final String trainDataPath = basePath + "trainData";
			File trainDataDir = new File(trainDataPath);

			// init path
			final String testInputPath = basePath + "testInput";
			File testInputDir = new File(testInputPath);

			// init path for line imgs
			String testDataPath = basePath + "testData";
			File testDataDir = new File(testDataPath);

			if (!trainInputDir.mkdir() || !trainDataDir.mkdir() || !testInputDir.mkdir() || !testDataDir.mkdir()) {
				throw new IOException("Could not create all directories in: " + basePath);
			}

			// pages [1, N-1] are train pages, page N is test page
			final String trainPages = "1-" + (doc.getNPages() - 1);
			final String testPage = "" + doc.getNPages();

			// init docExporter
			DocExporter ex = new DocExporter();
			CommonExportPars trainExportPars = CommonExportPars.getDefaultParSetForHtrTraining();
			trainExportPars.setDir(trainInputPath);
			trainExportPars.setPages(trainPages);

			try {
				ex.exportDoc(doc, trainExportPars);
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail("Could not export Train GT document to: " + trainExportPars.getDir());
			}

			// process baseline2polygon on all page XMLs
			String[] input = null;
			try {
				input = B2PUtils.baseline2polygon(laParser, trainInputDir);
			} catch (IOException ioe) {
				Assert.fail("Baseline2polygon failed! " + ioe.getMessage());
			}

			// set exporter opts
			CommonExportPars testExportPars = CommonExportPars.getDefaultParSetForHtrTraining();
			testExportPars.setDir(testInputPath);
			testExportPars.setPages(testPage);

			try {
				ex.exportDoc(doc, trainExportPars);
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail("Could not export Test GT document to: " + e.getMessage());
			}

			// process baseline2polygon on all page XMLs
			String[] valInput = null;
			try {
				valInput = B2PUtils.baseline2polygon(laParser, testInputDir);
			} catch (IOException ioe) {
				Assert.fail("Baseline2polygon failed! " + ioe.getMessage());
			}

			final String charMapFileName = "chars.txt";
			File charMapTrainFile = new File(trainDataPath + File.separator + charMapFileName);
			File charMapTestFile = new File(testDataPath + File.separator + charMapFileName);
			// create Test Data
			logger.info("Create test data...");
			trainer.createTrainData(valInput, testDataPath, charMapTestFile.getAbsolutePath(), createTrainDataProps);

			logger.info("Create train data...");
			trainer.createTrainData(input, trainDataPath, charMapTrainFile.getAbsolutePath(), createTrainDataProps);

			File htrInFile = new File(tmpDir.getAbsolutePath() + File.separator + "net_in.sprnn");

			logger.info("Create HTR...");
			trainer.createHtr(htrInFile.getAbsolutePath(), charMapTrainFile.getAbsolutePath(), null);

			Assert.assertTrue("Input HTR was not created!", htrInFile.exists());

			String cerFilePath = tmpDir.getAbsolutePath() + File.separator + "CER.txt";
			File cerFile = new File(cerFilePath);

			String cerTestFilePath = tmpDir.getAbsolutePath() + File.separator + "CER_test.txt";
			File cerTestFile = new File(cerTestFilePath);

			final CitLabHtrTrainTestParams params = Config.DEFAULT_TEST_TRAINING;
			
			File htrOutFile = new File(tmpDir.getAbsolutePath() + File.separator + "net.sprnn");
			String[] htrTrainProps = PropertyUtil.setProperty(null, Key.EPOCHS, "" + params.getNumEpochs());
			htrTrainProps = PropertyUtil.setProperty(htrTrainProps, Key.LEARNINGRATE, params.getLearnRate());
			htrTrainProps = PropertyUtil.setProperty(htrTrainProps, Key.NOISE, params.getNoise());
			htrTrainProps = PropertyUtil.setProperty(htrTrainProps, Key.THREADS, "" + params.getThreads());
			htrTrainProps = PropertyUtil.setProperty(htrTrainProps, Key.TRAINSIZE, "" + params.getTrainSize());
			htrTrainProps = PropertyUtil.setProperty(htrTrainProps, Key.PATH_TRAIN_LOG, cerFile.getAbsolutePath());
			htrTrainProps = PropertyUtil.setProperty(htrTrainProps, Key.PATH_TEST_LOG, cerTestFile.getAbsolutePath());

			logger.debug("Train HTR...");
			trainer.trainHtr(htrInFile.getAbsolutePath(), htrOutFile.getAbsolutePath(), trainDataPath, testDataPath,
					htrTrainProps);

			// now do final checks
			Assert.assertTrue("CerFile was not written!", cerFile.exists() && cerFile.length() > 0);
			double[] cerVals = null;
			try {
				cerVals = HtrUtils.parseCitlabCerFile(cerFile);
			} catch (IOException e) {
				Assert.fail("Could not parse cerFile!");
			}
			Assert.assertEquals("CER Log does not contain one value per epoch!", params.getNumEpochs(), cerVals.length);
		}
	}
	
	@Test
	public void testNewHtrTrainWorkflow() throws IOException {
		
		for (String path : TestFiles.TRAIN_TEST_DOC_PATHS) {
			// try to load the document
			TrpDoc doc = null;
			try {
				doc = LocalDocReader.load(path);
				Assert.assertNotNull(doc);
			} catch (IOException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
			// check all page XMLs
			for (TrpPage page : doc.getPages()) {
				URL xmlUrl = page.getCurrentTranscript().getUrl();
				try {
					PcGtsType pageXml = PageXmlUtils.unmarshal(xmlUrl);
					Assert.assertNotNull(pageXml);
				} catch (JAXBException e) {
					e.printStackTrace();
					Assert.fail("Page XML load failed for file: " + xmlUrl + " | " + e.getMessage());
				}
			}

			File tmpDir = super.createTmpDir();
			
			TrpCITlabHtrTrainer trainer = new TrpCITlabHtrTrainer(tmpDir);
			trainer.setTrainDoc(doc);
			trainer.setNumEpochs(3);
			trainer.setNoise("both");
			trainer.setLearnRate("2e-3");
			trainer.setTrainSize(20);
			trainer.setThreads(2);
			
			// pages [1, N-1] are train pages, page N is test page
			final String trainPages = "1-" + (doc.getNPages() - 1);
			
			logger.info("Store files locally...");
			trainer.storeTrainAndTestInputFromTrainDoc(trainPages);
			logger.info("Run b2p...");
			trainer.runBaseline2PolygonOnInput();
			logger.info("Create train and test data...");
			trainer.createTrainAndTestData();
			logger.info("Create HTR...");
			trainer.createHtr();
			logger.info("Train HTR...");
			trainer.runTraining();
		
			//test moveFiles()
			File testStorageDir = new File(tmpDir.getAbsolutePath() + "_storage");
			if(!testStorageDir.mkdir()) {
				throw new IOException("Could not create tmp storage dir: " + testStorageDir.getAbsolutePath());
			}
			logger.info("Moving files to: " + testStorageDir.getAbsolutePath());
			//add this to directories for removal
			tmpDirs.add(testStorageDir);
			File[] files = {};
			try {
				files = trainer.moveFiles(testStorageDir);
			} catch(Exception e) {
				Assert.fail("Could not move files to test storage dir! " + e.getMessage());
			}
			for(File f : files) {
				Assert.assertTrue("Could not read stored file: " + f.getAbsolutePath(), f.canRead());
			}
			
			File cerFile = trainer.getCerLogFile();
			// now do final checks
			Assert.assertTrue("CerFile was not written!", cerFile.exists() && cerFile.length() > 0);
			double[] cerVals = null;
			try {
				cerVals = HtrUtils.parseCitlabCerFile(cerFile);
			} catch (IOException e) {
				Assert.fail("Could not parse cerFile!");
			}
			Assert.assertEquals("CER Log does not contain one value per epoch!", trainer.getNumEpochs(), cerVals.length);
		}
	}
}
