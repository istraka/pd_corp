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
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class provides support for uploading files over ftp.
 * @see FileFtpOperationFuture
 * @author Ivan Straka
 */
public class FileUpload implements Callable<FileFtpOperationFuture> {
    private final static Logger logger = Logger.getLogger(FileUpload.class.getName());
    
    private final FTPClient client;
    private final String localFilename;
    private final NetAddress ftpDest;
    
    /**
     * Init uploading  file from ftp server.
     */
    public FileUpload(String localFilename, NetAddress ftpDest){
        client = new FTPClient();
        this.localFilename = localFilename;
        this.ftpDest = ftpDest;
        
    }
    
    /**
     * 
     * @return uploaded filename
     */
    private String uploadFileString(boolean includePath){
        if(includePath){
            return localFilename.substring(0,localFilename.lastIndexOf(File.separator)+1)+"_uploaded"+Node.getID()+"_"+localFilename.substring(localFilename.lastIndexOf(File.separator)+1);
        }
        else {    
            return "_uploaded"+Node.getID()+"_"+localFilename.substring(localFilename.lastIndexOf(File.separator)+1);
        }   
    }
    
    /**
     * 
     * @return filename without path
     */
    private String FilenameNoPath(String filename){
        return filename.substring(filename.lastIndexOf(File.separator)+1);
    }
    
    @Override
    public FileFtpOperationFuture call() {
        File localFile = new File(localFilename);
        File renamedLocalFile = new File(uploadFileString(true));
        localFile.renameTo(renamedLocalFile);
        try {
            client.connect(ftpDest.hostname, ftpDest.port);
            client.login("pd_corp", "][[asddddddddddd2j9d8238ud");
            client.setType(FTPClient.TYPE_BINARY);
            client.changeDirectory(ftpDest.dest);
            client.upload(renamedLocalFile);
            client.disconnect(true);
        } catch(FTPException ex){
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            return new FileFtpOperationFuture(localFilename, ftpDest.dest+uploadFileString(false), ex.getCode());
        } catch (IllegalStateException | IOException | FTPIllegalReplyException  | FTPDataTransferException | FTPAbortedException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            return new FileFtpOperationFuture(localFilename, ftpDest.dest+uploadFileString(false), FileFtpOperationFuture.FATAL);
        } finally{
            renamedLocalFile.renameTo(localFile);
        }
        return new FileFtpOperationFuture(localFilename, ftpDest.dest+uploadFileString(false));
    }
    
}
