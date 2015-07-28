package fr.cea.ig.grools.server;/*
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


import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import fr.cea.ig.database.pkgdb.GoMrCpd;
import fr.cea.ig.database.pkgdb.Organism;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.websocket.Session;
import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

import fr.cea.ig.grools.Grools;
import fr.cea.ig.grools.biology.BioPrediction;
import fr.cea.ig.grools.biology.BioPredictionBuilder;
import fr.cea.ig.grools.model.PriorKnowledge;
import fr.cea.ig.grools.server.common.BiMap;
import fr.cea.ig.grools.server.common.BidirectionalMap;
import fr.cea.ig.grools.server.common.Command;
import fr.cea.ig.grools.server.service.AvailableTagsEncoder;
import fr.cea.ig.grools.server.service.UserDataDecoder;

import  fr.cea.ig.grools.obo.OboIntegrator;

import org.apache.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

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

    private static String getId(final String prefix, final PriorKnowledge knowledge, final BiMap<String, PriorKnowledge> ids, final Map<String, String> dotIdLabel){
        boolean isRunning   = true;
        final String oriId  = prefix + "_" + knowledge.getId().replaceAll("[\\.\\s\\-\\+]", "_");
        String  newId       = oriId;
        int     copy        = 1;
        while( isRunning ){
            if( ids.containsKey(newId) && ids.get(newId) != knowledge ) {
                newId = oriId + String.valueOf( copy );
                copy++;
            }
            else{
                ids.put(newId, knowledge);
                isRunning = false;
            }
        }
        dotIdLabel.put(newId,knowledge.getId());
        return  newId;
    }

    private void generateId(final String graphName, final List<PriorKnowledge> knowledgeList, final BiMap<String, PriorKnowledge> ids, final Map<String, String> dotIdLabel){
        for( PriorKnowledge knowledge : knowledgeList) {
            getId("", knowledge, ids, dotIdLabel);
        }
    }

    private String writeDotFile(final String graphName, final BiMap<String, PriorKnowledge> ids, final Map<String, String> dotIdLabel) throws Exception {
        final String    dotFilename         = Paths.get(CONTEXT_PATH, graphName + ".dot").toString();
        final DotFile   dotFile             = new DotFile(graphName, dotFilename);
        //final Map<String,String> id_label   = new HashMap<>();
        String          color               = "Black";
        String          id;
        PriorKnowledge knowledge;

        for( final Map.Entry<String,PriorKnowledge> entry : ids.entrySet()){
            knowledge   = entry.getValue();
            id          = entry.getKey();
            switch (knowledge.getConclusion()){
                case UNCONFIRMED_PRESENCE:
                case UNCONFIRMED_ABSENCE:   color = "PaleTurquoise"; break;
                case CONFIRMED_ABSENCE:
                case CONFIRMED_PRESENCE:    color = "PaleGreen "; break;
                case UNEXPECTED:
                case UNEXPECTED_ABSENCE:
                case UNEXPECTED_PRESENCE:   color = "MistyRose "; break;
                case CONTRADICTORY:
                case CONTRADICTORY_PRESENCE:
                case CONTRADICTORY_ABSENCE: color = "LavenderBlush"; break;
                default:                    color = "GhostWhite"; break;
            }
            switch ( knowledge.getNodeType() ){
                case OR: dotFile.addNode(id, color, "octagon", dotIdLabel.get(id)); break;
                case AND:
                default: dotFile.addNode(id, color, "", dotIdLabel.get(id) ); break;
            }
            BiMap<PriorKnowledge,String> idsInv = ids.inverse();
            for( final PriorKnowledge parent: knowledge.getPartOf() ) {
                final String parentId = idsInv.get(parent);
                dotFile.linkNode( parentId, id );
            }
        }
        dotFile.close();
        dotToSvg( graphName, dotFile);
        return dotFilename;
    }

    private void dotToSvg( final String graphName, final DotFile dotFile ) throws Exception {
        final String outFile = Paths.get(CONTEXT_PATH, graphName + ".svg").toString();
        Command.run("dot", Arrays.asList("-Tsvg", "-o" + outFile, dotFile.getAbsolutePath()));
    }

    private List<String[]> addNodeEvent( final String graphName, final Map<String, PriorKnowledge> ids ) throws IOException {
        String         color       = "";
        List<String[]> result      = new ArrayList<String[]>();
        PriorKnowledge knowledge;
        for( final Map.Entry<String,PriorKnowledge> entry : ids.entrySet()){
            knowledge = entry.getValue();
            switch (knowledge.getConclusion()){
                case CONFIRMED_ABSENCE:
                case CONFIRMED_PRESENCE:    color = "green"; break;
                case UNCONFIRMED_PRESENCE:
                case UNCONFIRMED_ABSENCE:   color = "Chartreuse"; break;
                default:                    color = "LightPink"; break;
            }
            result.add(new String[]{entry.getKey(), knowledge.getConclusion().toString(), color});
        }
        return result;
    }

    private List<String[]> reasonning( final String species, final String strain, final String graphName, final String contextPath){
        List<String[]> svgEventNode = null;
        TypedQuery<Organism> organismsQuery = pkgdbManager.createQuery("SELECT o FROM Organism o WHERE o.OName = :species AND o.OStrain = :strains", Organism.class)
                                                          .setParameter("species", species)
                                                          .setParameter("strains", strain);
        final List<Organism> organisms = organismsQuery.getResultList();

        // check if organism is known and match strictly one
        if( organisms.size() != 1 )
            LOG.warn("query species id using species: "+species+" strains: "+strain+" give "+Integer.toString(organisms.size()) +" result(s)");
        else{

            TypedQuery<GoMrCpd> metacycQuery = pkgdbManager.createQuery(
                                "SELECT mr  FROM Organism o, Replicon r, Sequence s, GenomicObject go, GoMrCpd mr"          +
                                        " WHERE o.OId = :id and r.OId = o.OId and s.RId = r.RId "                           +
                                        "and go.SId = s.SId and go.goUpdate = 'current' and s.SStatus = 'inProduction'"     +
                                        "and mr.OId = o.OId and mr.goId = go.goId "                                         +
                                        "and go.goUpdate ='current' and s.SStatus = 'inProduction' and mr.goId = go.goId"    +
                                        " GROUP BY mr.mrId", GoMrCpd.class
                                    )
                                    .setParameter("id", organisms.get(0).getOId());

            List<GoMrCpd> results = metacycQuery.getResultList();
            Grools grools = new Grools();
            OboIntegrator obo = new OboIntegrator(grools);
            try {
                obo.useDefault();
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }

            for( final GoMrCpd item: results){
                final BioPrediction prediction = new BioPredictionBuilder().setId(String.valueOf(item.getGomrcId()))
                                                                           .setKnowledgeId(item.getMrId())
                                                                           .setName(item.getMrId())
                                                                           .setSource("Metacyc")
                                                                           .create();
                grools.insert(prediction);
//                  LOG.info("Prediction '"+prediction.getName()+"' inserted");
            }
            grools.fireAllRules();

            List<PriorKnowledge> knowledges = new ArrayList<>();
            for( final Object obj : grools.getKieSession().getObjects()){
                if( obj instanceof PriorKnowledge )
                    knowledges.add((PriorKnowledge) obj);
            }
            final BiMap<String, PriorKnowledge> ids         = new BidirectionalMap<String, PriorKnowledge>();
            final Map<String,String>            dotIdLabel  = new HashMap<>();
            generateId(graphName, knowledges, ids, dotIdLabel);
            String    dotFilename;
            try {
                dotFilename = writeDotFile( graphName, ids, dotIdLabel);
                svgEventNode  = addNodeEvent(graphName, ids);
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
            LOG.info("grools mode ended");
        }
        return svgEventNode;
    }

    @OnMessage
    public AvailableTags onMessage(final UserData msg, Session s){
        LOG.info(msg);
        String                      svg             = "";
        List<String[]>              svgEventNode    = null;
        String[]                    species         = {};
        Map<String,List<String>>    strains         = new HashMap<>();
        List<Organism>              organisms       = null;
        if ( msg.getSpeciesSelected() && msg.getStrainsSelected() ){
            LOG.info("Starting reasonning mode");
            final String graphName = msg.getSpecies().replace(" ", "_")+  "_" + msg.getStrains();
            species = new String[]{msg.getSpecies()};
            strains.put( msg.getSpecies(), Arrays.asList(new String[]{msg.getStrains()}));
            svgEventNode = reasonning( msg.getSpecies(), msg.getStrains(), graphName, CONTEXT_PATH);
            Gson gson = new Gson();
            try {
                FileWriter writer = new FileWriter( Paths.get(CONTEXT_PATH, graphName + ".json").toString() );
                writer.write( gson.toJson(svgEventNode) );
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            svg = Paths.get( PATHWAY_SVG_PATH, graphName + ".svg").toString();
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
        final AvailableTags availableTags = new AvailableTags(svg, svgEventNode,species,strains);
        LOG.info(availableTags);
        return availableTags;
    }

}
