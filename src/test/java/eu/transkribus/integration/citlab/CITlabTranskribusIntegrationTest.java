package eu.transkribus.integration.citlab;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.dea.fimgstoreclient.utils.MimeTypes;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uro.citlab.module.baseline2polygon.B2PSeamMultiOriented;
import de.uro.citlab.module.baseline2polygon.Baseline2PolygonParser;
import de.uro.citlab.module.train.TrainHtr;
import de.uro.citlab.module.types.Key;
import de.uro.citlab.module.util.PropertyUtil;
import eu.transkribus.core.io.DocExporter;
import eu.transkribus.core.io.DocExporter.ExportOptions;
import eu.transkribus.core.io.LocalDocReader;
import eu.transkribus.core.io.util.ImgPriority;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.core.util.HtrUtils;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.IBaseline2Polygon;
import eu.transkribus.interfaces.types.Image;

public class CITlabTranskribusIntegrationTest {
	private static final Logger logger = LoggerFactory.getLogger(CITlabTranskribusIntegrationTest.class);

	private static LinkedList<File> tmpDirs = new LinkedList<>();

	@Before
	public void checkResources() {
		for (String path : TestFiles.TEST_DOC_NAMES) {
			Assert.assertTrue(new File(path).canRead());
		}
		logger.info("Resource check is done.");
	}

	@Before
	public void loadOpenCv() {
		try {
			System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
			logger.info("OpenCV name: " + Core.NATIVE_LIBRARY_NAME);
		} catch (java.lang.UnsatisfiedLinkError error) {
			Assert.fail("Could not load OpenCv!");
		}
	}

	@Before
	public void checkTmpDirWriteable() {
		Assert.assertTrue(Config.TMP_DIR.canWrite());
	}

