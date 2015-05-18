/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import execution.TaskManager;
import java.util.logging.Logger;
import org.apache.ftpserver.FtpServer;

/**
 * Class used for executorService pool and FtpServer cleanup.
 * @author Ivan Straka
 */
public class ShutdownHook extends Thread{
    private static final Logger logger = Logger.getLogger(ShutdownHook.class.getName());
    
    public volatile TaskManager tasksManager;
    public volatile FtpServer server;
    
    public ShutdownHook(){
        tasksManager = null;
        server = null;
    }
    
    @Override
    public void run(){
        if (server != null) {
            server.stop();
        }
        if (tasksManager != null) {
            tasksManager.shutdownNow();
        }
    }
    
    public void setManager(TaskManager tm){
        tasksManager = tm;
    }
    public void setServer(FtpServer s){
        server = s;
    }
}
