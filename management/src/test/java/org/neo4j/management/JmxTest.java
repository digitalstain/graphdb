/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.management;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public class JmxTest
{
    private static AbstractGraphDatabase graphDb;

    @BeforeClass
    public static synchronized void startGraphDb()
    {
		graphDb = new EmbeddedGraphDatabase("target" + File.separator + "var"
				+ File.separator + JmxTest.class.getSimpleName());
    }

    @AfterClass
    public static synchronized void stopGraphDb()
    {
        if ( graphDb != null )
        {
            graphDb.shutdown();
            graphDb = null;
        }
    }

    @Test
    public void canAccessKernelBean() throws Exception
    {
        // START SNIPPET: getKernel
        Kernel kernel = graphDb.getManagementBean( Kernel.class );
        // END SNIPPET: getKernel
        assertNotNull( "kernel bean is null", kernel );
        assertNotNull( "MBeanQuery of kernel bean is null", kernel.getMBeanQuery() );
    }

    @Test
    public void canListAllBeans() throws Exception
    {
        Neo4jManager manager = getManager();
        assertTrue( "No beans returned", manager.allBeans().size() > 0 );
    }

    @Test
    public void canGetConfigurationParameters() throws Exception
    {
        Neo4jManager manager = getManager();
        Map<String, Object> configuration = manager.getConfiguration();
        assertTrue( "No configuration returned", configuration.size() > 0 );
    }

    private Neo4jManager getManager()
    {
        return new Neo4jManager( graphDb.getManagementBean( Kernel.class ) );
    }

    @Test
    public void canGetCacheBean() throws Exception
    {
        assertNotNull( getManager().getCacheBean() );
    }

    @Test
    public void canGetLockManagerBean() throws Exception
    {
        assertNotNull( getManager().getLockManagerBean() );
    }

    @Test
    public void canGetMemoryMappingBean() throws Exception
    {
        assertNotNull( getManager().getMemoryMappingBean() );
    }

    @Test
    public void canGetPrimitivesBean() throws Exception
    {
        assertNotNull( getManager().getPrimitivesBean() );
    }

    @Test
    public void canGetStoreFileBean() throws Exception
    {
        assertNotNull( getManager().getStoreFileBean() );
    }

    @Test
    public void canGetTransactionManagerBean() throws Exception
    {
        assertNotNull( getManager().getTransactionManagerBean() );
    }

    @Test
    public void canGetXaManagerBean() throws Exception
    {
        assertNotNull( getManager().getXaManagerBean() );
    }

    @Test
    public void canAccessMemoryMappingCompositData() throws Exception
    {
        assertNotNull( "MemoryPools is null", getManager().getMemoryMappingBean().getMemoryPools() );
    }

    @Test
    public void canAccessXaManagerCompositData() throws Exception
    {
        assertNotNull( "MemoryPools is null", getManager().getXaManagerBean().getXaResources() );
    }
}
