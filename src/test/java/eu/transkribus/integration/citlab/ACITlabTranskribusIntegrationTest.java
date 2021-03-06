package eu.transkribus.integration.citlab;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.planet.tech.trainer.Trainer;
import eu.transkribus.core.model.beans.TrpHtr;
import eu.transkribus.core.util.HtrCITlabUtils;
import eu.transkribus.core.util.HtrCITlabUtils.DictFileFilter;
import eu.transkribus.persistence.logic.HtrManager;

public abstract class ACITlabTranskribusIntegrationTest {
	private static final Logger logger = LoggerFactory.getLogger(ACITlabTranskribusIntegrationTest.class);

	protected static List<File> tmpDirs = new LinkedList<>();
	protected static List<TrpHtr> htrs = new LinkedList<>();
	protected static List<File> dictList = new ArrayList<>(0);

	@Before
	public void checkPlanetJar() throws URISyntaxException {
		URL url = Trainer.class.getProtectionDomain().getCodeSource().getLocation();
		File planetJarFile = new File(url.toURI());
		logger.info("Planet Jar name = " + planetJarFile.getName());
		logger.info("Last modified = " + new Date(planetJarFile.lastModified()));
	}

	@Before
	public void checkResources() {
		for (String path : TestFiles.TRAIN_TEST_DOC_PATHS) {
			Assert.assertTrue(new File(path).canRead());
		}
		logger.info("Resource check is done.");
	}

	@Before
	public void loadHtrs() throws FileNotFoundException {
		for (String path : TestFiles.HTR_PATHS) {
			File dir = new File(path);
			if (!dir.canRead() || !dir.isDirectory()) {
				throw new FileNotFoundException("Specified HTR does not exist at: " + path);
			}
			if (!new File(path + File.separator + HtrCITlabUtils.CITLAB_SPRNN_FILENAME).exists()) {
				throw new FileNotFoundException("Specified HTR does not include a net! path = " + path);
			}
			// set data that is necessary for testing
			TrpHtr htr = new TrpHtr();
			htr.setPath(path);
			htr.setName(dir.getName());
			htr.setProvider(HtrCITlabUtils.PROVIDER_CITLAB);
			HtrManager.loadDataFromFileSystem(htr);

			htrs.add(htr);
		}
	}

	/**
	 * 
	 * TODO use TrpDict objects instead of Files
	 * @throws FileNotFoundException
	 */
	@Before
	public void loadDicts() throws FileNotFoundException {
		File dictDir = new File(Config.getDictPath());
		if (!dictDir.isDirectory()) {
			throw new FileNotFoundException(
					"Check your config. Dictionary path is not a directory: " + Config.getDictPath());
		}
		File[] dictFiles = dictDir.listFiles(new DictFileFilter());
		dictList = Arrays.asList(dictFiles);
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

	public File createTmpDir() throws IOException {
		File tmpDir = new File(Config.TMP_DIR.getAbsolutePath() + File.separator + UUID.randomUUID());
		if (!tmpDir.mkdir()) {
			throw new IOException("Could not create tmpDir at: " + tmpDir.getAbsolutePath());
		}
		logger.info("Created tmp dir at: " + tmpDir.getAbsolutePath());
		tmpDirs.add(tmpDir);
		return tmpDir;
	}

	protected File createCopyOfTestResource(File tmpDir, String resourcePath) throws IOException {
		File resource = new File(resourcePath);
		File destinationInTmpDir = new File(tmpDir.getAbsolutePath() + File.separator + resource.getName());
		FileUtils.copyDirectory(resource, destinationInTmpDir);
		return destinationInTmpDir;
	}
}
