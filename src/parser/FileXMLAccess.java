/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

/**
 *
 * @author Ivan Straka
 */
public class FileXMLAccess {
    public final NodeXML nodeXML;
    public final String filename;

    /**
     *
     * @param nodeXML
     * @param filename
     */
    public FileXMLAccess(NodeXML nodeXML, String filename){
        this.nodeXML = nodeXML;
        this.filename = filename;
    }
}
