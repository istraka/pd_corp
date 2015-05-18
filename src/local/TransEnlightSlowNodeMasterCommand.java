/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import network.*;

/**
 * Cmd commands node get to the similar finish time as slower node.
 * Send {@link MsgPromptToEnlightNode}, need {@link MsgAck}
 * @author Ivan Straka
 */
public class TransEnlightSlowNodeMasterCommand extends Transaction {
    private final static Logger logger = Logger.getLogger(TransEnlightSlowNodeMasterCommand.class.getName());

    private final NodeInfo slower;
    private final NodeInfo faster;
    private boolean finished;
    private final long timeStart;
    private long lastResend;
    
    public TransEnlightSlowNodeMasterCommand(NodeInfo slower, NodeInfo faster){
        super(Node.getID(),Transaction.getNewTransactionID());
        this.faster = faster;
        this.slower = slower;
        faster.setDownloading(slower);
        slower.setUploading(true);
        finished = false;
        timeStart = System.currentTimeMillis();
    }
    
    /**
     * Sends obtainFilesFrom message.
     * @see network.Message#composeGetToSameLevelCmd(int, java.lang.String, java.lang.String) 
     */
    @Override
    public void execute() {
        try {
            faster.getDevice().send(new MsgPromptToEnlightNode(nodeID, transactionID, slower.getNodeAddress(), slower.getFtpAddress()));
            logger.log(Level.INFO, "{0} prompted to lighten {1}", new Object[]{faster.getNodeAddress(), slower.getNodeAddress()});
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Sending command to lighten node has failed", ex);
        }
        lastResend = System.currentTimeMillis();
    }

    /**
     * Process ack.
     * @param resp
     */
    @Override
    public void putResponse(Object resp) {       
        if(!(resp instanceof Message)) {
            return;
        }
        Message message = (Message) resp;
        
        if(message instanceof MsgAck){
            finished = true;
        }
    }

    /**
     * Resend command.
     */
    @Override
    public void update() {
        if (System.currentTimeMillis() - lastResend > Transaction.resendMilis && !finished){
            try {
                logger.log(Level.FINE, "Resending: {0} prompted to lighten {1}", new Object[]{slower.getNodeAddress(), faster.getNodeAddress(), transactionID});
                faster.getDevice().send(new MsgPromptToEnlightNode(nodeID, transactionID, slower.getNodeAddress(), slower.getFtpAddress()));
                lastResend = System.currentTimeMillis();
            } catch (IOException ex) {
            logger.log(Level.SEVERE, "Resending command to lighten node has failed", ex);
            finished = true;
            }
        }
    }

    @Override
    public void finish() {
    }

    @Override
    public void cancel() {
        logger.log(Level.INFO,"TIMEOUT: {0} prompted to lighten {1}", new Object[]{slower.getNodeAddress(), faster.getNodeAddress(), transactionID});
        slower.setUploading(false);
        faster.unsetDownloading();
    }

    @Override
    public boolean canFinish() {
        return finished;
    }

    @Override
    public boolean isTimeout() {
        return(!finished && System.currentTimeMillis() - timeStart > Transaction.timeoutMilis);
    }
    
}
