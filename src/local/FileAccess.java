/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

/**
 * Class encapsulate node's access to file with path.
 * @author Ivan Straka
 */
public class FileAccess {
    
    /**
     * Node.
     */
    public final NodeInfo node;
    /**
     * Node's destination to file.
     */
    public final String filename;
    
    public FileAccess(NodeInfo node, String filename){
        this.node = node;
        this.filename = filename;
    }
}
