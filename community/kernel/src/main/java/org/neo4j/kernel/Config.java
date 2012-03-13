/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.neo4j.graphdb.factory.GraphDatabaseSetting;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.helpers.Args;
import org.neo4j.helpers.collection.PrefetchingIterator;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.kernel.impl.nioneo.xa.NeoStoreXaDataSource;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.kernel.info.DiagnosticsPhase;
import org.neo4j.kernel.info.DiagnosticsProvider;

import static java.util.regex.Pattern.*;

/**
 * A non-standard configuration object.
 */
public class Config implements DiagnosticsProvider
{
    static final String NIO_NEO_DB_CLASS = "org.neo4j.kernel.impl.nioneo.xa.NeoStoreXaDataSource";
    public static final String DEFAULT_DATA_SOURCE_NAME = "nioneodb";

    static final String LUCENE_DS_CLASS = "org.neo4j.index.lucene.LuceneDataSource";
    static final String LUCENE_FULLTEXT_DS_CLASS = "org.neo4j.index.lucene.LuceneFulltextDataSource";

    /**
     * Tell Neo4j to use memory mapped buffers for accessing the native storage
     * layer
     */
    @Documented
    public static final String USE_MEMORY_MAPPED_BUFFERS = "use_memory_mapped_buffers";
    /** Print out the effective Neo4j configuration after startup */
    @Documented
    public static final String DUMP_CONFIGURATION = "dump_configuration";
    /**
     * Make Neo4j keep the logical transaction logs for being able to backup the
     * database
     */
    @Documented
    public static final String KEEP_LOGICAL_LOGS = "keep_logical_logs";
    /** Enable a remote shell server which shell clients can log in to */
    @Documented
    public static final String ENABLE_REMOTE_SHELL = "enable_remote_shell";
    /** Enable a support for running online backups */
    @Documented
    public static final String ENABLE_ONLINE_BACKUP = "enable_online_backup";
    /** Mark this database as a backup slave. */
    @Documented
    public static final String BACKUP_SLAVE = "backup_slave";

    /** Only allow read operations from this Neo4j instance. */
    @Documented
    public static final String READ_ONLY = "read_only";
    /** Relative path for where the Neo4j storage directory is located */
    @Documented
    public static final String STORAGE_DIRECTORY = "store_dir";
    /**
     * Use a quick approach for rebuilding the ID generators. This give quicker
     * recovery time, but will limit the ability to reuse the space of deleted
     * entities.
     */
    @Documented
    public static final String REBUILD_IDGENERATORS_FAST = "rebuild_idgenerators_fast";
    /** The size to allocate for memory mapping the node store */
    @Documented
    public static final String NODE_STORE_MMAP_SIZE = "neostore.nodestore.db.mapped_memory";
    /** The size to allocate for memory mapping the array property store */
    @Documented
    public static final String ARRAY_PROPERTY_STORE_MMAP_SIZE = "neostore.propertystore.db.arrays.mapped_memory";
    /**
     * The size to allocate for memory mapping the store for property key
     * strings
     */
    @Documented
    public static final String PROPERTY_INDEX_KEY_STORE_MMAP_SIZE = "neostore.propertystore.db.index.keys.mapped_memory";
    /**
     * The size to allocate for memory mapping the store for property key
     * indexes
     */
    @Documented
    public static final String PROPERTY_INDEX_STORE_MMAP_SIZE = "neostore.propertystore.db.index.mapped_memory";
    /** The size to allocate for memory mapping the property value store */
    @Documented
    public static final String PROPERTY_STORE_MMAP_SIZE = "neostore.propertystore.db.mapped_memory";
    /** The size to allocate for memory mapping the string property store */
    @Documented
    public static final String STRING_PROPERTY_STORE_MMAP_SIZE = "neostore.propertystore.db.strings.mapped_memory";
    /** The size to allocate for memory mapping the relationship store */
    @Documented
    public static final String RELATIONSHIP_STORE_MMAP_SIZE = "neostore.relationshipstore.db.mapped_memory";
    /** Relative path for where the Neo4j logical log is located */
    @Documented
    public static final String LOGICAL_LOG = "logical_log";
    /** Relative path for where the Neo4j storage information file is located */
    @Documented
    public static final String NEO_STORE = "neo_store";

    /**
     * The type of cache to use for nodes and relationships, one of [weak, soft,
     * none]
     */
    @Documented
    public static final String CACHE_TYPE = "cache_type";

    /**
     * The name of the Transaction Manager service to use as defined in the TM
     * service provider constructor, defaults to native.
     */
    @Documented
    public static final String TXMANAGER_IMPLEMENTATION = "tx_manager_impl";

    /**
     * Determines whether any TransactionInterceptors loaded will intercept
     * prepared transactions before they reach the logical log. Defaults to
     * false.
     */
    @Documented
    public static final String INTERCEPT_COMMITTING_TRANSACTIONS = "intercept_committing_transactions";

    /**
     * Determines whether any TransactionInterceptors loaded will intercept
     * externally received transactions (e.g. in HA) before they reach the
     * logical log and are applied to the store. Defaults to false.
     */
    @Documented
    public static final String INTERCEPT_DESERIALIZED_TRANSACTIONS = "intercept_deserialized_transactions";

