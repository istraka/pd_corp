/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package execution;

import java.util.logging.Logger;

/**
 * Class define methods for managing tasks.
 * @author Ivan Straka
 */
public abstract class TaskManager {
    private static final Logger logger = Logger.getLogger(TaskManager.class.getName());
    
    public static final double NOTHING_DONE = Double.POSITIVE_INFINITY;
    public static final double UNKNOWN = -1;
    /**
     * Number of done tasks to number of all tasks ratio.
     * Used for validation estimated finish time - time can not be estimated after processing just one file.
     * If actual ratio is higher than this one, finish time can be estimated.
     */
    public static final double TASK_DONE_VALID_RATIO = 0.05;
    
    /**
     * Remove file from queue.
     * @return true if file was in queue, false otherwise
     */
    public abstract boolean removeFile(String file);
    public abstract void addFile(String f);
    public abstract String getFile();
    public abstract void taskDone();
    public abstract String getCommand();
    public abstract void setCommand(String command);
    public abstract double getEstimatedFinishTimeSec();
    public abstract boolean finished();
    public abstract double getTheoreticalEstimatedFinishTimeSec(int fileNumberChange);
    public abstract void start(NodeNotificator node);
    public abstract void shutdownNow();
    public abstract void shutdown() throws InterruptedException;
}
