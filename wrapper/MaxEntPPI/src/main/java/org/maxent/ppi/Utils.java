package org.maxent.ppi;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class Utils {

    public void createAttribs(File inFolder, File ppiFile, File annoFile) throws FileNotFoundException {
        FileReader ppiFR = new FileReader(ppiFile);



    }

   /* public static Set<InteractomeBinaryInteraction> readBinInts(File inFile, boolean withHeader) throws IOException {
        Set<InteractomeBinaryInteraction> bis = new HashSet<InteractomeBinaryInteraction>(2);

        FileReader fr = new FileReader(inFile);
        BufferedReader br = new BufferedReader(fr);
        String line ;
        if (withHeader){
            line = br.readLine(); //header
        }
        while ((line = br.readLine()) != null){
            String [] aux = line.split("\t");
            BinaryInteraction bi = ReaderUtils.parseBinaryInteraction(aux);
            bis.add(new InteractomeBinaryInteraction(bi.getProtein_A(), bi.getGene_A_symbol(),
                    bi.getProtein_B(), bi.getGene_B_symbol()));

        }
        br.close();

        return bis;
    }*/
}
