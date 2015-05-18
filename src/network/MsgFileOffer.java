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
 * <br>1B op code: 4
 * <br>4B int message id
 * <br>4B int transaction id
 * <br>4B int request id
 * <br>String filename zero-terminated
 * <br>String return folder:host:ftpPort if returning result is requested, empty string otherwise zero-terminated
 * <br>8B double estimated finish time
 * @author Ivan Straka
 */
public class MsgFileOffer extends Message {
    public static final int opCode = 4;
    
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
     * Offered filename.
     */
    public final String filename;
    /**
     * Return folder for result, if returning is requested. Empty string otherwise.
     */
    public final NetAddress returnFtpFolder;
    /**
     * Estimated finish time.
     */
    public final double estimatedFinishTime;
    
    /**
     * Construct class from known values.
     * @param nodeID
     * @param commandID
     * @param requestID
     * @param filename
     * @param returnFtpFolder
     * @param estimatedFinishTime
     */
    public MsgFileOffer(int nodeID, int commandID, int requestID, String filename, NetAddress returnFtpFolder, double estimatedFinishTime){
        this.nodeID = nodeID;
        this.transactionID = commandID;
        this.requestID = requestID;
        this.filename = filename;
        this.returnFtpFolder = returnFtpFolder;
        this.estimatedFinishTime = estimatedFinishTime;
    }
    
    /**
     * Construct class from byte[] packet.
     * @param data 
     * @throws java.io.IOException 
     */
    public MsgFileOffer(byte[] data) throws IOException{
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
        
        ds.skip(1);
        nodeID = ds.readInt();
        transactionID = ds.readInt();
        requestID = ds.readInt();
        estimatedFinishTime = ds.readDouble();
        filename = Message.readString(ds);
        returnFtpFolder = NetAddress.getAddress(Message.readString(ds));
    }
    
    @Override
    public byte[] getBytes() throws IOException{
        
        ByteArrayOutputStream bs = new ByteArrayOutputStream(NetworkService.packetLen);
        DataOutputStream ds = new DataOutputStream(bs);
        
        ds.write(opCode);
        ds.writeInt(nodeID);
        ds.writeInt(transactionID);
        ds.writeInt(requestID);
        ds.writeDouble(estimatedFinishTime);
        Message.writeString(ds, filename);
        Message.writeString(ds, (returnFtpFolder != null ? returnFtpFolder.toString() : ""));
        
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
