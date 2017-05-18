package fr.cea.ig.grools.server;
/*
 * Copyright LABGeM 15/06/15
 *
 * author: Jonathan MERCIER
 *
 * This software is a computer program whose purpose is to annotate a complete genome.
 *
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */


import fr.cea.ig.database.pkgdb.GoMrCpd;
import fr.cea.ig.database.pkgdb.Organism;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.websocket.Session;
import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

import fr.cea.ig.grools.fact.Concept;
import fr.cea.ig.grools.fact.Observation;
import fr.cea.ig.grools.fact.ObservationImpl;
import fr.cea.ig.grools.fact.ObservationType;
import fr.cea.ig.grools.fact.PriorKnowledge;
import fr.cea.ig.grools.fact.Relation;
import fr.cea.ig.grools.fact.RelationImpl;
import fr.cea.ig.grools.genome_properties.GenomePropertiesIntegrator;
import fr.cea.ig.grools.logic.TruthValue;
import fr.cea.ig.grools.reasoner.Integrator;
import fr.cea.ig.grools.reasoner.Mode;
import fr.cea.ig.grools.reasoner.Reasoner;
import fr.cea.ig.grools.reasoner.ReasonerImpl;
import fr.cea.ig.grools.reporter.Reporter;
import fr.cea.ig.grools.server.common.MetabolicNetworkModel;
import fr.cea.ig.grools.server.service.AvailableTagsEncoder;
import fr.cea.ig.grools.server.service.UserDataDecoder;

import  fr.cea.ig.grools.obo.UniPathwayIntegrator;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
/*
 * @startuml
 * class OrganismsEndPoint{
 * }
 * @enduml
 */
@SuppressWarnings("unused")
@Stateless
@ServerEndpoint(value="/organisms",  decoders=UserDataDecoder.class, encoders=AvailableTagsEncoder.class)
public class OrganismsEndPoint {
    private static final Logger LOG                 = Logger.getLogger(OrganismsEndPoint.class);
    private static final String PATHWAY_SVG_PATH    = System.getProperty("file.separator") + "pathway_svg";
    private static final String CONTEXT_PATH        = System.getProperty("jboss.server.temp.dir")
                                                                + System.getProperty("file.separator")
                                                                + "grools_server"
                                                                + PATHWAY_SVG_PATH;

    @PersistenceContext(unitName="PkGDB")
    protected EntityManager pkgdbManager;

    private static ObservationType observationTypeStrToObservationType( final String observationTypeStr ) {
        ObservationType obsType = null;
        try {
            obsType = ObservationType.valueOf( observationTypeStr );
        }
        catch( IllegalArgumentException e ) {
            LOG.error( "Error: the record " + observationTypeStr + " is not an accepted value" );
            LOG.error( "Accepted values: " + Arrays.stream( ObservationType.values( ) )
                    .map( i -> i.toString( ) )
                    .collect( Collectors.joining( ", " ) ) );
        }
        return obsType;
    }

    private static TruthValue isPresentStrToTruthValue( final String isPresentStr ) {
        TruthValue isPresent;

        switch( isPresentStr ) {
            case "t":
            case "T":
            case "True":
            case "TRUE":
            case "true":
                isPresent = TruthValue.t;
                break;
            case "f":
            case "F":
            case "False":
            case "FALSE":
            case "false":
                isPresent = TruthValue.f;
                break;
            default:
                isPresent = TruthValue.t;
        }
        return isPresent;
    }

    private static Set<PriorKnowledge> evidenceForToPriorKnowledge( final String evidenceFor, final String source, final Integrator integrator ) {
        final Set<PriorKnowledge> pks = integrator.getPriorKnowledgeRelatedToObservationNamed( source, evidenceFor );
        if( pks == null || pks.size( ) == 0 )
            LOG.warn( "Unknown prior-knowledge " + evidenceFor );
        return pks;
    }

    private static Observation toObservation( final String name, final String label, final String source, final String description, TruthValue isPresent, final ObservationType type ) {
        return ObservationImpl.builder( )
                .name( name )
                .label( label )
                .source( source )
                .description( description )
                .truthValue( isPresent )
                .type( type )
                .build( );
    }

