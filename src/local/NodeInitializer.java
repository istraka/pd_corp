/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import java.io.IOException;
import parser.ParserException;
import ssh.SshExecutorException;

/**
 *
 * @author Ivan Straka
 */
public interface NodeInitializer {
    
    public Node Prepare() throws NodeInitializerException, ParserException, SshExecutorException, IOException;
}

class NodeInitializerException extends Exception{

    public NodeInitializerException() {
    }

    public NodeInitializerException(String message) {
        super(message);
    }

    public NodeInitializerException(String message, Throwable cause) {
        super(message, cause);
    }

    public NodeInitializerException(Throwable cause) {
        super(cause);
    }
    
}