    /**
     * Boolean (one of true,false) defining whether to allow a store upgrade
     * in case the current version of the database starts against an older store
     * version. Setting this to true does not guarantee successful upgrade, just
     * allows an attempt at it.
     */
    @Documented
    public static final String ALLOW_STORE_UPGRADE = "allow_store_upgrade";
    public static final String STRING_BLOCK_SIZE = "string_block_size";
    public static final String ARRAY_BLOCK_SIZE = "array_block_size";
    /**
     * A list of property names (comma separated) that will be indexed by
     * default.
     * This applies to Nodes only.
     */
    @Documented
    public static final String NODE_KEYS_INDEXABLE = "node_keys_indexable";
    /**
     * A list of property names (comma separated) that will be indexed by
     * default.
     * This applies to Relationships only.
     */
    @Documented
    public static final String RELATIONSHIP_KEYS_INDEXABLE = "relationship_keys_indexable";

    /**
     * Boolean value (one of true, false) that controls the auto indexing
     * feature for nodes. Setting to false shuts it down unconditionally,
     * while true enables it for every property, subject to restrictions
     * in the configuration.
     * The default is false.
     */
    @Documented
    public static final String NODE_AUTO_INDEXING = "node_auto_indexing";

    /**
     * Boolean value (one of true, false) that controls the auto indexing
     * feature for relationships. Setting to false shuts it down
     * unconditionally, while true enables it for every property, subject
     * to restrictions in the configuration.
     * The default is false.
     */
    @Documented
    public static final String RELATIONSHIP_AUTO_INDEXING = "relationship_auto_indexing";

    /**
     * Integer value that sets the maximum number of open lucene index searchers.
     * The default is Integer.MAX_VALUE
     */
    @Documented
    public static final String LUCENE_SEARCHER_CACHE_SIZE = "lucene_searcher_cache_size";

    /**
     * Integer value that sets the maximum number of open lucene index writers.
     * The default is Integer.MAX_VALUE
     */
    @Documented
    public static final String LUCENE_WRITER_CACHE_SIZE = "lucene_writer_cache_size";

    static final String LOAD_EXTENSIONS = "load_kernel_extensions";

    private Map<String, String> params;

    public Config(StringLogger msgLog, Map<String, String> inputParams)
    {
        // Migrate settings
        ConfigurationMigrator configurationMigrator = new ConfigurationMigrator( msgLog );
        inputParams = configurationMigrator.migrateConfiguration( inputParams );

        // Apply defaults
        ConfigurationDefaults configurationDefaults = new ConfigurationDefaults( msgLog, GraphDatabaseSettings.class );
        params = configurationDefaults.apply( inputParams );

        // Apply autoconfiguration for memory settings
        AutoConfigurator autoConfigurator = new AutoConfigurator( get(NeoStoreXaDataSource.Configuration.store_dir), getBoolean( GraphDatabaseSettings.use_memory_mapped_buffers ), getBoolean( GraphDatabaseSettings.dump_configuration ) );
        Map<String,String> autoConfiguration = autoConfigurator.configure( );
        for( Map.Entry<String, String> autoConfig : autoConfiguration.entrySet() )
        {
            // Don't override explicit settings
            if (!inputParams.containsKey( autoConfig.getKey() ))
                params.put( autoConfig.getKey(), autoConfig.getValue() );
        }

        // Configuration may not be changed at runtime
        this.params = Collections.unmodifiableMap(this.params);
    }

    public Map<String, String> getParams()
    {
        return this.params;
    }
    
    public String get(GraphDatabaseSetting setting)
    {
        return params.get( setting.name() );
    }
    
    public boolean getBoolean(GraphDatabaseSetting.BooleanSetting setting)
    {
        return Boolean.parseBoolean( get( setting ) );
    }
    
    public int getInteger(GraphDatabaseSetting.IntegerSetting setting)
    {
        return Integer.parseInt(get( setting ));
    }

    public long getLong(GraphDatabaseSetting.LongSetting setting)
    {
        return Long.parseLong(get( setting ));
    }

    public double getDouble(GraphDatabaseSetting.DoubleSetting setting)
    {
        return Double.parseDouble(get( setting ));
    }

    public float getFloat(GraphDatabaseSetting.FloatSetting setting)
    {
        return Float.parseFloat( get( setting ));
    }

    
    public <T extends Enum<T>> T getEnum( Class<T> enumType,
                                          GraphDatabaseSetting.OptionsSetting graphDatabaseSetting)
    {
        return Enum.valueOf( enumType, get( graphDatabaseSetting ) );
    }
    
    
    public static boolean configValueContainsMultipleParameters( String configValue )
    {
        return configValue != null && configValue.contains( "=" );
    }

    public static Args parseMapFromConfigValue( String name, String configValue )
    {
        Map<String, String> result = new HashMap<String, String>();
        for ( String part : configValue.split( quote( "," ) ) )
        {
            String[] tokens = part.split( quote( "=" ) );
            if ( tokens.length != 2 )
            {
                throw new RuntimeException( "Invalid configuration value '" + configValue +
                        "' for " + name + ". The format is [true/false] or [key1=value1,key2=value2...]" );
            }
            result.put( tokens[0], tokens[1] );
        }
        return new Args( result );
    }

    @Override
    public String getDiagnosticsIdentifier()
    {
        return getClass().getName();
    }
    
    @Override
    public void acceptDiagnosticsVisitor( Object visitor )
    {
        // nothing visits configuration
    }
    
    @Override
    public void dump( DiagnosticsPhase phase, StringLogger log )
    {
        if ( phase.isInitialization() || phase.isExplicitlyRequested() )
        {
            log.logLongMessage( "Neo4j Kernel properties:", new PrefetchingIterator<String>()
            {
                final Iterator<String> keys = params.keySet().iterator();

                @Override
                protected String fetchNextOrNull()
                {
                    while ( keys.hasNext() )
                    {
                        Object key = keys.next();
                        if ( key instanceof String )
                        {
                            Object value = params.get( key );
                            return key + "=" + value;
                        }
                    }
                    return null;
                }
            }, true );
        }
    }
}