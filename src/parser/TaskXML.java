/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

/**
 *
 * @author Ivan Straka
 */
public class TaskXML {
    public final boolean isSingleFileTask;
    public final String cmd;
    public final String returnFolder;
    public final String generalResultFile;
    
    
    /**
     *
     * @param task
     * @param singleFileTask
     * @param returnFolder
     */
    public TaskXML(String task, boolean singleFileTask, String returnFolder){
        this(task, singleFileTask, returnFolder, null);
    }
    /**
     *
     * @param task
     * @param singleFileTask
     * @param returnFolder
     * @param generalResultFile
     */
    public TaskXML(String task, boolean singleFileTask, String returnFolder, String generalResultFile){
        this.cmd = task;
        this.isSingleFileTask = singleFileTask;
        this.returnFolder = returnFolder;
        this.generalResultFile = generalResultFile;
    }
}
