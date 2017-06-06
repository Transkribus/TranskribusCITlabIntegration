package eu.transkribus.integration.citlab;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.appserver.logic.TrpCITlabHtrReader;
import eu.transkribus.core.io.LocalDocReader;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.TrpTranscriptMetadata;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextRegionType;

public class AsvDictionaryTest extends ACITlabTranskribusIntegrationTest {
	private static final Logger logger = LoggerFactory.getLogger(AsvDictionaryTest.class);
	/**
	 * pick a model and try out all dictionaries in dictDir (see test.properties) on a single line recognition
	 * @throws IOException
	 */
	@Test
	public void htrTest() throws IOException {
		final File tmpDir = super.createTmpDir();
		TrpCITlabHtrReader reader = new TrpCITlabHtrReader(tmpDir);
		
		reader.loadHtr(htrs.get(0));
		
		File destinationInTmpDir = super.createCopyOfTestResource(tmpDir, TestFiles.BENTHAM_1_PAGE_1_LINE_TEST_DOC_PATH);
		
		TrpDoc doc = LocalDocReader.load(destinationInTmpDir.getAbsolutePath());
		
		TrpPage page = doc.getPages().get(0);
		TrpTranscriptMetadata tmd = page.getCurrentTranscript();
		
		Map<String, Throwable> failedDicts = new HashMap<>();
		
		for(File dictFile : dictList) {
			logger.info("Running HTR with dict: " + dictFile.getName());
			try {
				reader.loadDict(dictFile);

				PcGtsType result = reader.process(page, tmd);
				TrpRegionType region = result.getPage().getTextRegionOrImageRegionOrLineDrawingRegion().get(0);
				TrpTextRegionType tr = (TrpTextRegionType)region;
				final String text = tr.getTextLine().get(0).getTextEquiv().getUnicode();
				
				logger.info("HTR succeeded. Result = " + text);
			} catch (Throwable t) {
				logger.error("HTR failed with dict = " + dictFile.getName(), t);
				failedDicts.put(dictFile.getName(), t);
			}
		}
		if(!failedDicts.isEmpty()) {
			logger.error("Error Summary:");
			for(Entry<String, Throwable> e : failedDicts.entrySet()) {
				logger.error(e.getKey() + ": " + e.getValue().getMessage());
			}
		}
		Assert.assertTrue("Some dicts failed!", failedDicts.isEmpty());
	}
}
