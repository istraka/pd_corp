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
 * <br>1B op code: 12
 * <br>4B int node id
 * <br>4B int transaction id
 * <br>filename zero-terminated
 * @author Ivan Straka
 */
public class MsgFileRemove extends Message{
    public static final int opCode = 12;
    
    /**
     * Sender id.
     */
    public final int nodeID;
    /**
     * Sender's transaction id.
     */
    public final int transactionID;
    /**
     * Offered filename.
     */
    public final String filename;
    
    /**
     *
     * @param nodeID
     * @param commandID
     * @param filename
     */
    public MsgFileRemove(int nodeID, int commandID, String filename){
        this.nodeID = nodeID;
        this.transactionID = commandID;
        this.filename = filename;
    }
    
    public MsgFileRemove(byte[] data) throws IOException{
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
        
        ds.skip(1);
        nodeID = ds.readInt();
        transactionID = ds.readInt();
        filename = Message.readString(ds);
    }

    @Override
    public byte[] getBytes() throws IOException {
        
        ByteArrayOutputStream bs = new ByteArrayOutputStream(NetworkService.packetLen);
        DataOutputStream ds = new DataOutputStream(bs);
        
        ds.write(opCode);
        ds.writeInt(nodeID);
        ds.writeInt(transactionID);
        Message.writeString(ds, filename);
        
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
