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
 * <br>1B op code: 6
 * <br>4B int node id
 * <br>4B int node port
 * <br>4B int frp port
 * <br>String return folder:host:ftpPort if returning result is requested, empty string otherwise zero-terminated
 * @author Ivan Straka
 */
public class MsgReady extends Message {
    public static final int opCode = 6;
    
    /**
     * Slave's id.
     */
    public final int nodeID;
    /**
     * Slave's port.
     */
    public final int nodePort;
    /**
     * Slave's ftp port.
     */
    public final int ftpPort;
    /**
     * Slave's result return address
     */
    public final NetAddress returnFileAddress;
    
    /**
     * Construct class from known values.
     * @param nodeID
     * @param nodePort
     * @param ftpPort
     * @param returnFileAddress
     */
    public MsgReady(int nodeID, int nodePort, int ftpPort, NetAddress returnFileAddress){
        this.nodeID = nodeID;
        this.nodePort = nodePort;
        this.ftpPort = ftpPort;
        this.returnFileAddress = returnFileAddress;
    }
    
    public MsgReady(byte[] data) throws IOException{
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
        
        ds.skip(1);
        nodeID = ds.readInt();
        nodePort = ds.readInt();
        ftpPort = ds.readInt();
        returnFileAddress = NetAddress.getAddress(Message.readString(ds));
    }

    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream bs = new ByteArrayOutputStream(NetworkService.packetLen);
        DataOutputStream ds = new DataOutputStream(bs);
        
        ds.write(opCode);
        ds.writeInt(nodeID);
        ds.writeInt(nodePort);
        ds.writeInt(ftpPort);
        Message.writeString(bs, returnFileAddress.toString());
        
        ds.flush();
        return bs.toByteArray();
    }

    @Override
    public int getNodeID(){
        return nodeID;
    }

    @Override
    public int getTransactionID(){
        throw new UnsupportedOperationException("Ready message does not contain command id."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
