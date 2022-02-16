package net.sourceforge.jex.xmlMVC;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TestBOM {
	private static final byte[] UTF8BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, };
	private static final String target = "target/test-classes/";

	@BeforeAll
	static void setUpBeforeClass() throws IOException {
		String output = target + "pom-with-bom.xml";
		String input = target + "pom.xml";
		
		File f = new File(output);
		if (!f.exists()) {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(input));
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output));
			bos.write(UTF8BOM);
			IOUtils.copy(bis, bos);
			bis.close();
			bos.close();
		}
	}
	
	@Test
	void test() {
		String input = target + "pom.xml";
		
		File xmlFile = new File(input);
		assertTrue(xmlFile.exists());
		XMLModel model = new XMLModel(false);
		model.openXMLFile(xmlFile);
		XmlNode root = model.getRootNode();
		assertEquals(root.getXmlElement().getName(), "project");
				
	}

	@Test
	void testBOM() {
		String input = target + "pom-with-bom.xml";
		
		File xmlFile = new File(input);
		assertTrue(xmlFile.exists());
		XMLModel model = new XMLModel(false);
		model.openXMLFile(xmlFile);
		XmlNode root = model.getRootNode();
		assertEquals(root.getXmlElement().getName(), "project");
				
	}

}
