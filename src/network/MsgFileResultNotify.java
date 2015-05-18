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
import java.util.LinkedList;
import java.util.List;

/**
 * <br>1B op code: 15
 * <br>4B int node id
 * <br>4B int transaction id
 * <br>8B double estimated finish time
 * <br>Zero-terminated strings defiining {@link NetAddress}. Every one is pointing at the same file.
 * @author Ivan Straka
 */
public class MsgFileResultNotify extends Message{
    public static final int opCode = 15;
    
    /**
     * Sender id.
     */
    public final int nodeID;
    /**
     * Sender's transaction id.
     */
    public final int transactionID;
    /**
     * List of nodes that has access to file.
     * Adress contains hostname and filename. Master must identify nodes by hostnames.
     */
    public final List<NetAddress> address;
    
    public MsgFileResultNotify(int nodeID, int commandID, List<NetAddress> addresses){
        this.nodeID = nodeID;
        this.transactionID = commandID;
        this.address = addresses;
    }
    
    public MsgFileResultNotify(byte[] data) throws IOException{
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
        
        ds.skip(1);
        nodeID = ds.readInt();
        transactionID = ds.readInt();
        
        address = new LinkedList<>();
        while(true){
            String add = Message.readString(ds);
            if(add.equals("")) {
                break;
            }
            address.add(NetAddress.getAddress(add));
        }
    }
    
    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream bs = new ByteArrayOutputStream(NetworkService.packetLen);
        DataOutputStream ds = new DataOutputStream(bs);
        
        ds.write(opCode);
        ds.writeInt(nodeID);
        ds.writeInt(transactionID);
        
        for(NetAddress add: address){
            Message.writeString(ds, add.toString());
        }
        
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
