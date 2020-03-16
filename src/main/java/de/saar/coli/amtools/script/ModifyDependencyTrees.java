package de.saar.coli.amtools.script;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.saar.basic.Pair;
import de.saar.coli.amrtagging.AlignedAMDependencyTree;
import static de.saar.coli.amrtagging.AlignedAMDependencyTree.decodeNode;
import de.saar.coli.amrtagging.AmConllEntry;
import de.saar.coli.amrtagging.AmConllSentence;
import de.saar.coli.amrtagging.formalisms.amr.AMRBlobUtils;
import de.saar.coli.amrtagging.formalisms.sdp.dm.DMBlobUtils;
import de.saar.coli.amrtagging.formalisms.sdp.pas.PASBlobUtils;
import de.saar.coli.amrtagging.formalisms.sdp.psd.PSDBlobUtils;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.codec.IsiAmrInputCodec;
import de.up.ling.irtg.util.Counter;
import de.up.ling.tree.ParseException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.eclipse.collections.impl.factory.Sets;
import se.liu.ida.nlp.sdp.toolkit.graph.Edge;
import se.liu.ida.nlp.sdp.toolkit.graph.Graph;
import se.liu.ida.nlp.sdp.toolkit.graph.Node;
import se.liu.ida.nlp.sdp.toolkit.io.GraphReader2015;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static de.saar.coli.amtools.script.FindPatternsAcrossSDP.*;
import de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra;
import de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.Type;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.algebra.graph.SGraphDrawer;

public class ModifyDependencyTrees {

    //SDP corpora (i.e. original graphs)
    @Parameter(names = {"--corpusDM", "-dm"}, description = "Path to the input corpus (en.dm.sdp) or subset thereof")
    private String corpusPathDM = "/home/matthias/Schreibtisch/Hiwi/Koller/uniformify2020/original_decompositions/dm/dev/dev.sdp";

    @Parameter(names = {"--corpusPAS", "-pas"}, description = "Path to the input corpus (en.pas.sdp) or subset thereof")
    private String corpusPathPAS = "/home/matthias/Schreibtisch/Hiwi/Koller/uniformify2020/original_decompositions/pas/dev/dev.sdp";

    @Parameter(names = {"--corpusPSD", "-psd"}, description = "Path to the input corpus (en.psd.sdp) or subset thereof")
    private String corpusPathPSD = "/home/matthias/Schreibtisch/Hiwi/Koller/uniformify2020/original_decompositions/psd/dev/dev.sdp";

    // amconll files (i.e. AM dependency trees)
    @Parameter(names = {"--amconllDM", "-amdm"}, description = "Path to the input corpus (.amconll) or subset thereof")
    private String amconllPathDM = "/home/matthias/Schreibtisch/Hiwi/Koller/uniformify2020/original_decompositions/dm/gold-dev/gold-dev.amconll";

    @Parameter(names = {"--amconllPAS", "-ampas"}, description = "Path to the input corpus (.amconll) or subset thereof")
    private String amconllPathPAS = "/home/matthias/Schreibtisch/Hiwi/Koller/uniformify2020/original_decompositions/pas/gold-dev/gold-dev.amconll";

    @Parameter(names = {"--amconllPSD", "-ampsd"}, description = "Path to the input corpus (.amconll) or subset thereof")
    private String amconllPathPSD = "/home/matthias/Schreibtisch/Hiwi/Koller/uniformify2020/original_decompositions/psd/gold-dev/gold-dev.amconll";

    @Parameter(names = {"--outputPath", "-o"}, description = "Path to the output folder")
    private String outputPath = "../../github/";



    @Parameter(names = {"--help", "-?","-h"}, description = "displays help if this is the only command", help = true)
    private boolean help=false;


    private static DMBlobUtils dmBlobUtils = new DMBlobUtils();
    private static PASBlobUtils pasBlobUtils = new PASBlobUtils();
    private static PSDBlobUtils psdBlobUtils = new PSDBlobUtils();
    
    private int negations = 0;
    private int negationsFixedPSD = 0;
    private int negationsFixedPAS = 0;
    
    private int never = 0;
    private int neverFixedPSD = 0;
    private int neverFixedPAS = 0;

