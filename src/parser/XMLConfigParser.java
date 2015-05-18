/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parser for configuration file.
 * @author Ivan Straka
 */
public class XMLConfigParser {
    private final static Logger logger = Logger.getLogger(XMLConfigParser.class.getName());
    
    Document dom;
    private String xmlFile;
    private Map<String, NodeXML> mapForAccess;
    private Element root;
    public List<FileXML> filesToProcess;
    public List<NodeXML> slaves;
    public NodeXML master;
    
    public XMLConfigParser(String xmlFile) throws ParserException{
        mapForAccess = new HashMap();
        filesToProcess = new LinkedList<>();
        slaves = new LinkedList<>();
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            dom = db.parse(xmlFile);
            root = dom.getDocumentElement();
            
		}catch(ParserConfigurationException | SAXException | IOException ex) {
			throw new ParserException(ex.getMessage());
		}
    }
    
    /**
     * Parse xml
     * @throws parser.ParserException
     */
    public void parse() throws ParserException{
    //    parseTask();
        parseNodes();
        parseFiles();
    }
    
    /**
     * Method returns text content of text element of specified parent node.
     */
    private String getText(Node node) throws ParserException{
        NodeList list = node.getChildNodes();
        for (int i=0; i < list.getLength(); i++){
            if(list.item(i).getNodeType() == Node.TEXT_NODE) {
                return list.item(i).getTextContent().trim();
            }
        }
        throw new ParserException("Node has no text element, at: "+ dom.compareDocumentPosition(node));
    }
    
    /**
     * Method get and check nodes configuration.
     * Get user and hostname/domin of each node.
     * @throws local.XMLConfigParser.XMLParseException 
     */
    private void parseNodes() throws ParserException{
        NodeList fileNodesList = root.getElementsByTagName("file");
        NodeList nodeNodesList = root.getElementsByTagName("node");
        NodeXML tmp;
        for(int i=0; i<nodeNodesList.getLength(); i++){
            tmp = getNode((Element) nodeNodesList.item(i));
            mapForAccess.put(tmp.hostname,tmp);
            if(tmp.isMaster) {
                master = tmp;
            }
            else {
                slaves.add(tmp);
            }
        }
    }
    
    /**
     * Parse one node element.
     * @param node
     * @return 
     * @throws local.XMLConfigParser.XMLParseException 
     */
    private NodeXML getNode(Element node) throws ParserException{
        NodeList userNDList = node.getElementsByTagName("user");
        NodeList hostNDList = node.getElementsByTagName("hostname");
        NodeList portNDList = node.getElementsByTagName("port");
        NodeList downloadFolderNDList = node.getElementsByTagName("downloadFolder");
        NodeList jarPathNDList = node.getElementsByTagName("jarPath");
        NodeList taskNDList = node.getElementsByTagName("task");
        NodeList logFileNDList = node.getElementsByTagName("logFile");
        boolean isMaster;
        boolean compute;
        int nodePort =  0;
        int ftpPort = 0;
        int sshPort = 0;
        String user = null;
        String downloadFolder = "";
        String jarPath = "";
        String host;
        String logFile = null;
        TaskXML task = null;
        
        switch (((Element) node).getAttribute("type")){
            case "master":
                isMaster = true;
                compute = ((Element) node).getAttribute("compute").equals("true");
                user = null;
                break;
                
            case "slave":
                isMaster = false;
                compute = true;
                break;
            
            default:
                throw new ParserException("Node node should have defined type(master/slave) at line: "+dom.compareDocumentPosition(node));   
        }
        
        if(!isMaster && userNDList.getLength() != 1){
            throw new ParserException("Too many or missing user definition, line: "+dom.compareDocumentPosition(node));
        }
        if(!isMaster && jarPathNDList.getLength() != 1){
            throw new ParserException("Too many or missing jarPath definition, line: "+dom.compareDocumentPosition(node));
        }
        if(hostNDList.getLength() != 1){
            throw new ParserException("Too many or missing hostname definition, line: "+dom.compareDocumentPosition(node));
        }
        if(compute && downloadFolderNDList.getLength() != 1){
            throw new ParserException("Too many or missing downloadFolder definition, line: "+dom.compareDocumentPosition(node));
        }
        if(compute && taskNDList.getLength() != 1){
            throw new ParserException("Too many or missing downloadFolder definition, line: "+dom.compareDocumentPosition(node));
        }
        
        if(!isMaster && logFileNDList.getLength() != 1){
            throw new ParserException("Too many or missing definition for log file, line: "+dom.compareDocumentPosition(node));
        }
        
        if (logFileNDList.getLength() == 1){
            logFile = getText(logFileNDList.item(0));
        }
        
        
        if(!isMaster){
            user = getText(userNDList.item(0));
            jarPath = getText(jarPathNDList.item(0));
        }
        
        if(taskNDList.getLength() == 1){
            downloadFolder = getText(downloadFolderNDList.item(0));
            task = getTask((Element) taskNDList.item(0));
        }
        
        host = getText(hostNDList.item(0));
        if(!isMaster) user = getText(userNDList.item(0));
        
        if(portNDList != null){
            for(int i=0; i<portNDList.getLength(); i++){
                switch (((Element) portNDList.item(i)).getAttribute("service")) {
                    case "ftp":
                        ftpPort = Integer.parseInt(getText(portNDList.item(i)));
                        break;
                    case "node":
                        nodePort = Integer.parseInt(getText(portNDList.item(i)));
                        break;
                    case "ssh":
                        sshPort = Integer.parseInt(getText(portNDList.item(i)));
                        break;
                    default:
                        throw new ParserException("Missing or undefined type of service of port node, line: "+dom.compareDocumentPosition(node));
                }
            }
        }
        return new NodeXML(jarPath, user, host, downloadFolder, nodePort, ftpPort, sshPort, task, isMaster, compute, logFile);
    }
    
    private TaskXML getTask(Element node) throws ParserException{
        
        switch(node.getAttribute("fileFeed").toLowerCase()){
            case "all":
                return new TaskXML(getText(node), false, node.getAttribute("returnFolder"), node.getAttribute("result"));
            case "single":
                return new TaskXML(getText(node), true, node.getAttribute("returnFolder"), node.getAttribute("result"));
            default:
                throw new ParserException("missing or undefined whether task is fed with one file or with all defined files using pipes, line: "+dom.compareDocumentPosition(node));
        }
    }
    
    /**
     * Method get and check file information.
     * @throws local.XMLConfigParser.XMLParseException 
     */
    private void parseFiles() throws ParserException{
        NodeList fileNodesList = root.getElementsByTagName("file");
        for(int i=0; i<fileNodesList.getLength(); i++){
            filesToProcess.add(getFile((Element) fileNodesList.item(i)));
        }
    }
    
    /**
     * parse one file node.
     * @param fileNode
     * @return
     * @throws local.XMLConfigParser.XMLParseException 
     */
    private FileXML getFile(Element fileNode) throws ParserException{
        NodeList accessNDList = fileNode.getElementsByTagName("access");
        
        if( accessNDList == null){
            throw new ParserException("Missing access definition for file, line: "+dom.compareDocumentPosition(fileNode));
        }
        List<FileXMLAccess> fileAccessList = new LinkedList();
        String path;
        String host;
        Element fileAccess;
        for(int i=0; i<accessNDList.getLength(); i++){
            fileAccess = (Element) accessNDList.item(i);
            if(!fileAccess.hasAttribute("host")){
                throw new ParserException("Missing host in access definition, line: "+dom.compareDocumentPosition(fileNode));
            }
            if(!fileAccess.hasAttribute("path")){
                throw new ParserException("Missing path in access definition, line: "+dom.compareDocumentPosition(fileNode));
            }
            path = fileAccess.getAttribute("path");
            host = fileAccess.getAttribute("host");
            fileAccessList.add(new FileXMLAccess(mapForAccess.get(host), path));
        }
        return new FileXML(fileAccessList);
    }  
}
