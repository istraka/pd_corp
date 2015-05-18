/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import generator.ConfigGenerator;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import network.NetAddress;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author Ivan Straka
 */
public class ResultGenerator extends ConfigGenerator {
    private final static Logger logger = Logger.getLogger(ResultGenerator.class.getName());

    private final Map<String, NodeInfo> ipNodeMap;
    private final List<NodeInfo> nodes;
    private final List<List<NetAddress>> results;
    private final String output;
    
    public ResultGenerator(List<NodeInfo> nodes, List<List<NetAddress>> results, String output) throws ParserConfigurationException{
        super(output);
        ipNodeMap = new HashMap<>();
        this.nodes = nodes;
        this.results = results;
        this.output = output;
        
        for(NodeInfo node: nodes){
            String ip;
            if (node.getAddress() != null) {
                ip = node.getAddress().getHostAddress();
            }
            else{
                try {
                    ip = InetAddress.getByName(node.getHostname()).getHostAddress();
                } catch (UnknownHostException ex) {
                    ip = node.getHostname();
                    logger.log(Level.SEVERE, ex.getMessage());
                }
            }
            ipNodeMap.put(ip, node);
        }
    }
    
    @Override
    public void generate() {
        for(NodeInfo node: nodes){
            root.appendChild(createNode(node));
        }
        
        for(List<NetAddress> result: results){
            root.appendChild(createFile(result));
        }
        
        try {
            print();
        } catch (TransformerException | IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
    }
    
    private Node createNode(NodeInfo nodeInfo){
        Element node = dom.createElement("node");
        node.setAttribute("type", nodeInfo.isMaster()?"master":"slave");
        node.setAttribute("type", nodeInfo.isComputing()?"true":"false");
        
        node.appendChild(createPortNode("ssh", nodeInfo.sshPort));
        node.appendChild(createPortNode("node", nodeInfo.getNodePort()));
        node.appendChild(createPortNode("ftp", nodeInfo.getFtpPort()));
        
        node.appendChild(createTextNode("host",nodeInfo.getHostname()));
        node.appendChild(createTextNode("jarPath",nodeInfo.getJarPath()));
        node.appendChild(createTextNode("downloadFolder",nodeInfo.getDownloadFolder()));
        node.appendChild(createTextNode("user",nodeInfo.getUser()));
        
        
        return node;
    }
    
    private Node createFile(List<NetAddress> file){
        Element node = dom.createElement("file");
        for(NetAddress address: file){
            node.appendChild(createAccess(address));
        }
        return node;
        
    }
    
    private Node createAccess(NetAddress a){
        Element node = dom.createElement("access");
        try {
            node.setAttribute("path", a.dest);
            String rawIP = InetAddress.getByName(a.hostname).getHostAddress();
            if (ipNodeMap.containsKey(rawIP)){
                node.setAttribute("host", ipNodeMap.get(rawIP).getHostname());
            }
            else{
                node.setAttribute("host", rawIP);
                logger.log(Level.SEVERE,"Unable to determine host for {0}:{1}, raw ip: {2}", new Object[]{a.dest, a.hostname, rawIP});
            }
        } catch (UnknownHostException ex) {
            logger.log(Level.SEVERE,"Unable to determine host for {0}:{1}", new Object[]{a.dest, a.hostname});
        }
        return node;
    }
    
}
