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
package org.neo4j.kernel.impl.nioneo.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.neo4j.kernel.impl.AbstractNeo4jTestCase;

public class TestIdGenerator
{
    private String path()
    {
        String path = AbstractNeo4jTestCase.getStorePath( "xatest" );
        new File( path ).mkdirs();
        return path;
    }
    
    private String file( String name )
    {
        return path() + File.separator + name;
    }
    
    private String idGeneratorFile()
    {
        return file( "testIdGenerator.id" );
    }
    
    @Test
    public void testCreateIdGenerator() throws IOException
    {
        try
        {
            IdGeneratorImpl.createGenerator( null );
            fail( "Null filename should throw exception" );
        }
        catch ( IllegalArgumentException e )
        {
        } // good
        try
        {
            IdGeneratorImpl.createGenerator( idGeneratorFile() );
            new IdGeneratorImpl( idGeneratorFile(), 0 ).close();
            fail( "Zero grab size should throw exception" );
        }
        catch ( IllegalArgumentException e )
        {
        } // good
        try
        {
            new IdGeneratorImpl( "testIdGenerator.id", -1 ).close();
            fail( "Negative grab size should throw exception" );
        }
        catch ( IllegalArgumentException e )
        {
        } // good

        try
        {
            IdGenerator idGenerator = new IdGeneratorImpl( idGeneratorFile(),
                1008 );
            try
            {
                IdGeneratorImpl.createGenerator( idGeneratorFile() );
                fail( "Creating a id generator with existing file name "
                    + "should throw exception" );
            }
            catch ( IllegalStateException e )
            {
            } // good
            idGenerator.close();
            // verify that id generator is ok
            FileChannel fileChannel = new FileInputStream( 
                idGeneratorFile() ).getChannel();
            ByteBuffer buffer = ByteBuffer.allocate( 9 );
            assertEquals( 9, fileChannel.read( buffer ) );
            buffer.flip();
            assertEquals( (byte) 0, buffer.get() );
            assertEquals( 0l, buffer.getLong() );
            buffer.flip();
            int readCount = fileChannel.read( buffer );
            if ( readCount != -1 && readCount != 0 )
            {
                fail( "Id generator header not ok read 9 + " + readCount
                    + " bytes from file" );
            }
            fileChannel.close();
        }
        finally
        {
            File file = new File( idGeneratorFile() );
            if ( file.exists() )
            {
                assertTrue( file.delete() );
            }
        }
    }

    @Test
    public void testStickyGenerator()
    {
        try
        {
            IdGeneratorImpl.createGenerator( idGeneratorFile() );
            IdGenerator idGen = new IdGeneratorImpl( idGeneratorFile(), 3 );
            try
            {
                new IdGeneratorImpl( idGeneratorFile(), 3 );
                fail( "Opening sticky id generator should throw exception" );
            }
            catch ( StoreFailureException e )
            { // good
            }
            idGen.close();
        }
        finally
        {
            File file = new File( idGeneratorFile() );
            if ( file.exists() )
            {
                assertTrue( file.delete() );
            }
        }
    }

    @Test
    public void testNextId()
    {
        try
        {
            IdGeneratorImpl.createGenerator( idGeneratorFile() );
            IdGenerator idGenerator = new IdGeneratorImpl( idGeneratorFile(), 3 );
            for ( long i = 0; i < 7; i++ )
            {
                assertEquals( i, idGenerator.nextId() );
            }
            idGenerator.freeId( 1 );
            idGenerator.freeId( 3 );
            idGenerator.freeId( 5 );
            assertEquals( 7l, idGenerator.nextId() );
            idGenerator.freeId( 6 );
            idGenerator.close();
            idGenerator = new IdGeneratorImpl( idGeneratorFile(), 5 );
            idGenerator.freeId( 2 );
            idGenerator.freeId( 4 );
            assertEquals( 1l, idGenerator.nextId() );
            idGenerator.freeId( 1 );
            assertEquals( 3l, idGenerator.nextId() );
            idGenerator.freeId( 3 );
            assertEquals( 5l, idGenerator.nextId() );
            idGenerator.freeId( 5 );
            assertEquals( 6l, idGenerator.nextId() );
            idGenerator.freeId( 6 );
            assertEquals( 8l, idGenerator.nextId() );
            idGenerator.freeId( 8 );
            assertEquals( 9l, idGenerator.nextId() );
            idGenerator.freeId( 9 );
            idGenerator.close();
            idGenerator = new IdGeneratorImpl( idGeneratorFile(), 3 );
            assertEquals( 2l, idGenerator.nextId() );
            assertEquals( 4l, idGenerator.nextId() );
            assertEquals( 1l, idGenerator.nextId() );
            assertEquals( 3l, idGenerator.nextId() );
            assertEquals( 5l, idGenerator.nextId() );
            assertEquals( 6l, idGenerator.nextId() );
            assertEquals( 8l, idGenerator.nextId() );
            assertEquals( 9l, idGenerator.nextId() );
            assertEquals( 10l, idGenerator.nextId() );
            assertEquals( 11l, idGenerator.nextId() );
            idGenerator.close();
        }
        finally
        {
            File file = new File( idGeneratorFile() );
            if ( file.exists() )
            {
                assertTrue( file.delete() );
            }
        }
    }

