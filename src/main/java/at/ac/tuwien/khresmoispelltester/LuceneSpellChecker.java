/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.khresmoispelltester;

import java.io.FileReader;
import java.io.IOException;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.suggest.FileDictionary;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

/**
 *
 * @author mihailupu
 */
public class LuceneSpellChecker implements at.ac.tuwien.khresmoispelltester.SpellChecker {

    public final String baseDictionary;
    private final SpellChecker sc;
    
    private LuceneSpellChecker() throws IOException {
        this.baseDictionary = "/Users/mihailupu/work/_TUW/Khresmoi/clef/termDictionary.txt";         
        RAMDirectory spellDir = new RAMDirectory();
        sc = new SpellChecker(spellDir);
        FileDictionary fd = new FileDictionary(new FileReader(baseDictionary), "\t");
        IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_4_9, null);
        sc.indexDictionary(fd, conf, false);
    }

    /**
     * The base dictionary is a file with one word per line.
     *
     * @param baseDictionary
     */
    public LuceneSpellChecker(String baseDictionary) throws IOException {
        this.baseDictionary = baseDictionary;
        RAMDirectory spellDir = new RAMDirectory();
        sc = new SpellChecker(spellDir);
        FileDictionary fd = new FileDictionary(new FileReader(baseDictionary), "\t");
        IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_4_9, null);
        sc.indexDictionary(fd, conf, false);
    }

    @Override
    public String spellCheck(String term, String language) throws IOException {
    
        if (sc.exist(term)) {
            return term;
        } else {
            String[] results = sc.suggestSimilar(term, 10);
            if (results.length > 0) {
                return results[0];
            } else {
                return term;
            }
        }
    }

    @Override
    public String getName() {
        return "LuceneSpellChecker";
    }

}
