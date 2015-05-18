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
 * <br>1B op code: 3
 * <br>4B int node id
 * <br>4B int transaction id
 * <br>4B int request id
 * @author Ivan Straka
 */
public class MsgFileRequest extends Message{
    public static final int opCode = 3;
    
    /**
     * Sender id.
     */
    public final int nodeID;
    /**
     * Sender's transaction id.
     */
    public final int transactionID;
    /**
     * Request id.
     */
    public final int requestID;
    
    /**
     * Construct class from known values
     * @param nodeID
     * @param commandID
     * @param requestID
     */
    public MsgFileRequest(int nodeID, int commandID, int requestID){
        this.nodeID = nodeID;
        this.transactionID = commandID;
        this.requestID = requestID;
    }
    
    /**
     * Construct class from byte[] packet.
     * @param data 
     * @throws java.io.IOException 
     */
    public MsgFileRequest(byte[] data) throws IOException{
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
        
        ds.skip(1);
        nodeID = ds.readInt();
        transactionID = ds.readInt();
        requestID = ds.readInt();
    }

    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream bs = new ByteArrayOutputStream(NetworkService.packetLen);
        DataOutputStream ds = new DataOutputStream(bs);
        
        ds.write(opCode);
        ds.writeInt(nodeID);
        ds.writeInt(transactionID);
        ds.writeInt(requestID);
        
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
