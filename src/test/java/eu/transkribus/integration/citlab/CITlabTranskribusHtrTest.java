package eu.transkribus.integration.citlab;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.appserver.logic.TrpCITlabHtrReader;
import eu.transkribus.core.io.LocalDocReader;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextRegionType;

public class CITlabTranskribusHtrTest extends ACITlabTranskribusIntegrationTest {
	private static final Logger logger = LoggerFactory.getLogger(CITlabTranskribusHtrTest.class);
	/**
	 * just pick any model, use no dict, run recognition on one line to check basic functionality
	 * @throws IOException
	 */
	@Test
	public void htrTest() throws IOException {
		final File tmpDir = super.createTmpDir();
		TrpCITlabHtrReader reader = new TrpCITlabHtrReader(tmpDir);
		
		reader.loadHtr(htrs.get(0));
		
		File resource = new File(TestFiles.BENTHAM_1_PAGE_1_LINE_TEST_DOC_PATH);
		File destinationInTmpDir = new File(tmpDir.getAbsolutePath() + File.separator + resource.getName());
		FileUtils.copyDirectory(resource, destinationInTmpDir);		
		TrpDoc doc = LocalDocReader.load(destinationInTmpDir.getAbsolutePath());
		
		logger.info("Running HTR...");
		PcGtsType result = reader.process(doc.getPages().get(0));
		TrpRegionType region = result.getPage().getTextRegionOrImageRegionOrLineDrawingRegion().get(0);
		TrpTextRegionType tr = (TrpTextRegionType)region;
		final String text = tr.getTextLine().get(0).getTextEquiv().getUnicode();
		
		logger.info("HTR succeeded. Result = " + text);		
	}
	
}
