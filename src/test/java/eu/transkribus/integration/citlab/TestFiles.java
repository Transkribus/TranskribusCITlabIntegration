package eu.transkribus.integration.citlab;

public class TestFiles {
	public final static String BASE_PATH = "src/test/resources/";
	
	public final static String BENTHAM_6_PAGES_TEST_DOC_NAME = "Batch1-Final_p1-p6";
	public final static String BENTHAM_6_PAGES_TEST_DOC_PATH = BASE_PATH + BENTHAM_6_PAGES_TEST_DOC_NAME;
	
	public final static String KONZILSPROTOKOLLE_5_PAGES_TEST_DOC_NAME = "TEST_CITlab_Konzilsprotokolle_M4_p1-p5";
	public final static String KONZILSPROTOKOLLE_5_PAGES_TEST_DOC_PATH = BASE_PATH + 
			KONZILSPROTOKOLLE_5_PAGES_TEST_DOC_NAME;
	
	public final static String BENTHAM_1_PAGE_1_LINE_TEST_DOC_NAME = "Bentham_box35_1page_1line";
	public final static String BENTHAM_1_PAGE_1_LINE_TEST_DOC_PATH = BASE_PATH + 
			BENTHAM_1_PAGE_1_LINE_TEST_DOC_NAME;
	
	/**
	 * Those docs will be used for training tests
	 */
	public final static String[] TRAIN_TEST_DOC_PATHS = {
			BENTHAM_6_PAGES_TEST_DOC_PATH
			};
	
	public final static String HTR_BASE_PATH = BASE_PATH + "HTR/";
	public final static String BENTHAM_MODEL_NAME = "Bentham";
	public final static String KONZILSPROTOKOLLE_MODEL_NAME = "Konzilsprotokolle_M4";
	/**
	 * Those HTRs will be used in all recognition tests
	 */
	public final static String[] HTR_PATHS = {
			HTR_BASE_PATH + BENTHAM_MODEL_NAME,
			HTR_BASE_PATH + KONZILSPROTOKOLLE_MODEL_NAME
	};
}
