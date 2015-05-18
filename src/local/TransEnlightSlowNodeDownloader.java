/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import execution.TaskManager;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import network.NetAddress;
import static local.TransEnlightSlowNodeDownloader.states.*;
import network.*;

/**
 * Transaction provide support for used file obtaining protocol.
 * Downloading node {@link TransEnlightSlowNodeDownloader} sends {@link MsgFileRequest} with 
 * increasing {@link MsgFileRequest#requestID} with each request.
 Uploading node {@link TransOfferFiles} sends {@link MsgFileOffer}. 
 * If {@link TransEnlightSlowNodeDownloader} wants to quit, sends {@link MsgAck} and 
 * {@link TransOfferFiles}responds with ack too. Communication is over.
 * If {@link TransEnlightSlowNodeDownloader}) is unwilling to accept offered file(in case 
 * of some error), node sends {@link MsgAbort} and {@link TransOfferFiles} sends 
 * {@link MsgAck}. {@link TransEnlightSlowNodeDownloader} may then continue with another 
 * {@link MsgFileRequest} or {@link MsgAck} to end communication.
 * {@link TransEnlightSlowNodeDownloader} will inform master about the end of the transmition.
 * {@link TransEnlightSlowNodeFinished}
 * @see TransOfferFiles
 * @see TransEnlightSlowNodeFinished
 * @author Ivan Straka
 */

public class TransEnlightSlowNodeDownloader extends Transaction {
    
    private final static Logger logger = Logger.getLogger(TransEnlightSlowNodeDownloader.class.getName());
    
    private final TaskManager tasks;
    private final NodeDevice device;
    private FileDownload fileService;
    private states state;
    private long lastResend;
    private long lastStateChange;
    private String fileSrc;
    private Future<FileFtpOperationFuture> future;
    private ExecutorService pool;
    private final NetAddress ftpAdd;
    private final Node node;
    private int requestID;
    private double serverEstimatedTime;
    private NetAddress returnAddress;
    private long lastTransactionAck;

    /**
     * @param state the state to set
     */
    private void setState(states state) {
        if(this.state != state){
            this.state = state;
            lastStateChange = System.currentTimeMillis();
        }
    }

    private void acknowledgeTransfer() {
        try {
            device.send(new MsgTransactionAlive(nodeID, transactionID));
            lastTransactionAck = System.currentTimeMillis();
        } catch (IOException ex) {
           logger.log(Level.SEVERE, "Unable to send transaction alive message", ex);
        }
    }
    
    enum states{REQUEST, ROLLBACK, DOWNLOADING, NOOFFER, DONE, FATALERROR, FATALERRORACK, EXIT};
    
    public TransEnlightSlowNodeDownloader(TaskManager tasks, NodeDevice device, NetAddress ftpAdd, Node node){
        super(Node.getID(), Transaction.getNewTransactionID());
        this.tasks = tasks;
        this.device = device;
        lastStateChange = System.currentTimeMillis();
        pool = Executors.newFixedThreadPool(1);
        this.ftpAdd = ftpAdd;
        this.node = node;
        requestID = 0;
        state = REQUEST;
        serverEstimatedTime = 0;
    }
    
