/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.khresmoispelltester;

import java.io.IOException;

/**
 * A dummy spell checker always returns the original term. Used to see what the results are without spellchecking.
 * @author mihailupu
 */
public class DummySpellChecker implements SpellChecker{

    @Override
    public String spellCheck(String term, String language) throws IOException {
       return term;
    }

    @Override
    public String getName() {
        return "Dummy";
    }
    
}
