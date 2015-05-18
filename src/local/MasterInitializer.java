/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import execution.PipeFeeder;
import execution.SingleFileFeeder;
import execution.TaskManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import network.MsgQuit;
import network.MsgReady;
import network.MsgSteady;
import network.NetAddress;
import network.NetworkService;
import network.ReceivedObject;
import parser.ArgParse;
import parser.FileXML;
import parser.FileXMLAccess;
import parser.NodeXML;
import parser.ParserException;
import parser.TaskXML;
import parser.XMLConfigParser;
import ssh.SshExecutor;
import ssh.SshExecutorException;

/**
 *
 * @author Ivan Straka
 */
public class MasterInitializer implements NodeInitializer{
    private final static Logger logger = Logger.getLogger(MasterInitializer.class.getName());
    
    private final ArgParse args;
    private final XMLConfigParser config;
    private final ShutdownHook hook;
    
    public MasterInitializer(ArgParse args, XMLConfigParser config, ShutdownHook hook){
        this.args = args;
        this.config = config;
        this.hook = hook;
    }
    
    @Override
    public Node Prepare() throws NodeInitializerException, ParserException, IOException, SshExecutorException{
        final List<NodeInfo> nodes;
        final NetworkService network;
        final TaskManager taskManager;
        final Node localNode;

        SshExecutor sshExecutor = new SshExecutor(args.getKnownHosts(), args.getRsa());

        if(args.isReturnProcessedDownloadedFiles() && !checkDefinedReturnFolder(config.slaves, config.master)){
            throw new NodeInitializerException("Some nodes has not defined folder for returning remotely processed files!");
        }
        if((args.isGeneratingResultConfig() || args.isReturnProcessedDownloadedFiles()) &&
                !checkGettingResultPossibility(config.slaves, config.master)){
            throw new NodeInitializerException("Some nodes can not determine result of task!");
        }

        network = new NetworkService(config.master.port);
        nodes = initNodeInformations(args, config, network.getLocalPort(), config.master.ftpPort);

        if(args.isChecksumMultiaccessFiles() && !checkMd5MultiaccessFiles(sshExecutor, nodes)){
            throw new NodeInitializerException("Some multiaccessed files has not the same md5 sum!");
        }

        taskManager = getTasksManager(nodes, config.master.task);
        hook.setManager(taskManager);
            
        try {
            localNode = (Node) initMaster(nodes, taskManager, network, args);
            runSlaves(sshExecutor,nodes, NetAddress.getAddress(config.master.hostname,network.getLocalPort()), args.isReturnProcessedDownloadedFiles());
            if(!startingPhase(nodes, network)){
                quitNodes(nodes);
                throw new NodeInitializerException("Starting phase failed!");
            }
            sshExecutor.disconnect();
            return localNode;
        } catch (UnknownHostException|SshExecutorException ex) {
            quitNodes(nodes);
            throw(ex);
        }
    }
    
    private boolean checkDefinedReturnFolder(List<NodeXML> slaves, NodeXML master){
        boolean correct = true;
        if(master.isComputing && (master.task == null || master.task.returnFolder.equals(""))){
            logger.log(Level.WARNING, "{0} has not defined return folder needed for returning results", master.hostname);
            correct = false;    
        }
        for(NodeXML node: slaves){
            if(node.task == null || node.task.returnFolder.equals("")){
                logger.log(Level.WARNING, "{0} has not defined return folder needed for returning results", node.hostname);
                correct = false;
            }
        }
        return correct;
    }
    
