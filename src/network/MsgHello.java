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
 * <br>byte op code: 1
 * <br>4B int node id
 * <br>8B double estimated finish time 
 * @author Ivan Straka
 */
public class MsgHello extends Message{
    
    /**
     * Opcode, first byte of packet.
     */
    public static final byte opCode = 1;
    
    /**
     * Node id.
     */
    public final int nodeID;
    /**
     * Estimated finish time.
     */
    public final double estimatedFinishTime;
    
    /**
     * Construct class from known values.
     * @param nodeID
     * @param estimatedFinishTime
     */
    public MsgHello(int nodeID, double estimatedFinishTime){
        this.nodeID = nodeID;
        this.estimatedFinishTime = estimatedFinishTime;
    }
    
    /**
     * Construct from byte[] packet.
     * @param data
     * @throws IOException 
     */
    public MsgHello(byte[] data) throws IOException{
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
        
        ds.skip(1);
        nodeID = ds.readInt();
        estimatedFinishTime = ds.readDouble();
    }

    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream bs = new ByteArrayOutputStream(NetworkService.packetLen);
        DataOutputStream ds = new DataOutputStream(bs);
        
        // opcode
        ds.write(opCode);
        ds.writeInt(nodeID);
        ds.writeDouble(estimatedFinishTime);
        
        ds.flush();
        return bs.toByteArray();
    }

    @Override
    public int getNodeID() {
        return nodeID;
    }

    @Override
    public int getTransactionID() {
        throw new UnsupportedOperationException("Hello message does no contains transaction id."); 
    }
}
