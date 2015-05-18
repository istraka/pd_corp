/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import execution.TaskManager;
import java.io.InputStream;
import static java.lang.System.exit;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import network.*;

/**
 * Class represents master node that manages remote computers.
 * Main function is load balancing.
 * @author Ivan Straka
 */
public class Master extends Node {
    
    public static final long nodeDeadTimeoutMilis = 600000;
    public static final long masterTimeoutMilis = 240000;
    public static final long sendHelloMilis = 60000;
    // time between two nodes to take some files
    public static final long computingNodesDiffTimeToTransferFilesSec = 4500;
    
    // debugging
    //public static final long computingNodesDiffTimeToTransferFilesSec = 200;
    //public static final long sendHelloMilis = 10000;
           
    
    private final static Logger logger = Logger.getLogger(Master.class.getName());
    
    private List<NodeInfo> nodes;
    private final List<List<NetAddress>> results;
    private NodeInfo masterNodeInfo;
    
    /**
     * Construct master that participate to computing.
     */
    public Master(TaskManager taskManager, NetworkService net, List<NodeInfo> nodes,  boolean notifyMaster, boolean returnResults, NetAddress returnResultAddress, NetAddress ftpAddress) throws UnknownHostException {
        super(taskManager, net, null, notifyMaster, returnResults, returnResultAddress, ftpAddress);
        this.nodes = nodes;
        for(NodeInfo node: nodes){
            if(node.isMaster()){
                masterNodeInfo = node;
                break;
            }
        }
        started = true;
        results = Collections.synchronizedList(new LinkedList<List<NetAddress>>());
    }
    
    /**
     * Construct master that does not participate to computing.
     */
    public Master(NetworkService net, List<NodeInfo> nodes, boolean returnResult, NetAddress returnResultAddress, NetAddress ftpAddress) throws UnknownHostException {
        super(null, net, null, false, returnResult, returnResultAddress, ftpAddress);
        this.nodes = nodes;
        results = Collections.synchronizedList(new LinkedList<List<NetAddress>>());
    }
    
    @Override
    public void fileProcessed(String cmd, InputStream err, int errCode) {
        masterNodeInfo.hello(taskManager.getEstimatedFinishTimeSec());
        super.fileProcessed(cmd, err, errCode);
    }
    
    @Override
    public void fileProcessed(){
        masterNodeInfo.hello(taskManager.getEstimatedFinishTimeSec());
    }
    
    @Override
    public void fileProcessed(String inputFile, String[] outputFiles) {
        masterNodeInfo.hello(taskManager.getEstimatedFinishTimeSec());
        super.fileProcessed(inputFile, outputFiles);
    }
    
    @Override
    public void start(){
        try {
            if(taskManager != null){
                taskManager.start(this);
                masterNodeInfo.hello(taskManager.getEstimatedFinishTimeSec());
            }
            transactionService.addTransaction(new TransStartSlaves(getNodes()));
            ReceivedObject recv;
            NodeInfo fastNode;
            NodeInfo slowNode;
            Double nodeEstimatedFinishTime;
            while(!quit){
                slowNode = null;
                fastNode = null;
                
                recv = network.receive();
                while(recv != null){
                    processMessage(recv);
                    recv = network.receive();
                    transactionService.update();
                }
                if(allFinishedOrDead()){
                    transactionService.addTransaction(new TransQuitSlave(getNodes()));

                    if(taskManager != null) {
                        taskManager.shutdown();
                    }
                    return;
                }
                // selection the fastest and the slowest node
                for(NodeInfo node: getNodes()){
                    if(!node.isMaster() && !node.isAlive()){
                        handleDead(node);
                        continue;
                    }
                    
                    slowNode = getBetterCandidateToBeEnlighted(node, slowNode);
                    fastNode = getBetterCandidateToEnlightSlowNode(node, fastNode);
                    
                }
                if(slowNode != null && !tryUnloadNode(slowNode) && fastNode != null && fastNode != slowNode && 
                    slowNode.getEstimatedFinishTimeSec() - fastNode.getEstimatedFinishTimeSec() > Master.computingNodesDiffTimeToTransferFilesSec)
                {
                    if(fastNode.isMaster()){
                        downloadFile(slowNode);
                    }
                    else{
                        transactionService.addTransaction(new TransEnlightSlowNodeMasterCommand(slowNode, fastNode));
                    }
                }
            }
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
            exit(1);
        }
    }
    
