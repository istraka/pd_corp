/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

/**
 * Class encapsulate network address.
 * <br>String representation is destination:hostname:port
 * <br>if destination is folder, make sure that does or does not end with {@link java.io.File#pathSeparator}
 * according to further usage.
 * @author IvanStraka
 */
public class NetAddress {

    public static NetAddress getAddress(String s) {
        return (!s.equals("")) ? new NetAddress(s) : null;
    }

    public static NetAddress getAddress(String host, int port) {
        return NetAddress.getAddress("", host, port);
    }
    public static NetAddress getAddress(String dest, String host, int port) {
        return new NetAddress(dest, host, port);
    }
    
    public final int port;
    public final String hostname;
    public final String dest;
    
    private NetAddress(String str){
        port = Integer.parseInt(str.substring(str.lastIndexOf(":")+1));
        str = str.substring(0, str.lastIndexOf(":"));
        hostname = str.substring(str.lastIndexOf(":")+1);
        dest = str.substring(0, str.lastIndexOf(":"));
    }
    
    private NetAddress(String filename, String host, int port){
        this.port = port;
        hostname = host;
        this.dest = filename;
    }
    
    private NetAddress (String host, int port){
        this("", host, port);
    }
    
    @Override
    public String toString(){
        return dest+":"+hostname+":"+port;
    }
    
    public byte[] getBytes(){
        return toString().getBytes();
    }
    
}
