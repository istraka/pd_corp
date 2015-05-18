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
 * Transaction inform master node about the state of the transfering.
 * Send {@link MsgGetToSameLevelFinished}, receive {@link MsgAck}.
 * @author Ivan Straka
 */
public class TransEnlightSlowNodeFinished extends Transaction {
    private final static Logger logger = Logger.getLogger(TransEnlightSlowNodeMasterCommand.class.getName());
    
    private final NodeDevice device;
    private boolean finished;
    private final double finishTime;
    private final double slowNodeFinishTime;
    private final states state;
    private long lastResend;
    private final long timeStart;
    
    public enum states{CORRECT, EMPTY, ERROR};
    
    public TransEnlightSlowNodeFinished(NodeDevice master, double finishTime, double slowNodeFinishTime, states state){
        
        super(Node.getID(),Transaction.getNewTransactionID());
        device = master;
        this.finishTime = finishTime;
        this.slowNodeFinishTime = slowNodeFinishTime;
        this.state = state;
        finished = false;
        timeStart = System.currentTimeMillis();
    }
    
    /**
     * Send obtainging files done message with given state.
     */
    @Override
    public void execute() {
        try {
            device.send(new MsgGetToSameLevelFinished(nodeID, transactionID, state, finishTime, slowNodeFinishTime));
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
                device.send(new MsgGetToSameLevelFinished(nodeID, transactionID, state, finishTime, slowNodeFinishTime));
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
        return !finished && System.currentTimeMillis() - timeStart > Transaction.timeoutMilis;
    }
}
