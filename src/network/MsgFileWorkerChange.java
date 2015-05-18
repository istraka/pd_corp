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
 * <br>1B op code: 16
 * <br>4B int message id
 * <br>4B int transaction id
 * <br>String previous worker hostname zero-terminated
 * <br>String previous worker file's filename zero-terminated
 * <br>String new worker file's filename zero-terminated
 * @author Ivan Straka
 */
public class MsgFileWorkerChange extends Message {
    
    /**
     * Opcode, first byte of packet.
     */
    public static final int opCode = 16; 

    /**
     * Sender id.
     */
    public final int nodeID;

    /**
     * Sender's transaction id.
     */
    public final int transactionID;
    
    /**
     * Hostname of previous worker.
     */
    public final String oldWorkerHostname;
    /**
     * Previous worker file's filename.
     */
    public final String oldWorkerFilename;
    /**
     * New worker file's filename.
     */
    public String newWorkerFilename;

    public MsgFileWorkerChange(int nodeID, int commandID, String oldWorkerHostname, String oldWorkerFilename, String newWorkerFilename) {
        this.nodeID = nodeID;
        this.transactionID = commandID;
        this.oldWorkerHostname = oldWorkerHostname;
        this.oldWorkerFilename = oldWorkerFilename;
        this.newWorkerFilename = newWorkerFilename;
    }

    public MsgFileWorkerChange(byte[] data) throws IOException {
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
        
        ds.skip(1);
        nodeID = ds.readInt();
        transactionID = ds.readInt();
        oldWorkerHostname = Message.readString(ds);
        oldWorkerFilename = Message.readString(ds);
        newWorkerFilename = Message.readString(ds);
    }
    
    
    @Override
    public byte[] getBytes() throws IOException{
        
        ByteArrayOutputStream bs = new ByteArrayOutputStream(NetworkService.packetLen);
        DataOutputStream ds = new DataOutputStream(bs);
        
        ds.write(opCode);
        ds.writeInt(nodeID);
        ds.writeInt(transactionID);
        Message.writeString(ds, oldWorkerHostname);
        Message.writeString(ds, oldWorkerFilename);
        Message.writeString(ds, newWorkerFilename);
        
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
