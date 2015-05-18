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
import local.TransEnlightSlowNodeFinished.states;
import static local.TransEnlightSlowNodeFinished.states.*;

/**
 * <br>1B op code: 5
 * <br>4B int node id
 * <br>4B int transaction id
 * <br>1B state
 * <br>8B double sender node estimated finish time
 * <br>8B double slow node estimated finish time
 * @author Ivan Straka
 */
public class MsgGetToSameLevelFinished extends Message{
    public static final int opCode = 5;
    
    /**
     * Sender id.
     */
    public final int nodeID;
    /**
     * Sender's transaction id.
     */
    public final int transactionID;
    /**
     * State. 
     * <br>0:Correct
     * <br>1:Empty
     * <br>2:Error
     */
    public final states state;
    /**
     * Estimated finish time.
     */
    public final double estimatedFinishTime;
    public final double serverEstimatedFinishTime;
    
    /**
     * Construct class from known values
     * @param nodeID
     * @param commandID
     * @param state
     * @param estimatedFinishTime
     * @throws IOException
     */
    public MsgGetToSameLevelFinished (int nodeID, int commandID, states state, double estimatedFinishTime, double serverEstimatedFinishTime){
        this.nodeID = nodeID;
        this.transactionID = commandID;
        this.state = state;
        this.estimatedFinishTime = estimatedFinishTime;
        this.serverEstimatedFinishTime = serverEstimatedFinishTime;
    }
    
    public MsgGetToSameLevelFinished(byte[] data) throws IOException{
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
        
        ds.skip(1);
        nodeID = ds.readInt();
        transactionID = ds.readInt();
        switch(ds.read()){
            case 0:
                state = CORRECT;
                break;
            case 1:
                state = EMPTY;
                break;
            case 2:
            default:
                state = ERROR;
                break;
        }
        estimatedFinishTime = ds.readDouble();
        serverEstimatedFinishTime = ds.readDouble();
    }
    
    @Override
    public byte[] getBytes() throws IOException{
        ByteArrayOutputStream bs = new ByteArrayOutputStream(NetworkService.packetLen);
        DataOutputStream ds = new DataOutputStream(bs);
        
        ds.write(opCode);
        ds.writeInt(nodeID);
        ds.writeInt(transactionID);
        switch(state){
            case CORRECT:
                ds.write(0);
                break;
            case EMPTY:
                ds.write(1);
                break;
            case ERROR:
            default:
                ds.write(2);
                break;
        }
        ds.writeDouble(estimatedFinishTime);
        ds.writeDouble(serverEstimatedFinishTime);
        
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
