/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import execution.TaskManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import network.*;
import static local.TransTransferSharedFiles.states.*;

/**
 * Transaction remove files that can be accessed from more nodes and distribute 
 * them to other nodes.
 * Send {@link MsgFileRemove} {@MsgFileAdd}, receive {@link MsgFileAck}
 * @author Ivan Straka
 */
public class TransTransferSharedFiles extends Transaction {
    private final static Logger logger = Logger.getLogger(TransTransferSharedFiles.class.getName());
    
    private final List<FileObject> candidates;
    private FileObject fileRequest;
    private final NodeInfo slowNode;
    private states state;
    private final TaskManager tasks;
    private long lastSend;
    private NodeInfo fastNode;
    private final Node node;
    private long lastResend;
    private long lastStateChange;
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
    
    enum states{START, REMOVE, ADD, FINISHSLOWNODE, END, FINISHFASTNODE, ROLLBACK};
    
    /**
     *
     * @param slow
     * @param candidates
     * @param tasks
     * @param node
     */
    public TransTransferSharedFiles(NodeInfo slow, List<FileObject> candidates, TaskManager tasks, Node node){
        super(Node.getID(),Transaction.getNewTransactionID());
        this.slowNode = slow;
        slow.lockFiles();
        this.candidates =  new ArrayList<>(candidates);
        logger.log(Level.INFO, "Unloading {0} has started.",slow.getHostname());
        // open communication with slownode or end.
        state = setNewRequest() ? REMOVE : END;
        this.tasks = tasks;
        this.node = node;
        lastStateChange = System.currentTimeMillis();
    }
    
    private boolean setNewRequest(){
        if(fastNode != null) {
            fastNode.unlockFiles();
        }
        for(int i=0; i<candidates.size();i++){
            NodeInfo ni = findFastNode(candidates.get(i));
            if(ni == null){
                candidates.remove(i);
                i--;
            }
            else{
                logger.log(Level.FINER, "Possible {0} to {1}.", new Object[]{candidates.get(i).getWorkerFilename(),ni.getHostname()});
                fileRequest = candidates.get(i);
                fastNode = ni;
                ni.lockFiles();
                candidates.remove(i);
                return true;
            }
        }
        logger.log(Level.FINER,"No possible request.");
        return false;
    }
    
    private NodeInfo findFastNode(FileObject file){
        for(FileAccess access: file.getAllAccesses()){
            if(!access.node.equals(slowNode) && 
                access.node.isAlive() &&
                !access.node.areFilesLocked() && 
                slowNode.getEstimatedFinishTimeSec() - access.node.getEstimatedFinishTimeSec() > Master.computingNodesDiffTimeToTransferFilesSec)
            {
                
                return access.node;
            }
        }
        return null;
    }

    @Override
    public void execute() {
        lastResend = System.currentTimeMillis();
        if(state == REMOVE) {
            try {
                requestToRemove(slowNode);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                setState(END);
                execute();
            }
        }
        else if(state == ADD) {
            try {
                requestToAdd(fastNode, START);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                setState(ROLLBACK);
                execute();
            }
        }
        else if(state == FINISHFASTNODE) {
            try {
                endTransmition(fastNode);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                setState(FINISHSLOWNODE);
                execute();
            }
        }
        else if(state == FINISHSLOWNODE){
            try {
                endTransmition(slowNode);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                setState(END);
            }
        }
        else if(state == ROLLBACK){
            try {
                requestToAdd(slowNode, END);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                setState(END);
            }
        }
        // check the constructor
        // communication has been opened, therefore there should be reaction for
        // finish slow node state.
        else if(state == START){
            if(!setNewRequest()) {
                setState(FINISHSLOWNODE);
            }
            else {
                setState(REMOVE);
            }
            execute();
        }
    }
    
