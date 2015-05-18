/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package execution;

import java.io.InputStream;

/**
 * Used for informing some master that file has been processed.
 * @author Ivan Straka
 */
public interface NodeNotificator {
    
    public void fileProcessed(String cmd, InputStream err, int errCode);
    public void fileProcessed();
    public void fileProcessed(String inputFile, String[] outputFiles);
}
