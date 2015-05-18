/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package execution;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Subprocess executor.
 * Executor repeatedly obtain file from task manager and run new subprocess, wait and inform {@link NodeNotificator}.
 * @author Ivan Straka
 */
public class SingleFileExecutor implements Runnable {
    private static final Logger logger = Logger.getLogger(SingleFileExecutor.class.getName());
    
    private final SingleFileFeeder work;
    private final NodeNotificator node;
    
    SingleFileExecutor(SingleFileFeeder work, NodeNotificator node){
        this.work = work;
        this.node = node;
    }
    @Override
    public void run() {
        long timeStart;
        long timeEnd;
        String[] cmd = {"/bin/sh", "-c", ""};
        String filename;
        Process p;
        
        while(true)
        {
            try {
                filename = work.obtainTask();
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, null, ex);
                return;
            }
                cmd[2]=work.getExecCmd(filename);
            try {
                p = Runtime.getRuntime().exec(cmd);
                p.waitFor();
                work.taskDone();
                node.fileProcessed(cmd[2], p.getInputStream(), p.exitValue());
                node.fileProcessed(filename, new String[]{work.getResultFilename(filename)});
            } catch (InterruptedException|IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                work.taskDone();
            } 
        }
    }
    
}
