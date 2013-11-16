package com.fortytwo;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.WhitespaceTokenizer;
import edu.stanford.nlp.trees.Tree;

import java.io.*;
import java.net.URI;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: Radzinzki
 * Date: 10/11/13
 * Time: 4:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class Initializer {
    public static final String DB_NAME = "database.db";
    public static final String POSTGRES_URL = "jdbc:postgresql://ec2-54-204-37-113.compute-1.amazonaws.com:5432/da51bj93nvtud3?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";
    public static final String POSTGRES_USER = "oazxckmebglypc";
    public static final String POSTGRES_PASSWORD = "GO9ixYeLFecx74dASeey5u_kgG";

    public static final String PLAINTEXT_SET_NAME = "nlp_merged.clean.normal";
    public static final String TAGGED_SET_NAME = "nlp_merged.clean.normal.pos";

//    public static final String TAGGED_SET_NAME = "SampleSet1_POS.txt";
//    public static final String PLAINTEXT_SET_NAME = "plaintextSet1.txt";
    public static final String SERIALIZED_MODEL = "SerializedModel10";
    public static final String PLAINTEXT_TEMP = "plaintext.temp";
    public static final String TAGGED_TEMP = "tagged.temp";

    public static void main (String []args)
    {
        String plaintext_file = "data/" + PLAINTEXT_SET_NAME;
        String tagged_file = "data/" + TAGGED_SET_NAME;
        if (args.length > 0)
        {
            plaintext_file = args[0];
            tagged_file = args[1];
        }
        createDatabase("data/" + DB_NAME);
        readAndStoreTweets("data/" + DB_NAME, plaintext_file, tagged_file);
        printData("data/" + DB_NAME);
    }

    private static void readAndStoreTweets(String databaseFile, String plaintextFile, String taggedFile)
    {
        try {
            BufferedReader br1 = new BufferedReader(new FileReader(plaintextFile));
            BufferedReader br2 = new BufferedReader(new FileReader(taggedFile));
            String plain_line  = null;
            String tagged_line = null;

            List <String> plaintextTweets = new ArrayList<String>();
            List <String> taggedTweets = new ArrayList<String>();
            int count = 0;
            while ((plain_line = br1.readLine()) != null)
            {
                tagged_line = br2.readLine();
                count ++;
                plaintextTweets.add(plain_line);
                taggedTweets.add(tagged_line);
                if (count % 1000 == 0)
                {
                    cacheSentences(plaintextTweets, taggedTweets);
                    List<Tree> trees = parseFile("models/"+ SERIALIZED_MODEL, "temp/" + TAGGED_TEMP);
                    store(databaseFile, plaintextTweets, trees);
                    plaintextTweets = new ArrayList<String>();
                    taggedTweets = new ArrayList<String>();
                    System.out.println("Progression: " + (count*100/563010) + "%");
                }
            }
            cacheSentences(plaintextTweets, taggedTweets);
            List<Tree> trees = parseFile("models/"+ SERIALIZED_MODEL, "temp/" + TAGGED_TEMP);
            store(databaseFile, plaintextTweets, trees);
            System.out.println("Progression: " + (count * 100 / 563010) + "%");
        } catch (Exception e) {

            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static void cacheSentences (List<String> plaintextSentences, List<String> taggedSentences) throws IOException {
        BufferedWriter bw1 = new BufferedWriter (new FileWriter("temp/"+ PLAINTEXT_TEMP));
        for (int i = 0; i < plaintextSentences.size(); i++)
        {
            bw1.write(plaintextSentences.get(i) + "\n");
        }
        bw1.close();
        BufferedWriter bw2 = new BufferedWriter (new FileWriter("temp/"+ TAGGED_TEMP));
        for (int i = 0; i < taggedSentences.size(); i++)
        {
            bw2.write(taggedSentences.get(i) + "\n");
        }
        bw2.close();
    }

    private static void printData(String filename)
    {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + filename);
            c.setAutoCommit(false);

//            Class.forName("org.postgresql.Driver");
//            c = DriverManager.getConnection(POSTGRES_URL, POSTGRES_USER, POSTGRES_PASSWORD);
//            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery( "SELECT * FROM TWEETS;" );
            while ( rs.next() ) {
                String text = rs.getString("text");
                Tree parseTree = (Tree) Serializer.deserialize(rs.getBytes("parsetree"));
                System.out.println(text);
                parseTree.pennPrint();
                System.out.println();
            }
            c.commit();
            rs.close();
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Operation done successfully");
    }

    public static void createDatabase(String filename)
    {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + filename);
//            Class.forName("org.postgresql.Driver");
//            c = DriverManager.getConnection(POSTGRES_URL, POSTGRES_USER, POSTGRES_PASSWORD);

            stmt = c.createStatement();

//            String dropSql = "DROP TABLE tweets";
//            stmt.execute(dropSql);

            String sql = "CREATE TABLE TWEETS " +
                    "(TEXT           TEXT    NOT NULL, " +
                    " PARSETREE      BLOB     NOT NULL)";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();

        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage());
            System.out.println("Try deleting data/database.db and try again.");
            System.exit(0);
        }
        System.out.println("Database created successfully");
    }
