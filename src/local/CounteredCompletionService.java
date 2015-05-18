/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Class extends {@link ExecutorCompletionService} by counting threads.
 * @author IvanStraka
 * @param <V> type
 */
public class CounteredCompletionService <V> extends ExecutorCompletionService<V> {
    private static final Logger logger = Logger.getLogger(CounteredCompletionService.class.getName());
    
    private final AtomicLong threadCounter;
    
    public CounteredCompletionService(Executor e){
        super(e);
        threadCounter = new AtomicLong();
    }
    
    public CounteredCompletionService(Executor e, BlockingQueue<Future<V>> q){
        super(e,q);
        threadCounter = new AtomicLong();
    }
    
    @Override
    public Future<V> poll(){
        Future future = super.poll();
        if(future != null){
            threadCounter.decrementAndGet();
        }
        return future;
    }
    
    @Override
    public Future<V> poll (long timeout, TimeUnit unit) throws InterruptedException{
        Future future = super.poll(timeout, unit);
        if(future != null){
            threadCounter.decrementAndGet();
        }
        return future;
    }
    
    @Override
    public Future<V> submit(Callable task){
        Future future = super.submit(task);
        threadCounter.incrementAndGet();
        return future;
    }
    
    @Override
    public Future<V> submit(Runnable task, V result){
        Future future = super.submit(task, result);
        threadCounter.incrementAndGet();
        return future;
    }
    
    @Override
    public Future<V> take() throws InterruptedException{
        Future future = super.take();
        threadCounter.decrementAndGet();
        return future;
    }
    
    /**
     * 
     * @return true if all threads have finished.
     */
    public boolean isEmpty(){
        return threadCounter.compareAndSet(0, 0);
    }
}
