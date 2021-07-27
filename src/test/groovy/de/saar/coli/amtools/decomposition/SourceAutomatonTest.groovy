package de.saar.coli.amtools.decomposition

import de.saar.basic.Pair
import de.up.ling.irtg.automata.TreeAutomaton
import de.up.ling.irtg.codec.BinaryIrtgInputCodec
import de.up.ling.tree.Tree
import de.up.ling.tree.TreeParser
import de.up.ling.irtg.automata.Rule

import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

import static org.junit.Assert.*
import org.junit.*
import static de.up.ling.irtg.util.TestingTools.*;

class SourceAutomatonTest {

    BinaryIrtgInputCodec irtbCodec = new BinaryIrtgInputCodec();

    @Test
    public void testDMDecomposition() {
        String[] args = "-t examples/decomposition_input/mini.dm.sdp -d examples/decomposition_input/mini.dm.sdp -o examples/decomposition_input/dm_out/ -ct DM -s 2 -a automata --noNE".split(" ");
        SourceAutomataCLI.main(args);
        String zipPath = "examples/decomposition_input/dm_out/train.zip"
        String acceptedTree = "APP_S1('(ART-ROOT<root> / --LEX--  :art-snt1 (i_4<S1>))--TYPE--(S1())',APP_S0(APP_S1('(i_4<root> / --LEX--  :ARG1 (i_3<S1>)  :ARG2 (i_6<S0>))--TYPE--(S0(S1()))',MOD_S1(MOD_S1('(i_3<root> / --LEX--)--TYPE--()','(i_1<root> / --LEX--  :BV (i_3<S1>))--TYPE--(S1())'),'(i_2<root> / --LEX--  :ARG1 (i_3<S1>))--TYPE--(S1())')),'(i_2<root> / --LEX--  :ARG1 (i_3<S1>))--TYPE--(S1())'))"
//        printZipFileParts(zipPath)
        checkZipFile(zipPath, "1\tThe\t_\tThe\tDT\t_\t_\t_\t_\t3\tIGNORE\ttrue",
            23, acceptedTree, "[[](0), {Si4=>S1}]", "(ART-ROOT<root> / --LEX--  :art-snt1 (i_4<S1>))--TYPE--(S1())",
                new Pair(7,"(ART-ROOT<root> / --LEX--  :art-snt1 (i_4<S1>))--TYPE--(S1())"),
                "[[0](1), {Si3=>S1,Si6=>S0}]", "APP_S1", new String[]{"[[0](0), {Si3=>S1,Si6=>S0}]", "[[0, 0](2), {}]"},
                new Pair(new Pair(4,3), "APP_S1"))
    }

    @Test
    public void testPASDecomposition() {
        String[] args = "-t examples/decomposition_input/mini.pas.sdp -d examples/decomposition_input/mini.pas.sdp -o examples/decomposition_input/pas_out/ -ct PAS -s 2 -a automata --noNE".split(" ");
        SourceAutomataCLI.main(args);
        String zipPath = "examples/decomposition_input/pas_out/train.zip"
//        printZipFileParts(zipPath)
        String acceptedTree = "APP_S1('(ART-ROOT<root> / --LEX--  :art-snt1 (i_4<S1>))--TYPE--(S1())',APP_S0(APP_S1('(i_4<root> / --LEX--  :verb_ARG1 (i_3<S1>)  :verb_ARG2 (i_6<S0>))--TYPE--(S0(S1()))',MOD_S0(MOD_S0('(i_3<root> / --LEX--)--TYPE--()','(i_2<root> / --LEX--  :adj_ARG1 (i_3<S0>))--TYPE--(S0())'),'(i_1<root> / --LEX--  :det_ARG1 (i_3<S0>))--TYPE--(S0())')),'(i_6<root> / --LEX--  :verb_ARG1 (i_3<S1>))--TYPE--(S1())'))"
        checkZipFile(zipPath, "1\tThe\t_\tThe\tDT\t_\t_\t_\t_\t3\tIGNORE\ttrue",
                23, acceptedTree, "[[0](0), {Si3=>S1,Si6=>S0}]", "(i_4<root> / --LEX--  :verb_ARG1 (i_3<S1>)  :verb_ARG2 (i_6<S0>))--TYPE--(S0(S1()))",
                new Pair(4,"(i_4<root> / --LEX--  :verb_ARG1 (i_3<S1>)  :verb_ARG2 (i_6<S0>))--TYPE--(S0(S1()))"),
                "[[0, 0](2), {}]", "MOD_S0", new String[]{"[[0, 0](1), {}]", "[[0, 0, 1](0), {Si3=>S0}]"},
                new Pair(new Pair(3,1), "MOD_S0"))
    }

