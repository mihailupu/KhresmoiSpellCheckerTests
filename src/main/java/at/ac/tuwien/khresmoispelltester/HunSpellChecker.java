/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.khresmoispelltester;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.languagetool.rules.spelling.hunspell.Hunspell;

/**
 *
 * @author mihailupu
 */
public class HunSpellChecker implements SpellChecker {

    public final String baseDictionary ;
    private HunSpellChecker(){
        baseDictionary="/Users/mihailupu/work/_TUW/Khresmoi/spellcorector/en_US/en_US";
    }
    
    /**
     * The base dictionary is neither a file nor a folder. For instance, if you give it "/some/path/en_US"
     * it will expect to find two files "/some/path/en_US.dic" and "/some/path/en_US/en_US.aff"
     * @param baseDictionary 
     */
    public HunSpellChecker(String baseDictionary){
        this.baseDictionary=baseDictionary;
    }
    
    private static final Logger LOG = Logger.getLogger(HunSpellChecker.class.getName());

    @Override
    public String spellCheck(String term, String language) throws IOException {

        Hunspell hunspell = Hunspell.getInstance();
        Hunspell.Dictionary dict = hunspell.getDictionary(baseDictionary);
        String result = term;

        if (dict.misspelled(term)) {
            List<String> suggestions = dict.suggest(term);
            if (!suggestions.isEmpty())
            result = suggestions.get(0);
        }

        return result;
    }

    @Override
    public String getName() {
        return "HunspellChecker";
    }

}
