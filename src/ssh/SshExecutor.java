/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SshExecutor executes ssh command.
 * User need to set command for host and then execute it.
 * It is recommended to call {@link #IODone()} after reading whole output.
 * It is also recommened to call this function if no reading is performed.
 * <br> passpharse for rsa file will be used only in every connection of instance.
 * <br>Note that given password may be used for other username@hosts than the password was asked for.
 * <br>After finishing all exections {@link #disconnect()} should be called.
 * @author Ivan Straka
 */
public class SshExecutor {
    private final static Logger logger = Logger.getLogger(SshExecutor.class.getName());
    
    private final List <SshUser> userList;
    private final String passpharse;
    private final Console console;
    private final String knownHosts;
    private final String rsa;
    private final JSch jsch;
    private final Map<String, Session> hostSessionMap;
    
    private String command;
    private ChannelExec channel;
    
    /**
     * Construct executor with unknown hosts and rsa file. 
     */
    public SshExecutor() throws SshExecutorException{
        this(null, null);
    }
    
    /**
     * Construct executor with known hosts and unknown rsa file.
     */
    public SshExecutor(String knownHosts) throws SshExecutorException{
        this(knownHosts, null);
    }
    
    /**
     * Construct executor woth known hosts and known rsa file.
     */
    public SshExecutor(String knownHosts, String rsa) throws SshExecutorException{
        this.knownHosts = knownHosts;
        this.rsa = rsa;
        console = System.console();
        passpharse = (rsa != null)? new String(console.readPassword()) : null;
        jsch=new JSch();
        userList = new LinkedList<>();
        hostSessionMap = new HashMap<>();
        channel = null;
        command = null;
        
        try {
            if(knownHosts != null) {
                jsch.setKnownHosts(knownHosts);
            }
            if(passpharse != null) {
                jsch.addIdentity(rsa, passpharse);
            }
            else if(rsa != null)   {
                jsch.addIdentity(rsa);
            }
        } catch (JSchException e) {
            throw new SshExecutorException(e.getMessage(), e);
        }
    }
    
    /**
     * Set command to execute on remote host with ssh port 22.
     */
    public void setCommand(String cmd, String username, String host) throws UnknownHostException, SshExecutorException{
        setCommand(cmd, username, host, 22);
    }
    
    /**
     * Set command to execute on remote host.
     */
    public void setCommand(String cmd, String username, String host, int port) throws UnknownHostException, SshExecutorException{
        command = cmd;
        Session s = getSession(username, host, port);
        try {
            if(!s.isConnected()) {
                s.connect();
            }
            if(channel != null && channel.isConnected()) {
            //    channel.disconnect();
            }
            channel = (ChannelExec) s.openChannel("exec");
            
        } catch (JSchException e) {
            throw new SshExecutorException(e.getMessage(), e);
        }
        
    }
    
    
    public InputStream getInputStream() throws SshExecutorException, IOException{
        if(channel == null){
            throw new SshExecutorException("Command has not been set yet.");
        }
        
        return channel.getInputStream();
    }
    
    /**
     * Execute command.
     * Channel wont be closed.
     */ 
    public void execute() throws SshExecutorException{
        if(command == null || channel == null){
            throw new SshExecutorException("Command has not been set yet.");
        }
        try {
            channel.setCommand(command);
            channel.connect();
        } catch (JSchException e) {
            throw new SshExecutorException(e.getMessage(), e);
        } finally{
            command = null;
        }
    }
    
    /**
     * Disconnect channel.
     * Session won't be closed.
     */
    public void IODone(){
        channel.disconnect();
        channel = null;
    }
    
    /**
     * Return or create and map session to host for further executions.
     * If session has not been created for the host yet, known {@link SshUser}
     * will be tried. New {@link SshUser} will be created after known user's info will be tried.
     */
    private Session getSession(String username, String host, int port) throws UnknownHostException, SshExecutorException{
        String rawIP = getHost(host);
        if(hostSessionMap.containsKey(rawIP)){
            return hostSessionMap.get(rawIP);
        }
        // session for host does not exists yet
        // find user
        Session newSession;
        for(SshUser user: userList){
            try {
                newSession = jsch.getSession(username, host, port);
                newSession.setUserInfo(user);
                newSession.connect();
            
            // do nothing...try another userinfo or create the new one afterwards
            } catch (JSchException ex) {
                continue;
            }
            hostSessionMap.put(rawIP, newSession);
            return newSession;
        }
        
        SshUser newUserInfo = new SshUser(passpharse);
        try{
            newSession = jsch.getSession(username, host, port);
            newSession.setUserInfo(newUserInfo);
            newSession.connect();
        } catch (JSchException e){
            throw new SshExecutorException(e.getMessage(), e);
        }
        
        newUserInfo.setWork();
        hostSessionMap.put(rawIP, newSession);
        userList.add(newUserInfo);
        return newSession;
    }
    
    /**
     * Get raw ip.
     */
    private String getHost(String host) throws UnknownHostException{
        return InetAddress.getByName(host).getHostAddress();
    }
    
    /**
     * Disconnect. Clear all sessions and known users.
     */
    public void disconnect(){
        if(channel != null && channel.isConnected()) channel.disconnect();
        for(Map.Entry<String, Session> entry: hostSessionMap.entrySet()){
            entry.getValue().disconnect();
        }
        hostSessionMap.clear();
        userList.clear();
    }
    
    
}
