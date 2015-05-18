/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import network.NetAddress;
import execution.NodeNotificator;
import execution.TaskManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import network.ReceivedObject;
import network.*;
import org.apache.commons.io.IOUtils;

/**
 * Class represents slave node, that runs on remote pc managed by {@link Master}.
 * Main function is sending {@link MsgHello} and responding to messages. 
 * Response may be sending a message or create the transaction and cede management to it.
 * @author Ivan Straka
 */
public class Node implements NodeNotificator {
    
    private final static Logger logger = Logger.getLogger(Node.class.getName());
    private static int id;
    
    public static void setID(int id){
        Node.id = id;
    }
    
    public static int getID(){
        return id;
    }
    
    protected final NetworkService network;
    protected final TaskManager taskManager;
    protected final NodeDevice masterDevice;
    protected boolean downloading;
    protected final ConcurrentMap<String, NetAddress> inputFileReturnAddressMap;
    
    protected final TransactionService transactionService;
    
    protected boolean quit;
    protected boolean started;
    
    protected final boolean returnResult;
    protected final NetAddress returnResultAddress;
    protected final NetAddress ftpAddress;
    protected final boolean generateResultConfig;
    
    public Node(TaskManager taskManager, NetworkService net, NodeDevice masterDevice, boolean generateResultConfig, boolean returnResult, NetAddress returnResultAddress, NetAddress ftpAddress){
        this.taskManager = taskManager;      
        this.masterDevice = masterDevice;
        this.network = net;
        quit = false;
        downloading = false;
        transactionService = new TransactionService();
        this.generateResultConfig = generateResultConfig;
        this.returnResult = returnResult;
        this.returnResultAddress = returnResultAddress;
        inputFileReturnAddressMap = new ConcurrentHashMap<>();
        this.ftpAddress = ftpAddress;
    }
    
    /**
     * Node is always computing.
     * @return 
     */
    public boolean isComputing(){
        return true;
    }
    
