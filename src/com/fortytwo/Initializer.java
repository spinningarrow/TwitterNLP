package com.fortytwo;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.WhitespaceTokenizer;
import edu.stanford.nlp.trees.Tree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
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
    public static final String DB_NAME = "database_full.db";
    public static final String PLAINTEXT_SET_NAME = "nlp_merged.clean.normal";
    public static final String TAGGED_SET_NAME = "nlp_merged.clean.normal.pos";
    public static final String SERIALIZED_MODEL = "SerializedModel10";

    public static void main (String []args)
    {

        createDatabase("data/" + DB_NAME);
        readAndStoreTweets("data/" + DB_NAME, "data/" + PLAINTEXT_SET_NAME, "data/" + TAGGED_SET_NAME);
//        printData("data/" + DB_NAME);
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
            while ((plain_line = br1.readLine()) != null)
            {
                tagged_line = br2.readLine();
                plaintextTweets.add(plain_line);
                taggedTweets.add(tagged_line);
            }
            store(databaseFile, plaintextTweets, taggedTweets);
        } catch (Exception e) {

            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static void printData(String filename)
    {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + filename);
            c.setAutoCommit(false);
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
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            String sql = "CREATE TABLE TWEETS " +
                    "(TEXT           TEXT    NOT NULL, " +
                    " PARSETREE      BLOB     NOT NULL)";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();

        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Records created successfully");
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
    private static void store (String filename, List<String> plaintextSentences, List<String> taggedSentences)
    {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + filename);
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
                String taggedSentence = taggedSentences.get(i);
                ps.setString(1, sentence);
                ps.setBytes(2, Serializer.serialize(parseSentence("models/" + SERIALIZED_MODEL, taggedSentence)));
                ps.executeUpdate();

                System.out.println("Count: " + i);
            }

            ps.close();
            c.commit();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
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
}