    /**
     * prints CSV tables for all auxiliary verbs according to wikipedia. Information includes total counts, and counts of
     * edge patterns.
     * @param args
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ParseException
     * @throws ParserException
     * @throws AlignedAMDependencyTree.ConllParserException
     */
    public static void main(String[] args) throws FileNotFoundException, IOException, ParseException, AlignedAMDependencyTree.ConllParserException, ParserException, Exception {
        //just getting command line args
        ModifyDependencyTrees cli = new ModifyDependencyTrees();
        JCommander commander = new JCommander(cli);
        try {
            commander.parse(args);
        } catch (com.beust.jcommander.ParameterException ex) {
            System.err.println("An error occured: " + ex.toString());
            System.err.println("\n Available options: ");
            commander.usage();
            return;
        }
        if (cli.help) {
            commander.usage();
            return;
        }


        //setup
        new File(cli.outputPath).mkdirs();
        GraphReader2015 grDM = new GraphReader2015(cli.corpusPathDM);
        GraphReader2015 grPAS = new GraphReader2015(cli.corpusPathPAS);
        GraphReader2015 grPSD = new GraphReader2015(cli.corpusPathPSD);
        Graph dmGraph;
        Graph pasGraph;
        Graph psdGraph;
        List<AmConllSentence> amDM = AmConllSentence.read(new FileReader(cli.amconllPathDM));
        List<AmConllSentence> amPSD = AmConllSentence.read(new FileReader(cli.amconllPathPSD));
        List<AmConllSentence> amPAS = AmConllSentence.read(new FileReader(cli.amconllPathPAS));
        // map IDs to AmConllSentences so we can look the AmConllSentences up
        Map<String, AmConllSentence> id2amDM = new HashMap<>();
        amDM.stream().forEach(sent -> id2amDM.put(sent.getId(), sent));
        Map<String, AmConllSentence> id2amPAS = new HashMap<>();
        amPAS.stream().forEach(sent -> id2amPAS.put(sent.getId(), sent));
        Map<String, AmConllSentence> id2amPSD = new HashMap<>();
        amPSD.stream().forEach(sent -> id2amPSD.put(sent.getId(), sent));
        Set<String> decomposedIDs = Sets.intersectAll(id2amDM.keySet(), id2amPAS.keySet(), id2amPSD.keySet());

        List<AmConllSentence> newAmDM = new ArrayList<>();
        List<AmConllSentence> newAmPAS = new ArrayList<>();
        List<AmConllSentence> newAmPSD = new ArrayList<>();
        
        ModifyDependencyTrees treeModifier = new ModifyDependencyTrees();

        while ((dmGraph = grDM.readGraph()) != null && (pasGraph = grPAS.readGraph()) != null && (psdGraph = grPSD.readGraph()) != null) {
            if (decomposedIDs.contains(dmGraph.id)) {
                //now we know the graph was decomposed in all graphbanks, and we have all three AM dep trees for it.
                // we can also look at the original graphs (dmGraph etc) if we need to.
                String id = dmGraph.id;
                AmConllSentence dmDep = id2amDM.get(id);
                AmConllSentence pasDep = id2amPAS.get(id);
                AmConllSentence psdDep = id2amPSD.get(id);
                String originalDMDepStr = dmDep.toString();
                String originalPSDDepStr = psdDep.toString();
                String originalPASDepStr = pasDep.toString();


                SGraph dmSGraph = AlignedAMDependencyTree.fromSentence(dmDep).evaluate(true);
                onlyIndicesAsLabels(dmSGraph);
                SGraph psdSGraph = AlignedAMDependencyTree.fromSentence(psdDep).evaluate(true);
                onlyIndicesAsLabels(psdSGraph);
                SGraph pasSGraph = AlignedAMDependencyTree.fromSentence(pasDep).evaluate(true);
                onlyIndicesAsLabels(pasSGraph);
                //System.out.println(dmSGraph);

                //modify new dep trees here
                fixDeterminer(psdDep, dmDep, pasDep);
                treeModifier.fixNegation(psdDep, dmDep, pasDep);


                SGraph newdmSGraph = null;
                SGraph newpsdSGraph = null;
                SGraph newpasSGraph = null;
                //try {
                newdmSGraph = AlignedAMDependencyTree.fromSentence(dmDep).evaluate(true);
                onlyIndicesAsLabels(newdmSGraph);
                newpsdSGraph = AlignedAMDependencyTree.fromSentence(psdDep).evaluate(true);
                onlyIndicesAsLabels(newpsdSGraph);
                newpasSGraph = AlignedAMDependencyTree.fromSentence(pasDep).evaluate(true);
                onlyIndicesAsLabels(newpasSGraph);
                
                //} catch (IllegalArgumentException e) {
                //    System.err.println(psdDep);
                //    System.err.println(pasDep);
                //    e.printStackTrace();
                //}

                if (!newdmSGraph.equals(dmSGraph)) {
                    System.err.println(originalDMDepStr);
                    System.err.println(dmDep);
                    System.err.println(dmSGraph.toIsiAmrStringWithSources());
                    System.err.println(newdmSGraph.toIsiAmrStringWithSources());
                    SGraphDrawer.draw(dmSGraph, "original");
                    SGraphDrawer.draw(newdmSGraph,"modified");
                    
                    throw new Exception("Difference in DM");
                }
                if (!newpsdSGraph.equals(psdSGraph)) {
                    System.err.println(originalPSDDepStr);
                    System.err.println(psdDep);
                    System.err.println(psdSGraph.toIsiAmrStringWithSources());
                    System.err.println(newpsdSGraph.toIsiAmrStringWithSources());
                    SGraphDrawer.draw(psdSGraph, "original");
                    SGraphDrawer.draw(newpsdSGraph,"modified");
                    throw new Exception("Difference in PSD");
                }
                if (!newpasSGraph.equals(pasSGraph)) {
                    System.err.println(originalPASDepStr);
                    System.err.println(pasDep);
                    System.err.println(pasSGraph.toIsiAmrStringWithSources());
                    System.err.println(newpasSGraph.toIsiAmrStringWithSources());
                    SGraphDrawer.draw(pasSGraph, "original");
                    SGraphDrawer.draw(newpasSGraph,"modified");
                    throw new Exception("Difference in PAS");
                }
                
                newAmDM.add(dmDep);
                newAmPAS.add(pasDep);
                newAmPSD.add(psdDep);
            }
        }

        AmConllSentence.write(new FileWriter(cli.outputPath+"/dm.amconll"), newAmDM);
        AmConllSentence.write(new FileWriter(cli.outputPath+"/pas.amconll"), newAmPAS);
        AmConllSentence.write(new FileWriter(cli.outputPath+"/psd.amconll"), newAmPSD);
        
        System.out.println("Negations:");
        System.out.println(treeModifier.negations);
        System.out.println("Fixed in PSD:");
        System.out.println(treeModifier.negationsFixedPSD);
        System.out.println("Fixed in PAS:");
        System.out.println(treeModifier.negationsFixedPAS);


    }
    
