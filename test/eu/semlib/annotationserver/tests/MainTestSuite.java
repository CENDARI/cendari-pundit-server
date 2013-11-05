/*
 *  File:    MainTestSuite.java
 *  Created: 30-mag-2011
 */
package eu.semlib.annotationserver.tests;

import eu.semlib.annotationserver.tests.restapis.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * This is the main tests suite for the Annotation Server
 * 
 * @author Michele Nucci
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    Test_00_UtilsManagerTest.class,
    Test_01_RepositoryInitializationTest.class,
    Test_02_NotebooksAPITest.class,
    Test_03_GetAllTriplesTest.class,
    Test_04_GetAnnotationDataTest.class,
    Test_05_JSONPTest.class,
    Test_06_DeletionTest.class,
    Test_07_AnnotationAPITest.class,
    Test_08_DataEncodingTest.class,
    Test_09_UserManagementTest.class,
    Test_10_JSONConverterTest.class,
    Test_11_RDBMSStorageTest.class,
    Test_12_OpenAPITest.class,
    Test_13_RegularExpressionTest.class,
    Test_14_SPARQLEndPointTest.class,
    //Test_15_SendEmailTest.class
})

public class MainTestSuite {

    @BeforeClass
    public static void setUpClass() throws Exception {        
    }

    @AfterClass
    public static void tearDownClass() throws Exception {        
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Starting test suite for Annotation Server...");
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Ending test suite for Annotation Server...");
    }
    
}
