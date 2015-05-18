/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import execution.TaskManager;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import local.Node;
import local.NodeInfo;
import network.*;

/**
 * Transaction sends {@link MsgStart} messages to given nodes.
 * @author Ivan Straka
 */
public class TransStartSlaves extends Transaction{
    
    private final static Logger logger = Logger.getLogger(TransStartSlaves.class.getName());
    
    private final List<NodeInfo> nodes;
    
    public TransStartSlaves(List<NodeInfo> nodes){
        super(Node.getID(),0);
        this.nodes = nodes;
    }
    
    /**
     * Sends start messages.
     * @see network.Message#composeStart() 
     */
    @Override
    public void execute(){
        for(NodeInfo node: nodes){
            if(node.isReady() && !node.isMaster()){
                try {
                    node.hello(TaskManager.NOTHING_DONE);
                    logger.log(Level.INFO, "Starting slave: {0}", node.getNodeAddress());
                    node.getDevice().send(new MsgStart());
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "unable to start "+node.getAddress(), ex);
                }
            }
        }
    }
    
    
    @Override
    public void putResponse(Object resp) {}
    @Override
    public void update() {}
    @Override
    public void finish() {}
    @Override
    public void cancel() {}
    @Override
    public boolean canFinish() {return true;}
    @Override
    public boolean isTimeout() {return false;}
    
}
