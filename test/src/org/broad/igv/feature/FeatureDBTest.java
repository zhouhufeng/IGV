/*
 * Copyright (c) 2007-2012 by The Broad Institute of MIT and Harvard.All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

package org.broad.igv.feature;

import junit.framework.AssertionFailedError;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.util.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.*;

/**
 * User: jacob
 * Date: 2011/12/15
 */
public class FeatureDBTest {


    public static final int LARGE = 500;

    private static final String CHECK_STR = "ABC";
    private static boolean reload = false;
    private static Genome genome;


    //Not a unit test
    public static void main(String[] args) {


    }


    @BeforeClass
    public static void setUpClass() throws Exception {
        genome = TestUtils.loadGenome();
        FeatureDB.setGenome(genome);
        reload = false;
    }

    @Before
    public void setUpTest() throws Exception {

        try {
            if (FeatureDB.size() == 0) {
                reload = true;
            }
        } catch (NullPointerException e) {
            reload = true;
        }
        if (reload) {
            setUpClass();
        }
    }

    @After
    public void tearDown() {
        //FeatureDB.clearFeatures();
    }

    @Test
    public void testFeaturesMap() throws Exception {
        Map<String, List<NamedFeature>> fMap = FeatureDB.getFeaturesMap(CHECK_STR);

        for (String k : fMap.keySet()) {

            assertTrue(k.startsWith(CHECK_STR));
        }

    }

    @Test
    public void testFeatureListSize() throws Exception {
        List<NamedFeature> features = FeatureDB.getFeaturesList(CHECK_STR, 3);
        assertEquals(3, features.size());

        features = FeatureDB.getFeaturesList(CHECK_STR, LARGE);
        assertTrue(features.size() < LARGE);
        int expected = 50;
        assertEquals(expected, features.size());
    }

    @Test
    public void testFeatureList() throws Exception {
        List<NamedFeature> features = FeatureDB.getFeaturesList(CHECK_STR, LARGE);
        for (NamedFeature f : features) {
            assertTrue(f.getName().startsWith(CHECK_STR));
            assertNotNull(FeatureDB.getFeature(f.getName()));
        }

    }

    /**
     * Test thread safety by trying to read the map and clear it at the same time.
     *
     * @throws Exception
     */
    @Test
    public void testThreadSafety() throws Exception {

        final Map<Integer, AssertionFailedError> map = new HashMap<Integer, AssertionFailedError>();
        List<NamedFeature> features = FeatureDB.getFeaturesList(CHECK_STR, LARGE);
        final int expected = features.size();

        Thread read = new Thread(new Runnable() {
            public void run() {
                try {
                    List<NamedFeature> features = FeatureDB.getFeaturesList(CHECK_STR, LARGE);
                    for (NamedFeature f : features) {
                        //Check for data corruption
                        assertTrue(f.getName().startsWith(CHECK_STR));
                    }
                    assertEquals(expected, features.size());
                } catch (AssertionFailedError e) {
                    map.put(0, e);
                }
            }
        });

        Thread write = new Thread(new Runnable() {
            public void run() {
                FeatureDB.clearFeatures();
            }
        });

        read.start();
        write.start();
        read.join();

        write.join();

        features = FeatureDB.getFeaturesList(CHECK_STR, LARGE);
        assertEquals(0, features.size());

        if (map.containsKey(0)) {
            AssertionFailedError e = map.get(0);
            throw e;
        }

    }

    @Test
    public void testMultiRetrieve() throws Exception {
        String checkstr = "EGFLAM";
        Map<String, List<NamedFeature>> fMap = FeatureDB.getFeaturesMap(checkstr);
        List<NamedFeature> data = fMap.get(checkstr);
        assertEquals(4, data.size());
    }

    @Test
    public void testMultipleEntries() throws Exception {
        String checkstr = "EG";
        Map<String, List<NamedFeature>> fMap = FeatureDB.getFeaturesMap(checkstr);
        for (String k : fMap.keySet()) {
            List<NamedFeature> data = fMap.get(k);
            //System.out.println("key " + k + " has " + data.size());
            for (int ii = 0; ii < data.size() - 1; ii++) {
                NamedFeature feat1 = data.get(ii);
                NamedFeature feat2 = data.get(ii + 1);
                int len1 = feat1.getEnd() - feat1.getStart();
                int len2 = feat2.getEnd() - feat2.getStart();
                //We require either the start or end to be different,
                //which is the same as start or length
                assertTrue("Coordinates are the same for " + k,
                        len2 != len1 || feat1.getStart() != feat2.getStart());

                assertTrue("Data for key " + k + " not sorted", len1 >= len2);
            }
        }
    }


    @Test
    public void testMutationSearch() throws Exception {

        String name = "EGFR";
        // EGFR starts with proteins MRPSG
        char[] symbols = new char[]{'M', 'R', 'P', 'S', 'G'};
        List<NamedFeature> matches;
        for (int ii = 0; ii < symbols.length; ii++) {
            matches = FeatureDB.getMutation(name, ii + 1, symbols[ii]);
            assertEquals(4, matches.size());
        }


        name = "EGFLAM";
        int exp_start = 38439399;
        matches = FeatureDB.getMutation(name, 2, 'H');
        assertEquals(1, matches.size());
        NamedFeature feat = matches.get(0);
        assertEquals(exp_start, feat.getStart());

        char[] others = new char[]{'D', 'R', 'E'};
        for (char c : others) {
            matches = FeatureDB.getMutation(name, 2, c);
            assertEquals(1, matches.size());
            BasicFeature bf = (BasicFeature) matches.get(0);
            assertEquals(bf.getCodon(genome, 2).getAminoAcid().getSymbol(), c);
        }


    }

}