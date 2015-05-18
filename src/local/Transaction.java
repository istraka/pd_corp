/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

/**
 * Abstract class that represents some action that needs to be executed, process 
 * response, and provides futher actions by update method that can be called in cycle.
 * @author Ivan Straka
 */
abstract public class Transaction {
    
    private static int counter = 1;
    public static final long timeoutMilis = 60000;
    public static final long resendMilis =  10000;
    
    
    /**
     * Create new local uniq message ID.
     * @return id
     */
    public static int getNewTransactionID(){
        counter++;
        if(counter == 0) {
            counter++;
        }
        return counter;
    }
    
    protected int transactionID;
    protected int nodeID;
    
    public Transaction(int nodeID, int cmdID){
        this.nodeID = nodeID;
        transactionID = cmdID;
    }
    
    
    /**
     * Called when command is created
     */
    abstract public void execute();
    
    /**
     * Called after message arrived with this command's id.
     */
    abstract public void putResponse(Object resp);
    /**
     * Called periodicaly.
     */
    abstract public void update();
    /**
     * Called after {@link canFinish()} returns true. 
     */
    abstract public void finish();
    /**
     * Explicitly called after {@link isTimeout()} return true.
     */
    abstract public void cancel();
    abstract public boolean canFinish();
    abstract public boolean isTimeout();
    
}
