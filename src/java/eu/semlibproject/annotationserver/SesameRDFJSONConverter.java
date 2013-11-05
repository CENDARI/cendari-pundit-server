/*
 * Copyright (c) 2013 Net7 SRL, <http://www.netseven.it/>
 * 
 * This file is part of Pundit: Annonation Server.
 * 
 * Pundit: Annonation Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Pundit: Annonation Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Pundit: Annonation Server.  If not, see <http ://www.gnu.org/licenses/>.
 *
 * See LICENSE.TXT or visit <http://thepund.it> for the full text of the license.
 */

package eu.semlibproject.annotationserver;

import eu.semlibproject.annotationserver.repository.OntologyHelper;
import eu.semlibproject.annotationserver.repository.RepositoryException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openrdf.model.*;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

/**
 * A generic class that convert a Sesame Graph to JSON
 * 
 * @author Michele Nucci
 */
public class SesameRDFJSONConverter {
    
    private static SesameRDFJSONConverter instance = null;
    
    private SesameRDFJSONConverter() { }
    
    public static SesameRDFJSONConverter getInstance() {
        
        if (instance == null) {
            instance = new SesameRDFJSONConverter();
        }
        
        return instance;
    }
    
    
    public String RDFToJson(String rdfContent) throws RepositoryException {
        try {
            // we will use a temporary in memory repository to obtain correct
            // rdf data to be converted in JSON/RDF
            SailRepository tempRepository = new SailRepository(new MemoryStore());
            tempRepository.initialize();
                        
            RepositoryConnection tempRepositoryConnection = tempRepository.getConnection();
            tempRepositoryConnection.add(new StringReader(rdfContent), OntologyHelper.SEMLIB_NAMESPACE, RDFFormat.RDFXML);
            
            RepositoryResult<Statement> statements = tempRepositoryConnection.getStatements(null, null, null, false);
                        
            Graph newGraph = new GraphImpl();
            while(statements.hasNext()) {
                newGraph.add(statements.next());
            }
            
            tempRepositoryConnection.clear();
            tempRepositoryConnection.close();
            tempRepository.shutDown();
            
            return this.RDFGraphToJson(newGraph);
            
        } catch (Exception ex) {
            throw new RepositoryException(ex.toString());
        }
    }
    
    
    public String RDFGraphToJson(Graph graph) {
        JSONObject result = new JSONObject();
        try {
            Set<Resource> subjects = new HashSet<Resource>();
            for (Statement s1 : graph) {
                subjects.add(s1.getSubject());
            }
            for (Resource subject : subjects) {
                JSONObject predicateObj = new JSONObject();
                Set<URI> predicates = new HashSet<URI>();
                Iterator<Statement> s2 = graph.match(subject, null, null);
                while (s2.hasNext()) {
                    predicates.add(s2.next().getPredicate());
                }
                for (URI predicate : predicates) {
                    JSONArray valueArray = new JSONArray();
                    Iterator<Statement> stmnts = graph.match(subject, predicate, null);
                    while (stmnts.hasNext()) {
                        Value v = stmnts.next().getObject();
                        JSONObject valueObj = new JSONObject();
                        valueObj.put("value", v.stringValue());
                        if (v instanceof Literal) {
                            valueObj.put("type", "literal");
                            Literal l = (Literal) v;
                            if (l.getLanguage() != null) {
                                valueObj.put("lang", l.getLanguage());
                            } else if (l.getDatatype() != null) {
                                valueObj.put("datatype", l.getDatatype().stringValue());
                            }
                        } else if (v instanceof BNode) {
                            valueObj.put("type", "bnode");
                        } else if (v instanceof URI) {
                            valueObj.put("type", "uri");
                        }
                        valueArray.put(valueObj);
                    }
                    
                    System.out.println(predicate.stringValue());
                    predicateObj.put(predicate.stringValue(), valueArray);
                    
                }
                result.put(subject.stringValue(), predicateObj);
            }
            return result.toString(2);
        } catch (JSONException e) {
            return null;
        }
        
    }
    
}
