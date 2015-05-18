/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import network.Message;

/**
 * Class provides methods for manipulating with transactions.
 * Thread safe.
 * @author Ivan Straka
 */
public class TransactionService {
    private final static Logger logger = Logger.getLogger(Node.class.getName());
    
    protected final List<Transaction> transactions;
    protected final Map<Integer, Map<Integer,Transaction>> commandsMap;
    private final Object lock;
    
    
    public TransactionService(){
        transactions = new LinkedList<>();
        commandsMap = new HashMap<>();
        commandsMap.put(0, new HashMap<Integer, Transaction>());
        commandsMap.put(Node.getID(), new HashMap<Integer, Transaction>());
        lock = new Object();
    }
    
    /**
     * Delete all transaction of cls type and nodeID.
     */
    public void deleteNodeTransactionsOfType(int nodeID, Class cls){
        deleteNodeTransactionsOfType(nodeID, cls.getName());
    }
    
    /**
     * Delete all transaction of cls type and nodeID.
     */
    public void deleteNodeTransactionsOfType(int nodeID, String cls){
        synchronized(lock){
            for(int i=0; i<transactions.size(); i++){
                Transaction tr = transactions.get(i);
                if(tr.nodeID == nodeID && tr.getClass().getName().equals(cls)){
                    tr.cancel();
                    logger.log(Level.FINE, "{0} {1} {2} transaction deleted", new Object[]{tr.getClass().getName(), tr.nodeID, tr.transactionID});
                    commandsMap.get(tr.nodeID).remove(tr.transactionID);
                    transactions.remove(i);
                    i--;
                }
            }
            
        }
    } 
    
    /**
     * 
     * @return true if no transacions left
     */
    public boolean isEmpty(){
        synchronized(lock){
            return transactions.isEmpty();
        }
    }
    
    /**
     * Update all active transactions.
     */
    public void update() {
        // in some command's finish methods may be addTransaction called
        synchronized(lock){
            for(int i=0; i<transactions.size(); i++){
                Transaction tr = transactions.get(i);
                tr.update();
                if(tr.canFinish()){
                    logger.log(Level.FINE, "{0} {1} {2} transaction finished", new Object[]{tr.getClass().getName(), tr.nodeID, tr.transactionID});
                    tr.finish();
                    commandsMap.get(tr.nodeID).remove(tr.transactionID);
                    transactions.remove(i);
                    i--;
                }
                else if(tr.isTimeout()){
                    logger.log(Level.FINE, "{0} {1} {2} transaction timeout",new Object[]{tr.getClass().getName(), tr.nodeID, tr.transactionID});
                    tr.cancel();
                    commandsMap.get(tr.nodeID).remove(tr.transactionID);
                    transactions.remove(i);
                    i--;
                }
            }
        }
    }
    
    /**
     * Add transaction.
     */
    public void addTransaction(Transaction tr){
        tr.execute();
        if(tr.canFinish()) {
            tr.finish();
        }
        else{
            synchronized(lock){
                logger.log(Level.FINE, "{0} {1} {2} transaction add", new Object[]{tr.getClass().getName(), tr.nodeID, tr.transactionID});
                transactions.add(tr);
                if(!commandsMap.containsKey(tr.nodeID)) {
                    commandsMap.put(tr.nodeID, new HashMap<Integer, Transaction>());
                }
                commandsMap.get(tr.nodeID).put(tr.transactionID, tr);
            }
        }
    }
    
    /**
     * Put {@link Message} response.
     */
    public boolean put(Message m) {
        synchronized(lock){
            if (!commandsMap.containsKey(m.getNodeID()) || 
                !commandsMap.get(m.getNodeID()).containsKey(m.getTransactionID())){
                return false;
            }
            commandsMap.get(m.getNodeID()).get(m.getTransactionID()).putResponse(m);
            return true;
        }
    }
    
    
}
