/*
 *  File:    Test_1_ClearingRepository.java
 *  Created: 10-giu-2011
 */
package eu.semlib.annotationserver.tests.restapis;

import eu.semlib.annotationserver.tests.SesameRepositoryTestHelper;
import eu.semlib.annotationserver.tests.TestHelper;
import eu.semlibproject.annotationserver.managers.RepositoryManager;
import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Michele Nucci
 */
public class Test_01_RepositoryInitializationTest {
    
    public static String serverAddress  = null;
    public static String userDataAsTrig = null;
    
    public Test_01_RepositoryInitializationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("Clearing the main repository...");
        SesameRepositoryTestHelper.getInstance().clearRepository();
        
        System.out.println("Getting repository address...");
        serverAddress = TestHelper.getServerAddress();
        assertNotNull("Server address is null!", serverAddress);
        System.out.println("Server address: " + serverAddress);
        
        // Reade Fixture data
        userDataAsTrig = TestHelper.readFixture("test/fixture_users_1.trig");
        if (StringUtils.isBlank(userDataAsTrig)) {
            fail("Problem loading fixture for users!");
        }
    }

    
     @Test
     public void test_0_clearRepository() {
        try {
            boolean result = SesameRepositoryTestHelper.getInstance().clearRepository();
            if (!result) {
                fail("Unable to clear the main repository!");
            } else {
                System.out.println("Main repository empty.");
            }
        } catch (Exception ex) {
            fail("Unable to clear the main repository!");
        }
     }
     
     
     @Test
     public void test_1_clearRDBMSTable() {         
         System.out.println("Setting RDBMS parameters...");
         if (TestHelper.setRDBMSConnection()) {
             System.out.println("RDBMS parameters set!");
         } else {
             System.out.println("Problem setting RDBMS parameters.");
         }

         System.out.println("Clear all RDBMS tables...");
         boolean result = RepositoryManager.getInstance().clearAllDataStorageTables();
         if (result) {
             System.out.println("RDBMS tables are empty.");
         } else {
             fail("Unable to clear RDBMS tables");
         }
     }
     
     
     @Test
     public void test_2_importUserData() {
         System.out.println("Importing fixture data for user into Sesame...");
         try {
             SesameRepositoryTestHelper.getInstance().importTrigData(userDataAsTrig);
             System.out.println("User data imported!");
         } catch (Exception e) {
             System.out.println("Problem importing user fixture data!");
             fail("Unable to import user fixture data!");
         }
     }
     
     
     
}
