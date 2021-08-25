package de.saar.coli.amtools.cfq;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class CfqToGraphConverter {
    public static void main(String[] args) throws IOException {
        // "/Users/koller/Downloads/data/mcd1/train/train_decode.txt"
        BufferedReader r = new BufferedReader(new FileReader(args[0]));
        String line = null;

        while( (line = r.readLine()) != null ) {
            String s = "PREFIX ns: <http://ns/> " + line.strip(); //ad pseudo prefix to cope with freebase constants

            for (String x : List.of("M0", "M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9")){
                s = s.replace(x, "ns:"+x);
            }

            Query q = QueryFactory.create(s);
            ElementGroup conditions = (ElementGroup) q.getQueryPattern();
            for( Element el : conditions.getElements() ) {
                System.err.printf("-> %s\n", el);
            }
            System.exit(0);
        }
    }
}