    @Test
    public void testFreeId()
    {
        try
        {
            IdGeneratorImpl.createGenerator( idGeneratorFile() );
            IdGenerator idGenerator = new IdGeneratorImpl( idGeneratorFile(), 3 );
            for ( long i = 0; i < 7; i++ )
            {
                assertEquals( i, idGenerator.nextId() );
            }
            try
            {
                idGenerator.freeId( -1 );
                fail( "Negative id should throw exception" );
            }
            catch ( IllegalArgumentException e )
            { // good
            }
            try
            {
                idGenerator.freeId( 7 );
                fail( "Greater id than ever returned should throw exception" );
            }
            catch ( IllegalArgumentException e )
            { // good
            }
            for ( int i = 0; i < 7; i++ )
            {
                idGenerator.freeId( i );
            }
            idGenerator.close();
            idGenerator = new IdGeneratorImpl( idGeneratorFile(), 2 );
            assertEquals( 0l, idGenerator.nextId() );
            assertEquals( 1l, idGenerator.nextId() );
            assertEquals( 2l, idGenerator.nextId() );
            idGenerator.close();
            idGenerator = new IdGeneratorImpl( idGeneratorFile(), 2 );
            assertEquals( 4l, idGenerator.nextId() );
            assertEquals( 5l, idGenerator.nextId() );
            assertEquals( 6l, idGenerator.nextId() );
            assertEquals( 3l, idGenerator.nextId() );
            idGenerator.close();
        }
        finally
        {
            File file = new File( idGeneratorFile() );
            if ( file.exists() )
            {
                assertTrue( file.delete() );
            }
        }
    }

    @Test
    public void testClose()
    {
        try
        {
            IdGeneratorImpl.createGenerator( idGeneratorFile() );
            IdGenerator idGenerator = new IdGeneratorImpl( idGeneratorFile(), 2 );
            idGenerator.close();
            try
            {
                idGenerator.nextId();
                fail( "nextId after close should throw exception" );
            }
            catch ( IllegalStateException e )
            { // good
            }
            try
            {
                idGenerator.freeId( 0 );
                fail( "freeId after close should throw exception" );
            }
            catch ( IllegalArgumentException e )
            { // good
            }
            idGenerator = new IdGeneratorImpl( idGeneratorFile(), 2 );
            assertEquals( 0l, idGenerator.nextId() );
            assertEquals( 1l, idGenerator.nextId() );
            assertEquals( 2l, idGenerator.nextId() );
            idGenerator.close();
            try
            {
                idGenerator.nextId();
                fail( "nextId after close should throw exception" );
            }
            catch ( IllegalStateException e )
            { // good
            }
            try
            {
                idGenerator.freeId( 0 );
                fail( "freeId after close should throw exception" );
            }
            catch ( IllegalArgumentException e )
            { // good
            }
        }
        finally
        {
            File file = new File( idGeneratorFile() );
            if ( file.exists() )
            {
                assertTrue( file.delete() );
            }
        }
    }

