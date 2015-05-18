/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import network.NetAddress;

/**
 * Uploade results to ftp destination.
 * @see FileUpload
 * @author Ivan Straka
 */
public class TransReturnResult extends Transaction {
    private final static Logger logger = Logger.getLogger(TransReturnResult.class.getName());
    
    
    private final List<String> toUpload;
    private final NetAddress ftpPath;
    private volatile ExecutorService pool;
    private volatile boolean finish;
    private final CounteredCompletionService<FileFtpOperationFuture> completionService;
    private final Node node;
    
    public TransReturnResult(List<String> toUpload, NetAddress ftpPath, Node node){
        super(Node.getID(),Transaction.getNewTransactionID());
        this.toUpload = toUpload;
        this.ftpPath = ftpPath;
        pool = Executors.newFixedThreadPool(toUpload.size());
        finish = false;
        this.node = node;
        completionService = new CounteredCompletionService<>(pool);
    }
    
    public TransReturnResult(String toUpload, NetAddress ftpPath, Node node){
        this(Arrays.asList(toUpload), ftpPath, node);
    }

    @Override
    public void execute() {
        for(String file: toUpload){
            completionService.submit(new FileUpload(file, ftpPath));
        }
    }

    @Override
    public void putResponse(Object resp){}

    @Override
    public void update() {
        FileFtpOperationFuture fileInfo;
        Future<FileFtpOperationFuture> future;
        while(true){
            if((future = completionService.poll()) == null) {
                break;
            }
            
            // necessary catch - never happens(checked before)
            try {
                fileInfo = future.get();
                if(fileInfo.isCorrect()){
                    logger.log(Level.INFO, "{0} returned to {1}:{2}", new Object[]{fileInfo.getSrc(), fileInfo.getDest(), ftpPath.hostname});
                    // notify master about result
                    node.notifyMasterResult(
                            Arrays.asList( 
                                NetAddress.getAddress(fileInfo.getSrc(), node.getHostname(), 0), 
                                NetAddress.getAddress(fileInfo.getDest(), ftpPath.hostname, 0)
                            )
                    );
                }
                else{
                    logger.log(Level.WARNING, "Returning {1} to {1}:{2} failed! Code: {3}", new Object[]{fileInfo.getSrc(), fileInfo.getDest(), ftpPath.hostname, fileInfo.getCode()});
                    node.notifyMasterResult(
                            Arrays.asList( 
                                NetAddress.getAddress(fileInfo.getSrc(), node.getHostname(), 0)
                            )
                    );
                }
            } catch (InterruptedException | ExecutionException | IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        
        finish = completionService.isEmpty();        
    }

    @Override
    public void finish() {
        pool.shutdownNow();
        pool = null;
    }

    @Override
    public void cancel(){}

    @Override
    public boolean canFinish() {
        return finish;
    }

    @Override
    public boolean isTimeout() {
        return false;
    }
}