    private boolean checkGettingResultPossibility(List<NodeXML> slaves, NodeXML master){
        boolean correct = true;
        if(master.isComputing && (master.task == null || (master.task.isSingleFileTask && master.task.generalResultFile.equals("")))){
            logger.log(Level.WARNING, "{0} has not defined result needed for returning results", master.hostname);
            correct = false;    
        }
        for(NodeXML node: slaves){
            if(node.task == null || (node.task.isSingleFileTask && node.task.generalResultFile.equals(""))){
                logger.log(Level.WARNING, "{0} has not defined result needed for returning results", node.hostname);
                correct = false;
            }
        }
        return correct;
    }
    
    
    /**
     * Function initialize and return list of nodes.
     * Fullfill nodes with files to process
     */
    private List<NodeInfo> initNodeInformations(ArgParse args, XMLConfigParser parser, int masterPort, int masterFtpPort){
        List<NodeInfo> nodeInfoList = new ArrayList<>(parser.slaves.size());
        List<FileObject> filesToProcess = new ArrayList<>(parser.filesToProcess.size());
        // hostname -> node
        Map<String, NodeInfo> accessMap = new HashMap<>();
        
        initNodeInfoAndHostNodeMap(args, parser, accessMap, nodeInfoList, filesToProcess, masterPort, masterFtpPort);
        fulfillNodesWithFiles(filesToProcess, accessMap, nodeInfoList);
        
        
        return nodeInfoList;
    }
    
    private void initNodeInfoAndHostNodeMap(ArgParse args, XMLConfigParser parser, Map<String, NodeInfo> accessMap, List<NodeInfo> nodesInfoList, List<FileObject> filesToProcess, int masterPort, int masterFtpPort){
        //master
        NodeInfo node = new NodeInfo(0, "", parser.master.hostname, parser.master.jarPath, parser.master.sshPort, parser.master.downloadDest, true, parser.master.isComputing, parser.master.logFile);
        if(parser.master.task != null){
            node.setTask(parser.master.task.cmd, parser.master.task.isSingleFileTask, parser.master.task.returnFolder, args.isGeneratingResultConfig(), parser.master.task.generalResultFile);
            String returnFolder = !parser.master.task.returnFolder.endsWith(File.separator)?parser.master.task.returnFolder + File.separatorChar: parser.master.task.returnFolder;
            node.setReturnFileAddress(NetAddress.getAddress(returnFolder,parser.master.hostname,masterFtpPort));
        }
        node.setNodePort(masterPort);
        node.setFtpPort(masterFtpPort);
        accessMap.put(parser.master.hostname, node);
        nodesInfoList.add(0,node);
        
        //slaves
        int idCounter = 1;
        for(NodeXML xmlNode: parser.slaves){
            node = new NodeInfo(idCounter, xmlNode.user, xmlNode.hostname, xmlNode.jarPath, xmlNode.sshPort, xmlNode.downloadDest, false, true, xmlNode.logFile);
            node.setTask(xmlNode.task.cmd, xmlNode.task.isSingleFileTask, xmlNode.task.returnFolder, args.isGeneratingResultConfig(), xmlNode.task.generalResultFile);
            node.setNodePort(xmlNode.port);
            node.setFtpPort(xmlNode.ftpPort);
            accessMap.put(xmlNode.hostname, node);
            nodesInfoList.add(idCounter, node);
            idCounter++;
        }
        
        for(FileXML fileXML: parser.filesToProcess){
            FileObject file = new FileObject();
            for(FileXMLAccess access: fileXML.access){
                file.addAccess(accessMap.get(access.nodeXML.hostname), access.filename);
            }
            filesToProcess.add(file);
        }
    }
    