//    private static void insertTweet (String databaseFile, List<String> plaintextTweets, List<String> taggedTweets)
//    {
//        // parse it
//        Tree parsedSentence = parseSentence("models/" + SERIALIZED_MODEL, taggedTweet);
//
//        // store it
//        try {
//            store(databaseFile, plaintextTweet, Serializer.serialize(parsedSentence));
//        } catch (IOException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }
//    }
    private static void store (String filename, List<String> plaintextSentences, List<Tree> trees)
    {
        Connection c = null;
        Statement stmt = null;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + filename);
//            Class.forName("org.postgresql.Driver");
//            c = DriverManager.getConnection(POSTGRES_URL, POSTGRES_USER, POSTGRES_PASSWORD);
            c.setAutoCommit(false);

            //stmt = c.createStatement();
//            String sql = "INSERT INTO TWEETS (TEXT,PARSETREE) " +
//                    "VALUES (" + sentence + "," + parseTree + " );";
//            stmt.executeUpdate(sql);

            PreparedStatement ps = c.prepareStatement("INSERT INTO TWEETS (TEXT,PARSETREE) " +
                    "VALUES (?,? );");

            for (int i = 0; i < plaintextSentences.size(); i++)
            {
                String sentence = plaintextSentences.get(i);
                ps.setString(1, sentence);
                ps.setBytes(2, Serializer.serialize(trees.get(i)));
                ps.executeUpdate();

            }

            ps.close();
            c.commit();
            c.close();

        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.out.println("Try deleting data/database.db and try again.");
            System.exit(0);
        }
    }


    public static Tree parseSentence (String modelFile, String sentence)
    {
        LexicalizedParser lp = LexicalizedParser.loadModel(modelFile);
        DocumentPreprocessor dp = new DocumentPreprocessor(new StringReader(sentence));
//        DocumentPreprocessor dp = new DocumentPreprocessor("data/Sample1Tweet.txt");
        TokenizerFactory<CoreLabel> tf = WhitespaceTokenizer.newCoreLabelTokenizerFactory("");

        dp.setSentenceDelimiter("\n");
        dp.setTagDelimiter("_");
        dp.setTokenizerFactory(tf);


        Tree parseTree = lp.parse(sentence);
        return parseTree;
    }

    public static List<Tree> parseFile (String modelFile, String filename)
    {
        LexicalizedParser lp = LexicalizedParser.loadModel(modelFile);
//        DocumentPreprocessor dp = new DocumentPreprocessor(new StringReader(sentence));
        DocumentPreprocessor dp = new DocumentPreprocessor(filename);
        TokenizerFactory<CoreLabel> tf = WhitespaceTokenizer.newCoreLabelTokenizerFactory("");

        dp.setSentenceDelimiter("\n");
        dp.setTagDelimiter("_");
        dp.setTokenizerFactory(tf);

        List<Tree> trees = new ArrayList<Tree>();
        Iterable<List<? extends HasWord>> sentences;
        ArrayList<List<? extends HasWord>> tmp = new ArrayList<List<? extends HasWord>>();
//        int count = 0;
        for (List<HasWord> sentence : dp)
        {
            tmp.add(sentence);
//            count ++;
//            if (count % 1000 == 0)
//            {
////                System.out.println("Sentence addition count: " + count);
//            }
        }
//        count = 0;
        sentences = tmp;
        for (List <?extends HasWord> sentence : sentences)
        {
            Tree t = lp.parse(sentence);
            trees.add(t);
//            t.pennPrint();
//            count ++;
//            if (count % 1000 == 0)
//            {
////                System.out.println("Parse count: " + count);
//            }
        }
        return trees;
    }
}
