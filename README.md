# PDFLayoutTextStripper

Converts a PDF file into a text file while keeping the layout of the original PDF. Useful to extract the content from a table or a form in a PDF file. PDFLayoutTextStripper is a subclass of PDFTextStripper class (from the [Apache PDFBox](https://pdfbox.apache.org/) library).

* Use cases
* How to install
* How to use

## Use cases
Data extraction from a table in a PDF file
![example](sample.png)
-
Data extraction from a form in a PDF file
![example](sample2.png)

## How to install

1) Install **apache pdfbox** manually ([to get the v2.0.6 click here](https://mvnrepository.com/artifact/org.apache.pdfbox/pdfbox/2.0.6) ) and its two dependencies
commons-logging.jar and fontbox

>**warning**: only pdfbox versions **from version 2.0.0 upwards** are compatible with this version of PDFLayoutTextStripper.java


### How to use on Linux
```
cd PDFLayoutTextStripper
javac -cp .:/pathto/pdfbox-2.0.6.jar:/pathto/commons-logging-1.2.jar:/pathto/PDFLayoutTextStripper/fontbox-2.0.6.jar *.java
java -cp .:/pathto/pdfbox-2.0.6.jar:/pathto/commons-logging-1.2.jar:/pathto/PDFLayoutTextStripper/fontbox-2.0.6.jar test
```

### How to use on Windows

The same as for Linux (see above) but replace :  with ;