    /**
     * Add files to NodeInfo objects.
     * Firstly, files with single access defined will be added to their nodes. 
 If master has only access to some files and does not participate to computing, files will be distributed to another nodes evenly.
 Files that multiple nodes may access will be distributed to nodes by minimum queue length
     * @param xmlFileList file List to add
     * @param accessMap  other nodes mapped by hostname string
     * @param nodes list of all nodes
     */
    private void fulfillNodesWithFiles(List<FileObject> fileList, Map<String, NodeInfo> accessMap, List<NodeInfo> nodes){
        List<FileObject> unresolvedFiles = new LinkedList<>();
        for(FileObject file: fileList){
            if(file.getAccessCount() > 1){
                unresolvedFiles.add(file);
            }
            else{
                NodeInfo node = file.getAllAccesses().get(0).node;
                if(node.isComputing()){
                    node.addFile(file);
                    file.setWorker(node);
                }
                // this happen if master has only acces to file to process and computing is disabled
                // distribute those files to other slaves
                else{
                    logger.log(Level.WARNING, "No computing node has access to {0}:{1}", new Object[]{
                            file.getAllAccesses().get(0).filename,
                            file.getAllAccesses().get(0).node.getHostname()
                        });
                }
            }
        }
        // reslove files with multiple access
        // files will be distributed to nodes evenly.
        for(FileObject file: unresolvedFiles){
            NodeInfo ni = getLeastWorkingNode(file.getNodes());
            ni.addFile(file);
            file.setWorker(ni);
        }
    }
    
    /**
     * Method return NodeInfo object that has the lowest size of queues.
     * Method garantuee that NodeInfo has computing set to true.
     * @param nodes
     * @return 
     */
    private NodeInfo getLeastWorkingNode(List<NodeInfo> nodes){
        if(nodes.isEmpty()){
            return null;
        }
        NodeInfo min = nodes.get(0);
        for (NodeInfo tmp : nodes) {
            if(tmp.isComputing() && tmp.queueLen() < min.queueLen()){
                min = tmp;
            }
        }
        return min;
    }
    
    private boolean checkMd5MultiaccessFiles(SshExecutor sshExecutor, List<NodeInfo> nodes) throws IOException {
        boolean error = false;
        for(NodeInfo node: nodes){
            List<FileObject> files = node.getFilesWithMultipleAccess();
            for(FileObject file: files){
                String md5sum=null;
                for(FileAccess access: file.getAllAccesses()){
                    try {
                        BufferedReader br;
                        String result = access.node.isMaster() ? getMd5LocalFile(access.filename) : getMd5RemoteFile(access, sshExecutor);
                        if(md5sum == null){
                            md5sum = result;
                        }
                        else if(!md5sum.equals(result)){
                            logger.log(Level.SEVERE, "Bad md5 checksum for {0}:{1}@{2}:{3}", 
                                    new Object[]{
                                        "md5sum "+access.filename,
                                        access.node.getUser(),
                                        access.node.getHostname(),
                                        access.node.sshPort});
                            error = true;
                        }
                        
                        
                    } catch (UnknownHostException | NoSuchAlgorithmException | SshExecutorException | FileNotFoundException ex) {
                        logger.log(Level.SEVERE, ex.getMessage());
                        error = true;
                    }
                }
            }
        }
        return !error;
    }
    
    private String getMd5RemoteFile(FileAccess access, SshExecutor sshExecutor) throws UnknownHostException, SshExecutorException, IOException{
        sshExecutor.setCommand(
            "md5sum "+access.filename,
            access.node.getUser(),
            access.node.getHostname(),
            access.node.sshPort);
        BufferedReader br = new BufferedReader(new InputStreamReader(sshExecutor.getInputStream()));
        sshExecutor.execute();
        return br.readLine();
    }
    
    private String getMd5LocalFile(String filename) throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        File file = new File(filename);
        FileInputStream inputStream = new FileInputStream(file);
        MessageDigest digest = MessageDigest.getInstance("md5");

