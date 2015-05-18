/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package execution;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class provide partial implementation for managing collection of tasks.
 * @author Ivan Straka
 */
public abstract class Feeder extends TaskManager {
    private static final Logger logger = Logger.getLogger(Feeder.class.getName());
    
    protected final LinkedBlockingQueue<String> todo;
    protected final Object lock;
    protected volatile int tasksDoneCounter;
    protected volatile long startTime;
    protected volatile int allTasksCounter;
    
    public Feeder(Collection<String> files){
        todo = new LinkedBlockingQueue<>();
        if(files != null) {
            todo.addAll(files);
        }
        lock = new Object();
        allTasksCounter= todo.size();
    }
    
    @Override
    public boolean removeFile(String file){
        synchronized(lock){
            if(todo.remove(file)){
                allTasksCounter--;
                return true;
            }
            return false;
        }
    }
    
    /**
     * Put task into queue.
     * @param f file
     */
    @Override
    public void addFile(String f){
        synchronized(lock){
            allTasksCounter++;
            todo.add(f);
        }  
    }
    
    /**
     * Return the head of queue if not empty or null.
     * @return Task
     */
    @Override
    public String getFile(){
        synchronized(lock){
            String result = todo.poll();
            if(result != null){
                allTasksCounter--;
            }
            return result;
        }
    }
    
    @Override
    public void taskDone(){
        synchronized(lock){
            tasksDoneCounter++;
        }
    }
    
    @Override
    public double getEstimatedFinishTimeSec(){
        synchronized(lock){
            double result = (tasksDoneCounter / (double) allTasksCounter < TaskManager.TASK_DONE_VALID_RATIO)? TaskManager.NOTHING_DONE : getTheoreticalEstimatedFinishTimeSec(0);
            logger.log(Level.INFO, "Estimated finish time: {0}, done: {1}, all: {2}, done/all ratio to cumpute time: {3}", new Object[]{result, tasksDoneCounter, allTasksCounter, TASK_DONE_VALID_RATIO});
            return result;
        }
    }
    
    @Override
    public boolean finished(){
        synchronized(lock){
            return allTasksCounter - tasksDoneCounter <= 0;
        }
        
    }
    
    /**
     * Get theoretical estimated finish time after changing numbers of files in queue.
     */
    @Override
    public double getTheoreticalEstimatedFinishTimeSec(int fileNumberChange){
        synchronized(lock){
            int remain = allTasksCounter + fileNumberChange - tasksDoneCounter;
            if(remain <= 0){
                return 0;
            }
            else if(tasksDoneCounter == 0){
                return TaskManager.NOTHING_DONE;
            }
            else{
                return (remain * ((System.currentTimeMillis() - startTime) / (double)tasksDoneCounter))/1000;
            }
        }
    }
    
    protected void start(){
        startTime = System.currentTimeMillis();
    }
    
    
    public String obtainTask() throws InterruptedException{
        return todo.take();
    }
}
