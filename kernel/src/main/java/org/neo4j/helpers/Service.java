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
package org.neo4j.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.neo4j.helpers.collection.FilteringIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.NestingIterable;
import org.neo4j.helpers.collection.PrefetchingIterator;

/**
 * A utility for locating services. This implements the same functionality as <a
 * href="http://java.sun.com/javase/6/docs/api/java/util/ServiceLoader.html">
 * the Java 6 ServiceLoader interface</a>, in fact it uses the
 * <code>ServiceLoader</code> if available, but backports the functionality to
 * previous Java versions and adds some error handling to ignore misconfigured
 * service implementations.
 *
 * Additionally this class can be used as a base class for implementing services
 * that are differentiated by a String key. An example implementation might be:
 *
 * <pre>
 * <code>
 * public abstract class StringConverter extends org.neo4j.commons.Service
 * {
 *     protected StringConverter(String id)
 *     {
 *         super( id );
 *     }
 *
 *     public abstract String convert( String input );
 *
 *     public static StringConverter load( String id )
 *     {
 *         return org.neo4j.commons.Service.load( StringConverter.class, id );
 *     }
 * }
 * </code>
 * </pre>
 *
 * With for example these implementations:
 *
 * <pre>
 * <code>
 * public final class UppercaseConverter extends StringConverter
 * {
 *     public UppercaseConverter()
 *     {
 *         super( "uppercase" );
 *     }
 *
 *     public String convert( String input )
 *     {
 *         return input.toUpperCase();
 *     }
 * }
 *
 * public final class ReverseConverter extends StringConverter
 * {
 *     public ReverseConverter()
 *     {
 *         super( "reverse" );
 *     }
 *
 *     public String convert( String input )
 *     {
 *         char[] chars = input.toCharArray();
 *         for ( int i = 0; i < chars.length/2; i++ )
 *         {
 *             char intermediate = chars[i];
 *             chars[i] = chars[chars.length-1-i];
 *             chars[chars.length-1-i] = chars[i];
 *         }
 *         return new String( chars );
 *     }
 * }
 * </code>
 * </pre>
 *
 * This would then be used as:
 *
 * <pre>
 * <code>
 * String atad = StringConverter.load( "reverse" ).convert( "data" );
 * </code>
 * </pre>
 *
 * @author Tobias Ivarsson
 */
public abstract class Service
{
    /**
     * Designates that a class implements the specified service and should be
     * added to the services listings file (META-INF/services/[service-name]).
     *
     * The annotation in itself does not provide any functionality for adding
     * the implementation class to the services listings file. But it serves as
     * a handle for an Annotation Processing Tool to utilize for performing that
     * task.
     *
     * @author Tobias Ivarsson
     */
    @Target( ElementType.TYPE )
    @Retention( RetentionPolicy.SOURCE )
    public @interface Implementation
    {
        /**
         * The service(s) this class implements.
         *
         * @return the services this class implements.
         */
        Class<?>[] value();
    }

    /**
     * A base class for services, similar to {@link Service}, that compares keys
     * using case insensitive comparison instead of exact comparison.
     *
     * @author Tobias Ivarsson
     */
    public static abstract class CaseInsensitiveService extends Service
    {
        /**
         * Create a new instance of a service implementation identified with the
         * specified key(s).
         *
         * @param key the main key for identifying this service implementation
         * @param altKeys alternative spellings of the identifier of this
         *            service implementation
         */
        protected CaseInsensitiveService( String key, String... altKeys )
        {
            super( key, altKeys );
        }

        @Override
        final boolean matches( String key )
        {
            for ( String id : keys )
            {
                if ( id.equalsIgnoreCase( key ) ) return true;
            }
            return false;
        }
    }
    /**
     * Load all implementations of a Service.
     *
     * @param <T> the type of the Service
     * @param type the type of the Service to load
     * @return all registered implementations of the Service
     */
    public static <T> Iterable<T> load( Class<T> type )
    {
        Iterable<T> loader;
        if ( null != ( loader = osgiLoader( type ) ) ) return loader;
        if ( null != ( loader = java6Loader( type ) ) ) return loader;
        if ( null != ( loader = sunJava5Loader( type ) ) ) return loader;
        if ( null != ( loader = ourOwnLoader( type ) ) ) return loader;
        return Collections.emptyList();
    }

    /**
     * Load the Service implementation with the specified key.
     *
     * @param <T> the type of the Service
     * @param type the type of the Service to load
     * @param key the key that identifies the desired implementation
     * @return the matching Service implementation
     */
    public static <T extends Service> T load( Class<T> type, String key )
    {
        for ( T impl : load( type ) )
        {
            if ( impl.matches( key ) ) return impl;
        }
        throw new NoSuchElementException( String.format(
                "Could not find any implementation of %s with a key=\"%s\"",
                type.getName(), key ) );
    }

    final Set<String> keys;

