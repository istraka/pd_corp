/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package execution;

import java.io.File;
import java.io.IOException;
import static java.lang.Math.ceil;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Feeder is used for parallel executing given command.
 * Numbers of threads depends on architecture. If architecture provides 12 threads
 * 12 threads processing files will be running.
 * Feeder provides methods of obtaining file and injecting it into command. 
 * Injecting means replacing @file with given filename containing its path and @filename 
 * with filename without path.
 * @author Ivan Straka
 */
public class SingleFileFeeder extends Feeder {
    private static final Logger logger = Logger.getLogger(SingleFileFeeder.class.getName());
    
    public final ExecutorService pool;
    protected volatile String cmd;
    private final String generalResult;
    private static final int threads = (int) Runtime.getRuntime().availableProcessors();
    
    /**
     * 
     * @param generalResult - string, that contains @file and @filename to get result filename
     */
    public SingleFileFeeder(String cmd, String generalResult, Collection<String> files) throws IOException{
        super(files);
        pool = Executors.newFixedThreadPool(threads);
        this.cmd = cmd;
        this.generalResult = generalResult;
    }
    
    public SingleFileFeeder(String cmd, String generalResult) throws IOException{
        this(cmd, generalResult, null);
    }
    
    public SingleFileFeeder(String cmd) throws IOException{
        this(cmd, null, null);
    }
    
    public SingleFileFeeder(String cmd, Collection<String> files) throws IOException{
        this(cmd, null, files);
    }
    
    protected String getResultFilename(String input){
        return generalResult == null ? 
                null : 
                generalResult.replace(
                        "@filename", 
                        input
                            .substring(input.lastIndexOf(File.separator)+1))
                            .replace("@file", input);
    }
    
    
    
    
    @Override
    public String getCommand(){
        return cmd;
    }
    
    @Override
    public void setCommand(String command){
        cmd = command;
    }
    
    /**
     * Get estimated finish time.
     * @return 0 if queue is empty, infinity if no task has been done or estimated time.
     */
    
    
    @Override
    public void shutdownNow(){
        shutdown();
    }
    @Override
    public void shutdown(){
        pool.shutdownNow();
    }
    
    @Override
    public void start(NodeNotificator node){
        super.start();
        for(int i=0; i<threads; i++){
            pool.submit(new SingleFileExecutor(this, node));
        }    
    }
    
    public String getExecCmd(String filename){
        return cmd.replace("@filename", filename.substring(filename.lastIndexOf('/')+1)).replace("@file", filename);
    }
}
