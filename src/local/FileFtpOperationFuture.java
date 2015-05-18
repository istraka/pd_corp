/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

/**
 * Result of uploading or downloading file.
 * @author Ivan Straka
 */

public class FileFtpOperationFuture{
    /**
     * Fatal err code.
     */
    static final int FATAL = -1;
    /**
     * Transferr error code.
     */
    static final int TRANSFERERR = 550;
    
    private int code; 
    private final String destFile;
    private final String srcFile;
    
    public FileFtpOperationFuture(int code){
        this("","");
        this.code = code;
    }
    public FileFtpOperationFuture(String src, String dest){
        this(src, dest, 0);
    }
    public FileFtpOperationFuture(String src, String dest, int code){
        destFile = dest;
        srcFile = src;
        this.code = code;
    }
    
    /**
     * 
     * @return true, if operation was successful
     */
    public boolean isCorrect(){
        return code == 0;
    }
    
    /**
     * 
     * @return true, if fatal transmition error has occured.
     */
    public boolean isTransferError(){
        return !(code == 0 || code == TRANSFERERR);
    }
    
    /**
     * 
     * @return destination filename
     */
    public String getDest(){
        return destFile;
    }
    
    /**
     * 
     * @return source filename
     */
    public String getSrc(){
        return srcFile;
    }
    
    /**
     * 
     * @return response code
     */
    public int getCode(){
        return code;
    }
}
