/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import execution.PipeFeeder;
import execution.SingleFileFeeder;
import execution.TaskManager;
import java.io.File;
import java.io.IOException;
import static java.lang.System.exit;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import network.MsgReady;
import network.MsgSteady;
import network.NetAddress;
import network.NetworkService;
import network.NodeDevice;
import network.ReceivedObject;
import parser.ArgParse;
import parser.ParserException;
import parser.XMLConfigParser;
import ssh.SshExecutorException;

/**
 *
 * @author Ivan Straka
 */
public class SlaveInitializer implements NodeInitializer {
    private final static Logger logger = Logger.getLogger(MasterInitializer.class.getName());
    
    private final ArgParse args;
    private final ShutdownHook hook;
    
    public SlaveInitializer(ArgParse args, ShutdownHook hook){
        this.args = args;
        this.hook = hook;
    }

    @Override
    public Node Prepare() throws NodeInitializerException, IOException, ParserException {
        final NetworkService network;
        final TaskManager taskManager;
        final Node localNode;
        
        network = new NetworkService(args.getNodePort());
        final NodeDevice masterDevice = network.createDevice(args.getMasterAddress().hostname, args.getMasterAddress().port);
                
        taskManager = getTasksManager(args);
        hook.setManager(taskManager);
        
        localNode = initSlave(args, taskManager, network, NetAddress.getAddress(args.getLocalHostname(), args.getFtpPort()));
        long startTime = System.currentTimeMillis();
        try {     
            startingPhase(masterDevice, network, network.getLocalPort(), args.getFtpPort(), localNode.returnResultAddress);
        } catch (InterruptedException ex) {
            throw new NodeInitializerException(ex.getMessage(), ex.initCause(ex));
        }
        
        return localNode;
    }
    
    /**
     * Initilize taskManager for slave node.
     * @param parser
     * @return 
     */
    private TaskManager getTasksManager(ArgParse parser) throws IOException, ParserException{
        if(parser.isSingleFileTask()){
            return new SingleFileFeeder(parser.getTask(), parser.getGeneralResult(), parser.getFilesToProcess());
        }
        else {
            return new PipeFeeder(parser.getTask(), parser.getFilesToProcess()); 
        }
    }
    
    /**
     * Initialize slave node.
     */
    private Node initSlave(ArgParse args, TaskManager tasksManager, NetworkService network, NetAddress ftpAdd) throws UnknownHostException, ParserException {
        Node.setID(args.getId());
        String retFolder = (args.getRemotelyProcessedReturnFolder().endsWith(File.separator)? args.getRemotelyProcessedReturnFolder():args.getRemotelyProcessedReturnFolder() + File.separator);
        return new Node(tasksManager, network, network.createDevice(args.getMasterAddress().hostname, args.getMasterAddress().port), args.isGeneratingResultConfig(), args.isReturnProcessedDownloadedFiles(), NetAddress.getAddress(retFolder, args.getLocalHostname(), args.getFtpPort()), ftpAdd);
    }
    
    private void startingPhase(NodeDevice masterDevice, NetworkService network, int nodePort, int ftpPort, NetAddress returnResultAddress) throws IOException, InterruptedException, ParserException{
        
        long startTime = System.currentTimeMillis();
        while(true){
            masterDevice.send(new MsgReady(args.getId(), nodePort, ftpPort, returnResultAddress));

            Thread.sleep(3000);

            ReceivedObject resp = network.receive();
            if(resp != null && resp.message instanceof MsgSteady){

                logger.info("steady received");
                break;
            }
            if(System.currentTimeMillis() - startTime > Master.masterTimeoutMilis){
                logger.log(Level.SEVERE, "master timout - steady message expected");
                exit(1);
            }
        }
    }
}