    @Override
    public void fileProcessed(String cmd, InputStream err, int errCode) {
        try {
            String errStr = IOUtils.toString(err);
            logger.log(Level.INFO,"{0}\tstderr: {1}\terrcode: {2}",new Object[] {cmd,errStr, Integer.toString(errCode)});
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void fileProcessed(){}
    
    public void start(){
        // computing
        long lastHelloTime = 0;
        long beginTime = System.currentTimeMillis();
        try {           
            while(!quit){
                // send hello
                if(started){
                    if(System.currentTimeMillis() - lastHelloTime > Master.sendHelloMilis){
                        sendHello();
                        lastHelloTime = System.currentTimeMillis();
                    }
                    Thread.sleep(1);
                }
                else if(!started && System.currentTimeMillis() - beginTime > Master.masterTimeoutMilis){
                    logger.log(Level.SEVERE, "Master timeout - start message expecting");
                    return;
                }
                ReceivedObject receive = network.receive();
                if(receive != null) processMessage(receive);
                transactionService.update();
            }
        } catch(InterruptedException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        finally{
            try {
                taskManager.shutdown();
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }  
    
    private void sendHello(){
        try {
            double timeLeft = taskManager.getEstimatedFinishTimeSec();
            if(timeLeft == 0 && !transactionService.isEmpty()){
                timeLeft = 1;
            }
            if (masterDevice != null){
                masterDevice.send(new MsgHello(id, timeLeft));
            }
            
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Sending hello failed!", ex);
        }
    }

    protected void processMessage(ReceivedObject recv) {
        
        if(recv.message instanceof MsgQuit){
            processQuit();
        }
        else if(recv.message instanceof MsgStart){
            processStart();
        }
        else if(recv.message instanceof MsgPromptToEnlightNode){
            processGetToSameLevel(recv);
        }
        else if(recv.message instanceof MsgFileRequest){
            processFileRequest(recv);
        }
        else if(recv.message instanceof MsgAck || 
                recv.message instanceof MsgAbort ||
                recv.message instanceof MsgTransactionAlive || 
                recv.message instanceof MsgFileOffer){
            processCtrl(recv);
        }
        else if(recv.message instanceof MsgFileAdd ||
                recv.message instanceof MsgFileRemove){
            processQueueManipulate(recv);
        }
        else if(recv.message instanceof MsgCancelNodeTransaction){
            processCancelTransaction(recv);
        }
        
    }
    
    protected void processQuit() {
        logger.log(Level.INFO, "Quit");
        taskManager.shutdownNow();
        quit = true;
    }
    
    protected void processStart(){
        logger.log(Level.INFO, "Start");
        taskManager.start(this);
        this.started = true;
    }
    
    protected void processGetToSameLevel(ReceivedObject data) {
        MsgPromptToEnlightNode m = (MsgPromptToEnlightNode) data.message;
                
        NodeDevice device;
        try {
            device = network.createDevice(m.nodeAddress);
        } catch (UnknownHostException ex) {
            logger.log(Level.SEVERE, "Unknown host {0}, unable to lighten node.", m.nodeAddress);
            return;
        }
        acknowledge(m.getNodeID(), m.getTransactionID(), data.device);
        if(downloading){
            return;
        }
        downloading = true;
        transactionService.addTransaction(new TransEnlightSlowNodeDownloader(taskManager, device, m.ftpAddress, this));
    }
    
    protected void processFileRequest(ReceivedObject data) {
        MsgFileRequest m = (MsgFileRequest) data.message;
        if(!transactionService.put(m)){
            transactionService.addTransaction(new TransEnlightSlowNodeUploader(m.getNodeID(), m.getTransactionID(), taskManager, data.device, returnResult?returnResultAddress:null));
        }
    }
    
    protected void processCtrl(ReceivedObject data) {
        transactionService.put(data.message);
    }
    
    public void fileObtained(String oldWorkerHostname, String oldWorkerFilename, String newWorkerFilename) {
        transactionService.addTransaction(new TransObtainedFileSlave(masterDevice, oldWorkerHostname, oldWorkerFilename, newWorkerFilename));
    }
    
    public void gettingToSameLevelFinished(TransEnlightSlowNodeFinished.states state, double slowNodeFinishTime) {
        switch (state){
            case CORRECT:
                downloading = false;
                transactionService.addTransaction(new TransEnlightSlowNodeFinished(masterDevice, taskManager.getEstimatedFinishTimeSec(), slowNodeFinishTime, local.TransEnlightSlowNodeFinished.states.CORRECT));
                break;
            case EMPTY:
                downloading = false; 
                transactionService.addTransaction(new TransEnlightSlowNodeFinished(masterDevice, taskManager.getEstimatedFinishTimeSec(), slowNodeFinishTime, local.TransEnlightSlowNodeFinished.states.EMPTY));
                break;
            
            case ERROR:
            default:    
                downloading = false; 
                transactionService.addTransaction(new TransEnlightSlowNodeFinished(masterDevice, taskManager.getEstimatedFinishTimeSec(), slowNodeFinishTime, local.TransEnlightSlowNodeFinished.states.ERROR));
        }
    }
    

    private void processQueueManipulate(ReceivedObject data) {
        
        if(!transactionService.put(data.message)){
            transactionService.addTransaction(new TransQueueManipulateSlave(data.message, taskManager, data.device, this));
        }
    }

    protected void processCancelTransaction(ReceivedObject data) {
        MsgCancelNodeTransaction m = (MsgCancelNodeTransaction) data.message;
        cancelTransaction(m.transactionNodeID, m.transactionClassName);
        acknowledge(m.getNodeID(), m.getTransactionID(), data.device);
    }
    
    protected void cancelTransaction(int nodeID, String className){
        transactionService.deleteNodeTransactionsOfType(nodeID, className);
    }

    
    public boolean returnRemotelyProcessedFile(){
        return returnResult;
    }

    @Override
    public void fileProcessed(String inputFile, String[] outputFiles) {
        if(outputFiles == null) {
            return;
        }
        // return file
        try {
            if(returnResult && inputFileReturnAddressMap.containsKey(inputFile)){
                transactionService.addTransaction(new TransReturnResult(Arrays.asList(outputFiles), inputFileReturnAddressMap.get(inputFile), this));
            }
            else if(generateResultConfig){
                for(String file: outputFiles){
                    notifyMasterResult(NetAddress.getAddress(file, this.ftpAddress.hostname, 0));
                }
            }
        } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.getMessage());
            }
        
        
    }
    
    public void notifyMasterResult(List<NetAddress> result) throws IOException{
        if(generateResultConfig) {
            transactionService.addTransaction(new TransFileResultNotify(masterDevice, result));
        }
    }
    
    public void notifyMasterResult(NetAddress result) throws IOException{
        notifyMasterResult(Arrays.asList(result));
    }
    
    public String getHostname(){
        return ftpAddress.hostname;
    }

    public void planToReturn(String fileSrc, NetAddress ftpPath) {
        inputFileReturnAddressMap.put(fileSrc, ftpPath);
    }
    
    protected void acknowledge(int nodeID, int transactionID, NodeDevice device){
        try {
            device.send(new MsgAck(nodeID, transactionID));
        } catch (IOException ex) {
            logger.log(Level.SEVERE,"Unable to send message to {0} node id:{1} transaction id:{2}", new Object[]{device.getAddress(), nodeID, transactionID});
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
