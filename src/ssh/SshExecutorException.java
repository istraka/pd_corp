/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssh;

/**
 *
 * @author Ivan Straka
 */
public class SshExecutorException extends Exception{


    public SshExecutorException() {
    }

    public SshExecutorException(String message) {
        super(message);
    }

    public SshExecutorException(String message, Throwable cause) {
        super(message, cause);
    }

    public SshExecutorException(Throwable cause) {
        super(cause);
    }

    public SshExecutorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
}
