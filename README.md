# maxent-ppi

Datasets based on [CYC2008 v2.0](http://wodaklab.org/cyc2008/) a <i>S. cerevisiae</i> curated protein complex data set and 1,379 binary interactions observed in at least one out of 14 biochemical conditions [Celaj et al., 2017](https://www.ncbi.nlm.nih.gov/m/pubmed/28705884/).
Dataset folder:
- protein/gene GO annotaiton files: go2ppi_fbgn.anno and go2ppi_gene_symbol.anno
- protein/gene interaction files: go2ppi_fgbn.ppi and go2ppi_gene_symbol.ppi (first 500 positive set, last 500 negative set)
- Gene Ontology OBO file: go2ppi_fbgn.anno
- Yeast 2017: yeast_ppis_ORF_1379.txt and yeast_ppis_ORF_1379_IDs_624.txt (positive set); lowConf_yeast_ppis1379_IDs618_ppis.txt and lowConf_yeast_ppis1379_IDs618_prots.txt (negative set); ppi_GO_merged.txt (postive and negative set together) 

gis-maxent
- package downloaded from [Sourceforge Maxent](https://sourceforge.net/projects/maxent/files/Maxent/3.0.0/) project
- modification of opennlp.maxent.GISTrainer: offer correction constant alternative to maximum length of a feature vector (previous implementation) to median length of feature vector (modified version: lines 307-326)

Java wrapper functions:
- training:
```java -jar maxent-ppi-wrapper.jar -train -i train.dat -o model.out```
- evaluation: 
```java -jar maxent-ppi-wrapper.jar -eval -i test.dat -m model.out -o test.out```
- evaluation with raw scores:
```java -jar maxent-ppi-wrapper.jar -evalScore -i test.dat -m model.out -o test.out```
- export weights: 
```java -jar maxent-ppi-wrapper.jar -weight -i model.out -o modelWeights.out```
