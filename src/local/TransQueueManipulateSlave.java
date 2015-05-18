/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import execution.TaskManager;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static local.TransQueueManipulateSlave.Action.*;
import network.*;

/**
 * Transaction add file and acknowledge it.
 * {@link TransTransferSharedFiles} sends {@link MsgFileAdd}, {@link MsgFileAck} is send back.
 * @author Ivan Straka
 */
public class TransQueueManipulateSlave extends Transaction {
    private final static Logger logger = Logger.getLogger(TransQueueManipulateSlave.class.getName());
    private final TaskManager taskManager;
    private final NodeDevice device;
    private String filename;
    private boolean finish;
    private final Node node;
    private Action action;
    private boolean actionResult;
    private String lastRequest;
    private long lastActualizationTime;
    private Action lastAction;
    protected enum Action {ADD, REMOVE};

    /**
     * 
     * @param nodeID invoking message's {@link Message#getNodeID() } {@link TransTransferSharedFiles#nodeID}
     * @param commandID invoking message's {@link Message#getNodeID() } {@link TransTransferSharedFiles#transactionID}
     * @param node local node
     */
    public TransQueueManipulateSlave (Message m, TaskManager task, NodeDevice device, Node node) {
        super(m.getNodeID(), m.getTransactionID());
        this.taskManager = task;
        finish = false;
        this.device = device;
        if(m instanceof MsgFileAdd){
            this.filename = ((MsgFileAdd) m).filename;
            action = ADD;
        }
        else if(m instanceof MsgFileRemove){
            this.filename = ((MsgFileRemove) m).filename;
            action = REMOVE;
        }
        lastRequest = "";
        this.node = node;
        actionResult = false;
        lastAction = null;
        lastActualizationTime = System.currentTimeMillis();
    }
    
    @Override
    public void execute() {
        if(!(lastAction == action && lastRequest.equals(filename))){
            lastRequest = filename;
            lastAction = action;
            if(action == ADD){
                taskManager.addFile(filename);
                actionResult = true;
                logger.log(Level.INFO, "{0} added to queue", filename);
            }
            else if(action == REMOVE){
                actionResult = taskManager.removeFile(filename);
                if(actionResult) {
                    logger.log(Level.INFO, "{0} removed from queue", filename);
                }
            }
        }
        
        try {
            device.send(new MsgFileAck(nodeID, transactionID, taskManager.getEstimatedFinishTimeSec(), actionResult));
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Sending ACK failed!", ex);
            if(action == REMOVE){
                cancel();
            }
        }
    }

    @Override
    public void putResponse(Object resp) {   
        
        lastActualizationTime = System.currentTimeMillis();
        if(resp instanceof MsgFileAdd){
            this.filename = ((MsgFileAdd) resp).filename;
            action = ADD;
            execute();
        }
        else if(resp instanceof MsgFileRemove){
            this.filename = ((MsgFileRemove) resp).filename;
            action = REMOVE;
            execute();
        }
        // end transaction
        else if(resp instanceof MsgAck){
            try {
                device.send(new MsgAck(nodeID, transactionID));
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Sending ACK failed!", ex);
            }
            finish = true;
        }
    }

    @Override
    public void update(){}

    @Override
    public void finish() {}

    @Override
    public void cancel(){
        if (action == ADD) {
            taskManager.addFile(filename);
            logger.log(Level.INFO, "{0} has been returned to the queue.",filename);
        }
        finish = true;
    }

    @Override
    public boolean canFinish() {
        return finish;
    }

    @Override
    public boolean isTimeout() {
        return System.currentTimeMillis() - lastActualizationTime > Master.nodeDeadTimeoutMilis;
    }
}