    /**
     * Sends requst with last id set.
     */
    @Override
    public void execute() {
        logger.log(Level.FINE, "Request to: {0}", device.getAddress());
        
        if(state == REQUEST){
            try {
                device.send(new MsgFileRequest(nodeID, transactionID, requestID));
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
            lastResend = System.currentTimeMillis();
        }
    }
    
    /**
     * Sends ack to end communication.
     */
    private void endTransmition() {
        try {
            device.send(new MsgAck(nodeID, transactionID));
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        lastResend = System.currentTimeMillis();
    }
    
    /**
     * Set state according to response and trigger action.
     */
    @Override
    public void putResponse(Object resp) {    
        if(!(resp instanceof Message)) {
            return;
        }
        Message message = (Message) resp;
        
        // acknowledged message
        if(state == ROLLBACK && message instanceof MsgAck){
            logger.log(Level.FINEST, "Rollback acknowledged");
            setState(REQUEST);
            execute();
        }
        else if(state == NOOFFER && message instanceof MsgAck){
            node.gettingToSameLevelFinished(TransEnlightSlowNodeFinished.states.EMPTY, serverEstimatedTime);
            setState(EXIT);
        }
        else if(state == DONE && message instanceof MsgAck){
            setState(EXIT);
            node.gettingToSameLevelFinished(TransEnlightSlowNodeFinished.states.CORRECT, serverEstimatedTime);
        }
        else if(state == FATALERROR && message instanceof MsgAck){
            endTransmition();
            setState(FATALERRORACK);
        }
        else if(state == FATALERRORACK && message instanceof MsgAck){
            setState(EXIT);
            node.gettingToSameLevelFinished(TransEnlightSlowNodeFinished.states.ERROR, 0);
        }
        else if(state == REQUEST && message instanceof MsgFileOffer){
            MsgFileOffer m = (MsgFileOffer) message;
            if(m.requestID != requestID){
                return;
            }
            requestID++;
            fileSrc = m.filename;
            returnAddress = m.returnFtpFolder;
            serverEstimatedTime = m.estimatedFinishTime;
            // Uploading node offer nothing
            if(fileSrc.equals("")){
                logger.log(Level.FINE,"No offer.");
                setState(NOOFFER);
                endTransmition();
            }
            // start downloading offered file
            else{
                
                logger.log(Level.FINE, "Downloading file {0}", fileSrc);
                setState(DOWNLOADING);
                fileService = new FileDownload(fileSrc, ftpAdd.hostname, ftpAdd.port);
                future = pool.submit(fileService);
            }
        }
    }

    /**
     * Trigger action according to state of process.
     * If downloading has finished and nodes get to the simillar level, communication will be finished.
     * Resend ack and abort messages if timeouted.
     */
    @Override
    public void update() {
        
        long actTime = System.currentTimeMillis();
        //
        if(actTime - lastTransactionAck > Master.sendHelloMilis){
            acknowledgeTransfer();
        }
        try {
            // node has finished downloading
            if(state == DOWNLOADING && future.isDone()){
                FileFtpOperationFuture fileInfo = future.get();
                // download finished correctly
                if(fileInfo.isCorrect()){
                    logger.log(Level.INFO, "Downloaded {0} from {1} to {2}", new Object[]{fileSrc, device.getAddress(), fileInfo.getDest()});
                    
                    // if return file has been requested, put it into schedule
                    if(returnAddress != null){
                        node.planToReturn(fileInfo.getDest(), returnAddress);
                    }
                    
                    tasks.addFile(fileInfo.getDest());
                    node.fileObtained(device.getAddress().getHostName(), fileSrc, fileInfo.getDest());
                    if(serverEstimatedTime-tasks.getEstimatedFinishTimeSec()>Master.computingNodesDiffTimeToTransferFilesSec){
                        setState(REQUEST);
                        execute();
                    }
                    else{
                        setState(DONE);
                        endTransmition();
                    }
                }
                // error such as directory does not exists
                else if(!fileInfo.isTransferError()){
                    logger.log(Level.INFO, "FTP error {0} for {1}. Rollback and request for another file", new Object[]{fileInfo.getCode(), fileSrc});
                    setState(ROLLBACK);
                    rollback();
                }
                // fatal error with connection or something else
                // try to cancel
                else{
                    setState(FATALERROR);
                    rollback();
                }
            }
            // request resend
            else if((state == REQUEST) && actTime - lastResend > Transaction.resendMilis){
                execute();
            }
            // resend abort messsage
            else if((state == FATALERROR || state == ROLLBACK) && actTime - lastResend > Transaction.resendMilis){
                rollback();
            }
            else if((state == DONE || state == NOOFFER || state == FATALERRORACK) && actTime - lastResend > Transaction.resendMilis){
                endTransmition();
            }
        } catch (InterruptedException | ExecutionException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
            cancel();
        }
    }

    @Override
    public void finish() {
        logger.log(Level.INFO,"Obtaining files from {0} has finished.", device.getAddress());
        pool.shutdown();
        pool = null;
    }

    /**
     * Inform master node about fatal error.
     */
    @Override
    public void cancel() {
        node.gettingToSameLevelFinished(TransEnlightSlowNodeFinished.states.ERROR, 0);
    }
    
    /**
     * Sends abort message.
     */
    private void rollback() {
        logger.log(Level.INFO,"Rollback last file.");
        try {
            device.send(new MsgAbort(nodeID, transactionID));
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        lastResend = System.currentTimeMillis();
    }

    /**
     * 
     * @return whether command may finish
     */
    @Override
    public boolean canFinish() {
        return state == EXIT;
    }

    /**
     * 
     * @return whether uploading node timeouted.
     */
    @Override
    public boolean isTimeout() {
        return state != DOWNLOADING && System.currentTimeMillis() - lastStateChange > Transaction.timeoutMilis;
    }
    
}
