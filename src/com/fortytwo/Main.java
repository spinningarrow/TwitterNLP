package com.fortytwo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.trees.*;

class Main {

    /**
     * The main method demonstrates the easiest way to load a parser.
     * Simply call loadModel and specify the path, which can either be a
     * file or any resource in the classpath.  For example, this
     * demonstrates loading from the models jar file, which you need to
     * include in the classpath for Main to work.
     */
    public static void main(String[] args) {

        // Pass DB path and query as command line arguments

        String query, dbPath;
        if (args.length > 0) {
            dbPath = args[0];
            query = args[1];
        } else {
//            return;
            dbPath = "data/database.db";
            query = "ipad air";
        }

        query = query.toLowerCase();

        System.err.println("Showing describing phrases for query: " + query);

        // Connect to the database
        Connection c;
        Statement stmt;

        try {

            Class.forName("org.sqlite.JDBC");

            c = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            c.setAutoCommit(false);

            stmt = c.createStatement();
//            ResultSet rs = stmt.executeQuery( "SELECT * FROM TWEETS;");
            ResultSet rs = stmt.executeQuery( "SELECT * FROM TWEETS WHERE LOWER(text) LIKE '%" + query + "%';");

            // Read each row
            // For each row, get the sentence and its parse tree
            while ( rs.next() ) {
                String sentence = rs.getString("text");
                Tree parseTree = (Tree) Serializer.deserialize(rs.getBytes("parsetree"));

                String describingPhrase = findDescribingPhrase(sentence, query, parseTree);
                if (describingPhrase != null) System.out.println(describingPhrase);
            }

            rs.close();
            stmt.close();
            c.close();
        }

        catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }

    private static boolean isTagInTree(String tag, Tree treeToCheck)
    {
        return treeToCheck.toString().matches(".*\\b" + tag + "\\b.*");
    }

    private static Tree getDescriptiveSubtree(Tree completeTree, Tree matchedNode)
    {
        if (matchedNode == null || matchedNode.parent(completeTree) == null) return completeTree;
        List<Tree> siblings = matchedNode.parent(completeTree).siblings(completeTree);
        if (siblings == null || siblings.size() == 0)  // No siblings
        {
            return getDescriptiveSubtree(completeTree, matchedNode.parent(completeTree));
        }
        else
        {
            for (Tree sibling : siblings)
            {
                if (sibling.value().equals("PP") && !(isTagInTree("JJ", sibling) || isTagInTree("VB", sibling) || isTagInTree("VBZ", sibling)))
                {
                    return getDescriptiveSubtree(completeTree, matchedNode.parent(completeTree));
                    //break;
                }
                if (sibling.value().equals("IN"))
                {
                    return getDescriptiveSubtree(completeTree, matchedNode.parent(completeTree));
                    //break;
                }
                return sibling;
            }
        }

        return completeTree;
    }

    private static String findDescribingPhrase(String sentence, String query, Tree parse) {

        String result = "";

        // Loop through the parse tree till you get to the node containing the last word of the query
        // (there are two nodes, one containing the label and the word, and one containing just the word)
        Iterator treeIterator = parse.iterator();
        Tree currentTree;

        String[] words= query.split(" ");
        String lastWord = words[words.length-1].toLowerCase();

        while (treeIterator.hasNext()) {

            currentTree = (Tree) treeIterator.next();

            if (currentTree.depth() != 1) {
                continue;
            }

            if (currentTree.yieldWords().get(0).toString().toLowerCase().matches(".*\\b" + lastWord + "\\b.*")) {

                Tree descriptionTree = getDescriptiveSubtree(parse, currentTree);

                if (descriptionTree != null) {
                    // Print out the words in the tree (should be the phrase describing the iPad)
                    for (Word word : descriptionTree.yieldWords()) {
                        result += word + " ";
                    }
                }

                // Don't continue iteration, we already found the query
                break;
            }
        }

        result = result.toLowerCase().indexOf(query) == 0 ? result.toLowerCase().replace(query, "").trim() : result;

        if (result == "" || result.toLowerCase().trim().indexOf(' ') == -1) {
            result = null;
        }
        return result;
    }

    private Main() {
    } // static methods only

}
