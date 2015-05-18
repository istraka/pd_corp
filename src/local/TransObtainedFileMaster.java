/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ivan Straka
 */

// todo comments
public class TransObtainedFileMaster extends Transaction {
    private final static Logger logger = Logger.getLogger(TransObtainedFileMaster.class.getName());
    private final static long WAIT_TIME_MILIS = 5*60*1000;
    
    private FileObject file;
    private final String newWorkerFilename;
    
    private enum states {WAIT, EXEC, FINISHED};
    
    private final String oldWorkerHostname;
    private final String oldWorkerFilename;
    private final NodeInfo newWorker;
    private final long timeStart;
    private states state;
    private final NodeInfo oldWorker;

    public TransObtainedFileMaster(int nodeID, int cmdID, Master master, 
            String prevWorkerHostname,
            String prevWorkerFilename,
            int newWorkerID,
            String newWorkerFilename) {
        
        super(nodeID, cmdID);
        this.oldWorkerHostname = prevWorkerHostname;
        this.oldWorkerFilename = prevWorkerFilename;
        this.newWorkerFilename = newWorkerFilename;
        newWorker = master.getNodeById(nodeID);
        oldWorker = master.getNodeByHostname(prevWorkerHostname);
        if(oldWorker == null){
            state = states.FINISHED;
            logger.log(Level.WARNING, "{0} des not exist!", prevWorkerHostname);
        }
        else{
            file = oldWorker.getFileByName(prevWorkerFilename);
            if (file != null){
                state = states.EXEC; 
            }
            // if new woker has already the file, finish
            // if old worker doesnt - wait. This may happen if file goes from 1
            // to 2 and bak to 1. both inform master about this and msg from 1 come sooner than msg from 2.
            else{
                state = newWorker.hasFile(oldWorker, prevWorkerHostname)? states.FINISHED : states.WAIT;
            }
        }
        timeStart = System.currentTimeMillis();
    }
    
    public TransObtainedFileMaster(Master master, 
            String prevWorkerHostname,
            String prevWorkerFilename,
            int newWorkerID,
            String newWorkerFilename){
        this(Node.getID(), Transaction.getNewTransactionID(), master, prevWorkerHostname, prevWorkerFilename, newWorkerID, newWorkerFilename);
    }
    
    @Override
    public void execute() {
        if(state == states.EXEC){
            file.addAccess(newWorker, newWorkerFilename);
            file.changeWorker(newWorker);
            state = states.FINISHED;
        }
    }

    @Override
    public void putResponse(Object resp) {}

    @Override
    public void update() {
        if(state == states.WAIT){
            file = oldWorker.getFileByName(oldWorkerFilename);
            if (file != null){
                state = states.EXEC; 
            }
            // test new worker, if has access to file
            // just in case
            else{
                state = newWorker.hasFile(oldWorker, oldWorkerHostname)? states.FINISHED : states.WAIT;
            }
        }
    }

    @Override
    public void finish() {}

    @Override
    public void cancel() {}

    @Override
    public boolean canFinish() {
        return state == states.FINISHED;
    }

    @Override
    public boolean isTimeout() {
        return state != states.FINISHED && System.currentTimeMillis() - timeStart > WAIT_TIME_MILIS;
    }
    
}
