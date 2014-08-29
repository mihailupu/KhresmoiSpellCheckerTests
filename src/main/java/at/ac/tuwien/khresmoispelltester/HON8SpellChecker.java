/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.khresmoispelltester;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.healthonnet.spellchecker.client.SpellcheckDictionary;
import org.healthonnet.spellchecker.client.SpellcheckRequester;
import org.healthonnet.spellchecker.client.data.SpellcheckResponse;
import org.healthonnet.spellchecker.client.data.Suggestion;

/**
 * Same as HONSpellChecker, but with a frequency filter
 * @author mihailupu
 */
public class HON8SpellChecker implements SpellChecker {

    private static final Logger LOG = Logger.getLogger(HON8SpellChecker.class.getName());

    @Override
    public String spellCheck(String term, String language) throws IOException {
// language, number of suggestions to return, input string
        SpellcheckResponse spellcheckResponse
                = SpellcheckRequester.getSpellcheckResponse(SpellcheckDictionary.English, 25, term);

        List<Suggestion> suggestions = spellcheckResponse.getSpellcheck().getSuggestions();

        if (suggestions.isEmpty()) {
            LOG.log(Level.WARNING, "No suggestions found for " + term);
            return term;
        }
        if (suggestions.get(0).getSuggestedCorrections().get(0).getFreq()/suggestions.get(0).getOrigFreq()>=8){
            return suggestions.get(0).getSuggestedCorrections().get(0).getWord();
        }else{
            return term;
        }
    }

    @Override
    public String getName() {
        return "HON8spellChecker";
    }

}
