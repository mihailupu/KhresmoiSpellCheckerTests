/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.khresmoispelltester;

import java.io.IOException;

/**
 *
 * @author mihailupu
 */
public interface SpellChecker {
    
    public String spellCheck(String term,String language) throws IOException ;      
    public String getName();
}
