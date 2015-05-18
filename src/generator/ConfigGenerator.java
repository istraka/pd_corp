/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * @author Ivan Straka
 */
public abstract class ConfigGenerator {
    protected final Document dom;
    protected final Element root;
    private final String out;
    
    public ConfigGenerator(String out) throws ParserConfigurationException{
        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        dom = db.newDocument();
        root = dom.createElement("root");
        dom.appendChild(root);
        this.out = out;
    }
    
    public abstract void generate();
    
    
    
    protected Node createPortNode(String service, int port){
        Element portNode = dom.createElement("port");
        portNode.setAttribute("service", service);
        Text portText = dom.createTextNode(Integer.toString(port));
        portNode.appendChild(portText);
        return portNode;
        
    }
    
    protected Node createTextNode(String name, String hostname){
        Element hostNode = dom.createElement(name);
        Text hostText = dom.createTextNode(hostname);
        hostNode.appendChild(hostText);
        return hostNode;
    }
    
    
    
    protected void print() throws TransformerException, IOException{
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        FileWriter w = new FileWriter(new File(out));
        tf.transform(new DOMSource(dom), new StreamResult(w));
        w.flush();
    }
}