    /**
     * Takes an s-graph in which node names and labels are encoded into the labels and strips off the node names
     * and only keeps the alignment
     * @param sg 
     */
    private static void onlyIndicesAsLabels(SGraph sg){
         for (String nodeName : sg.getAllNodeNames()) {
            Pair<Integer, Pair<String, String>> infos = decodeNode(sg.getNode(nodeName));
            sg.getNode(nodeName).setLabel(Integer.toString(infos.left));
        }
    }

    public static void fixDeterminer(AmConllSentence psdDep, AmConllSentence dmDep, AmConllSentence pasDep) throws ParseException{
        // Determiners for PSD
        // we find all words that are determiners (have DT POS tag)
        // and that are ignored in the PSD graph (have incoming IGNOREn edge).
        // Then we change the PSD entry to have an empty modifier graph,
        // and attach it to the head it has in DM.
        int index = 0;
        for (AmConllEntry word : psdDep){
            if (word.getPos().equals("DT") && word.getEdgeLabel().equals("IGNORE")){
                // System.err.println(index);
                // System.err.println(dmDep.getParent(index));
                if (dmDep.getParent(index) == null) continue; // if DM ignored determiner as well, skip this
                int head = dmDep.getParent(index).getId();// index is 0-based, head is 1-based
                word.setHead(head);
                word.setEdgeLabel("MOD_det");
                word.setDelexSupertag("(u<root, det>)");// empty modifier graph: one unlabeled node with root and det source.
                word.setType(new ApplyModifyGraphAlgebra.Type("(det)")); // the type of the DelexSupertag
            }
            index++;
        }
    }
    
    
    /**
     * if we have MOD_x(oldHead, oldDependent) we can turn that into
     * APP_source(oldDependent, oldHead)
     * @param deptree
     * @param oldDependent
     * @param oldHead
     * @param source 
     */
    public void swapHead(AlignedAMDependencyTree deptree, AmConllEntry oldDependent, AmConllEntry oldHead, String source){
        if (!oldDependent.getEdgeLabel().startsWith("MOD")) throw new IllegalArgumentException("Dependent must be attached with MOD");
        
        String modifierSource = oldDependent.getEdgeLabel().split("_")[1]; 
        
        int incomingEdge = oldHead.getHead();
        String incomingLabel = oldHead.getEdgeLabel();
        
        // Manipulate the type of the dependent to contain the new source and require the type at the head
        
        Type oldHeadType = deptree.getTermTypeAt(oldHead);
        Set<String> origins = oldHeadType.getOrigins();
        Type newHeadType = oldHeadType.addSource(source);
        for (String o : origins){
            newHeadType = newHeadType.setDependency(source, o, o);
        }
        
        // Take the original s-graph, rename the x-source (corresponding to MOD_x) to the new source name
        // and move the root source to the node that we used to modify with.
        
        SGraph oldDependentFragment = new IsiAmrInputCodec().read(oldDependent.getDelexSupertag());
        String modifierNode = oldDependentFragment.getNodeForSource(modifierSource);
        
        oldDependentFragment = oldDependentFragment.renameSource(modifierSource, source);
        
        Set<String> sources = oldDependentFragment.getAllSources();
        sources.remove("root");
        oldDependentFragment = oldDependentFragment.forgetSourcesExcept(sources);
        
        oldDependentFragment.addSource("root", modifierNode);
        
        // make old head an APP dependent
        oldHead.setHead(oldDependent.getId());
        oldHead.setEdgeLabel("APP_"+source);
        
        //make old dependent the head
        oldDependent.setHead(incomingEdge);
        oldDependent.setEdgeLabel(incomingLabel);
        oldDependent.setType(newHeadType);
        oldDependent.setDelexSupertag(oldDependentFragment.toIsiAmrStringWithSources());
        
        
    }
   
    
    
