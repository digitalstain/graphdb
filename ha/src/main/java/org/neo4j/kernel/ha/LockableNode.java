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
package org.neo4j.kernel.ha;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.Traverser.Order;

public class LockableNode implements Node
{
    private final int id;

    public LockableNode( int id )
    {
        this.id = id;
    }

    public void delete()
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public long getId()
    {
        return this.id;
    }

    public GraphDatabaseService getGraphDatabase()
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public Object getProperty( String key )
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public Object getProperty( String key, Object defaultValue )
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public Iterable<String> getPropertyKeys()
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public Iterable<Object> getPropertyValues()
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public boolean hasProperty( String key )
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public Object removeProperty( String key )
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public void setProperty( String key, Object value )
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public boolean equals( Object o )
    {
        if ( !(o instanceof Node) )
        {
            return false;
        }
        return this.getId() == ((Node) o).getId();
    }

    public int hashCode()
    {
        return id;
    }

    public String toString()
    {
        return "Lockable node #" + this.getId();
    }

    public Relationship createRelationshipTo( Node otherNode,
            RelationshipType type )
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public Iterable<Relationship> getRelationships()
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public Iterable<Relationship> getRelationships( RelationshipType... types )
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public Iterable<Relationship> getRelationships( Direction dir )
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public Iterable<Relationship> getRelationships( RelationshipType type,
            Direction dir )
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public Relationship getSingleRelationship( RelationshipType type,
            Direction dir )
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public boolean hasRelationship()
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public boolean hasRelationship( RelationshipType... types )
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public boolean hasRelationship( Direction dir )
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public boolean hasRelationship( RelationshipType type, Direction dir )
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public Traverser traverse( Order traversalOrder,
            StopEvaluator stopEvaluator,
            ReturnableEvaluator returnableEvaluator,
            RelationshipType relationshipType, Direction direction )
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public Traverser traverse( Order traversalOrder,
            StopEvaluator stopEvaluator,
            ReturnableEvaluator returnableEvaluator,
            RelationshipType firstRelationshipType, Direction firstDirection,
            RelationshipType secondRelationshipType, Direction secondDirection )
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }

    public Traverser traverse( Order traversalOrder,
            StopEvaluator stopEvaluator,
            ReturnableEvaluator returnableEvaluator,
            Object... relationshipTypesAndDirections )
    {
        throw new UnsupportedOperationException( "Lockable node" );
    }
}