    /**
     * Sends ack to end communication.
     * @throws IOException if can not send message
     */
    private void endTransmition(NodeInfo node) throws IOException{
        if(node.isMaster()){
            if(state == FINISHSLOWNODE) {
                setState(END);
            }
            else if(state == FINISHFASTNODE) {
                setState(FINISHSLOWNODE);
            }
        }
        else{
            node.getDevice().send(new MsgAck(nodeID, transactionID));
            lastSend = System.currentTimeMillis();
        }
    }
    
    private void requestToRemove(NodeInfo target) throws IOException{
        if(!target.isMaster()){
            target.getDevice().send(new MsgFileRemove(nodeID, transactionID, fileRequest.getNodeFilename(target)));
            lastSend = System.currentTimeMillis();
        }
        else{
            if(tasks.removeFile(fileRequest.getNodeFilename(target))){
                setState(ADD);
                execute();
            }
            else{
                logger.log(Level.FINER, "Master refused {0}", fileRequest.getNodeFilename(target));
                setState(START);
                execute();
            }
        }
    }
    
    private void requestToAdd(NodeInfo target, states nextState) throws IOException{
        if(!target.isMaster()){
            target.getDevice().send(new MsgFileAdd(nodeID, transactionID, fileRequest.getNodeFilename(target)));
            lastSend = System.currentTimeMillis();
        }
        else{
            tasks.addFile(fileRequest.getNodeFilename(target));
            fileRequest.changeWorker(target);
            setState(nextState);
            execute();
        }
    }

    @Override
    public void putResponse(Object resp) {   
        if(!(resp instanceof Message)) {
            return;
        }
        Message message = (Message) resp;
        
        if(state == REMOVE && message instanceof MsgFileAck){
            MsgFileAck m = (MsgFileAck) message;
            slowNode.hello(m.estimatedFinishTime);
            // removed
            if(m.successful){
                setState(ADD);
            }
            // not removed, start finding another
            else{
                logger.log(Level.FINER, "{0} refused {1}", new Object[]{slowNode.getHostname(),fileRequest.getWorkerFilename()});
                slowNode.removeFile(fileRequest);
                setState(START);
            }
            execute();
        }
        else if(state == ADD && message instanceof MsgFileAck){
            fastNode.hello(((MsgFileAck) message).estimatedFinishTime);
            logger.log(Level.INFO, "{0} from {1} transfered to {2} as {3}", new Object[]{fileRequest.getWorkerFilename(), slowNode.getHostname(), fastNode.getHostname(), fileRequest.getNodeFilename(fastNode)});
            fileRequest.changeWorker(fastNode);
            setState(FINISHFASTNODE);
            execute();
        }
        else if(state == FINISHFASTNODE && message instanceof MsgAck){
            setState(START);
            execute();
        }
        else if(state == FINISHSLOWNODE && message instanceof MsgAck){
            setState(END);
        }
        
    }

    @Override
    public void update() {
        long actTime = System.currentTimeMillis();
        if(actTime - lastTransactionAck > Master.sendHelloMilis){
            acknowledgeSlowNode();
        }
        if(actTime - lastStateChange > Transaction.timeoutMilis && state == ADD){
            setState(ROLLBACK);
            execute();
        }
        else if (actTime - lastResend > Transaction.resendMilis) {
            execute();
        }
        
    }
    
    

    private void acknowledgeSlowNode() {
        try {
            slowNode.getDevice().send(new MsgTransactionAlive(nodeID, transactionID));
            lastTransactionAck = System.currentTimeMillis();
        } catch (IOException ex) {
           logger.log(Level.SEVERE, "Unable to send transaction alive message", ex);
        }
    }

    @Override
    public void finish() {
        logger.log(Level.INFO, "Unloading {0} has finished.",slowNode.getHostname());
        slowNode.unlockFiles();
    }

    @Override
    public void cancel() {}

    @Override
    public boolean canFinish() {
        return state == END;
    }

    @Override
    public boolean isTimeout() {
        return false;
    }
    
}
