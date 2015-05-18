/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package execution;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listener for subprocess that communicat over std in and std out.
 * Feed subprocess with files from queue {@link PipeFeeder}.
 * <br>
 * Subprocess ask with putting empty on stdout string for another file and read it from stdin. 
 * Subprocess may inform application about results by putting "inputfile \t outputs" without quotation marks. If there are more than one output file, 
 * outputfiles will be separated by space.
 * If exit is printed on stdin, no subprocess may end.
 * @author Ivan Straka
 */
public class Listener implements Runnable {
    private static final Logger logger = Logger.getLogger(Listener.class.getName());
    static final Pattern pattern=Pattern.compile("^[^\\s]+\\t([^\\s]+ )*[^\\s]+$");
    
    private final BufferedReader outPipe;
    private final BufferedWriter inPipe;
    private final PipeFeeder taskManager;
    private final Object lock;
    private volatile boolean closed;
    private NodeNotificator node;
    
    

    Listener(BufferedReader out, BufferedWriter in, PipeFeeder taskManager) {
        outPipe = out;
        inPipe = in;
        this.taskManager = taskManager;
        closed = false;
        lock = new Object();
    }
    
    public void setNodeNotificator(NodeNotificator node){
        this.node = node;
    }
    
    @Override
    public void run() {
        String input;
        String file;
        try {
            // infinite cycle reading output and write to input
            while(true){
                input = outPipe.readLine().trim();
                
                // informing about file and its output after process
                if(!input.equals("")){
                    Matcher m = pattern.matcher(input);
                    if(m.find()){
                        String[] tmp = input.split("\t");
                        String File = tmp[0];
                        String[] outputs = tmp[1].split(" ");
                        node.fileProcessed(File, outputs);
                    }
                    else{
                        node.fileProcessed();
                    }
                    taskManager.taskDone();
                }
                // '\n' received, request for another file
                else{
                    // get another task 
                    try {
                        file = taskManager.obtainTask();
                        synchronized(lock){
                            if (closed){
                                logger.log(Level.SEVERE, "Subprocess's stdin and stdout are closed but shouldnt't be. Something went wrong.");
                                return;
                            }
                            inPipe.write(file +"\n");
                            inPipe.flush();
                        }
                    } catch (InterruptedException ex) {
                        synchronized(lock){
                            if(closed) {
                                return;
                            }
                            inPipe.write("exit\n");
                            inPipe.flush();
                            inPipe.close();
                            outPipe.close();
                            closed = true;
                        }
                        return;
                    }
                }
            }
        } catch (IOException ex){
            logger.log(Level.SEVERE, ex.getMessage());
        }
        
    }
    
    public void exit() throws IOException{
        synchronized(lock){
            if(closed){
                return;
            }
            inPipe.write("exit\n");
            inPipe.flush();
            inPipe.close();
            outPipe.close();
            closed = true;
        }
    }
    
}
