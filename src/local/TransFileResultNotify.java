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
import network.MsgAck;
import network.MsgFileResultNotify;
import network.Message;
import network.NetAddress;
import network.NodeDevice;

/**
 * Transaction notify master about result destinations.
 * Slave sends {@link MsgFileResultNotify}, master sends {@link MsgAck}
 * @author Ivan Straka
 */
public class TransFileResultNotify extends Transaction {
    private final static Logger logger = Logger.getLogger(TransFileResultNotify.class.getName());
        
    private final NodeDevice masterDevice;
    private final List<NetAddress> addresses;
    private boolean finished;
    private long lastResend;
    private long timeStart;
    
    /**
     * 
     * @param addresses List of adresses defiing more accesses to one file.
     */
    public TransFileResultNotify(NodeDevice masterDevice, List<NetAddress> addresses){
        super(Node.getID(), Transaction.getNewTransactionID());
        this.masterDevice = masterDevice;
        this.addresses = addresses;
        finished = false;
        timeStart = System.currentTimeMillis();
    }
    
    /**
     * 
     * @param address one address
     */
    public TransFileResultNotify(NodeDevice masterDevice, NetAddress address){
        this(masterDevice, Arrays.asList(address));
    }

    @Override
    public void execute() {
        try {
            masterDevice.send(new MsgFileResultNotify(nodeID, transactionID, addresses));
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
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

    @Override
    public void update() {
        if (!finished && System.currentTimeMillis() - lastResend > Transaction.resendMilis){
            try {
                masterDevice.send(new MsgFileResultNotify(nodeID, transactionID, addresses));
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
            lastResend = System.currentTimeMillis();
        }
    }

    @Override
    public void finish(){}

    @Override
    public void cancel() {
        logger.log(Level.INFO, "Unable to notify master about result: {0} and its possible copies on other nodes.",addresses.get(0));
    }

    @Override
    public boolean canFinish() {
        return finished;
    }
    
    @Override
    public boolean isTimeout() {
        return(System.currentTimeMillis() - timeStart > Transaction.timeoutMilis);
    }
    
}
