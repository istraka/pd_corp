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
 * <br>byte op code: 18
 * <br>4B int node id
 * <br>4B int estimated finish time 
 * @author Ivan Straka
 */
public class MsgTransactionAlive extends Message{
    
    /**
     * Opcode, first byte of packet.
     */
    public static final byte opCode = 18;
    
    /**
     * Node id.
     */
    public final int nodeID;
    /**
     * Transaction id.
     */
    public final int transactionID;
    
    /**
     * Construct class from known values.
     */
    public MsgTransactionAlive(int nodeID, int transactionID){
        this.nodeID = nodeID;
        this.transactionID = transactionID;
    }
    
    /**
     * Construct from byte[] packet.
     * @param data
     * @throws IOException 
     */
    public MsgTransactionAlive(byte[] data) throws IOException{
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
        
        ds.skip(1);
        nodeID = ds.readInt();
        transactionID = ds.readInt();
    }

    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream bs = new ByteArrayOutputStream(NetworkService.packetLen);
        DataOutputStream ds = new DataOutputStream(bs);
        
        // opcode
        ds.write(opCode);
        ds.writeInt(nodeID);
        ds.writeInt(transactionID);
        
        ds.flush();
        return bs.toByteArray();
    }

    @Override
    public int getNodeID() {
        return nodeID;
    }

    @Override
    public int getTransactionID() {
        return transactionID;
    }
}
