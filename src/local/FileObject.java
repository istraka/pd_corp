/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import network.NetAddress;

/**
 * Class encapsulat information about file and nodes that has access to it.
 * @author Ivan Straka
 */
public class FileObject {
    private final List<FileAccess> accessList;
    private final List<NodeInfo> nodeList;
    private final Map<NodeInfo, String> accessMap;
    private NodeInfo worker;
    
    public FileObject(){
        accessList = new LinkedList<>();
        accessMap = new HashMap<>();
        nodeList = new LinkedList<>();
    }
    
    /**
     * 
     * @return number of nodes that has access to the file.
     */
    public int getAccessCount(){
        return accessList.size();
    }
    
    /**
     * Add access to file.
     * @param node
     * @param filename 
     */
    public void addAccess(NodeInfo node, String filename){
        FileAccess tmpA = new FileAccess(node, filename);
        accessList.add(tmpA);
        nodeList.add(node);
        accessMap.put(node, filename);
    }
    
    /**
     * Return all accesses.
     * @return 
     */
    public List<FileAccess> getAllAccesses(){
        return accessList;
    }
    
    /**
     * Return filename for given node.
     * @param node
     * @return
     */
    public String getNodeFilename(NodeInfo node){
        return accessMap.containsKey(node)? accessMap.get(node) : null;
    }
    
    /**
     * Set node that is planned to process the file.
     * @param node 
     */
    public void setWorker(NodeInfo node){
        worker = node;
    }
    
    public void changeWorker(NodeInfo node){
        worker.removeFile(this);
        node.addFile(this);
        setWorker(node);
    }
    
    /**
     * 
     * @return working node's file destination
     */
    public String getWorkerFilename(){
        return accessMap.get(worker);
    }
    
    /**
     * 
     * @return list of all nodes that has access to the file
     */
    public List<NodeInfo> getNodes(){
        return nodeList;
    }

    public NodeInfo getWorker() {
        return worker;
    }
    
    
}