    @Test
    public void testPSDDecomposition() {
        String[] args = "-t examples/decomposition_input/mini.psd.sdp -d examples/decomposition_input/mini.psd.sdp -o examples/decomposition_input/psd_out/ -ct PSD -s 2 -a automata --noNE --useLegacyPSDpreprocessing".split(" ");
        SourceAutomataCLI.main(args);
        String zipPath = "examples/decomposition_input/psd_out/train.zip"
//        printZipFileParts(zipPath)
        String acceptedTree = "APP_S0('(ART-ROOT<root> / --LEX--  :art-snt1 (i_4<S0>))--TYPE--(S0())',APP_S0(APP_S1('(i_4<root> / --LEX--  :ACT-arg (i_3<S0>)  :PAT-arg (i_6<S1>))--TYPE--(S1(S0()))','(i_6<root> / --LEX--  :ACT-arg (i_3<S0>))--TYPE--(S0())'),MOD_S1('(i_3<root> / --LEX--)--TYPE--()','(i_2<root> / --LEX--  :RSTR-of (i_3<S1>))--TYPE--(S1())')))"
        checkZipFile(zipPath, "1\tThe\t_\tThe\tDT\t_\t_\t_\t_\t0\tIGNORE\ttrue",
                19, acceptedTree, "[[0](0), {Si3=>S1,Si6=>S0}]", "(i_4<root> / --LEX--  :ACT-arg (i_3<S1>)  :PAT-arg (i_6<S0>))--TYPE--(S0(S1()))",
                new Pair(4,"(i_4<root> / --LEX--  :ACT-arg (i_3<S1>)  :PAT-arg (i_6<S0>))--TYPE--(S0(S1()))"),
                "[[0](2), {Si3=>S1,Si6=>S0}]", "APP_S1", new String[]{"[[0](1), {Si3=>S1,Si6=>S0}]", "[[0, 1](1), {}]"},
                new Pair(new Pair(4,3), "APP_S1"))
    }


    @Test
    public void testAMRDecomposition() {
        String[] args = "-t examples/decomposition_input/mini_amr.corpus -d examples/decomposition_input/mini_amr.corpus -o examples/decomposition_input/amr_out/ -s 2 -a automata".split(" ");
        SourceAutomataCLIAMR.main(args);
        String zipPath = "examples/decomposition_input/amr_out/train.zip"
//        printZipFileParts(zipPath)
        String acceptedTree = "APP_S0(APP_S1('(y<root> / --LEX--  :ARG0 (d<S1>)  :ARG1 (f<S0>))--TYPE--(S0(S1()))',MOD_S0('(d<root> / --LEX--)--TYPE--()','(l<root> / --LEX--  :mod-of (d<S0>))--TYPE--(S0())')),'(f<root> / --LEX--  :ARG0 (d<S1>))--TYPE--(S1())')"
        checkZipFile(zipPath, "#flavor:2",
                13, acceptedTree, "[[](0), {Sd=>S1,Sf=>S0}]", "(y<root> / --LEX--  :ARG0 (d<S1>)  :ARG1 (f<S0>))--TYPE--(S0(S1()))",
                new Pair(4,"(y<root> / --LEX--  :ARG0 (d<S1>)  :ARG1 (f<S0>))--TYPE--(S0(S1()))"),
                "[[0](1), {}]", "MOD_S1", new String[]{"[[0](0), {}]", "[[0, 0](0), {Sd=>S1}]"},
                new Pair(new Pair(3,2), "MOD_S1"))
    }

