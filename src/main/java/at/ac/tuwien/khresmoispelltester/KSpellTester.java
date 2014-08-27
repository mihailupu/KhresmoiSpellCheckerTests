/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.khresmoispelltester;

import at.ac.tuwien.khresmoispelltester.util.LevenshteinDistance;
import ie.dcu.computing.dcumimirrunmavenized.ExtractText;
import ie.dcu.computing.dcumimirrunmavenized.HitsObj;
import ie.dcu.computing.dcumimirrunmavenized.runMIMIRretrievalExp;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

/**
 *
 * @author mihailupu
 */
public class KSpellTester {

    private static final Logger LOG = Logger.getLogger(KSpellTester.class.getName());
    private static String mimirURL = "http://usfd1-khresmoi.ms.mff.cuni.cz:8080/mimir/dcu-relevance-oct-13/search/postQuery?queryString=";
    private static String connectionUrl = "/Users/mihailupu/work/_TUW/Khresmoi/spellcorector/mimirdata.db";
    String queriesTableName = null;
    String queryType = "textQuery";
    Properties properties = new Properties();
    CsvReader csvReader;
    CsvWriter csvWriter;
    boolean doRetrieval = true;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("One argument required: the properties file");
            System.exit(1);
        }
        try {
            KSpellTester kst = new KSpellTester();
            kst.mainRun(args[0]);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "catch all cought", e);
        }
    }

    private void mainRun(String propertiesFile) throws Exception {
        properties.load(new FileInputStream(propertiesFile));
        String loglevel = properties.getProperty("loglevel");
        if (loglevel != null) {
            LogManager.getLogManager().getLogger("").setLevel(Level.parse(loglevel));
        } else {
            LogManager.getLogManager().getLogger("").setLevel(Level.SEVERE);
        }

        String queriesFilePath = properties.getProperty("queriesFilePath");
        String spellCheckerName = properties.getProperty("spellCheckerName");
        doRetrieval = Boolean.parseBoolean(properties.getProperty("doRetrieval"));
        if (queriesFilePath == null || spellCheckerName == null) {
            LOG.log(Level.SEVERE, "Missing one of essential properties: queriesFilePath or spellCheckerName");
            throw new RuntimeException("Invalid or incomplete properties file");
        }

        if (properties.getProperty("mimirURL") != null) {
            mimirURL = properties.getProperty("mimirURL");
        }
        if (properties.getProperty("connectionUrl") != null) {
            connectionUrl = properties.getProperty("connectionUrl");
        }

        SpellChecker sc;
        if (spellCheckerName.equalsIgnoreCase("lucene")) {
            String luceneDictionaryFile = properties.getProperty("luceneDictionaryFile");
            if (luceneDictionaryFile == null) {
                LOG.log(Level.SEVERE, "For lucene spell checker a dictionary file is mandatory");
                throw new RuntimeException("Invalid or incomplete properties file");
            }
            sc = new LuceneSpellChecker(luceneDictionaryFile);
        } else if (spellCheckerName.equalsIgnoreCase("hunspell")) {
            String hunspellDictionary = properties.getProperty("hunspellDictionary");
            if (hunspellDictionary == null) {
                LOG.log(Level.SEVERE, "For hunspell spell checker a dictionary is mandatory");
                throw new RuntimeException("Invalid or incomplete properties file");
            }
            sc = new HunSpellChecker(hunspellDictionary);
        } else if (spellCheckerName.equalsIgnoreCase("hon")) {
            sc = new HONSpellChecker();
        } else {
            //default spell checkers is dummy
            sc = new DummySpellChecker();
        }

        queriesTableName = new File(queriesFilePath).getName().replace(".", "_");
        run(queriesFilePath, sc);

    }

    private void run(String queriesFilePath, SpellChecker sc) throws Exception {

        //BufferedReader br = new BufferedReader(new FileReader(queriesFilePath));
        csvReader = new CsvReader(new FileReader(queriesFilePath), ',');
        //BufferedWriter bw = new BufferedWriter(new FileWriter(queriesFilePath + ".output"));
        csvWriter = new CsvWriter(new FileWriter(queriesFilePath + ".output.csv"), ',');

        String line;
        Class.forName("org.sqlite.JDBC");
        Connection con = DriverManager.getConnection("jdbc:sqlite:" + connectionUrl);
        String sql = "CREATE TABLE IF NOT EXISTS IR" + queryType + sc.getName() + queriesTableName + "(queryNum INT, item_id INT, relevanceScore FLOAT, url TEXT)";
        Statement stat = con.createStatement();
        stat.execute(sql);
        runMIMIRretrievalExp rmr = new runMIMIRretrievalExp(queryType, sc.getName(), queriesTableName, 100);
        csvReader.readHeaders();
        int[] sumTermNumbers = new int[]{0,0,0};
        int sumEditDistances=0;
        int counter=0;
        int diffNumTermsCounter=0;
        while (csvReader.readRecord()) {
            String qid = csvReader.get("Query_number");
            String original = csvReader.get("Text_query_in_English_with_correct_spelling");
            String misspelled = csvReader.get("incorrectly_spelled");
            ArrayList<Term> originalTerms = queryTerms(original);
            ArrayList<Term> misspelledTerms = queryTerms(misspelled);
            ArrayList<Term> correctedTerms = new ArrayList<>();

            for (Term mTerm : misspelledTerms) {
                if (mTerm.isSpecial || mTerm.hasNonAlpha) {
                    //sb.append(mTerm.text).append(" ");
                    correctedTerms.add(mTerm);
                } else {

                    Term correctedTerm = new Term(mTerm);
                    correctedTerm.text = sc.spellCheck(mTerm.text, "en");
                    correctedTerms.add(correctedTerm);

                }
            }
            LOG.log(Level.INFO, "original:" + termsToQuery(originalTerms));
            LOG.log(Level.INFO, "misspelled:" + termsToQuery(misspelledTerms));
            LOG.log(Level.INFO, "corrected:" + termsToQuery(correctedTerms));
            int editDistance = LevenshteinDistance.computeLevenshteinDistance(original, termsToString(correctedTerms));
            int[] termNumbers = termsComparison(originalTerms, misspelledTerms, correctedTerms);
            
            csvWriter.writeRecord(new String[]{original, misspelled,
                termsToString(correctedTerms), Integer.toString(editDistance),
                Integer.toString(termNumbers[0]), Integer.toString(termNumbers[1]),
                Integer.toString(termNumbers[2])});
            if (termNumbers[0]>=0){
                counter++;
                sumTermNumbers[0]+=termNumbers[0];
                sumTermNumbers[1]+=termNumbers[1];
                sumTermNumbers[2]+=termNumbers[2];
            }else{
                diffNumTermsCounter++;
            }
            sumEditDistances+=editDistance;

            LOG.log(Level.INFO, "About to runMIMIRretrieval.......");

            String queryterms = termsToQuery(correctedTerms);
            /* mihai,2014-08-13: i'm removing some special characters which make MIMIR cry */
            queryterms = queryterms.replace("-", " "); //e.g. diabetes-aware changes to "diabetes aware" as a phrase, meaning the same thing
            queryterms = queryterms.replace("+", " "); //e.g. + appears once in the queries, perhaps the user meant AND...

            queryterms = java.net.URLEncoder.encode(queryterms, "ISO-8859-1");

            if (doRetrieval) {
                String queryURL0 = mimirURL + queryterms;
                LOG.log(Level.INFO, "queryURL = {0}", queryURL0);
                ExtractText et0 = new ExtractText();
                String text0 = et0.extractText(queryURL0, false);
                LOG.log(Level.INFO, "text0 is:: {0}", text0);

                String queryID = text0.replace("SUCCESS ", "");
                queryID = queryID.trim();

                String SQL1 = "DELETE FROM IR" + queryType + sc.getName() + queriesTableName + " where queryNum=?;";
                PreparedStatement pstmt1 = con.prepareStatement(SQL1);
                pstmt1.setInt(1, Integer.parseInt(qid));
                pstmt1.executeUpdate();
                LOG.log(Level.INFO, "Deleted all previous entries for this query number");

                if (!queryID.equals("")) {
                    //get results from MIMIR:
                    ArrayList<HitsObj> results;
                    try {
                        results = rmr.getResultsFromMIMIR(queryID, "");//the second parameter is actually not used in this method
                    } catch (java.lang.NumberFormatException nfe) {
                        LOG.log(Level.WARNING, "problems with the query", nfe);
                        results = new ArrayList<>();
                    }

                    for (HitsObj result : results) {

                        //sqllite server:
                        String SQL2 = "INSERT INTO IR" + queryType + sc.getName() + queriesTableName + "(queryNum, item_id, relevanceScore, url) VALUES(?, ?, ?, ?);";

                        PreparedStatement pstmt2 = con.prepareStatement(SQL2);
                        pstmt2.setString(1, qid);
                        pstmt2.setString(2, result.getId());
                        pstmt2.setDouble(3, result.getScore());
                        pstmt2.setString(4, result.getTitle());  //this is the URL

                        pstmt2.executeUpdate();
                    }

                } else {
                    LOG.log(Level.INFO, "Skipping -- no results for this query");
                }
            }
        }
        csvWriter.writeRecord(new String[]{"average", queriesFilePath,
                "average", Double.toString((double)sumEditDistances/(double)(counter+diffNumTermsCounter)),
                Double.toString(sumTermNumbers[0]/(double)counter), Double.toString(sumTermNumbers[1]/(double)counter),
                Double.toString(sumTermNumbers[2]/(double)counter)});
        csvWriter.close();
        csvReader.close();
    }

    /**
     * Compares the correct, misspelled and corrected terms sets. It will return
     * a string with three comma separated integers, representing, in order:
     * <ol>
     * <li>corrected terms: terms which were misspelled and were corrected to
     * their original form</li>
     * <li>non corrected terms: terms which were misspelled but even after
     * correction are still different from the original</li>
     * <li>miscorrected terms: terms which were not misspelled, but were
     * corrected to something else</li>
     * </ol>
     *
     * @param correct The set of terms in the correct query
     * @param misspelled The set of terms in the misspelled query
     * @param corrected The set of terms generated by the spellchecker;
     * @return A csv string with three integers representing
     * correctedTerms,notCorrectedTerms,misCorrectedTerms;
     */
    public int[] termsComparison(ArrayList<Term> correct, ArrayList<Term> misspelled, ArrayList<Term> corrected) {
        // if the misspelled is a different size compared to the original, i can no longer count terms
        if (correct.size() != misspelled.size()) {
            return new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        }
        int correctedTerms = 0;
        int miscorrectedTerms = 0;
        int notCorrectedTerms = 0;
        //otherwise, i can
        for (int i = 0; i < correct.size(); i++) {
            if (correct.get(i).text.equalsIgnoreCase(misspelled.get(i).text)) {
                if (!correct.get(i).text.equalsIgnoreCase(corrected.get(i).text)) {
                    miscorrectedTerms++;
                }
            }
            if (!correct.get(i).text.equalsIgnoreCase(misspelled.get(i).text)) {
                if (correct.get(i).text.equalsIgnoreCase(corrected.get(i).text)) {
                    correctedTerms++;
                } else {
                    notCorrectedTerms++;
                }
            }
        }
        return new int[]{correctedTerms, notCorrectedTerms, miscorrectedTerms};
    }

    public String termsToString(ArrayList<Term> terms) {
        StringBuilder sb = new StringBuilder();
        for (Term term : terms) {
            if (term.hasBeginQuote) {
                sb.append("\"");
            }
            sb.append(term.text);
            if (term.hasEndQuote) {
                sb.append("\"");
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    public String termsToQuery(ArrayList<Term> terms) {
        StringBuilder sb = new StringBuilder();
        boolean previousWasSpecial = false;
        boolean inQuotes = false;
        for (int i = 0; i < terms.size(); i++) {
            Term term = terms.get(i);
            if (term.isSpecial) {
                previousWasSpecial = true;
                sb.append(term.text).append(" ");
            } else {
                if (!previousWasSpecial && i > 0 && !inQuotes) {
                    sb.append(" OR ");
                }
                if (term.hasBeginQuote) {
                    sb.append("\"");
                    inQuotes = true;
                }
                sb.append(term.text);

                if (term.hasEndQuote) {
                    sb.append("\"");
                    inQuotes = false;
                }
                sb.append(" ");
                previousWasSpecial = false;
            }
        }
        return sb.toString().trim();
    }

    /**
     * Splits a query into terms, removing special character
     *
     * @param query
     * @return
     */
    private ArrayList<Term> queryTerms(String query) {
        ArrayList<Term> result = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(query, " ");
        while (st.hasMoreTokens()) {
            String text = st.nextToken();
            Term t = new Term();

            if (text.equals("AND") || text.equals("OR") || text.equals("+")) {
                t.isSpecial = true;
            }
            if (text.startsWith("\"")) {
                t.hasBeginQuote = true;
                text = text.substring(1);
            }
            if (text.endsWith("\"")) {
                t.hasEndQuote = true;
                text = text.substring(0, text.length() - 1);
            }

            Pattern p = Pattern.compile(".*\\W+.*");
            Matcher m = p.matcher(text);
            if (m.find()) {
                t.hasNonAlpha = true;
            }
            t.text = text;
            result.add(t);
        }
        return result;
    }

    private class Term {

        String text;
        boolean isSpecial = false;
        boolean hasBeginQuote = false;
        boolean hasEndQuote = false;
        boolean hasNonAlpha = false;

        @Override
        public String toString() {
            return text;
        }

        public Term() {

        }

        public Term(Term e) {
            this.text = e.text;
            this.isSpecial = e.isSpecial;
            this.hasBeginQuote = e.hasBeginQuote;
            this.hasEndQuote = e.hasEndQuote;
            this.hasNonAlpha = e.hasNonAlpha;
        }
    }
}
