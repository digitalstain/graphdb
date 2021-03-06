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
package org.neo4j.index.impl.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.neo4j.helpers.Pair;

class FullTxData extends ExactTxData
{
    private static final String ORPHANS_KEY = "__all__";
    private static final String ORPHANS_VALUE = "1";
    
    private Directory directory;
    private IndexWriter writer;
    private boolean modified;
    private IndexReader reader;
    private IndexSearcher searcher;
    private final Map<Long, Document> cachedDocuments = new HashMap<Long, Document>();
    private Set<String> orphans;

    FullTxData( LuceneIndex index )
    {
        super( index );
    }

    @Override
    TxData add( Object entityId, String key, Object value )
    {
        super.add( entityId, key, value );
        try
        {
            ensureLuceneDataInstantiated();
            long id = entityId instanceof Long ? (Long) entityId : ((RelationshipId)entityId).id;
            Document document = findDocument( id );
            boolean add = false;
            if ( document == null )
            {
                document = index.getIdentifier().entityType.newDocument( entityId );
                cachedDocuments.put( id, document );
                add = true;
            }
            
            if ( key == null && value == null )
            {
                // Set a special "always hit" flag
                document.add( new Field( ORPHANS_KEY, ORPHANS_VALUE, Store.NO, Index.NOT_ANALYZED ) );
                addOrphan( null );
            }
            else if ( value == null )
            {
                // Set a special "always hit" flag
                document.add( new Field( ORPHANS_KEY, key, Store.NO, Index.NOT_ANALYZED ) );
                addOrphan( key );
            }
            else
            {
                index.type.addToDocument( document, key, value );
            }
            
            if ( add )
            {
                writer.addDocument( document );
            }
            else
            {
                writer.updateDocument( index.type.idTerm( id ), document );
            }
            invalidateSearcher();
            return this;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    private void addOrphan( String key )
    {
        if ( orphans == null )
        {
            orphans = new HashSet<String>();
        }
        orphans.add( key );
    }

    private Document findDocument( long id )
    {
        return cachedDocuments.get( id );
    }

    private void ensureLuceneDataInstantiated()
    {
        if ( this.directory == null )
        {
            try
            {
                this.directory = new RAMDirectory();
                this.writer = new IndexWriter( directory, index.type.analyzer,
                        MaxFieldLength.UNLIMITED );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
    }

    @Override
    TxData remove( Object entityId, String key, Object value )
    {
        super.remove( entityId, key, value );
        try
        {
            ensureLuceneDataInstantiated();
            long id = entityId instanceof Long ? (Long) entityId : ((RelationshipId)entityId).id;
            Document document = findDocument( id );
            if ( document != null )
            {
                index.type.removeFromDocument( document, key, value );
                if ( LuceneDataSource.documentIsEmpty( document ) )
                {
                    writer.deleteDocuments( index.type.idTerm( id ) );
                }
                else
                {
                    writer.updateDocument( index.type.idTerm( id ), document );
                }
            }
            invalidateSearcher();
            return this;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    Pair<Collection<Long>, TxData> query( Query query, QueryContext contextOrNull )
    {
        return internalQuery( query, contextOrNull );
    }

    private Pair<Collection<Long>, TxData> internalQuery( Query query, QueryContext contextOrNull )
    {
        if ( this.directory == null )
        {
            return Pair.<Collection<Long>, TxData>of( Collections.<Long>emptySet(), this );
        }

        try
        {
            Sort sorting = contextOrNull != null ? contextOrNull.sorting : null;
            boolean prioritizeCorrectness = contextOrNull == null || !contextOrNull.tradeCorrectnessForSpeed;
            query = includeOrphans( query );
            Hits hits = new Hits( searcher( prioritizeCorrectness ), query, null, sorting, prioritizeCorrectness );
            Collection<Long> result = new ArrayList<Long>();
            for ( int i = 0; i < hits.length(); i++ )
            {
                result.add( Long.parseLong( hits.doc( i ).getField(
                    LuceneIndex.KEY_DOC_ID ).stringValue() ) );
            }
            return Pair.<Collection<Long>, TxData>of( result, this );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    private Query includeOrphans( Query query )
    {
        if ( orphans == null )
        {
            return query;
        }
        
        BooleanQuery result = new BooleanQuery();
        result.add( injectOrphans( query ), Occur.SHOULD );
        result.add( new TermQuery( new Term( ORPHANS_KEY, ORPHANS_VALUE ) ), Occur.SHOULD );
        return result;
    }
    
    private Query injectOrphans( Query query )
    {
        if ( query instanceof BooleanQuery )
        {
            BooleanQuery source = (BooleanQuery) query;
            BooleanQuery result = new BooleanQuery();
            for ( BooleanClause clause : source.clauses() )
            {
                result.add( injectOrphans( clause.getQuery() ), clause.getOccur() );
            }
            return result;
        }
        else
        {
            Set<Term> terms = new HashSet<Term>();
            query.extractTerms( terms );
            
            // TODO Don't only use the first term
            Term term = terms.iterator().next();
            
            BooleanQuery result = new BooleanQuery();
            result.add( query, Occur.SHOULD );
            result.add( new TermQuery( new Term( ORPHANS_KEY, term.field() ) ), Occur.SHOULD );
            return result;
        }
    }

    @Override
    void close()
    {
        safeClose( this.writer );
        safeClose( this.reader );
        safeClose( this.searcher );
    }

    private void invalidateSearcher()
    {
        this.modified = true;
    }

    private IndexSearcher searcher( boolean allowRefreshSearcher )
    {
        if ( this.searcher != null && (!modified || !allowRefreshSearcher) )
        {
            return this.searcher;
        }

        try
        {
            IndexReader newReader = this.reader == null ? this.writer.getReader() : this.reader.reopen();
            if ( newReader == this.reader )
            {
                return this.searcher;
            }
            safeClose( reader );
            this.reader = newReader;
            safeClose( searcher );
            searcher = new IndexSearcher( reader );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            if ( allowRefreshSearcher )
            {
                this.modified = false;
            }
        }
        return this.searcher;
    }

    private static void safeClose( Object object )
    {
        if ( object == null )
        {
            return;
        }

        try
        {
            if ( object instanceof IndexWriter )
            {
                ( ( IndexWriter ) object ).close();
            }
            else if ( object instanceof IndexSearcher )
            {
                ( ( IndexSearcher ) object ).close();
            }
            else if ( object instanceof IndexReader )
            {
                ( ( IndexReader ) object ).close();
            }
        }
        catch ( IOException e )
        {
            // Ok
        }
    }
    
    @Override
    Pair<Searcher, TxData> asSearcher( QueryContext context )
    {
        boolean refresh = context == null || !context.tradeCorrectnessForSpeed;
        return Pair.of( (Searcher) searcher( refresh ), (TxData) this );
    }
}