    private NodeInfo getBetterCandidateToEnlightSlowNode(NodeInfo first, NodeInfo second){
        first = filterCandidateToEnlightSlow(first);
        second = filterCandidateToEnlightSlow(second);
        if(first == null) {
            return second;
        }
        if(second == null) {
            return first;
        }
        return first.getEstimatedFinishTimeSec() < second.getEstimatedFinishTimeSec() ? first : second;
        
        
    }
    
    /**
     * Filter node.
     * Node can not be null, can not be downloading, nor uploading, nor locked files. 
     * Estimated finish time must not be {@link TaskManager#NOTHING_DONE} nor {@link  TaskManager#UNKNOWN}.
     * Node must be alive and participate in computing.
     * 
     * @return node if pass the filter
     */
    private NodeInfo filterCandidateToEnlightSlow(NodeInfo node){
        return (node != null && !node.isDownloading() && !node.isUploading() && 
                !node.areFilesLocked() && !node.isFtpError() &&
                node.getEstimatedFinishTimeSec() != TaskManager.NOTHING_DONE && 
                node.getEstimatedFinishTimeSec() != TaskManager.UNKNOWN &&
                (node.isAlive() || node.isMaster()) && node.isComputing())? 
                
                node : null;
    }
    
    private NodeInfo getBetterCandidateToBeEnlighted(NodeInfo first, NodeInfo second){
        first = filterCandidateToBeEnlighted(first);
        second = filterCandidateToBeEnlighted(second);
        if(first == null) {
            return second;
        }
        if(second == null) {
            return first;
        }
        return first.getEstimatedFinishTimeSec() > second.getEstimatedFinishTimeSec() ? first : second;
    }
    
    
    /**
     * Filter node.
     * Node can not be null, can not be downloading, nor locked files. 
     * Estimated finish time must not be {@link TaskManager#NOTHING_DONE} nor {@link  TaskManager#UNKNOWN}.
     * Node must be alive and participate in computing, and can not be empty.
     * 
     * @return node if pass the filter
     */
    private NodeInfo filterCandidateToBeEnlighted(NodeInfo node){
        return (node != null && !node.isDownloading() && !node.areFilesLocked() &&
                !node.isFtpError() &&
                node.getEstimatedFinishTimeSec() != TaskManager.NOTHING_DONE && 
                node.getEstimatedFinishTimeSec() != TaskManager.UNKNOWN &&
                (node.isAlive() || node.isMaster()) && node.isComputing() && !node.isEmpty())? 
                
                node : null;
    }
    
