package uk.ac.open.kmi.discou;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.open.kmi.discou.videofinder.VideofinderInputCollectorBuilder;

@Ignore("requires http connection")
public class VideofinderIndexingTest {
	private static Logger logger = LoggerFactory.getLogger(VideofinderIndexingTest.class);
	private static URL testdir = VideofinderIndexingTest.class.getClassLoader().getResource(".");
	private static File index;
	private static File dir;

	@Rule
	public TestName name = new TestName();

	@BeforeClass
	public static void beforeClass() throws URISyntaxException {
		logger.info("[start] ");
		dir = new File(testdir.toURI());
		dir.mkdirs();
		logger.info("[init] Test dir is: {}", dir.getAbsolutePath());
	}

	@AfterClass
	public static void afterClass() {
		logger.info("[end] ");
	}

	private static boolean deleteDir(File dir) {
		// logger.info(" deleting {}", dir);
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}

	@Before
	public void before() {
		String indexName = "DP_indexes";
		index = new File(dir, indexName);
		index.mkdirs();
		index.deleteOnExit();
	}

	@After
	public void after() {
		logger.info("cleanup test dir: {}", deleteDir(index));
	}

	public SparqlInputCollectorBuilder getBuilder() {
		return new VideofinderInputCollectorBuilder().endpoint("http://sdata.kmi.open.ac.uk/videofinder/sparql").from("http://data.open.ac.uk/context/videofinder")
				.title("http://purl.org/dc/terms/title").type("http://data.open.ac.uk/videofinder/ontology/VideofinderObject").description("http://purl.org/dc/terms/description")
				.content("http://data.open.ac.uk/videofinder/ontology/synopsis").content("http://data.open.ac.uk/videofinder/ontology/transcript");
	}

	@Test
	public void testOneResource() throws IOException {
		logger.info("start {}", name.getMethodName());
		long start = System.currentTimeMillis();
		DiscouInputCollector ic = getBuilder().limit(1).build();
		DiscouIndexer i = new DiscouIndexer(index);
		// i.setDBPediaSpotlightServiceURL("http://kmi-dev04.open.ac.uk:6081/rest/annotate");
		DiscouInputResource ir = ic.next();
		i.open();
		i.put(ir);
		i.commit();
		i.close();
		logger.info("written in {} ms", (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		// test read now
		DiscouReader reader = new DiscouReader(index);
		reader.open();
		DiscouResource ires = reader.getFromURI(ir.getUri());
//		Assert.assertFalse("Title contains some entities", "".equals(ires.getTitle()));
		Assert.assertFalse("Content contains some entities", "".equals(ires.getContent()));
		logger.info("Indexed content: {}", ires.getContent());
		Assert.assertTrue(ires.getUri().equals(ir.getUri()));
		reader.close();
		logger.info("read in {} ms", (System.currentTimeMillis() - start));
		logger.info("end {}", name.getMethodName());
	}

	@Test
	public void testMultiResource() throws IOException {
		logger.info("start {}", name.getMethodName());
		DiscouInputCollector ic = getBuilder().limit(1).build();
		DiscouInputResource ir = null;
		DiscouIndexer i = new DiscouIndexer(index);
		//i.setDBPediaSpotlightServiceURL("http://kmi-dev04.open.ac.uk:6081/rest/annotate");
		i.open();
		List<DiscouInputResource> rss = new ArrayList<DiscouInputResource>();
		while (ic.hasNext()) {
			ir = ic.next();
			// ir = new DiscouInputResourceImpl(ir.getUri(), ir.getTitle(), ir.getDescription(), ir.getContent());
			if(ir == null) break;
			logger.info("\n\turi: {}\n\ttitle: {}\n\tdescription: {}\n\tcontent: {}", new Object[] { ir.getUri(), ir.getTitle(), ir.getDescription(), ir.getContent() });
			rss.add(ir);
			i.put(ir);
		}
		i.commit();
		i.close();
		Assert.assertNotNull(ir);
		ir = null;
		
		// test read now
		DiscouReader reader = new DiscouReader(index);
		reader.open();
		for (DiscouInputResource ti : rss) {
			DiscouResource ires = reader.getFromURI(ti.getUri());
			//if("".equals(ires.getTitle()))
			//Assert.assertFalse("Title contains some entities", "".equals(ires.getTitle()));
			Assert.assertFalse("Content contains some entities", "".equals(ires.getContent()));
			logger.info("Indexed content: {}", ires.getContent());
			Assert.assertTrue(ires.getUri().equals(ti.getUri()));
		}
		reader.close();

		logger.info("end {}", name.getMethodName());
	}

}
