/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.semlib.annotationserver.tests.restapis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Michele Nucci
 */
public class Test_13_RegularExpressionTest {

    public Test_13_RegularExpressionTest() {        
    }
    
    @Test
    public void test_1_baseRegExTest() {
        
        String query = "PREFIX  data:  <http://example.org/foaf/>\n" +
                       "PREFIX  foaf:  <http://xmlns.com/foaf/0.1/>\n" +
                       "PREFIX  rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n\n" +
                       "SELECT ?mbox ?nick ?ppd\n" +
                       "FROM <http://abc>\n" +
                       "FROM <http://bce>\n" +
                       "FROM NAMED <http://example.org/foaf/aliceFoaf>\n" +
                       "FROM NAMED <http://example.org/foaf/bobFoaf>\n" +
                       "WHERE\n" +
                       "{" +
                       "   GRAPH data:aliceFoaf\n" +
                       "   {\n" +
                       "        ?alice foaf:mbox <mailto:alice@work.example> ;\n" +
                       "        foaf:knows ?whom .\n" +
                       "        ?whom  foaf:mbox ?mbox ;\n" +
                       "        rdfs:seeAlso ?ppd .\n" +
                       "        ?ppd  a foaf:PersonalProfileDocument .\n" +
                       "   } .\n" +
                       "   GRAPH ?ppd\n" +
                       "   {\n" +
                       "      ?w foaf:mbox ?mbox ;\n" +
                       "      foaf:nick ?nick\n" +
                       "   }\n" +
                       "}";
        
        String queryWithNoFrom           = "SELECT ?s ?p ?o WHERE {?s ?p ?o}";
        String queryWithOnlyOneFrom      = "SELECT ?s ?p ?o FROM <ABCDE> WHERE {?s ?p ?o}";
        String queryWithOnlyOneFromN     = "SELECT ?s ?p ?o FROM NAMED <ABCDE> WHERE {?s ?p ?o}";
        
        System.out.println("Query input:");
        System.out.println(query);
        
        int startIndex = -1;
        int endIndex   = -1;
        
        Pattern regExPattern  = Pattern.compile("(FROM\\s*<.+>\\s*|FROM\\s*NAMED\\s*<.+>\\s*)+");        
        
        Matcher matcher = regExPattern.matcher(query);
                
        while (matcher.find()) {
            if (startIndex == -1) {
                startIndex = matcher.start();
            }
            
            endIndex = matcher.end();
            
            for (int i = 0; i < matcher.groupCount(); i++) {                
                System.out.println("Group " + i + ":\n" + matcher.group(i) + "\n");
            }
        }
        
        if (startIndex == -1 || endIndex == -1) {
            fail("Incorrect indexes. RegEx test failed!");
        }
        
        System.out.println("Start Matching Index: " + startIndex);
        System.out.println("End Matching Index:   " + endIndex);

        String replacedQuery = matcher.replaceAll("");        
        
        System.out.println("\nNew query without FROM and FROM name: ");
        System.out.println(replacedQuery);
        
        // Now the query should be cleared without any FROM and FROM NAMED...recheck it
        Matcher newMatcher = regExPattern.matcher(replacedQuery);
        assertFalse("String still contains some FROM or FROM NAMED!", newMatcher.find());
        
        // Now construct the string with the custom FROM
        StringBuilder finalQuery = new StringBuilder(replacedQuery);
        finalQuery.insert(startIndex, " FROM <NEW> ");        
        System.out.println(finalQuery);
        
        // Other checks
        matcher = regExPattern.matcher(queryWithNoFrom);
        assertFalse("RegEx incorrect!", matcher.find());
        System.out.println("\nQuery with no FROM:");
        System.out.println(queryWithNoFrom);
        
        matcher = regExPattern.matcher(queryWithOnlyOneFrom);
        assertTrue("From not found!", matcher.find());
        
        System.out.println("String without From:");
        
        String strWithNoFrom = matcher.replaceAll("");
        System.out.println(strWithNoFrom);
        
        matcher = regExPattern.matcher(queryWithOnlyOneFromN);
        assertTrue("FROM NAMED not found!", matcher.find());
        
        String strWithNoFromNamed = matcher.replaceAll("");
        System.out.println("String without FROM NAMED:");
        System.out.println(strWithNoFromNamed);
        
        if (!queryWithNoFrom.equals(strWithNoFrom) && !queryWithNoFrom.equals(strWithNoFromNamed)) {
            fail("Query string not correct!");
        }
        
        int indexOfWhere = queryWithNoFrom.toLowerCase().indexOf("where");
        StringBuilder strBuilder = new StringBuilder(queryWithNoFrom);
        strBuilder.insert(indexOfWhere, "FROM <mynotebook> ");
        System.out.println("New processed string:\n" + strBuilder.toString());
        
        String query_1 = "  CONSTRuCT {?s ?p ?o} WHERE {?s ?p ?o}";
        Pattern startPattern = Pattern.compile("^\\s*CONSTRUCT.", Pattern.CASE_INSENSITIVE);
        Matcher nMatcher = startPattern.matcher(query_1);
        
        boolean found = nMatcher.find();
        assertTrue("RegEx for CONSTRUCT query is incorrect!", found);                        
    }
}
