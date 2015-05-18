/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import network.*;

/**
 * Quit slaves.
 * Send {@link MsgQuit}
 * @author Ivan Straka
 */
public class TransQuitSlave extends Transaction {
    
    private final static Logger logger = Logger.getLogger(TransQuitSlave.class.getName());

    private final List<NodeInfo> nodes;
    
    public TransQuitSlave(NodeInfo node){
        super(Node.getID(),0);
        nodes = (Arrays.asList(node));
    }
    
    public TransQuitSlave(List<NodeInfo> nodes){
        super(Node.getID(),0);
        this.nodes = nodes;
    }
    
    /**
     * Send mesages.
     * @see network.Message#composeQuit() 
     */
    @Override
    public void execute() {
        for(NodeInfo node: nodes){
            if(!node.isMaster() && node.getDevice() != null) {
                try {
                    logger.log(Level.INFO, "Quitting: {0}", node.getNodeAddress());
                    node.quit();
                    node.getDevice().send(new MsgQuit());
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "Sending message failed!", ex);
                }
            }
        }
    }
    
    @Override
    public void putResponse(Object response){}
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
