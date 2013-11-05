/*
 *  File:    Test_0_HashChecksumTest.java
 *  Created: 3-giu-2011
 */
package eu.semlib.annotationserver.tests.restapis;

import eu.semlibproject.annotationserver.managers.UtilsManager;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 * Some tests for UtilsManager class
 * 
 * @author Michele Nucci
 */
public class Test_00_UtilsManagerTest {
    
    public Test_00_UtilsManagerTest() {
    }

    
    @Before
    public void setUp() {
        System.out.println("Start testing UtilsManager...");
    }
    

    @Test
    public void testMD5() {
        try {
            String md5Test = UtilsManager.getInstance().MD5("My Test String");
            assertNotNull("MD5 HASH is null", md5Test);
            System.out.println("MD5 HASH (len: " + md5Test.length() + "): " + md5Test);
        } catch (Exception ex) {
            fail("Unable to compute the MD5 hash of a String");
        }
    }

    @Test
    public void testSHA1() {
        try {
            String sha1Test = UtilsManager.getInstance().SHA1("My Test String");
            assertNotNull("SHA1 HASH is null", sha1Test);
            System.out.println("SHA1 HASH (len: " + sha1Test.length() + "): " + sha1Test);
        } catch (Exception ex) {
            fail("Unable to compute the SHA1 hash of a String");
        }
    }

    @Test
    public void testCRC32() {
        try {
            String crc32Test = UtilsManager.getInstance().CRC32("My Test Strin");
            assertNotNull("CRC32 HASH is null", crc32Test);
            System.out.println("CRC32 HASH (len: " + crc32Test.length() + "): " + crc32Test);
        } catch (Exception ex) {
            fail("Unable to compute the CRC32 hash of a String");
        }
    }
}