    @Test
    public void testOddAndEvenWorstCase()
    {
        int capacity = 1024 * 8 + 1;
        try
        {
            IdGeneratorImpl.createGenerator( idGeneratorFile() );
            IdGenerator idGenerator = new IdGeneratorImpl( idGeneratorFile(),
                128 );
            for ( int i = 0; i < capacity; i++ )
            {
                idGenerator.nextId();
            }
            Map<Long,Object> freedIds = new HashMap<Long,Object>();
            for ( long i = 1; i < capacity; i += 2 )
            {
                idGenerator.freeId( i );
                freedIds.put( i, this );
            }
            idGenerator.close();
            idGenerator = new IdGeneratorImpl( idGeneratorFile(), 2000 );
            long oldId = -1;
            for ( int i = 0; i < capacity - 1; i += 2 )
            {
                long id = idGenerator.nextId();
                if ( freedIds.remove( id ) == null )
                {
                    throw new RuntimeException( "Id=" + id + " prevId=" + oldId
                        + " list.size()=" + freedIds.size() );
                }
                oldId = id;
            }
            assertTrue( freedIds.values().size() == 0 );
            idGenerator.close();
        }
        finally
        {
            File file = new File( idGeneratorFile() );
            if ( file.exists() )
            {
                assertTrue( file.delete() );
            }
        }
        try
        {
            IdGeneratorImpl.createGenerator( idGeneratorFile() );
            IdGenerator idGenerator = new IdGeneratorImpl( idGeneratorFile(),
                128 );
            for ( int i = 0; i < capacity; i++ )
            {
                idGenerator.nextId();
            }
            Map<Long,Object> freedIds = new HashMap<Long,Object>();
            for ( long i = 0; i < capacity; i += 2 )
            {
                idGenerator.freeId( i );
                freedIds.put( i, this );
            }
            idGenerator.close();
            idGenerator = new IdGeneratorImpl( idGeneratorFile(), 2000 );
            for ( int i = 0; i < capacity; i += 2 )
            {
                assertEquals( this, freedIds.remove( idGenerator.nextId() ) );
            }
            assertEquals( 0, freedIds.values().size() );
            idGenerator.close();
        }
        finally
        {
            File file = new File( idGeneratorFile() );
            if ( file.exists() )
            {
                assertTrue( file.delete() );
            }
        }
    }

    @Test
    public void testRandomTest()
    {
        int numberOfCloses = 0;
        java.util.Random random = new java.util.Random( System
            .currentTimeMillis() );
        int capacity = random.nextInt( 1024 ) + 1024;
        int grabSize = random.nextInt( 128 ) + 128;
        IdGeneratorImpl.createGenerator( idGeneratorFile() );
        IdGenerator idGenerator = new IdGeneratorImpl( idGeneratorFile(),
            grabSize );
        List<Long> idsTaken = new ArrayList<Long>();
        float releaseIndex = 0.25f;
        float closeIndex = 0.05f;
        int currentIdCount = 0;
        try
        {
            while ( currentIdCount < capacity )
            {
                float rIndex = random.nextFloat();
                if ( rIndex < releaseIndex && currentIdCount > 0 )
                {
                    idGenerator.freeId( idsTaken.remove(
                        random.nextInt( currentIdCount ) ).intValue() );
                    currentIdCount--;
                }
                else
                {
                    idsTaken.add( idGenerator.nextId() );
                    currentIdCount++;
                }
                if ( rIndex > (1.0f - closeIndex) || rIndex < closeIndex )
                {
                    idGenerator.close();
                    grabSize = random.nextInt( 128 ) + 128;
                    idGenerator = new IdGeneratorImpl( idGeneratorFile(),
                        grabSize );
                    numberOfCloses++;
                }
            }
            idGenerator.close();
        }
        finally
        {
            File file = new File( idGeneratorFile() );
            if ( file.exists() )
            {
                assertTrue( file.delete() );
            }
        }

    }

    @Test
    public void testUnsignedId()
    {
        try
        {
            IdGeneratorImpl.createGenerator( idGeneratorFile() );
            IdGenerator idGenerator = new IdGeneratorImpl( idGeneratorFile(), 1 );
            idGenerator.setHighId( 4294967293l );
            long id = idGenerator.nextId();
            assertEquals( 4294967293l, id );
            idGenerator.freeId( id );
            try
            {
                idGenerator.nextId();
            }
            catch ( StoreFailureException e ) 
            { // good, capacity exceeded
            }
            idGenerator.close();
            idGenerator = new IdGeneratorImpl( idGeneratorFile(), 1 );
            assertEquals( 4294967294l, idGenerator.getHighId() );
            id = idGenerator.nextId();
            assertEquals( 4294967293l, id );
            try
            {
                idGenerator.nextId();
            }
            catch ( StoreFailureException e ) 
            { // good, capacity exceeded
            }
            idGenerator.close();
        }
        finally
        {
            File file = new File( idGeneratorFile() );
            if ( file.exists() )
            {
                assertTrue( file.delete() );
            }
        }
    }
}