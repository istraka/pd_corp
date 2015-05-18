/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import network.Message;
import network.MsgAck;
import network.MsgCancelNodeTransaction;

/**
 * Transaction is used to inform node abou cancelling transaction of specified type of specified node id.
 * @author Ivan Straka
 */

public class TransCancelOfferingFiles extends Transaction{
    private final static Logger logger = Logger.getLogger(TransCancelOfferingFiles.class.getName());
    private final NodeInfo uploadingNode;
    private final NodeInfo downloadingNode;
    private final String cls;
    private boolean finished;
    private final long timeStart;
    private long lastResend;

    public TransCancelOfferingFiles(NodeInfo uploadingNode, NodeInfo downloadingNode, Class<? extends Transaction> cls) {
        super(Node.getID(),Transaction.getNewTransactionID());
        this.uploadingNode = uploadingNode;
        this.downloadingNode = downloadingNode;
        downloadingNode.lockFiles();
        this.cls = cls.getName();   
        finished = false;
        timeStart = System.currentTimeMillis();
    }
    
    /**
     * Sends message.
     * @see network.MsgCancelNodeTransaction
     */
    @Override
    public void execute() {
        try {
            logger.log(Level.INFO, "{0} prompted to delete {1} from {2}", new Object[]{uploadingNode.getHostname(), cls, downloadingNode.getHostname()});
            uploadingNode.getDevice().send(new MsgCancelNodeTransaction(nodeID, transactionID, downloadingNode.getId(), cls));
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Sending command has failed", ex);
        }
        lastResend = System.currentTimeMillis();
    }

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
                logger.log(Level.INFO, "Resending: {0} prompted to delete {1} associated with {2}", new Object[]{uploadingNode.getHostname(), cls, uploadingNode.getHostname()});
                uploadingNode.getDevice().send(new MsgCancelNodeTransaction(nodeID, transactionID, uploadingNode.getId(), cls));
                lastResend = System.currentTimeMillis();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Resending command to delete transaction failed", ex);
                finished = true;
            }
        }
    }

    @Override
    public void finish() {
        downloadingNode.unlockFiles();
    }

    @Override
    public void cancel() {
        logger.log(Level.INFO, "TIMEOUT: {0} prompted to delete {1} from {2}", new Object[]{uploadingNode.getHostname(), cls, uploadingNode.getHostname()});
        downloadingNode.unlockFiles();
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
