# maxent-ppi

Data set based on [CYC2008 v2.0](http://wodaklab.org/cyc2008/) a <i>S. cerevisiae</i> curated protein complex data set. 
Dataset folder:
- protein/gene GO annotaiton files: go2ppi_fbgn.anno and go2ppi_gene_symbol.anno
- protein/gene interaction files: go2ppi_fgbn.ppi and go2ppi_gene_symbol.ppi (first 500 positive set, last 500 negative set)
- Gene Ontology OBO file: go2ppi_fbgn.anno

gis-maxent
- package downloaded from [Sourceforge Maxent](https://sourceforge.net/projects/maxent/files/Maxent/3.0.0/) project
- modification of opennlp.maxent.GISTrainer: correction constant from max length of a feature vector to be median length of feature vector (lines 307-326)

Java wrapper functions:
- training:
```java -jar MaxEntWrapper.jar -train -i train.dat -o model.out```
- evaluation:
```java -jar MaxEntWrapper.jar -eval -i test.dat -m model.out -o test.out```
- export weights:
```java -jar MaxEntWrapper.jar -weight -i model.out -o modelWeights.out```
