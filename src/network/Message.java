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
 * Class provide methods to compose message, read and write strings.
 * @author Ivan Straka
 */
public abstract class Message {
    
    /**
     * create message based on opcode - first byte of byte[]
     * @throws IOException 
     */
    public static Message createMessageFromPacket(byte[] data) throws IOException{
        
        if (data.length < 1){
            return null;
        }
        
        switch(data[0]){
            
            case MsgAck.opCode:
                return new MsgAck(data);
                
            case MsgAbort.opCode:
                return new MsgAbort(data);
                
            case MsgFileAdd.opCode:
                return new MsgFileAdd(data);
                
            case MsgFileAck.opCode:
                return new MsgFileAck(data);
                
            case MsgFileOffer.opCode:
                return new MsgFileOffer(data);
                
            case MsgFileRemove.opCode:
                return new MsgFileRemove(data);
                
            case MsgFileRequest.opCode:
                return new MsgFileRequest(data);
                
            case MsgPromptToEnlightNode.opCode:
                return new MsgPromptToEnlightNode(data);
                
            case MsgGetToSameLevelFinished.opCode:
                return new MsgGetToSameLevelFinished(data);
                
            case MsgHello.opCode:
                return new MsgHello(data);
                
            case MsgQuit.opCode:
                return new MsgQuit();
                
            case MsgReady.opCode:
                return new MsgReady(data);
                
            case MsgStart.opCode:
                return new MsgStart();
                
            case MsgSteady.opCode:
                return new MsgSteady();
                
            case MsgFileResultNotify.opCode:
                return new MsgFileResultNotify(data);
                
            case MsgFileWorkerChange.opCode:
                return new MsgFileWorkerChange(data);
                
            case MsgCancelNodeTransaction.opCode:
                return new MsgCancelNodeTransaction(data);
        }
        
        return null;
    }
    
    /**
     * Read and return zero-terminated string from stream.
     * @throws IOException 
     */
    public static String readString(ByteArrayInputStream stream) throws IOException {
        return readString(new DataInputStream(stream));
    }
    
    /**
     * Read and return zero-terminated string from stream.
     * @throws IOException 
     */
    public static String readString(DataInputStream stream) throws IOException {
        String val = "";
        int c = stream.read();
        while (c != 0) {
            val += (char) c;
            c = stream.read();
            
            // string has to be zero-terminated
            if (c == -1) {
                return "";
            }
        }
        return val;
    }
    
    
    /**
     * write zero-terminated string to stream.
     * @throws IOException 
     */
    public static void writeString(ByteArrayOutputStream stream, String s) throws IOException {
        stream.write(s.getBytes());
        stream.write(0);
    }
    
    
    
    /**
     * write zero-terminated string to stream.
     * @throws IOException 
     */
    public static void writeString(DataOutputStream stream, String s) throws IOException {
        stream.write(s.getBytes());
        stream.write(0);
    }
    
    /**
     * 
     * @return byte[] for sending over network
     * @throws IOException 
     */
    
    
    public abstract byte[] getBytes() throws IOException;
    /**
     * Used for identification.
     * i.e. in MsgHello message define sender id.
 Also, this may be used with {@link Message#getTransactionID()} for unique command identification.
     * <br> Example: nodes A nad B
     * <br> A sends {@link MsgFileRequest} with A's id-2 and command id-3 to B. 
     * <br> B sends {@link MsgFileOffer} with node id-2 and command id-3 to A.
     * <br> A successfuly downloads file and sends {@link MsgAck} with nodeID 2 and transactionID 3 to B to end transfering.
 <br> B can map some object to 2=>3=>object to handle transfering.
     * @return node id
     */
    public abstract int getNodeID();
    /**
     * Used for identification.
     * i.e. in MsgHello message define sender id.
 Also, this may be used with {@link Message#getTransactionID()} for unique transaction identification.
     * <br> Example: nodes A nad B
     * <br> A sends {@link MsgFileRequest} with A's id-2 and command id-3 to B. 
     * <br> B sends {@link MsgFileOffer} with node id-2 and command id-3 to A.
     * <br> A successfuly downloads file and sends {@link MsgAck} with nodeID 2 and transactionID 3 to B to end transfering.
 <br> B can map some object to 2=>3=>object to handle transfering.
     * @return node id
     */
    public abstract int getTransactionID();
    
}