    /**
     * Create a new instance of a service implementation identified with the
     * specified key(s).
     *
     * @param key the main key for identifying this service implementation
     * @param altKeys alternative spellings of the identifier of this service
     *            implementation
     */
    protected Service( String key, String... altKeys )
    {
        if ( altKeys == null || altKeys.length == 0 )
        {
            this.keys = Collections.singleton( key );
        }
        else
        {
            this.keys = new HashSet<String>( Arrays.asList( altKeys ) );
            this.keys.add( key );
        }
    }

    @Override
    public String toString()
    {
        return getClass().getSuperclass().getName() + "" + keys;
    }

    boolean matches( String key )
    {
        return keys.contains( key );
    }

    private static <T> Iterable<T> filterExceptions( final Iterable<T> iterable )
    {
        return new Iterable<T>()
        {
            public Iterator<T> iterator()
            {
                return new PrefetchingIterator<T>()
                {
                    final Iterator<T> iterator = iterable.iterator();

                    @Override
                    protected T fetchNextOrNull()
                    {
                        while ( iterator.hasNext() )
                        {
                            try
                            {
                                return iterator.next();
                            }
                            catch ( Throwable e )
                            {
                            }
                        }
                        return null;
                    }
                };
            }
        };
    }

    private static volatile OSGiLoader osgiLoader;

    private static <T> Iterable<T> osgiLoader( Class<T> type )
    {
        OSGiLoader loader = osgiLoader;
        if ( loader == null )
        {
            osgiLoader = loader = OSGiLoader.instance();
        }
        return loader.load( type );
    }

    static class OSGiLoader
    {
        private static OSGiLoader instance;

        synchronized static OSGiLoader instance()
        {
            if ( instance == null )
            {
                try
                {
                    @SuppressWarnings( "unchecked" ) Class<? extends OSGiLoader> loaderClass =
                        (Class<? extends OSGiLoader>) Class.forName( "org.neo4j.helpers.OSGiServiceLoader" );
                    instance = loaderClass.newInstance();
                }
                catch ( LinkageError err )
                {
                }
                catch ( Exception ex )
                {
                }
                if ( instance == null )
                {
                    instance = new OSGiLoader();
                }
            }
            return instance;
        }

        <T> Iterable<T> load( @SuppressWarnings( "unused" ) Class<T> type )
        {
            return null;
        }
    }

    private static <T> Iterable<T> java6Loader( Class<T> type )
    {
        try
        {
            @SuppressWarnings( "unchecked" ) Iterable<T> result = (Iterable<T>)
                    Class.forName( "java.util.ServiceLoader" )
                    .getMethod( "load", Class.class )
                    .invoke( null, type );
            return filterExceptions( result );
        }
        catch ( Exception e )
        {
            return null;
        }
        catch ( LinkageError e )
        {
            return null;
        }
    }

    private static <T> Iterable<T> sunJava5Loader( final Class<T> type )
    {
        final Method providers;
        try
        {
            providers = Class.forName( "sun.misc.Service" ).getMethod( "providers", Class.class );
        }
        catch ( Exception e )
        {
            return null;
        }
        catch ( LinkageError e )
        {
            return null;
        }
        return filterExceptions( new Iterable<T>()
        {
            public Iterator<T> iterator()
            {
                try
                {
                    @SuppressWarnings( "unchecked" ) Iterator<T> result =
                        (Iterator<T>) providers.invoke( null, type );
                    return result;
                }
                catch ( Exception e )
                {
                    throw new RuntimeException(
                            "Failed to invoke sun.misc.Service.providers(forClass)",
                            e );
                }
            }
        } );
    }

    private static <T> Iterable<T> ourOwnLoader( final Class<T> type )
    {
        Collection<URL> urls = new HashSet<URL>();
        try
        {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader()
                    .getResources( "META-INF/services/" + type.getName() );
            while ( resources.hasMoreElements() )
            {
                urls.add( resources.nextElement() );
            }
        }
        catch ( IOException e )
        {
            return null;
        }
        return new NestingIterable<T, BufferedReader>(
                FilteringIterable.notNull( new IterableWrapper<BufferedReader, URL>( urls )
                {
                    @Override
                    protected BufferedReader underlyingObjectToObject( URL url )
                    {
                        try
                        {
                            return new BufferedReader( new InputStreamReader( url.openStream() ) );
                        }
                        catch ( IOException e )
                        {
                            return null;
                        }
                    }
                } ) )
        {
            @Override
            protected Iterator<T> createNestedIterator( final BufferedReader input )
            {
                return new PrefetchingIterator<T>()
                {
                    @Override
                    protected T fetchNextOrNull()
                    {
                        try
                        {
                            String line;
                            while ( null != ( line = input.readLine() ) )
                            {
                                try
                                {
                                    return type.cast( Class.forName( line ).newInstance() );
                                }
                                catch ( Exception e )
                                {
                                }
                                catch ( LinkageError e )
                                {
                                }
                            }
                            input.close();
                            return null;
                        }
                        catch ( IOException e )
                        {
                            return null;
                        }
                    }

                    /* Finalizer - close the input stream.
                     * Prevent leakage of open files. Finalizers impact GC performance,
                     * but there are expected to be few of these objects.
                     */
                    @Override
                    protected void finalize() throws Throwable
                    {
                        input.close();
                    }
                };
            }
        };
    }
}