    public void fixNegation(AmConllSentence psdDep, AmConllSentence dmDep, AmConllSentence pasDep) throws ParseException, AlignedAMDependencyTree.ConllParserException {
        int index = 0;
        
        SGraph desiredPSDSupertag = new IsiAmrInputCodec().read("(i<root> / --LEX--  :RHEM-of (j<mod>))");
        SGraph desiredPASSupertag = new IsiAmrInputCodec().read("(i_2<root> / --LEX--  :adj_ARG1 (i_3<mod>))");
        SGraph desiredDMSupertag = new IsiAmrInputCodec().read("(i_13<root> / --LEX--  :neg (i_12<mod>))");
        
        
        AlignedAMDependencyTree dmTree = AlignedAMDependencyTree.fromSentence(dmDep);
        
        for (AmConllEntry psdEntry : psdDep) {
            AmConllEntry dmEntry = dmDep.get(index);
            AmConllEntry pasEntry = pasDep.get(index);
            if (psdEntry.getLemma().equals("#Neg")){ // psdEntry is Negation word
                this.negations ++;
                // In DM we can be in the situation that the negation word is the head with outgoing APP_mod edge
                // or - in relative clauses - , the negation would be the depndent of the verb and we have an incoming MOD_mod edge
                
                if (dmDep.getChildren(index).isEmpty() && new IsiAmrInputCodec().read(dmEntry.getDelexSupertag()).equals(desiredDMSupertag) && dmEntry.getEdgeLabel().equals("MOD_mod")){
                    // we are probably in a relative clause
                    // so we first make DM consistent that the negation word is the head, then we can apply the transformation for PSD and PAS
                    
                    // let's make sure that we are in a relative clause and see what the clause modifies
                    AmConllEntry negatedDMverb = dmDep.getParent(index);
                    Type termTypeofNegatedDMverb = dmTree.getTermTypeAt(negatedDMverb);
                    if (negatedDMverb.getEdgeLabel().equals("MOD_s") || negatedDMverb.getEdgeLabel().equals("MOD_o")) {
                        // subject or object relative clause
                        // make negation child of what the clause modifies
                        swapHead(dmTree, dmEntry, negatedDMverb, "neg");
                    }
                    
                }
                
                // find verb or thing that is negated in DM: could be none, therefore use Optional
                // outgoing dep. edges in DM from negation: if it's an APPmod edge, its target is the negated thing
                
                Optional<AmConllEntry> potential_argument = dmDep.getChildren(index).stream().filter(child -> child.getEdgeLabel().equals("APP_mod") || child.getEdgeLabel().equals("APP_neg")).findFirst();
                if (potential_argument.isPresent()) {
                    AmConllEntry dmArgument = potential_argument.get();
                    // found DM negation
                    
                    // DM: rename mod source to neg source
                    if (new IsiAmrInputCodec().read(dmEntry.getDelexSupertag()).equals(desiredDMSupertag)) {
                        dmArgument.setEdgeLabel("APP_neg");
                        dmEntry.setDelexSupertag("(i_13<root> / --LEX--  :neg (i_12<neg>))");
                        Type negationType = Type.EMPTY_TYPE;
                        negationType = negationType.addSource("neg");
                        dmEntry.setType(negationType);
                    
                    }
                    
                    // PSD
                    //  - currently   --> argument --MOD_mod--> psdEntry (negation)
                    //  - would like:   argument <--APP_mod-- psdEntry (Negation) <--
                    //        plus changed Negation supertag (root source added at mod source place)
                    if (psdEntry.getEdgeLabel().equals("MOD_mod") &&
                            psdEntry.getHead() == dmArgument.getId()) {
                        // fix PSD
                        AlignedAMDependencyTree psdAlignedDeptree = AlignedAMDependencyTree.fromSentence(psdDep);
                        AmConllEntry psdNegated = psdDep.getParent(index); // verb or sth else

                        SGraph supertag = new IsiAmrInputCodec().read(psdEntry.getDelexSupertag());
                        
                        if (desiredPSDSupertag.equals(supertag)) {
                            // only change if negated element doesn't have a mod source
                            try {
                                // take term type of negated element, add neg source and create dependencies such that the requirement 
                                // at the neg source is the type of the negated element.
                                swapHead(psdAlignedDeptree, psdEntry, psdNegated, "neg");
                                negationsFixedPSD++;
                             } catch (IllegalArgumentException ex) { // introduces a cycle by adding the mod source and the dependencies
                             }

                        } else { //PSD has unexpected supertag, no case in dev data.

                        }


                    } else { //PSD doesn't have MOD_mod edge in right place:

                        
                    }

                    // PAS
                    //  - currently   --> argument --MOD_mod--> pasEntry (negation)
                    //  - would like:   argument <--APP_mod-- pasEntry (Negation) <--
                    //        plus changed Negation supertag (root source added at mod source place)
                    
                    if (pasEntry.getEdgeLabel().equals("MOD_mod") &&
                            pasEntry.getHead() == dmArgument.getId()) {
                        // fix PAS
                        AlignedAMDependencyTree pasAlignedDeptree = AlignedAMDependencyTree.fromSentence(pasDep);
                        AmConllEntry pasNegated = pasDep.getParent(index); // verb or sth else

                        SGraph supertag = new IsiAmrInputCodec().read(pasEntry.getDelexSupertag());
                        if (desiredPASSupertag.equals(supertag)) {
                            try {
                                swapHead(pasAlignedDeptree, pasEntry, pasNegated, "neg");
                                
                                negationsFixedPAS++;
                            } catch (IllegalArgumentException ex) { // introduces a cycle by adding the mod source and the dependencies
                            }
                            
                        } else { //PAS uses unexpected supertag, no cases in gold dev.
                          
                        }


                    } else { //PAS doesn't have MOD_mod edge in expected place:

                    }
                    
                    // DEBUG TODO maybe look at what is actually negated this way? only verbs?
                } // found negated argument 
                else {
                   // System.err.println(dmDep.getId());
                   // System.err.println(dmDep);
                }

            } // found #Neg lemma
            index++;
        } // for psdEntry
    }


}
