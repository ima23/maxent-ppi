package org.maxent.wrapper;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import opennlp.maxent.*;
import opennlp.maxent.io.GISModelReader;
import opennlp.maxent.io.GISModelWriter;
import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.model.AbstractModel;
import opennlp.model.EventStream;
import opennlp.maxent.GISModel;
import org.apache.commons.cli.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import static opennlp.maxent.ModelTrainer.USE_SMOOTHING;


/**
 * Created by irina on 4/20/17.
 */
public class MaxEntWrapper {
    public static void main(String[] args) {
        Options options = new Options();

        Option train = new Option("train",false, "train a model");
        // if option eval then input== model.txt and
        Option eval = new Option("eval",false, "evaluate data using a model");
        Option evalScore = new Option("evalScore",false, "evaluate data using a model - output is a score (0..1)");
        Option weights = new Option("weight",false, "extract feature weights");

        OptionGroup optgr = new OptionGroup();
        optgr.setRequired(true);
        optgr.addOption(train);
        optgr.addOption(eval);
        optgr.addOption(evalScore);
        optgr.addOption(weights);
        options.addOptionGroup(optgr);

        Option input = new Option("i", "input", true, "input file path");
        input.setRequired(true);
        options.addOption(input);

        Option output = new Option("o", "output", true, "output file");
        output.setRequired(true);
        options.addOption(output);

        Option modelFile = new Option("m", "model", true, "model file");
        modelFile.setRequired(false);
        options.addOption(modelFile);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);

            String inputFilePath = cmd.getOptionValue("input");
            System.out.println("input: " + inputFilePath);

            String outputFilePath = cmd.getOptionValue("output");
            System.out.println("output: " + outputFilePath);

            if (cmd.hasOption("train")){
                System.out.println("training");

                File inF = new File(inputFilePath);
                File outF = new File(outputFilePath);
                try {
                    train(inF, outF);
                } catch(IOException e){
                    System.out.println(e.getMessage());
                    System.exit(1);
                }

            } else if (cmd.hasOption("eval")){
                System.out.println("evaluating");
                File inputF = new File(inputFilePath);
                File outF = new File(outputFilePath);

                if (!cmd.hasOption("model")) {
                    System.out.println("missing model-file-path required for the option");
                    formatter.printHelp("java -jar MaxEntWrapperMaxEntWrapper.java -eval -m <model.txt> -i <data.txt> -o <out.txt>", options);

                    System.exit(1);
                    return;
                }
                String modelFilePath = cmd.getOptionValue("model");
                System.out.println("model: " + modelFilePath);
                File modelF = new File(modelFilePath);

                try {
                    evaluate(modelF,inputF, outF, false);

                } catch(IOException e){
                    System.out.println(e.getMessage());
                    System.exit(1);
                }


            } else if (cmd.hasOption("evalScore")){
                System.out.println("evaluating: output score");
                File inputF = new File(inputFilePath);
                File outF = new File(outputFilePath);

                if (!cmd.hasOption("model")) {
                    System.out.println("missing model-file-path required for the option");
                    formatter.printHelp("java -jar MaxEntWrapperMaxEntWrapper.java -evalScore -m <model.txt> -i <data.txt> -o <out.txt>", options);

                    System.exit(1);
                    return;
                }
                String modelFilePath = cmd.getOptionValue("model");
                System.out.println("model: " + modelFilePath);
                File modelF = new File(modelFilePath);

                try {
                    evaluate(modelF,inputF, outF, true);

                } catch(IOException e){
                    System.out.println(e.getMessage());
                    System.exit(1);
                }

            }
            else if (cmd.hasOption("weight")){
                System.out.println("extracting weights");

                try {
                    extractWeights(new File(inputFilePath), new File(outputFilePath));

                } catch(IOException e){
                    System.out.println(e.getMessage());
                    System.exit(1);
                }
            }

            System.out.println("Done!");

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -jar MaxEntWrapperMaxEntWrapper.java -train -i <trainingSet.txt> -o <model.txt>", options);

