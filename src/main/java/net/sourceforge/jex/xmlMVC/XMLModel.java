package net.sourceforge.jex.xmlMVC;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.io.*;

import javax.swing.JPanel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.input.BOMInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLModel implements XMLUpdateObserver, SelectionObserver{
	
	public static final String INPUT = "input";
	public static final String PROTOCOL = "protocol";
		
	private Document document; 
	private Document outputDocument;
	private Tree tree;			// tree model of template
	private Tree importTree;	// tree of a file used for importing fields
	
	private File currentFile;
	private boolean currentFileEdited;
	
	private ArrayList<XMLUpdateObserver> xmlObservers;
	private ArrayList<SelectionObserver> selectionObservers = new ArrayList<SelectionObserver>();;
	
	
	public static void main(String args[]) {
		new XMLModel();
	}
	
	
	// default constructor, instantiates empty ArrayList, then creates new View. 
	public XMLModel() {
		initModel(true);
	}
	
	public XMLModel(boolean intro) {
		initModel(intro);
	}

	private void initModel(boolean intro) {
		currentFile = new File("file");
		
		xmlObservers = new ArrayList<XMLUpdateObserver>();
		
		initialiseBlankDOMdoc();
		
		tree = new Tree(this, this);

		new XMLView(this, intro);
	}	
	
	public void openXMLFile(File xmlFile) {

		currentFile = xmlFile;
		
		readXMLtoDOM(xmlFile);	// overwrites document
		
		tree = new Tree(document, this, this);
		
		document = null;

		notifyXMLObservers();
	}
	
	public void notifyXMLObservers() {
		for (XMLUpdateObserver xmlObserver: xmlObservers) {
			xmlObserver.xmlUpdated();
		}
	}
	public void xmlUpdated() {
		currentFileEdited = true;
		notifyXMLObservers();
	}
	public void selectionChanged() {
		notifySelectionObservers();
	}
	public void notifySelectionObservers(){
		for (SelectionObserver selectionObserver: selectionObservers) {
			selectionObserver.selectionChanged();
		}
	}
	
	public void addXMLObserver(XMLUpdateObserver newXMLObserver) {
		xmlObservers.add(newXMLObserver);
	}
	public void addSelectionObserver(SelectionObserver newSelectionObserver) {
		selectionObservers.add(newSelectionObserver);
	}
	
	// used by default constructor to initialise blank document
	public void initialiseBlankDOMdoc() {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.newDocument();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
	}
	
	public void readXMLtoDOM(File xmlFile) {
		DocumentBuilderFactory factory =
            DocumentBuilderFactory.newInstance();
        //factory.setValidating(true);   
        //factory.setNamespaceAware(true);
        BOMInputStream bis = null;
        try {
           DocumentBuilder builder = factory.newDocumentBuilder();

           builder.setErrorHandler(
                   new org.xml.sax.ErrorHandler() {
                       // ignore fatal errors (an exception is guaranteed)
                       public void fatalError(SAXParseException exception)
                       throws SAXException {
                       }

                       // treat validation errors as fatal
                       public void error(SAXParseException e)
                       throws SAXParseException
                       {
                         throw e;
                       }

                       // dump warnings too
                       public void warning(SAXParseException err)
                       throws SAXParseException
                       {
                         System.out.println("** Warning"
                            + ", line " + err.getLineNumber()
                            + ", uri " + err.getSystemId());
                         System.out.println("   " + err.getMessage());
                       }
                   }
                 ); 

           bis = new BOMInputStream(new FileInputStream(xmlFile));
           document = builder.parse( bis );
           bis.close();
           bis = null;
           
        } catch (SAXException sxe) {
            // Error generated during parsing)
            Exception  x = sxe;
            if (sxe.getException() != null)
                x = sxe.getException();
            x.printStackTrace();

         } catch (ParserConfigurationException pce) {
             // Parser with specified options can't be built
             pce.printStackTrace();

         } catch (IOException ioe) {
            // I/O error
            ioe.printStackTrace();
         } finally {
             if (bis != null) {
             	try {
 					bis.close();
 				} catch (IOException e) {
 					e.printStackTrace();
 				}
             }
         }
	}
	
	public Tree getTreeFromNewFile(File xmlFile) {
		
		readXMLtoDOM(xmlFile);	// overwrites document
		
		Tree tree = new Tree(document);
		
		document = null; 	// release the memory
		
		return tree;
	}
	
	public void setImportTree(Tree tree) {
		
		importTree = tree;
		
		if (tree == null) return;
		
	}
	
	// import the selected nodes of the tree, or if none selected, import it all!
	public void importElementsFromImportTree() {
		if (importTree.getHighlightedFields().size() > 0) {
			tree.copyAndInsertElements(importTree.getHighlightedFields());
		}
			
		else {
			tree.copyAndInsertElement(importTree.getRootNode()); 
		}
		
		notifyXMLObservers();
		}
	
	public void writeTreeToDOM() {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			outputDocument = db.newDocument();
			Element protocol = outputDocument.createElement(PROTOCOL);
		} catch (Exception ex) { 
			ex.printStackTrace();
		}
		
		tree.buildDOMfromTree(outputDocument);
	} 
	
	public void transformXmlToHtml() {
		
		File outputXmlFile = new File("file");
		
		saveTreeToXmlFile(outputXmlFile);
		
		// opens the HTML in a browser window
		XmlTransform.transformXMLtoHTML(outputXmlFile);
	}

	public void saveTreeToXmlFile(File outputFile) {
		
		writeTreeToDOM();
		
		try {
			
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			Source source = new DOMSource(outputDocument);
			Result output = new StreamResult(outputFile);
			transformer.transform(source, output);
			
			setCurrentFile(outputFile);	// remember the current file. 
			
			currentFileEdited = false;
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void printDOM( Document docToPrint) {
		try {
			
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			Source source = new DOMSource( docToPrint );
			Result output = new StreamResult( System.out );
			transformer.transform(source, output);
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.out.println("");
	}

	
	// start a blank Xml file
	public void openBlankXmlFile() {
		tree.openBlankXmlFile();
		setCurrentFile(new File(""));	// no current file
		notifyXMLObservers();
	}

	
	// add a new xmlElement after the specified xmlElement
	public void addDataField() {
		tree.addElement();
		notifyXMLObservers();
	}
	
	// duplicate a xmlElement and add it at specified index
	public void duplicateDataFields() {
		tree.duplicateAndInsertElements();
		notifyXMLObservers();
	}
	
	
	
	// delete the highlighted dataFields
	public void deleteDataFields(boolean saveChildren) {
		tree.deleteElements(saveChildren);
		notifyXMLObservers();
	}
	
	public void demoteDataFields() {
		tree.demoteDataFields();
		notifyXMLObservers();
	}
	
	public void promoteDataFields() {
		tree.promoteDataFields();
		notifyXMLObservers();
	}
	
//	 if the highlighted fields have a preceeding sister, move it below the highlighted fields
	public void moveFieldsUp() {
		tree.moveFieldsUp();
		notifyXMLObservers();
	}
	
//	 if the highlighted fields have a preceeding sister, move it below the highlighted fields
	public void moveFieldsDown() {
		tree.moveFieldsDown();
		notifyXMLObservers();
	}
	
	public void setXmlEdited(boolean xmlEdited) {
		currentFileEdited = xmlEdited;
	}
	public boolean isCurrentFileEdited() {
		return currentFileEdited;
	}

	// called when saving, then used to set protocolFileName attribute
	public void setCurrentFile(File file) {
		currentFile = file;
	}
	public File getCurrentFile() {
		return currentFile;
	}
	
	
	public XmlNode getRootNode() {
		return tree.getRootNode();
	}
	public JPanel getAttributeEditorToDisplay() {
		return tree.getAttributeEditorToDisplay();
	}
}
