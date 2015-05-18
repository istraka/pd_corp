/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import local.Master;

/**
 * Class provide methods to non blocking receive from given port.
 * Create classes that send byte array to remote node.
 * @author Ivan Straka
 */
public class NetworkService {
    private final static Logger logger = Logger.getLogger(Master.class.getName());
    
    /**
     * Max length of message
     */
    public static final int packetLen = 1024;
    
    private final DatagramChannel channel;
    
    public NetworkService(int port) throws IOException{
        channel = DatagramChannel.open(java.net.StandardProtocolFamily.INET);
        channel.configureBlocking(false);
        channel.bind(new java.net.InetSocketAddress(port));
    }
    
    /**
     * Nonblocking receive.
     * Create {@link NodeDevice} so that receiver may respond.
     * @return {@link ReceivedObject}
     * @throws IOException 
     */
    public ReceivedObject receive() {
        
        try {
            byte[] bytes = new byte[packetLen];
            ByteBuffer dst = ByteBuffer.wrap(bytes);
            InetSocketAddress receive = (InetSocketAddress) channel.receive(dst);
            Message m = Message.createMessageFromPacket(dst.array());
            return (receive == null || m == null)? null: new ReceivedObject(createDevice(receive.getHostName(), receive.getPort()), m);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
    }
    
    public String getLocalAddress(){
        return channel.socket().getLocalAddress().getHostAddress();
    }
    
    /**
     * Create {@link NodeDevice} with propper socket.
     * @param ip remote domain name or ip address
     * @param port remote port
     * @return {@link NodeDevice}
     * @throws UnknownHostException 
     */
    public NodeDevice createDevice(String ip, int port) throws UnknownHostException{
        return new NodeDevice(channel, ip, port);
    }
    
    /**
     * Create {@link NodeDevice} with propper socket.
     * @param a
     * @return {@link NodeDevice}
     * @throws UnknownHostException 
     */
    public NodeDevice createDevice(NetAddress a) throws UnknownHostException{
        return new NodeDevice(channel, a.hostname, a.port);
    }
    
    /**
     * 
     * @return local port
     */
    public int getLocalPort(){
        return channel.socket().getLocalPort();
    }
    
}