        byte[] bytesBuffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
            digest.update(bytesBuffer, 0, bytesRead);
        }

        byte[] hashedBytes = digest.digest();

        return byteArrayToHexString(hashedBytes);
    }
    private String byteArrayToHexString(byte[] arrayBytes) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < arrayBytes.length; i++) {
            str.append(Integer.toString((arrayBytes[i] & 0xff), 16));
        }
        return str.toString();
    }
    
    
    /**
     * Initialize tasks manager for master node.
     * @return null or tasks manager whether master is participating to computing
     */
    private TaskManager getTasksManager(List<NodeInfo> nodes, TaskXML task) throws IOException {
        NodeInfo master;
        for(NodeInfo node: nodes){
            if(node.isMaster()){
                if(!node.isComputing()){
                    return null;
                }
                else{
                    if(task.isSingleFileTask){
                        return new SingleFileFeeder(task.cmd, task.generalResultFile, node.getFilenamesToProcess());
                    }
                    else { 
                        return new PipeFeeder(task.cmd, node.getFilenamesToProcess());
                    }
                }
            }
        }
        return null;
    }
    
     /**
     * Method finds master node in node list and create new instance according to isComputing attribute in xml config.
      */
    private Master initMaster(List<NodeInfo> nodes, TaskManager tasks, NetworkService net, ArgParse args) throws UnknownHostException{
        Node.setID(0);
        for(NodeInfo node: nodes){
            if(node.isMaster()){
                if(node.isComputing()){
                    return new Master(tasks, net, nodes, args.isGeneratingResultConfig(), args.isReturnProcessedDownloadedFiles(), node.getReturnFileAddress(), node.getFtpAddress());
                }
                else{
                    return new Master(net, nodes, args.isReturnProcessedDownloadedFiles(), node.getReturnFileAddress(), node.getFtpAddress());
                }
            }
        }
        return null;
    }
    
    /**
     * Use JSch to run remote nodes over ssh.
     */
    private void runSlaves(SshExecutor executor, List<NodeInfo> nodes, NetAddress masterDest, boolean returnResults) throws UnknownHostException, SshExecutorException{
        for (NodeInfo node : nodes) {
            if(node.isMaster()) {
                continue;
            }
            executor.setCommand(
                    node.getSshExecArgs(masterDest, returnResults),
                    node.getUser(),
                    node.getHostname(),
                    node.sshPort);
            executor.execute();
         }
    }
    
    /**
     * Send steady response for ready message from nodes.
     * @return true if all nodes have sent ready message
     */
    private static boolean startingPhase(List<NodeInfo> nodes, NetworkService network) throws IOException {
        int nodesLeft = nodes.size()-1;
        long startTime = System.currentTimeMillis();
        while(nodesLeft != 0){
            if(System.currentTimeMillis() - startTime > Main.STARTPHASE_TIMEOUT_MILIS){
                for(NodeInfo node: nodes){
                    if(!node.isReady() && !node.isMaster()){
                        node.quit();
                        logger.log(Level.WARNING, "node does not respond: {0}", node.getHostname());
                    }
                }
                return false;
            }  
            ReceivedObject response = network.receive();
            if(response != null  && response.message instanceof MsgReady && verifyAndInitNodeInfoNodeDevice(response, nodes, network)){
                response.device.send(new MsgSteady());
                nodesLeft--;
            }
        }  
        
        return true;
    }
    
    /**
     * Check if node has same id, hostname as defined in xml config and respond by steady message.
     * Set ftp and node ports to corresponding nodeInfo.ady has been received from the node.
     */
    private static boolean verifyAndInitNodeInfoNodeDevice(ReceivedObject recv, List<NodeInfo> nodes, NetworkService network) throws IOException{
        if(recv.message.getNodeID() > nodes.size() || !(recv.message instanceof MsgReady)){
            return false;    
        }
        MsgReady m = (MsgReady) recv.message;
        
        NodeInfo node = nodes.get(m.getNodeID());
        if(node.isReady()){
            return false;
        }
        logger.log(Level.INFO, "node {0}:{1} is steady, ftp {2}", new Object[]{node.getHostname(), m.nodePort, m.ftpPort});
        node.setNodePort(m.nodePort);
        node.setFtpPort(m.ftpPort);
        node.setReturnFileAddress(m.returnFileAddress);
        node.setDevice(recv.device);
        node.ready();
        return true;
    }
    
    private static void quitNodes(List<NodeInfo> nodes) throws IOException {
        for(NodeInfo node: nodes){
            if(node.getDevice() != null){
                node.getDevice().send(new MsgQuit());
            }
        }
    }
}

