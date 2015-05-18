/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * <br>byte op code: 9
 * @author Ivan Straka
 */
public class MsgQuit extends Message{
    public static final int opCode = 9;

    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream bs = new ByteArrayOutputStream(NetworkService.packetLen);
        DataOutputStream ds = new DataOutputStream(bs);
        
        ds.write(opCode);
        
        ds.flush();
        return bs.toByteArray();
    }

    @Override
    public int getNodeID(){
        throw new UnsupportedOperationException("Quit message does not contain node id."); 
    }

    @Override
    public int getTransactionID(){
        throw new UnsupportedOperationException("Quit message does not contain command id."); 
    }
    
}
