/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * Device for sending datagrams to another node.
 * Instance is made by Tsrasceiver
 * @author Ivan Straka
 */
public class NodeDevice {
    
    private final int port;
    private final String ip;
    private final InetSocketAddress add;
    private final DatagramChannel channel;
    
    protected NodeDevice(DatagramChannel channel, String ip, int port) throws UnknownHostException{
        this.port = port;
        this.ip = ip;
        add = new InetSocketAddress(ip, port);
        this.channel = channel;
    }
    
    /**
     * Sends message.
     * @param m message to send
     * @throws IOException 
     */
    public void send(Message m) throws IOException{
        channel.send(ByteBuffer.wrap(m.getBytes()), add);
    }
    
    /**
     * 
     * @return remote node's port
     */
    public int getRemotePort(){
        return port;
    }
    
    /**
     * 
     * @return local port
     */
    public int getLocalPort(){
        return channel.socket().getLocalPort();
    }
    
    /**
     * 
     * @return remote node's adress
     */
    public InetAddress getAddress(){
        return add.getAddress();
    }
    
    @Override
    public boolean equals(Object o){
        if (o instanceof NodeDevice){
            NodeDevice tmp = (NodeDevice) o;
            return(port==tmp.getRemotePort() && ip.equals(tmp.getAddress()) && getLocalPort() == tmp.getLocalPort());
        }
        return false;
    }
    
    @Override
    public int hashCode(){
        return port;
    }
}