            System.exit(1);
            return;
        }

    }




    private static void train(File trainData, File gisOutput) throws IOException {
        InputStream inputData = new FileInputStream(trainData);
        Reader datafr = new InputStreamReader(inputData);
        DataStream ds = new PlainTextByLineDataStream(datafr);
        EventStream es = new BasicEventStream(ds);
        GIS.SMOOTHING_OBSERVATION = 0.1;
        GISModel model = GIS.trainModel(es, 100, 0, USE_SMOOTHING,true, 1);

        GISModelWriter writer = new SuffixSensitiveGISModelWriter(model, gisOutput);
        writer.persist();
    }

    private static void evaluate(File modelFile, File inputFile, File outFile, boolean hcScoreOut) throws IOException {
        GISModelReader reader = new SuffixSensitiveGISModelReader(modelFile);
        AbstractModel model = reader.getModel();

        BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
        BufferedReader br = new BufferedReader(new FileReader(inputFile));
        String line = null;
        while ((line = br.readLine()) != null){
            String [] aux = line.split("\\s");
            double[] scores = model.eval(aux);
            String bestOut = "";
            if (hcScoreOut) {
                bestOut = model.getAllOutcomes(scores);
            } else {
                bestOut = model.getBestOutcome(scores);
            }

            bw.write(line +":"+ bestOut +"\n");
        }
        br.close();
        bw.close();
    }

    private static void extractWeights(File modelFile, File weightsOutFile) throws IOException {

        //create lucene index
        File luceneDir = new File(modelFile.getAbsolutePath()+ "_lucene_index");
        //System.out.println(luceneDir.getAbsolutePath());
        if(!luceneDir.exists()) {
            System.out.println("creating index ...");
            index(luceneDir, modelFile);
        }

        // extract general info
        Directory directory = new SimpleFSDirectory(luceneDir);
        IndexSearcher searcher = new IndexSearcher(directory, true);

        Document doctotalAttrs = searcher.doc(10);
        Field totalAttr = doctotalAttrs.getField("line");
        System.out.println("total nr attribs: " + totalAttr.stringValue());

        String classA = searcher.doc(4).getField("line").stringValue();
        String classB = searcher.doc(5).getField("line").stringValue();

        int totalAttribsNr  = Integer.valueOf(totalAttr.stringValue()).intValue();
        Document doctotalHCs = searcher.doc(7);
        Field totalHCs = doctotalHCs.getField("line");
        String [] aux = totalHCs.stringValue().split("\\s");
        int totalHCNr  = Integer.valueOf(aux[0]).intValue();
        System.out.println("total "+ classA +" nr attribs: " + totalHCNr);

        Document doctotalHCLCs = searcher.doc(8);
        Field totalHCLCs = doctotalHCLCs.getField("line");
        aux = totalHCLCs.stringValue().split("\\s");
        int totalHCLCNr  = Integer.valueOf(aux[0]).intValue();
        System.out.println("total " + classA + "-" + classB +" nr attribs: " + totalHCLCNr);

        Document doctotalLCs = searcher.doc(9);
        Field totalLCs = doctotalLCs.getField("line");
        aux = totalLCs.stringValue().split("\\s");
        int totalLCNr = Integer.valueOf(aux[0]).intValue();
        System.out.println("total " +classB + " nr attribs: " + totalLCNr);


        // extract weights
        FileWriter fw = new FileWriter(weightsOutFile);
        BufferedReader br = new BufferedReader(new FileReader(modelFile));
        String line ;
        int i=0;
        int j = 0;
        //skip first 10 lines: in a 2 class problem, first 10 lines are general details
        for (int sk=0; sk < 11; sk++){
            br.readLine();  i++;
        }
        while ((line = br.readLine()) != null){
            i++;
            if (line.matches("^[a-zA-Z].*") && !line.matches("GIS") && !line.matches("HIGH") && !line.matches("LOW") ){
                j ++;
                List<Double> scores = evalAttr(searcher, line, totalAttribsNr, totalHCNr, totalHCLCNr, totalLCNr);
                if (scores != null){
                    fw.append(line + "\t" + scores.get(0) + "\t" + scores.get(1) + "\n");
                }
            } else if (line.matches("")){
                j ++;
                List<Double> scores = evalAttr(searcher, line, totalAttribsNr, totalHCNr, totalHCLCNr, totalLCNr);
                if (scores != null){
                    if (scores.get(0) == null){
                        fw.append(line + "\t" + "NA" + "\t" + scores.get(1) + "\n");
                    } else if (scores.get(1) == null){
                        fw.append(line + "\t" + scores.get(0) + "\t" + "NA" + "\n");
                    } else {
                        fw.append(line + "\t" + scores.get(0) + "\t" + scores.get(1) + "\n");
                    }
                }
            }
        }
        fw.close();
        br.close();
        System.out.println("GisModel-file lines: " + i + " attribs: "+ j);

    }

    private static void index(File luceneDir, File modelFile) throws IOException {
        Directory directory = new SimpleFSDirectory(luceneDir);
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_30);

        //1 . create Lucene index
        boolean create = true;
        IndexWriter indexWriter = new IndexWriter(directory, analyzer, create, IndexWriter.MaxFieldLength.UNLIMITED);

        // parsing model file
        FileReader fr = new FileReader(modelFile);
        BufferedReader br = new BufferedReader(fr);

        String line;
        while ((line = br.readLine()) != null){
            Document document  =  new Document();
            document.add(new Field("line", line, Field.Store.YES, Field.Index.NOT_ANALYZED));
            indexWriter.addDocument(document);
        }

        br.close();
        indexWriter.optimize();
        indexWriter.close();
    }


    private static List<Double> evalAttr(IndexSearcher searcher, String attr, int totalAttribsNr, int totalHCNr, int totalHCLCNr, int totalLCNr) throws IOException {
        Term lineT = new Term("line", attr);
        Query query = new TermQuery(lineT);
        TopDocs hits = searcher.search(query, 3);
        if (hits.totalHits  >1 || hits.totalHits  == 0){
            System.out.println("Number of matching documents for " + attr + "= " + hits.totalHits + "!!");
            System.exit(1);
        }
        int indexWeightsStart = 10 + totalAttribsNr + 1;
        for (int i = 0; i < hits.totalHits; i++) {
            ScoreDoc doc = hits.scoreDocs[i];
            String [] aux = doc.toString().split("[=|\\s]");
            int lineNr = Integer.valueOf(aux[1]).intValue()+1;
            List<Double> result = new ArrayList<Double>(2);
            // get weights
            if (lineNr > 10+1 && lineNr <= totalHCNr + 11 ){
                // HC attribute
                int index = totalAttribsNr + lineNr -1;
                Document docVal = searcher.doc(index);
                Double weight = Double.valueOf(docVal.getField("line").stringValue());
                result.add(weight);
                result.add(new Double(0));//Added for layout
                return result;
            } else if (lineNr > totalHCNr + 10 && lineNr <= totalHCNr + totalHCLCNr + 11) {
                int attrOrder = lineNr - 10 -1;
                int index1 = indexWeightsStart + totalHCNr +(attrOrder - totalHCNr - 1) * 2;
                int index2 = index1+1;
                int indexHCLC = lineNr - totalHCNr - 10;
                Document docVal = searcher.doc(index1);
                Double weight1 = Double.valueOf(docVal.getField("line").stringValue());
                Double weight2 = Double.valueOf(searcher.doc(index2).getField("line").stringValue());
                result.add(weight1);
                result.add(weight2);
                return result;

            } else {
                // LC attribute
                int index = 10 + totalAttribsNr + totalHCNr + 2 * totalHCLCNr + ( totalLCNr - (11 + totalAttribsNr - lineNr));
                Document docVal = searcher.doc(index);
                Double weight = Double.valueOf(docVal.getField("line").stringValue());
                result.add(new Double(0));//Added for layout
                result.add(weight);
                return result;
            }

        }
        return null;
    }
}
