/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.semlib.annotationserver.tests.restapis;

import eu.semlib.annotationserver.tests.TestHelper;
import eu.semlibproject.annotationserver.JSONRDFConverter;
import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;


/**
 *
 * @author Michele Nucci
 */
public class Test_10_JSONConverterTest {
    
    private static String annotationJSON;
    
    public Test_10_JSONConverterTest() {
        
    }
    
    @Before
    public void setUp() {
        System.out.println("Start testing JSON-2-RDF Converter...");
        
        System.out.println("Loading fixtures...");
        
        try {
            // Read annotation fixture JSON format
            annotationJSON = TestHelper.readFixture("test/fixture_baddata_converter.json");
            if (StringUtils.isBlank(annotationJSON)) {
                fail("Problem loading fixture: fixture_baddata_converter.json");
            }
        } catch (Exception ex) {
            fail("Problem loading fixture: fixture_baddata_converter.json");
        }
    }
    
    
    @Test
    public void test_1_textJSON2RDFWithBadData() {        
        JSONRDFConverter converter = JSONRDFConverter.getInstance();
        
        String turtleData = converter.ConvertJSONToTurtle(annotationJSON);
        System.out.println("Converted Data: " + turtleData);
        assertNull("RDF Converted Data is not null!", turtleData);
    }
    
}
