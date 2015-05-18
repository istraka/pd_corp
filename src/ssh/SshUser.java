/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssh;

import com.jcraft.jsch.UserInfo;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.System.exit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ivan Straka
 */
public class SshUser implements UserInfo {
    
    private final String passpharse;
    public String password;
    
    private final Console console;
    
    private boolean work;
    
    
    public SshUser(){
        passpharse = "";
        console = System.console();
        password = "";
    }
    
    public SshUser(String passpharse){
        this.passpharse = passpharse;
        console = System.console();
        password = "";
    }

    public void setWork(){
        work = true;
    }
    
    @Override
    public String getPassphrase() {
        return passpharse;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean promptPassword(String string) {
        if(!password.equals("") && work) {
            return true;
        }
        
        System.out.print(string+":");

        // commented lines are supposed to be used in ide that does not support console.
/*                
        BufferedReader buffer=new BufferedReader(new InputStreamReader(System.in));
        String pass = null;
        try {
            pass=buffer.readLine();
            
        } catch (IOException ex) {
            Logger.getLogger(SshUser.class.getName()).log(Level.SEVERE, ex.getMessage(), ex.getLocalizedMessage());
            exit(1);
        }
*/        
        char[] passArr =  console.readPassword();
        String pass = new String(passArr);
        password = pass;
        return !pass.equals("");
        
    }

    @Override
    public boolean promptPassphrase(String string) {
        return passpharse.equals("");
    }

    @Override
    public boolean promptYesNo(String string) {
        try {
            System.out.print(string+" (yes or no):");
            BufferedReader buffer=new BufferedReader(new InputStreamReader(System.in));
            String answer=buffer.readLine();
            while(answer.equals("")){
                System.out.println("Please answer yes or no :");
            }
            return (answer.toLowerCase()).equals("yes");
        } catch (IOException ex) {
            
            Logger.getLogger(SshUser.class.getName()).log(Level.SEVERE, null, ex);
            exit(1);
        }
        return false;
    }

    @Override
    public void showMessage(String string) {
        System.out.print(string);
    }
    
    public void resetPassword(){
        password = "";
    }
    
}
