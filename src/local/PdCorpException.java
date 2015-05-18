/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

/**
 *
 * @author Ivan Straka
 */
public class PdCorpException extends Exception{

    private final String detail;


    public PdCorpException(String detail){
        this.detail = detail;
    }

    @Override
    public String toString(){
        return detail;
    }
    
}