    private boolean tryUnloadNode(NodeInfo slowNode){
        for(FileObject file: slowNode.getFilesWithMultipleAccess()){
            for(FileAccess access: file.getAllAccesses()){
                if(!access.node.isComputing()){
                    continue;
                }
                if(!slowNode.equals(access.node) && slowNode.getEstimatedFinishTimeSec() - access.node.getEstimatedFinishTimeSec() > Master.computingNodesDiffTimeToTransferFilesSec){
                    transactionService.addTransaction(
                            new TransTransferSharedFiles(slowNode, slowNode.getFilesWithMultipleAccess(), taskManager, this));
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Determine if all node has finished
     * @return boolean
     */
    private boolean allFinishedOrDead(){
        for(NodeInfo node: getNodes()){
            //master
            if(node.isMaster() && node.isComputing() && !node.isFinished()){
                return false;
            }
            // slave
            else if(!node.isMaster() && !node.isQuitted() && node.isAlive() && !node.isFinished()){
                return false;
            }
        }
        return true;
    }

    @Override
    protected void processMessage(ReceivedObject recv){
        if(recv.message instanceof MsgHello){
            processHello(recv);
        }
        else if(recv.message instanceof MsgFileRequest){
            processFileRequest(recv);
        }
        else if(recv.message instanceof MsgAck || 
                recv.message instanceof MsgAbort || 
                recv.message instanceof MsgFileAck ||
                recv.message instanceof MsgFileOffer){
            processCtrl(recv);
        }
        else if(recv.message instanceof MsgFileResultNotify){
            processFileResultNotification(recv);
        }
        else if(recv.message instanceof MsgGetToSameLevelFinished){
            processNodeObtainingFilesFinished(recv);
        }
        else if(recv.message instanceof MsgFileWorkerChange){
            processNodeObtainedFile(recv);
        }
        
    }
    
    private void processFileResultNotification(ReceivedObject data) {
        MsgFileResultNotify m = (MsgFileResultNotify) data.message;
        acknowledge(m.getNodeID(), m.getTransactionID(), data.device);
        results.add(m.address);
    }
    
    @Override
    public void notifyMasterResult(List<NetAddress> result) {
        if(generateResultConfig){
            results.add(result);
        }
    }
    
    @Override
    public boolean isComputing(){
        return !(taskManager == null);
    }
    
    private void downloadFile(NodeInfo node) {
        if(downloading){
            return;
        }
        downloading = true;
        masterNodeInfo.setDownloading(node);
        node.setUploading(true);
        transactionService.addTransaction(new TransEnlightSlowNodeDownloader(taskManager, node.device, node.getFtpAddress(), this));
        
    }
    
    private void processHello(ReceivedObject data) {
        MsgHello m = (MsgHello) data.message;
        if(m.getNodeID() < getNodes().size()){
            getNodes().get(m.getNodeID()).hello(m.estimatedFinishTime);
        }
    }

    /**
     * Method handle dead node.
     * Notify user so far.
     */
    private void handleDead(NodeInfo node) {
        if(!node.isSuspicious()) {
            logger.log(Level.SEVERE, "Dead: {0}", node.getNodeAddress());
            node.setSusicious(true);
        }
    }
    
    public void processNodeObtainingFilesFinished(ReceivedObject data) {
        MsgGetToSameLevelFinished m = (MsgGetToSameLevelFinished) data.message;
        
        acknowledge(m.getNodeID(), m.getTransactionID(), data.device);
        NodeInfo node = getNodes().get(m.getNodeID());
        switch(m.state){
            case CORRECT:
                logger.log(Level.FINER, "Node {0} finished download",node.getHostname());
                if(node.getDownloadingSrc() != null) {
                    node.getDownloadingSrc().setUploading(false);
                    node.getDownloadingSrc().hello(m.serverEstimatedFinishTime);
                }
                node.unsetDownloading();
                node.hello(m.estimatedFinishTime);
                node.setEmpty(false);
                break;
            // src is empty
            case EMPTY:
                if(node.getDownloadingSrc() != null) {
                    node.getDownloadingSrc().hello(m.serverEstimatedFinishTime);
                    logger.log(Level.FINER, "Node {0} finished download, {1} is empty",new Object[]{node.getHostname(), node.getDownloadingSrc().getHostname()});
                    node.getDownloadingSrc().setEmpty(true);
                    node.getDownloadingSrc().setUploading(false);
                }
                node.unsetDownloading();
                node.hello(m.estimatedFinishTime);
                node.setEmpty(false);
                break;
            // error
            case ERROR:
                if(node.getDownloadingSrc() != null) {
                    logger.log(Level.SEVERE, "Node {0} <- {1} ERROR!",new Object[]{
                        getNodeById(m.getNodeID()).getHostname(), 
                        node.getDownloadingSrc().getHostname()});
                    if (node.getDownloadingSrc().equals(masterNodeInfo)){
                        cancelTransaction(getID(), TransEnlightSlowNodeUploader.class.getName());
                    }
                    else{
                        transactionService.addTransaction(
                            new TransCancelOfferingFiles(
                                    node.getDownloadingSrc(), 
                                    node, 
                                    TransEnlightSlowNodeUploader.class));
                    }
                    node.getDownloadingSrc().ftpError();
                    node.getDownloadingSrc().setUploading(false);
                }
                node.ftpError();
                node.unsetDownloading();
                break;
                
        }
    }
    
    @Override
    public void gettingToSameLevelFinished(TransEnlightSlowNodeFinished.states state, double slowNodeFinishTime){
        switch(state){
            case CORRECT:
                downloading = false;
                masterNodeInfo.getDownloadingSrc().hello(slowNodeFinishTime);
                masterNodeInfo.getDownloadingSrc().setUploading(false);
                masterNodeInfo.unsetDownloading();
                logger.log(Level.FINER, "Finished download");
                break;
            case EMPTY:
                downloading = false; 
                logger.log(Level.FINER, "Master finished download, {0} is empty",new Object[]{masterNodeInfo.getDownloadingSrc().getHostname()});
                masterNodeInfo.getDownloadingSrc().hello(slowNodeFinishTime);
                masterNodeInfo.getDownloadingSrc().setUploading(false);
                masterNodeInfo.getDownloadingSrc().setEmpty(true);
                masterNodeInfo.unsetDownloading();
                break;
            case ERROR:
                downloading = false; 
                logger.log(Level.SEVERE, "Master <- {0} ERROR!",new Object[]{masterNodeInfo.getDownloadingSrc().getId()});
                transactionService.addTransaction(new TransCancelOfferingFiles(masterNodeInfo.getDownloadingSrc(), masterNodeInfo, TransEnlightSlowNodeUploader.class));
                masterNodeInfo.getDownloadingSrc().setUploading(false);
                masterNodeInfo.getDownloadingSrc().ftpError();
                masterNodeInfo.ftpError();
                masterNodeInfo.unsetDownloading();
                
        }
    }
    
    @Override
    public void fileObtained(String oldNodeHostname, String oldNodeFilename, String newNodeFilename) {
        transactionService.addTransaction(new TransObtainedFileMaster(this, oldNodeHostname, oldNodeFilename, getID(), newNodeFilename));
    }
    
    public void processNodeObtainedFile(ReceivedObject data) {
        MsgFileWorkerChange message = (MsgFileWorkerChange) data.message;
        acknowledge(message.getNodeID(), message.getTransactionID(), data.device);
        
        if(!transactionService.put(data.message)){   
            transactionService.addTransaction(
                    new TransObtainedFileMaster(message.getNodeID(),
                            message.getTransactionID(), 
                            this, message.oldWorkerHostname, 
                            message.oldWorkerFilename, message.getNodeID(),
                            message.newWorkerFilename));
        }
    }

    /**
     * @return the nodes
     */
    public List<NodeInfo> getNodes() {
        return nodes;
    }
    
    
    public NodeInfo getNodeByHostname(String hostname){
        String rawIP;
        try {
            rawIP = InetAddress.getByName(hostname).getHostAddress();
        } catch (UnknownHostException ex) {
            rawIP = hostname;
        }
        for(NodeInfo node: nodes){
            InetAddress nodeInetAdd = node.getAddress();
            if(nodeInetAdd != null && rawIP.equals(nodeInetAdd.getHostAddress())){
                return node;
            }
        }
        return null;
    }
    
    public NodeInfo getNodeById(int id){
        return id < nodes.size() ? nodes.get(id) : null;
    }

    /**
     * @return the results
     */
    public List<List<NetAddress>> getResults() {
        return results;
    }

    
}