	/**
	 * Load all documents in TEST_DOC_PATHS to ensure they are valid Transkribus
	 * documents Then try HTR Training workflow
	 */
	@Test
	public void testHtrTrainWorkflow() {
		IBaseline2Polygon laParser = new Baseline2PolygonParser(B2PSeamMultiOriented.class.getName());
		for (String path : TestFiles.TEST_DOC_NAMES) {
			TrpDoc doc = null;
			try {
				doc = LocalDocReader.load(path);
				Assert.assertNotNull(doc);
			} catch (IOException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
			// check pages
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
			Assert.assertTrue("Could not create tmpDir at: " + tmpDir.getAbsolutePath(), tmpDir.mkdir());
			logger.info("Created tmp dir at: " + tmpDir.getAbsolutePath());
			tmpDirs.add(tmpDir);

			// init trainer and properties
			TrainHtr trainer = new TrainHtr();
			String[] createTrainDataProps = PropertyUtil.setProperty(null, "dict", "true");
			createTrainDataProps = PropertyUtil.setProperty(createTrainDataProps, "stat", "true");

			// init train input path
			final String trainInputPath = tmpDir.getAbsolutePath() + File.separator + "trainInput";
			File trainInputDir = new File(trainInputPath);
			trainInputDir.mkdir();

			// init traindata Path
			final String trainDataPath = tmpDir.getAbsolutePath() + File.separator + "trainData";
			File trainDataDir = new File(trainDataPath);
			trainDataDir.mkdir();

			// init docExporter
			DocExporter ex = new DocExporter();
			ExportOptions opts = Config.HTR_TRAIN_EXPORT_OPTIONS;
			opts.dir = trainInputPath;

			// pages [1, N-1] are train pages, page N is test page
			final String trainPages = "1-" + (doc.getNPages() - 1);
			final String testPage = "" + doc.getNPages();

			// export gt document
			try {
				opts.pageIndices = CoreUtils.parseRangeListStr(trainPages, doc.getNPages());
			} catch (IOException e2) {
				Assert.fail("Bad range string: " + trainPages);
			}
			try {
				ex.exportDoc(doc, opts);
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail("Could not export Train GT document to: " + e.getMessage());
			}

			// process baseline2polygon on all page XMLs
			String[] input = null;
			try {
				input = baseline2polygon(laParser, trainInputDir);
			} catch (IOException ioe) {
				Assert.fail("Baseline2polygon failed! " + ioe.getMessage());
			}

			// init path
			final String testInputPath = tmpDir.getAbsolutePath() + File.separator + "testInput";
			File testInputDir = new File(testInputPath);
			testInputDir.mkdir();

			// init path for line imgs
			String testDataPath = tmpDir.getAbsolutePath() + File.separator + "testData";
			File testDataDir = new File(testDataPath);
			testDataDir.mkdir();

			// set exporter opts
			opts.dir = testInputPath;
			try {
				opts.pageIndices = CoreUtils.parseRangeListStr(testPage, doc.getNPages());
			} catch (IOException e1) {
				Assert.fail("Bad range string: " + testPage);
			}

			try {
				ex.exportDoc(doc, opts);
			} catch (Exception e) {
				Assert.fail("Could not export Test GT document to: " + e.getMessage());
			}

			// process baseline2polygon on all page XMLs
			String[] valInput = null;
			try {
				valInput = baseline2polygon(laParser, testInputDir);
			} catch (IOException ioe) {
				Assert.fail("Baseline2polygon failed! " + ioe.getMessage());
			}

			final String charMapFileName = "chars.txt";
			// create Test Data
			logger.info("Create test data...");
			trainer.createTrainData(valInput, testDataPath,
					testDataPath + File.separator + charMapFileName, createTrainDataProps);

			File charMapFile = new File(trainDataPath + File.separator + charMapFileName);

			logger.info("Create train data...");
			trainer.createTrainData(input, trainDataPath, charMapFile.getAbsolutePath(), createTrainDataProps);

			File htrInFile = new File(tmpDir.getAbsolutePath() + File.separator + "net_in.sprnn");
			
			logger.info("Create HTR...");
			trainer.createHtr(htrInFile.getAbsolutePath(), charMapFile.getAbsolutePath(), null);

			Assert.assertTrue("Input HTR was not created!", htrInFile.exists());

			String cerFilePath = tmpDir.getAbsolutePath() + File.separator + "CER.txt";
			File cerFile = new File(cerFilePath);

			String cerTestFilePath = tmpDir.getAbsolutePath() + File.separator + "CER_test.txt";
			File cerTestFile = new File(cerTestFilePath);

			File htrOutFile = new File(tmpDir.getAbsolutePath() + File.separator + "net.sprnn");
			String[] htrTrainProps = PropertyUtil.setProperty(null, Key.EPOCHS, "" + Config.NUM_EPOCHS); // "200");
																												// //5;2");
			htrTrainProps = PropertyUtil.setProperty(htrTrainProps, Key.LEARNINGRATE, Config.LEARN_RATE); // "2e-3");
																													// //5e-3;1e-3");
			htrTrainProps = PropertyUtil.setProperty(htrTrainProps, Key.NOISE, Config.NOISE); // "no");
			htrTrainProps = PropertyUtil.setProperty(htrTrainProps, Key.THREADS, "" + Config.THREADS);
			htrTrainProps = PropertyUtil.setProperty(htrTrainProps, Key.TRAINSIZE, "" + Config.TRAIN_SIZE); // "1000");

			htrTrainProps = PropertyUtil.setProperty(htrTrainProps, Key.PATH_TRAIN_LOG, cerFile.getAbsolutePath());
			htrTrainProps = PropertyUtil.setProperty(htrTrainProps, Key.PATH_TEST_LOG, cerTestFile.getAbsolutePath());
			
			logger.debug("Train HTR...");
			trainer.trainHtr(htrInFile.getAbsolutePath(), htrOutFile.getAbsolutePath(), trainDataPath, testDataPath,
						htrTrainProps);
			
			//now do final checks
			Assert.assertTrue("CerFile was not written!", cerFile.exists() && cerFile.length() > 0);
			double[] cerVals = null;
			try {
				cerVals = HtrUtils.parseCitlabCerFile(cerFile);
			} catch (IOException e) {
				Assert.fail("Could not parse cerFile!");
			}
			Assert.assertNotNull(cerVals);
			Assert.assertEquals("CER Log does not contain one value per epoch!", Config.NUM_EPOCHS, cerVals.length);
			
		}
	}

	@After
	public void cleanup() {
		for (File dir : tmpDirs) {
			try {
				FileUtils.deleteDirectory(dir);
			} catch (IOException e) {
			}
		}
		logger.info("Cleanup done.");
	}

	/**
	 * FIXME For now just a copy of the method from training job
	 * 
	 * @param laParser
	 * @param inputDir
	 * @return
	 * @throws IOException
	 */
	private String[] baseline2polygon(IBaseline2Polygon laParser, File inputDir) throws IOException {
		String[] pageXmls = inputDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return !name.equals("metadata.xml") && name.endsWith(".xml");
			}
		});

		ArrayList<String> inputList = new ArrayList<>(pageXmls.length);
		for (String p : pageXmls) {
			final String path = inputDir.getAbsolutePath() + File.separator + p;

			final String basename = FilenameUtils.getBaseName(p);

			String[] hits = inputDir.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					final String mime = MimeTypes.getMimeType(FilenameUtils.getExtension(name));
					// is allowed mimetype and not starts with ".", which may
					// occur on mac
					return name.startsWith(basename) && ImgPriority.priorities.containsKey(mime);
				}
			});

			if (hits.length != 1) {
				throw new IOException("No image found for page XML: " + path);
			}

			File imgFile = new File(inputDir.getAbsolutePath() + File.separator + hits[0]);

			Image img;
			try {
				img = new Image(imgFile.toURI().toURL());
			} catch (MalformedURLException e) {
				logger.error("Could not build URL: " + imgFile.getAbsolutePath(), e);
				continue;
			}
			
			logger.debug("Running B2P on: " + imgFile.getAbsolutePath());
			try {
				laParser.process(img, path, null, null);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}

			inputList.add(path);
		}
		String[] input = inputList.toArray(new String[inputList.size()]);
		return input;
	}
}