    private static Integrator doUnipathwayIntegration(final Reasoner grools, Organism organism, EntityManager pkgdbManager){
        UniPathwayIntegrator obo = null;
        try {
            obo = new UniPathwayIntegrator(grools);
        } catch (Exception e) {
            e.printStackTrace();
        }

        TypedQuery<GoMrCpd> metacycQuery = pkgdbManager.createQuery(
                "SELECT mr  FROM Organism o, Replicon r, Sequence s, GenomicObject go, GoMrCpd mr"  +
                        " WHERE o.OId = :id and r.OId = o.OId and s.RId = r.RId "                           +
                        "and go.SId = s.SId and go.goUpdate = 'current' and s.SStatus = 'inProduction'"     +
                        "and mr.OId = o.OId and mr.goId = go.goId "                                         +
                        "and go.goUpdate ='current' and s.SStatus = 'inProduction' and mr.goId = go.goId"   +
                        " GROUP BY mr.mrId", GoMrCpd.class
        ).setParameter("id", organism.getOId());

        final List<GoMrCpd> results     = metacycQuery.getResultList();

        results.stream()
                .map(item -> new ObservationImpl(String.valueOf(item.getGomrcId()), "Metacyc", item.getMrId(), "Prediction with Metacyc Id", ObservationType.COMPUTATION, TruthValue.t ) )
                .forEach( prediction ->  grools.insert(prediction));
        return obo;
    }

