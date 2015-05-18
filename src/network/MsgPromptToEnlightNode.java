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
* <br>1B op code: 2
 * <br>4B int node id
 * <br>4B int transaction id
* <br>String slow-node-address:port zero-terminated
* <br>String slow-node-ftp-address:port zero-terminated
* 
 * @author Ivan Straka
 */
public class MsgPromptToEnlightNode extends Message {

    /**
     * Opcode, first byte of packet.
     */
    public static final int opCode = 2; 

    /**
     * Sender id.
     */
    public final int nodeID;

    /**
     * Sender's transaction id.
     */
    public final int transactionID;

    /**
     * Address of slow node.
     */
    public final NetAddress nodeAddress;
    /**
     * Ftp address of slow node.
     */
    public final NetAddress ftpAddress;
    
    /**
     * Construct from known values.
     */
    public MsgPromptToEnlightNode(int nodeID, int commandID, NetAddress nodeAddress, NetAddress ftpAddress){
        this.nodeID = nodeID;
        this.transactionID = commandID;
        this.nodeAddress = nodeAddress;
        this.ftpAddress = ftpAddress;
    }
    
    public MsgPromptToEnlightNode(byte[] data) throws IOException{
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
        
        ds.skip(1);
        nodeID = ds.readInt();
        transactionID = ds.readInt();
        nodeAddress = NetAddress.getAddress(Message.readString(ds));
        ftpAddress = NetAddress.getAddress(Message.readString(ds));
        
    }

    @Override
    public byte[] getBytes() throws IOException{
        ByteArrayOutputStream bs = new ByteArrayOutputStream(NetworkService.packetLen);
        DataOutputStream ds = new DataOutputStream(bs);
        
        ds.write(opCode);
        ds.writeInt(nodeID);
        ds.writeInt(transactionID);
        Message.writeString(ds, nodeAddress.toString());
        Message.writeString(ds, ftpAddress.toString());
        
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
