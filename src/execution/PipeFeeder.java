/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package execution;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that provide queue for files to process and run one subprocess implementing protocol defined in {@link Listener}.
 * @author Ivan Straka
 */
public class PipeFeeder extends Feeder {
    private static final Logger logger = Logger.getLogger(PipeFeeder.class.getName());
    
    private final ExecutorService pool;
    private final Listener listener;
    private final Process p;
    
    protected volatile String cmd;
    
    
    public PipeFeeder(String cmd, Collection<String> files) throws IOException{
        super(files);
        pool = Executors.newSingleThreadExecutor();
        this.cmd = cmd;
        allTasksCounter= todo.size();String[] run = {"/bin/sh", "-c", cmd};
        p = Runtime.getRuntime().exec(run);
        listener = new Listener(new BufferedReader(new InputStreamReader(p.getInputStream())), new BufferedWriter(new OutputStreamWriter(p.getOutputStream())), this);
        
    }
    
    public PipeFeeder(String cmd) throws IOException{
        this(cmd, null);
    }
    
    @Override
    public String getCommand(){
        return cmd;
    }
    
    @Override
    public void setCommand(String command){
        cmd = command;
    }
    

    @Override
    public void start(NodeNotificator node) {
        super.start();
        listener.setNodeNotificator(node);
        pool.submit(listener);
    }

    @Override
    public void shutdownNow(){
        try {
            listener.exit();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
        finally{
            pool.shutdownNow();
        }
    }
    
    @Override
    public void shutdown(){
        try{
            p.waitFor();
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
        finally{
            pool.shutdownNow();
        }
    }
    
}