    private static Integrator doGenomePropertiesIntegration(final Reasoner grools){
        GenomePropertiesIntegrator gpi = null;
        try {
            gpi = new GenomePropertiesIntegrator(grools);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gpi;
    }

    private static Integrator doMetaCycIntegration(final Reasoner grools){
        final Integrator mi = null; // TODO
        /*MetaCycIntegrator mi = null;
        try {
            mi = new MetaCycIntegrator(grools);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        return mi;
    }

    private static List<PriorKnowledge> doUnipathwayReport(final Reasoner grools, final Set<PriorKnowledge> expectedPriorKnowledge  ){
        List<PriorKnowledge> tops = null;
        final Set<PriorKnowledge> tmp = grools.getRelations( )
                                              .stream( )
                                              .filter( rel -> rel.getSource( ) instanceof PriorKnowledge )
                                              .filter( rel -> rel.getTarget( ) instanceof PriorKnowledge )
                                              .filter( rel -> ( ! rel.getSource( ).getName( ).startsWith( "UPA" ) ) )
                                              .filter( rel -> rel.getTarget( ).getName( ).startsWith( "UPA" ) )
                                              .map( rel -> ( PriorKnowledge ) rel.getTarget( ) )
                                              .collect( Collectors.toSet( ) );
        // do not report expectation over UCR UPC UER ULS from global view only UPA
        tmp.addAll( expectedPriorKnowledge.stream()
                                          .filter( pk -> pk.getName().startsWith( "UPA" ) )
                                          .collect( Collectors.toSet( ) ) );
        tops = new ArrayList<>( tmp );
        tops.sort(Comparator.comparing(Concept::getName));
        return tops;
    }

    private static List<PriorKnowledge> doGenomeReport(final Reasoner grools, final Set<PriorKnowledge> expectedPriorKnowledge  ){
        List<PriorKnowledge> tops = null;
        return tops;
    }

    private static List<PriorKnowledge> doMetaCycReport(final Reasoner grools, final Set<PriorKnowledge> expectedPriorKnowledge  ){
        List<PriorKnowledge> tops = null;
        return tops;
    }
    private static String[][] getExpectedUnipathwayPriorKnowledge() {
        final String[][] records = {
                {"Exp_UPA00031", "UPA00031", "EXPERIMENTATION", "T", "", "Exp_UPA00031", "L-histidine biosynthesis"},
                {"Exp_UPA00035", "UPA00035", "EXPERIMENTATION", "T", "", "Exp_UPA00035", "L-tryptophan biosynthesis"},
                {"Exp_UPA00047", "UPA00047", "EXPERIMENTATION", "T", "", "Exp_UPA00047", "L-isoleucine biosynthesis"},
                {"Exp_UPA00048", "UPA00048", "EXPERIMENTATION", "T", "", "Exp_UPA00048", "L-leucine biosynthesis"},
                {"Exp_UPA00049", "UPA00049", "EXPERIMENTATION", "T", "", "Exp_UPA00049", "L-valine biosynthesis"},
                {"Exp_UPA00050", "UPA00050", "EXPERIMENTATION", "T", "", "Exp_UPA00050", "L-threonine biosynthesis"},
                {"Exp_UPA00068", "UPA00068", "EXPERIMENTATION", "T", "", "Exp_UPA00068", "L-arginine biosynthesis"},
                {"Exp_UPA00098", "UPA00098", "EXPERIMENTATION", "T", "", "Exp_UPA00098", "L-proline biosynthesis"},
                {"Exp_UPA00121", "UPA00121", "EXPERIMENTATION", "T", "", "Exp_UPA00121", "L-phenylalanine biosynthesis"},
                {"Exp_UPA00122", "UPA00122", "EXPERIMENTATION", "T", "", "Exp_UPA00122", "L-tyrosine biosynthesis"},
                {"Exp_UPA00133", "UPA00133", "EXPERIMENTATION", "T", "", "Exp_UPA00133", "L-alanine biosynthesis"},
                {"Exp_UPA00134", "UPA00134", "EXPERIMENTATION", "T", "", "Exp_UPA00134", "L-asparagine biosynthesis"},
                {"Exp_UPA00135", "UPA00135", "EXPERIMENTATION", "T", "", "Exp_UPA00135", "L-serine biosynthesis"},
                {"Exp_UPA00136", "UPA00136", "EXPERIMENTATION", "T", "", "Exp_UPA00136", "L-cysteine biosynthesis"},
                {"Exp_UPA00288", "UPA00288", "EXPERIMENTATION", "T", "", "Exp_UPA00288", "glycine biosynthesis"},
                {"Exp_UPA00404", "UPA00404", "EXPERIMENTATION", "T", "", "Exp_UPA00404", "L-lysine biosynthesis"},
                {"Exp_UPA00633", "UPA00633", "EXPERIMENTATION", "T", "", "Exp_UPA00633", "L-glutamate biosynthesis"},
                {"Exp_UPA00051", "UPA00051", "EXPERIMENTATION", "T", "", "Exp_UPA00051", "L-methionine biosynthesis via de novo pathway"},
                {"Exp_UPA01012", "UPA01012", "EXPERIMENTATION", "T", "", "Exp_UPA01012", "L-aspartate biosynthesis"},
                {"Exp_UPA01013", "UPA01013", "EXPERIMENTATION", "T", "", "Exp_UPA01013", "L-glutamine biosynthesis"}
        };
        return records;
    }

    private static String[][] getExpectedGenomePropertiesPriorKnowledge() {
        final String[][] records = null;
        return records;
    }
    private static String[][] getExpectedMetaCycPriorKnowledge() {
        final String[][] records = null;
        return records;
    }
        
    private static Set<PriorKnowledge> addExpectedPriorKnowledge(final Reasoner grools, final MetabolicNetworkModel metabolicNetworkModel, Integrator integrator){
        String[][] records = null;

        switch (metabolicNetworkModel){
            case UNIPATHWAY:        records = getExpectedUnipathwayPriorKnowledge(); break;
            case GENOME_PROPERTIES: records = getExpectedGenomePropertiesPriorKnowledge(); break;
            case METACYC:           records = getExpectedMetaCycPriorKnowledge(); break;
            default: LOG.info( "Metabolic model Not yet implemented: "+ metabolicNetworkModel);
        }

        final Set<PriorKnowledge> expectedPriorKnowledge = new HashSet<>( );
        for( final String[] line :records ) {
            final String                label       = line[5];
            final ObservationType       obsType     = observationTypeStrToObservationType(line[2]);
            final TruthValue            isPresent   = isPresentStrToTruthValue(line[3]);
            final String                source      = line[4];
            final String                name        = line[0];
            final String                description = line[6];
            final Set<PriorKnowledge>   evidenceFor = evidenceForToPriorKnowledge(line[1], source, integrator);

            final Observation o = toObservation(name, label, source, description, isPresent, obsType);
            grools.insert(o);


            if (evidenceFor != null) {
                for (final PriorKnowledge pk : evidenceFor) {
                    final Relation relation = new RelationImpl(o, pk);
                    grools.insert(relation);
                    if (o.getType() == ObservationType.CURATION || o.getType() == ObservationType.EXPERIMENTATION)
                        expectedPriorKnowledge.add(pk);
                }
            }
        }
        return expectedPriorKnowledge;
    }


    private Integrator addPrediction(final Organism organism, final String graphName, final MetabolicNetworkModel metabolicNetworkModel, final Reasoner grools, final String contextPath){
        Integrator          integrator  = null;
        switch (metabolicNetworkModel){
            case UNIPATHWAY:        integrator = doUnipathwayIntegration( grools, organism, pkgdbManager ); break;
            case GENOME_PROPERTIES: integrator = doGenomePropertiesIntegration( grools ); break;
            case METACYC:           integrator = doMetaCycIntegration( grools ); break;
            default: LOG.info( "Metabolic model Not yet implemented: "+ metabolicNetworkModel);
        }

        integrator.integration( );
        LOG.info( "Inserting observation..." );

        // TODO manage falsehood mode here see grools-application
        return integrator;
    }

    private static Reasoner doReport(final Reasoner grools, final Set<PriorKnowledge> expectedPriorKnowledge, final MetabolicNetworkModel metabolicNetworkModel, final Organism organism, final Mode mode, final String outpoutDir){

        grools.reasoning();

        List<PriorKnowledge> tops = null;

        switch (metabolicNetworkModel){
            case UNIPATHWAY:        tops = doUnipathwayReport( grools, expectedPriorKnowledge ); break;
            case GENOME_PROPERTIES: tops = doGenomeReport( grools, expectedPriorKnowledge ); break;
            case METACYC:           tops = doMetaCycReport( grools , expectedPriorKnowledge); break;
            default: LOG.info( "Metabolic model Not yet implemented: "+ metabolicNetworkModel);
        }

        Reporter reporter = null;

        try{
            reporter = new Reporter(outpoutDir, grools);
        }
        catch( Exception e ) {
            LOG.error( "while creating report into: " + outpoutDir );
        }
        return grools;
    }

    @OnMessage
    public AvailableTags onMessage( final UserData msg, final Session session ){
        LOG.info(msg);
        String                      svg             = "";
        List<String[]>              svgEventNode    = null;
        String[]                    species         = {};
        Map<String,List<String>>    strains         = new HashMap<>();
        List<Organism>              organisms       = null;
        Mode                        mode            = null;
        MetabolicNetworkModel       metabolicModel  = null;
        if ( msg.getSpeciesSelected() && msg.getStrainsSelected() ){
            LOG.info("Starting addPrediction mode");
            final String graphName = msg.getSpecies().replace(" ", "_")+  "_" + msg.getStrains();
            species         = new String[]{msg.getSpecies()};
            mode            = new Mode( msg.getMode() );
            metabolicModel  = MetabolicNetworkModel.valueOf(  msg.getMetabolicNetworkModel() ); // TODO manage case ?
            strains.put( msg.getSpecies(), Arrays.asList(new String[]{msg.getStrains()}));


            organisms = pkgdbManager.createQuery("SELECT o FROM Organism o WHERE o.OName = :species AND o.OStrain = :strains", Organism.class)
                                    .setParameter("species", species)
                                    .setParameter("strains", strains)
                                    .getResultList();

            // check if organism is known and match strictly one
            if( organisms.size() != 1 )
                LOG.warn("query species id using species: "+species+" strains: "+strains+" give "+Integer.toString(organisms.size()) +" result(s)");
            else {
                final Organism organism     = organisms.get(0);
                final String   outputDir    = CONTEXT_PATH + System.getProperty("file.separator")
                                                           + System.getProperty("file.separator") + organism.getOId()
                                                           + System.getProperty("file.separator") + metabolicModel.getName()
                                                           + System.getProperty("file.separator") + mode.getMode();// TODO: I think CONTEXT_PATH should be expanded as: CONTEXT_PATH + organisms + model + mode


                final Reasoner              grools                  = new ReasonerImpl(mode);
                final Integrator            integrator              = addPrediction(organism, graphName, metabolicModel, grools, outputDir);
                final Set<PriorKnowledge>   expectedPriorKnowledge  = addExpectedPriorKnowledge(grools, metabolicModel, integrator);
                doReport(grools, expectedPriorKnowledge, metabolicModel, organism, mode, outputDir);
            }
        }
        else {
            LOG.info("Starting query organisms");
            final String speciesPattern = msg.getSpecies() + "%";
            final String strainsPattern = msg.getStrains() + "%";
            if (msg.getSpecies().length() < 6) {
                TypedQuery<Organism> typedQuery = pkgdbManager.createQuery("SELECT o FROM Organism o WHERE o.OName LIKE  :species GROUP BY o.OName", Organism.class)
                                                              .setParameter("species", speciesPattern);

                organisms = typedQuery.getResultList();
                species = new String[organisms.size()];
                for (int i = 0; i < organisms.size(); i++)
                    species[i] = organisms.get(i).getOName();
            } else {
                Set<String> speciesSet = new HashSet<String>();
                TypedQuery<Organism> typedQuery = pkgdbManager.createQuery("SELECT o FROM Organism o WHERE o.OName LIKE  :species AND o.OStrain LIKE :strains GROUP BY o.OStrain", Organism.class)
                                                              .setParameter("species", speciesPattern)
                                                              .setParameter("strains", strainsPattern);

                organisms = typedQuery.getResultList();

                for (int i = 0; i < organisms.size(); i++) {
                    final String oname = organisms.get(i).getOName();
                    final String ostrains = organisms.get(i).getOStrain();
                    speciesSet.add(oname);
                    if (strains.containsKey(oname)) {
                        strains.get(oname).add(ostrains);
                    } else {
                        List<String> strainsList = new ArrayList<>();
                        strainsList.add(ostrains);
                        strains.put(oname, strainsList);
                    }
                }
                species = speciesSet.toArray(new String[speciesSet.size()]);
            }
        }
        final AvailableTags availableTags = new AvailableTags(species,strains);
        LOG.info(availableTags);
        return availableTags;
    }

}
