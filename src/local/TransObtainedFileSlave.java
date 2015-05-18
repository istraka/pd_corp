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
import network.MsgFileWorkerChange;
import network.NodeDevice;

/**
 *
 * @author Ivan Straka
 */

// todo comments
public class TransObtainedFileSlave extends Transaction{
    private final static Logger logger = Logger.getLogger(TransObtainedFileSlave.class.getName());
    
    private final String oldWorkerHostname;
    private final String oldWorkerFilename;
    private final String newWorkerFilename;
    private final long timeStart;
    private long lastResend;
    private boolean finished;
    private final NodeDevice device;

    public TransObtainedFileSlave(NodeDevice masterDevice, String oldWorkerHostname, String oldWorkerFilename, String newWorkerFilename) {
        super(Node.getID(), Transaction.getNewTransactionID());
        this.oldWorkerHostname = oldWorkerHostname;
        this.oldWorkerFilename = oldWorkerFilename;
        this.newWorkerFilename = newWorkerFilename;
        timeStart = System.currentTimeMillis();
        finished = false;
        device = masterDevice;
    }

    @Override
    public void execute() {
        try {
            device.send(new MsgFileWorkerChange(nodeID, transactionID, oldWorkerHostname, oldWorkerFilename, newWorkerFilename));
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        lastResend = System.currentTimeMillis();
    }

    /**
     * Process finish.
     */
    @Override
    public void putResponse(Object resp) {   
        if(!(resp instanceof Message)){
            return;
        }
        
        Message message = (Message) resp;
        
        if(message instanceof MsgAck){
            finished = true;
        }
    }
    
    
    /**
     * Resend message after {@link Transaction#resendMilis}.
     */
    @Override
    public void update() {
        if (System.currentTimeMillis() - lastResend > Transaction.resendMilis && !finished){
            try {
                device.send(new MsgFileWorkerChange(nodeID, transactionID, oldWorkerHostname, oldWorkerFilename, newWorkerFilename));
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
            lastResend = System.currentTimeMillis();
        }
    }
    
    @Override
    public void finish() {}
    @Override
    public void cancel() {}
    @Override
    public boolean canFinish() {return finished;}
    
    @Override
    public boolean isTimeout() {
        return(!finished && System.currentTimeMillis() - timeStart > Transaction.timeoutMilis);
    }
    
}
