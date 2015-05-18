/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import network.NetAddress;
import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class provides support for downloading files over ftp.
 * @see FileFtpOperationFuture
 * @author Ivan Straka
 */
public class FileDownload implements Callable<FileFtpOperationFuture> {
    
    private final static Logger logger = Logger.getLogger(FileDownload.class.getName());
    
    static private String downloadDest;
    
    /**
     * Set downlaod destination.
     */
    static public void setDownloadDest(String path){
        if(!path.endsWith(File.separator)){
            path += File.separatorChar;
        }
        downloadDest = path;
    }
    
    static private int uniqID = 0;
    
    /**
     * Set uniq id that will be part of download files.
     */
    static public void setUniqID(int id){
        uniqID = id;
    }
    
    static private int counter = 0;
    static private Object lock;
    
    private final FTPClient client;
    private String destFile;
    private final List<NetAddress> files;
    private Random rand;
    
    /**
     * Compose destination filename.
     * Original filename will be added beyond "pd_corp_" + uniq id +"_" + no. downloaded file + "_"
     * @return destination filename
     */
    private String getNewDestFilename(String srcFilename){
        counter++;
        return downloadDest+"pd_corp_"+uniqID+"_"+counter+"_"+srcFilename.substring(srcFilename.lastIndexOf("/")+1);
    }
    
    /**
     * Init downloading  file from ftp server
     * @param filename remoee dest
     */
    public FileDownload(String filename, String host, int port){
        this(NetAddress.getAddress(filename, host, port));
    }
    
    /**
     * Init downloading  file from ftp server.
     */
    public FileDownload(NetAddress file){
        client = new FTPClient();
        files = Arrays.asList(file);
        rand = new Random();
        rand.setSeed(System.currentTimeMillis());
    }
    
    /**
     * Init downloading file from ftp server.
     * @param ftpFilenames  list od ftp destinations where the file can be downloaded from.
     */
    public FileDownload(List<String> ftpFilenames){
        client = new FTPClient();
        files = new ArrayList<>(ftpFilenames.size());
        for(String ftpFilename: ftpFilenames){
            files.add(NetAddress.getAddress(ftpFilename));
        }
        rand = new Random();
        rand.setSeed(System.currentTimeMillis());
    }
    
    @Override
    public FileFtpOperationFuture call() {
        // create copy of the array of destinations where the file can be downloaded from
        // if there are more destinations, they will be picked up randomly
        List<NetAddress>filesCopy = files.size() == 1?files:new ArrayList<>(files);
        NetAddress remoteFile;
        int position;
        //infinity loop while there is some destination left
        while(!filesCopy.isEmpty()) {
            // if length of array is 1, there is only one option
            if(files.size() == 1){
                position = 0;
            }
            else{
                position = rand.nextInt(filesCopy.size());
            }
            remoteFile = filesCopy.get(position);
            // download
            try {
                destFile=getNewDestFilename(remoteFile.dest);
                client.connect(remoteFile.hostname, remoteFile.port);
                client.login("pd_corp", "][[asddddddddddd2j9d8238ud");
                client.setType(FTPClient.TYPE_BINARY);
                client.download(remoteFile.dest, new File(destFile));
                //logger.log(Level.INFO, "downloaded {0} from {1}:{2} to {4}", new Object[]{remoteFile.dest, remoteFile.hostname, remoteFile.port, downloadDest, destFile});
                client.disconnect(true);
            
            // ftp exception, try another destination or return future with ftp code
            } catch(FTPException ex){
                if(files.size() != 1 && !filesCopy.isEmpty()){
                    filesCopy.remove(position);
                    continue;
                }
                else
                    return new FileFtpOperationFuture(ex.getCode());
            
            // Some other severe exception, try another dest or return future with fatal err code 
            } catch (IllegalStateException | IOException | FTPIllegalReplyException  | FTPDataTransferException | FTPAbortedException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                if(files.size() != 1 && !filesCopy.isEmpty()){
                    filesCopy.remove(position);
                    continue;
                }
                else
                    return new FileFtpOperationFuture(FileFtpOperationFuture.FATAL);
            }
            return new FileFtpOperationFuture(remoteFile.dest, destFile);
        }
        return new FileFtpOperationFuture(FileFtpOperationFuture.FATAL);
    }
}