    private void checkZipFile(String path, String firstCorpusLine, int nrRules, String acceptedTree,
                              String supertagRuleParent, String supertagRuleLabel, Pair<Integer, String> supertagResult,
                              String edgeRuleParent, String edgeRuleLabel, String[] edgeRuleChildren, Pair<Pair<Integer, Integer>, String> edgeResult) {
        ZipFile zipFile = new ZipFile(path);

        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        TreeAutomaton<String> auto = null;

        while(entries.hasMoreElements()){
            ZipEntry entry = entries.nextElement();
            InputStream stream = zipFile.getInputStream(entry);
            if (entry.getName().equals("0.irtb")) {
                auto = irtbCodec.read(stream).getAutomaton();
                assert (auto.getNumberOfRules() == nrRules)
                assert auto.accepts(TreeParser.parse(acceptedTree))
                break;
            }
        }

        entries = zipFile.entries();

        while(entries.hasMoreElements()){
            ZipEntry entry = entries.nextElement();
            InputStream stream = zipFile.getInputStream(entry);
            switch (entry.getName()) {
                case "corpus.amconll":
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                    String line = reader.readLine();
                    assert (line.equals(firstCorpusLine))
                    break;

                case "0.supertagmap":
                    InputStream buffer = new BufferedInputStream(stream);
                    ObjectInput input = new ObjectInputStream(buffer);
                    Map<Rule, Pair<Integer, String>> rule2supertag = (Map<Rule, Pair<Integer, String>>) input.readObject();
                    Rule rule = auto.createRule(supertagRuleParent, supertagRuleLabel, new String[0])
                    assert rule2supertag.get(rule).equals(supertagResult)
                    break;
                case "0.edgemap":
                    InputStream buffer = new BufferedInputStream(stream);
                    ObjectInput input = new ObjectInputStream(buffer);
                    Map<Rule, Pair<Pair<Integer, Integer>, String>> rule2edge = (Map<Rule, Pair<Pair<Integer, Integer>, String>>) input.readObject();
                    Rule rule = auto.createRule(edgeRuleParent, edgeRuleLabel, edgeRuleChildren)
                    assert rule2edge.get(rule).equals(edgeResult)
                    break;
            }
        }

        zipFile.close();

    }

    private void printZipFileParts(String path) {
        ZipFile zipFile = new ZipFile(path);

        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        TreeAutomaton<String> auto = null;

        while(entries.hasMoreElements()){
            ZipEntry entry = entries.nextElement();
            InputStream stream = zipFile.getInputStream(entry);
            if (entry.getName().equals("0.irtb")) {
                auto = irtbCodec.read(stream).getAutomaton();
                println("accepted tree:")
                println(auto.languageIterator().next())
                println("number of rules:")
                println(auto.getNumberOfRules())
                break;
            }
        }

        entries = zipFile.entries();

        while(entries.hasMoreElements()){
            ZipEntry entry = entries.nextElement();
            InputStream stream = zipFile.getInputStream(entry);
            switch (entry.getName()) {
                case "corpus.amconll":
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                    String line = reader.readLine();
                    println("First corpus line:")
                    println(line)
                    break;

                case "0.supertagmap":
                    InputStream buffer = new BufferedInputStream(stream);
                    ObjectInput input = new ObjectInputStream(buffer);
                    Map<Rule, Pair<Integer, String>> rule2supertag = (Map<Rule, Pair<Integer, String>>) input.readObject();
                    Rule rule = rule2supertag.keySet().iterator().next();
                    println("Supertag rule:")
                    println(rule.toString(auto));
                    println("Supertag rule result:")
                    println(rule2supertag.get(rule))
                    break;
                case "0.edgemap":
                    InputStream buffer = new BufferedInputStream(stream);
                    ObjectInput input = new ObjectInputStream(buffer);
                    Map<Rule, Pair<Pair<Integer, Integer>, String>> rule2edge = (Map<Rule, Pair<Pair<Integer, Integer>, String>>) input.readObject();
                    Rule rule = rule2edge.keySet().iterator().next();
                    println("Edge rule:")
                    println(rule.toString(auto));
                    println("Edge rule result:")
                    println(rule2edge.get(rule))
                    break;
            }
        }

        zipFile.close();

    }

}