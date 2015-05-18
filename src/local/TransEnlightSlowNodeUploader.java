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
import network.*;

/**
 * Transaction provide support for used file obtaining protocol.
 * Downloading node {@link TransGetToSameLevel} sends {@link MsgFileRequest} with 
 * increasing {@link MsgFileRequest#requestID} with each request.
 Uploading node {@link TransEnlightSlowNodeUploader} sends {@link MsgFileOffer}. 
 * If {@link TransGetToSameLevel} wants to quit, sends {@link MsgAck} and 
 * {@link TransEnlightSlowNodeUploader}responds with ack too. Communication is over.
 * If {@link TransGetToSameLevel}) is unwilling to accept offered file(in case 
 * of some error), node sends {@link MsgAbort} and {@link TransEnlightSlowNodeUploader} sends 
 * {@link MsgAck}. {@link TransGetToSameLevel} may then continue with another 
 * {@link MsgFileRequest} or {@link MsgAck} to end communication.
 * {@link TransGetToSameLevel} will inform master about the end of the transmition.
 * {@link TransGetToSameLevelFinished}
 * @see TransGetToSameLevel
 * @author Ivan Straka
 */

public class TransEnlightSlowNodeUploader extends Transaction {
    private final static Logger logger = Logger.getLogger(TransEnlightSlowNodeUploader.class.getName());
    
    private final TaskManager tasks;
    private final NodeDevice device;
    private String offer;
    private boolean finished;
    private int requestID;
    private String firstRollbackFile;
    private final NetAddress returnAddress;
    private String lastRollback;
    private long lastActualizationTimeMilis;
    
    /**
     * 
     * @param nodeID invoking message's {@link Message#getNodeID() } {@link TransGetToSameLevel#nodeID}
     * @param commandID invoking message's {@link Message#getNodeID() } {@link TransGetToSameLevel#transactionID}
     */
    public TransEnlightSlowNodeUploader(int initNodeID, int cmdID, TaskManager tasks, NodeDevice sender, NetAddress returnAddress){
        super(initNodeID, cmdID);
        this.tasks = tasks;
        device = sender;
        finished = false;
        requestID = 0;
        offer = tasks.getFile();
        if(offer == null) {
            offer = "";
        }
        firstRollbackFile = "";
        lastRollback = "";
        this.returnAddress = returnAddress;
        lastActualizationTimeMilis = System.currentTimeMillis();
    }
    
    /**
     * Sends last offer.
     * @see network.Message#composeRespFile(int, int, java.lang.String, double) 
     */
    @Override
    public void execute() {
        logger.log(Level.FINE,"offer {0} to {1}", new Object[]{offer, device.getAddress(), requestID});
        try {
            device.send(new MsgFileOffer(nodeID, transactionID, requestID, offer, returnAddress, tasks.getTheoreticalEstimatedFinishTimeSec(0)));
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            cancel();
            finished = true;
        }
    }
    
    /**
     * Accept message and sends response.
     * Send {@link Message#composeAck(int)} to {@link Message#composeAck(int)} or {@link Message#composeAbort(int)} message. Trigger {@link #execute()} after receiving {@link Message#composeReqFile(int, int)}
     */
    @Override
    public void putResponse(Object resp) {   
        if(!(resp instanceof Message)) {
            return;
        }
        
        lastActualizationTimeMilis = System.currentTimeMillis();
        Message message = (Message) resp;
        
        // ack...finish communication
        if(message instanceof MsgAck){
            logger.log(Level.INFO,"Offering files to {0} has finished.", device.getAddress());
            try {
                device.send(new MsgAck(nodeID, transactionID));
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
            finished = true;
        }
        // req message
        else if(message instanceof MsgFileRequest){
            MsgFileRequest m = (MsgFileRequest) message;
            
            int respReqID = m.requestID;
            // new request
            if(respReqID == requestID+1){
                offer = tasks.getFile();
                requestID++;
            }
            // set offer, check for repeating already refused offer
            if(offer == null || (!firstRollbackFile.equals("") && offer.equals(firstRollbackFile))){
                offer = "";
            }
            // sends offer
            execute();
        }
        // rollback
        else if(message instanceof MsgAbort){
            if(!offer.equals("") && !offer.equals(lastRollback)){
                logger.log(Level.INFO, "{0} returned to queue", offer);
                lastRollback = offer;
                tasks.addFile(offer);
            } 
            if(firstRollbackFile.equals("")){
                firstRollbackFile = offer;
            }
            try {
                device.send(new MsgAck(nodeID, transactionID));
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                finished = true;
            }
        }
    }

    @Override
    public void update(){}
    @Override
    public void finish() {}
    @Override
    public void cancel(){
        tasks.addFile(offer);
        logger.log(Level.INFO, "{0} returned to queue", offer);
    }

    @Override
    public boolean canFinish() {
        return finished;
    }

    @Override
    public boolean isTimeout() {
        return System.currentTimeMillis() - lastActualizationTimeMilis > Master.nodeDeadTimeoutMilis;
    }
    
}
