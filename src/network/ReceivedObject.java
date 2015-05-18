/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

/**
 * Class contains derived class of {@link Message} and {@link NodeDevice} instance.
 * @author Ivan Straka
 */
public class ReceivedObject {
    
    /**
     * Received message.
     */
    public Message message;
    
    /**
     * Device for respond.
     */
    public NodeDevice device;
    
    public ReceivedObject(NodeDevice device, Message m){
        this.device = device;
        message = m;
    }
    
}
