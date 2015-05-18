/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package generator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import ssh.SshExecutor;
import ssh.SshExecutorException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Generate config file.
 * Input file consist of strings: user@hostname:sshport \t folder
 * Ouput will contain basic info about servers and set of files in given folders with propper host access.
 * @author Ivan Straka
 */
public class SshGen extends ConfigGenerator {
    private final static Logger logger = Logger.getLogger(SshGen.class.getName());
    
    private final String input;
    private final Element mark;
    private final Set<String> addedHostNodes;
    private final SshExecutor sshExecutor;
    
    public SshGen(String rsaFile, String knownHosts, String input, String output) throws ParserConfigurationException, SshExecutorException{
        super(output);
        sshExecutor = new SshExecutor(knownHosts, rsaFile);
        this.input = input;
        mark = dom.createElement("mark");
        root.appendChild(mark);
        addedHostNodes = new HashSet<>();
    }
    
    @Override
    public void generate(){
        try {
            for (String line : Files.readAllLines(Paths.get(input),Charset.forName("UTF-8"))) {
                line = line.trim();
                if(line.equals("")){
                    continue;
                }
                Destination dest = new Destination(line);
                for (String file: getFileList(dest)){
                    if(!addedHostNodes.contains(dest.hostname)){
                        root.insertBefore(createSlave(dest.username, dest.hostname, dest.port), mark);
                        addedHostNodes.add(dest.hostname);
                    }
                    root.appendChild(createFile(dest.hostname, file));
                }
            }
            root.removeChild(mark);
            sshExecutor.disconnect();
            print();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "IO Error ", ex.getMessage());
        } catch (TransformerException ex) {
            logger.log(Level.SEVERE,  ex.getMessageAndLocation());
        }
    }
    
    private Node createFile(String host, String file){
        Element fileNode = dom.createElement("file");
        
        Element access = dom.createElement("access");
        access.setAttribute("host", host);
        access.setAttribute("path", file);
        fileNode.appendChild(access);
        return fileNode;
    }
    
    private Node createSlave(String user, String host, int port){
        Element slaveNode = dom.createElement("node");
        slaveNode.setAttribute("type", "slave");
        
        slaveNode.appendChild(createTextNode("host",host));
        slaveNode.appendChild(createTextNode("user",user));
        slaveNode.appendChild(createPortNode("ssh", port));
        
        return slaveNode;
    }
    
    private List<String> getFileList(Destination dest){
        
        BufferedReader br = null;
        List<String> result = new LinkedList<>();
        try {
            sshExecutor.setCommand("find \""+dest.folder+"\" -maxdepth 1 -type f", dest.username, dest.hostname, dest.port);
            br=new BufferedReader(new InputStreamReader(sshExecutor.getInputStream()));
            sshExecutor.execute();
            String line;
            while ((line=br.readLine()) != null) {
                result.add(line);
            }
            sshExecutor.IODone();
        } catch (IOException|SshExecutorException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
        finally{
            try {
                if(br != null){
                    br.close();
                }
            } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return result;
    }
}

class Destination{
    public final String folder;
    public final String username;
    public final String hostname;
    public final int port;
    
    Destination(String line){
        String[] arr = line.split("\t",2);
        folder = arr[1];
        String[] hostInfo = arr[0].split("@", 2);
        username = hostInfo[0];
        if(hostInfo[1].contains(":")){
            String tmp[] = hostInfo[1].split(":");
            hostname = tmp[0];
            port = Integer.parseInt(tmp[1]);
        }
        else{
            hostname = hostInfo[1];
            port = 22;
        }
    }
}