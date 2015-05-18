/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * <br>1B op code: 13
 * <br>4B int node id
 * <br>4B int transaction id
 * <br>8B double estimated finish time
 * <br>1B 1-file was remove operation was successful, 0 otherwise
 * @author Ivan Straka
 */
public class MsgFileAck extends Message{
    public static final int opCode = 13;
    
    /**
     * Sender id.
     */
    public final int nodeID;
    /**
     * Sender's transaction id.
     */
    public final int transactionID;
    /**
     * Estimated finish time.
     */
    public final double estimatedFinishTime;
    /**
     * True if file was removed from the queue.
     */
    public final boolean successful;
    
    /**
     * Construct class from known values
     * @param nodeID
     * @param commandID
     * @param estimatedFinishTime
     * @param successful
     */
    public MsgFileAck(int nodeID, int commandID, double estimatedFinishTime, boolean successful){
        this.nodeID = nodeID;
        this.transactionID = commandID;
        this.estimatedFinishTime = estimatedFinishTime;
        this.successful = successful;
    }
    
    public MsgFileAck(byte[] data) throws IOException{
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
        
        ds.skip(1);
        nodeID = ds.readInt();
        transactionID = ds.readInt();
        estimatedFinishTime = ds.readDouble();
        successful = ds.read()==1;
    }
    
    @Override
    public byte[] getBytes() throws IOException{
        ByteArrayOutputStream bs = new ByteArrayOutputStream(NetworkService.packetLen);
        DataOutputStream ds = new DataOutputStream(bs);
        
        ds.write(opCode);
        ds.writeInt(nodeID);
        ds.writeInt(transactionID);
        ds.writeDouble(estimatedFinishTime);
        ds.write(successful?1:0);
        
        ds.flush();
        return bs.toByteArray();
    }

    @Override
    public int getNodeID(){
        return nodeID;
    }

    @Override
    public int getTransactionID(){
        return transactionID;
    }
    
}
