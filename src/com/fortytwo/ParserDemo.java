package com.fortytwo;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

class ParserDemo {

    /**
     * The main method demonstrates the easiest way to load a parser.
     * Simply call loadModel and specify the path, which can either be a
     * file or any resource in the classpath.  For example, this
     * demonstrates loading from the models jar file, which you need to
     * include in the classpath for ParserDemo to work.
     */
    public static void main(String[] args) {
        Iterable<List<? extends HasWord>> sentences;

        LexicalizedParser lp = LexicalizedParser.loadModel("models/SerializedModel4");
        DocumentPreprocessor dp = new DocumentPreprocessor("data/SampleSet1_POS.txt");
//        DocumentPreprocessor dp = new DocumentPreprocessor("data/Sample1Tweet.txt");
        TokenizerFactory<CoreLabel> tf = WhitespaceTokenizer.newCoreLabelTokenizerFactory("");

        dp.setSentenceDelimiter("\n");
        dp.setTagDelimiter("_");
        dp.setTokenizerFactory(tf);

        List<List<? extends HasWord>> tmp = new ArrayList<List<? extends HasWord>>();

        for (List<HasWord> sentence : dp) {
            tmp.add(sentence);
        }

        sentences = tmp;
        String query = "iPad Air";

        for (List<? extends HasWord> sentence : sentences) {
            Tree parse = lp.parse(sentence);
            printDescribingPhrase(sentence, query, parse);
        }
    }
    /**
     * demoDP demonstrates turning a file into tokens and then parse
     * trees.  Note that the trees are printed by calling pennPrint on
     * the Tree object.  It is also possible to pass a PrintWriter to
     * pennPrint if you want to capture the output.
     */
    public static void demoDP(LexicalizedParser lp, String filename) {
        // This option shows loading and sentence-segmenting and tokenizing
        // a file using DocumentPreprocessor.
        TreebankLanguagePack tlp = new PennTreebankLanguagePack();
        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
        // You could also create a tokenizer here (as below) and pass it
        // to DocumentPreprocessor
        for (List<HasWord> sentence : new DocumentPreprocessor(filename)) {
            Tree parse = lp.apply(sentence);
            parse.pennPrint();
            System.out.println();

            GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
            Collection tdl = gs.typedDependenciesCCprocessed();
            System.out.println(tdl);
            System.out.println();
        }
    }

    /**
     * demoAPI demonstrates other ways of calling the parser with
     * already tokenized text, or in some cases, raw text that needs to
     * be tokenized as a single sentence.  Output is handled with a
     * TreePrint object.  Note that the options used when creating the
     * TreePrint can determine what results to print out.  Once again,
     * one can capture the output by passing a PrintWriter to
     * TreePrint.printTree.
     */
    public static void demoAPI(LexicalizedParser lp) {
        // This option shows parsing a list of correctly tokenized words
        ArrayList<String> sentences = new ArrayList<String>();

//        String sentence = "iPad Air you skinny bitch!!";
        try {
            BufferedReader br  = new BufferedReader (new FileReader ("data/SampleSet3.txt"));

            String line = null;
            while ((line = br.readLine()) != null)
            {
                sentences.add(line);
            }

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
//        String[] sentences = {
//            "The iPad Air is lovely, light and awesome",
//            "I want the iPad Air",
//            "I LOVE the iPad Air ad.",
//            "I'm in love with the iPad Air, I'm just saying.",
//            "iPad air is the stupidest name I've heard"
//        };

//        for (String sentence : sentences) {
//            String sent[] = sentence.split(" ");
//            List<CoreLabel> rawWords = Sentence.toCoreLabelList(sent);
//            Tree parse = lp.apply(rawWords);
//            System.out.print(sentence + "|-|");
//            printDescribingPhrase(sentence, query, parse);
//        }


        // This option shows loading and using an explicit tokenizer
//        String sent2 = "This is another sentence.";
//        TokenizerFactory<CoreLabel> tokenizerFactory =
//                PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
//        List<CoreLabel> rawWords2 =
//                tokenizerFactory.getTokenizer(new StringReader(sent2)).tokenize();
//        parse = lp.apply(rawWords2);
//
//        TreebankLanguagePack tlp = new PennTreebankLanguagePack();
//        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
//        GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
//        List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
//        System.out.println(tdl);
//        System.out.println();
//
//        TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
//        tp.printTree(parse);
    }

    private static boolean isTagInTree(String tag, Tree treeToCheck)
    {
        return treeToCheck.toString().matches(".*\\b"+tag+"\\b.*");
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

    private static void printDescribingPhrase(List <? extends HasWord> sentence, String query, Tree parse) {

        // Check if the current sentence contains "iPad Air" in it
        parse.pennPrint();
        System.out.print(Sentence.listToString(sentence) + "\t|-|\t");
        if (!Sentence.listToString(sentence).toLowerCase().matches(".*" + query.toLowerCase() + ".*")) {
            return;
        }

        // Loop through the parse tree till you get to the node containing the word iPad
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

            if (currentTree.yieldWords().get(0).toString().toLowerCase().matches(".*\\b"+lastWord+"\\b.*")) {


//                Tree descriptionTree = currentTree.parent(parse).siblings(parse).get(0);
//                if (descriptionTree != null && !descriptionTree.value().equals("VB")) { // TODO VB is wrong, there are more cases
//                    List<Tree> subTrees = currentTree.parent(parse).parent(parse).siblings(parse);
//                    for (Tree subTree : subTrees) {
//                        if (subTree.value().equals("PP")) {
//                            descriptionTree = subTree;
//                            break;
//                        }
//                    }
//                }
                Tree descriptionTree = getDescriptiveSubtree(parse, currentTree);
                if (descriptionTree != null) {
                    // Print out the words in the tree (should be the phrase describing the iPad)
                    for (Word word : descriptionTree.yieldWords()) {
                        System.out.print(word + " ");
                    }
                    System.out.println();
                }

                // Don't continue iteration, we already found the iPad Air
                break;
            }
        }
    }

    private ParserDemo() {
    } // static methods only

}
