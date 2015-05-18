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
 * <br>1B op code: 17
 * <br>4B int node id
 * <br>4B int transaction id
 * <br>Zero-terminated string of class name
 * @author Ivan Straka
 */
public class MsgCancelNodeTransaction extends Message {
    public static final int opCode = 17;
    /**
     * Sender id.
     */
    public final int nodeID;
    /**
     * Sender's transaction id.
     */
    public final int transactionID;
    /**
     * node id of which transaction should be cancelled
     */
    public final int transactionNodeID;
    /**
     * class name
     */
    public final String transactionClassName;
    
    /**
     * Construct class from known values.
     */
    public MsgCancelNodeTransaction(int nodeID, int commandID, int transactionNodeID, String className){
        this.nodeID = nodeID;
        this.transactionID = commandID;
        this.transactionNodeID = transactionNodeID;
        this.transactionClassName = className;
    }
    
    /**
     * Construct class from byte[] packet.
     * @param data 
     * @throws java.io.IOException 
     */
    public MsgCancelNodeTransaction(byte[] data) throws IOException{
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
        
        ds.skip(1);
        nodeID = ds.readInt();
        transactionID = ds.readInt();
        transactionNodeID = ds.readInt();
        transactionClassName = Message.readString(ds);
    }
    
    @Override
    public byte[] getBytes() throws IOException{
        
        ByteArrayOutputStream bs = new ByteArrayOutputStream(NetworkService.packetLen);
        DataOutputStream ds = new DataOutputStream(bs);
        
        ds.write(opCode);
        ds.writeInt(nodeID);
        ds.writeInt(transactionID);
        ds.writeInt(transactionNodeID);
        Message.writeString(ds, transactionClassName);
        
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
