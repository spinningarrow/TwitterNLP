package com.fortytwo;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

/**
 * Created with IntelliJ IDEA.
 * User: Radzinzki
 * Date: 11/11/13
 * Time: 12:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class TreeVerifier {
    public static void main (String[] args)
    {
        boolean test = true;

        test = !test;

        String outputFile;
        String trainFile;
        if(!test)
        {
            outputFile = "data/parser_data/output_from_parser/output_10.txt";
            trainFile = "data/parser_data/training_for_parser/train10.txt";
        }
        else
        {
            outputFile = "data/test.txt";
            trainFile = "data/test2.txt";
        }
        if(args.length > 0)
        {
            outputFile = args[0];
            trainFile  = args[1];
        }
        verifyFiles(outputFile, trainFile);
    //    test();
    }
    public static void verifyFiles (String outputFile, String trainFile)
    {
        boolean log = false;

        List<List<String>> outputList = getTagListForFile(outputFile);
        System.out.println("\n\n");
        List<List<String>> correctedList = getTagListForFile(trainFile);
        System.out.println("\n\n" + outputList.size() + " | " + correctedList.size());
        int tp = 0, fp = 0, fn =0;
        for (int i = 0; i < outputList.size(); i++)
        {
            //tp --; // to ignore the count for the root
            List<String> outputTreeList = outputList.get(i);
//            System.out.println(outputTreeList.toString());
            List<String> correctedTreeList = correctedList.get(i);
//            System.out.println(correctedTreeList.toString()+"\n\n");
            int fnForTree = correctedTreeList.size();
            for (String node : outputTreeList)
            {
                if(log)
                    System.out.println(node);
                if (correctedTreeList.contains(node))
                {
                    tp ++;
                    fnForTree--;
                }
                else
                {
                    fp ++;
//                    fn ++;
                }
            }
            fn += fnForTree;
            if(log)
            {
                System.out.println(outputTreeList.toString());
                System.out.println(correctedTreeList.toString());
            }
            System.out.println("TP: " + tp + " | FP: " + fp + " | FN: " + fn);
        }
        System.out.println();
        System.out.println("===============");
        System.out.println("\tSummary\t");
        System.out.println("===============");
        System.out.println("\tTP\t" + tp);
        System.out.println("\tFP\t" + fp);
        System.out.println("\tFN\t" + fn);
    }
    public static void test ()
    {
        List<List<String>> outputList = getTagListForFile("data/test.txt");
    }
    public static List<List<String>> getTagListForFile(String filename)
    {
        List <List<String>> tagsForFile = new ArrayList<List<String>>();
        boolean log = true;
        try {
            Scanner sc = new Scanner (new File(filename));
            int wordPos = 0;
            String s;
            List<String> tagsForTree = new ArrayList<String>();
            Stack<String> stack = new Stack<String>();
            while (sc.hasNext())
            {
                s = sc.next();
                if (s.startsWith("("))
                {
                    stack.push(s.substring(1)+"("+ wordPos +",");
                    if(log)
                        System.out.print(stack.peek());
                }
                else if (s.endsWith(")"))
                {
                    if(log)
                        System.out.print(s);
                    wordPos ++;
                    int count = 1 + s.lastIndexOf(")") - s.indexOf(")");
                    for (int i = 0; i < count; i ++)
                    {
                        String tag = stack.pop();
                        if (tag.startsWith("NP")||tag.startsWith("VP")||tag.startsWith("ADJP")||tag.startsWith("ADVP")||tag.startsWith("PP")||tag.startsWith("CP")||tag.startsWith("QP")||tag.startsWith("WHADVP"))
                            tagsForTree.add(tag + wordPos + ")");
                    }
                    if (stack.isEmpty())
                    {
                        wordPos = 0;
                        tagsForFile.add(tagsForTree);
                        if(log)
                            System.out.println("\n" + tagsForTree.toString());
                        tagsForTree = new ArrayList<String>();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return tagsForFile;
    }
}
