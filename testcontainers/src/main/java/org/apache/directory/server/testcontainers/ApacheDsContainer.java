/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.directory.server.testcontainers;


import java.time.Duration;

import org.apache.directory.api.ldap.codec.api.LdapApiServiceFactory;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.constants.ServerDNConstants;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.DockerImageName;


public class ApacheDsContainer extends GenericContainer<ApacheDsContainer>
{
    public static final String ACTIVE_DIRECTORY_SCHEMA_RESOURCE =
        "org/apache/directory/server/testcontainers/active-directory/00-ad-compat-schema.ldif";

    public static final String ACTIVE_DIRECTORY_FIXTURE_RESOURCE =
        "org/apache/directory/server/testcontainers/active-directory/10-example-directory.ldif";

    public static final DockerImageName DEFAULT_IMAGE_NAME =
        DockerImageName.parse( "ghcr.io/openprojectx/directory-server/apacheds:latest" );

    public static final int LDAP_PORT = 10389;

    public static final int LDAPS_PORT = 10636;

    public static final String DEFAULT_ADMIN_DN = ServerDNConstants.ADMIN_SYSTEM_DN;

    public static final String DEFAULT_ADMIN_PASSWORD = "secret";

    public static final String DEFAULT_LDIF_DIRECTORY = "/var/lib/apacheds/default/ldif";

    private static final Duration DEFAULT_STARTUP_TIMEOUT = Duration.ofMinutes( 2 );


    public ApacheDsContainer()
    {
        this( DEFAULT_IMAGE_NAME );
    }


    public ApacheDsContainer( String imageName )
    {
        this( DockerImageName.parse( imageName ) );
    }


    public ApacheDsContainer( DockerImageName imageName )
    {
        super( imageName.asCompatibleSubstituteFor( DEFAULT_IMAGE_NAME ) );
        withExposedPorts( LDAP_PORT, LDAPS_PORT );
        waitingFor( Wait.forListeningPort().withStartupTimeout( DEFAULT_STARTUP_TIMEOUT ) );
    }


    public String getLdapHost()
    {
        return getHost();
    }


    public int getLdapPort()
    {
        return getMappedPort( LDAP_PORT );
    }


    public int getLdapsPort()
    {
        return getMappedPort( LDAPS_PORT );
    }


    public LdapNetworkConnection createAdminConnection() throws Exception
    {
        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( getLdapHost() );
        config.setLdapPort( getLdapPort() );
        config.setName( DEFAULT_ADMIN_DN );
        config.setCredentials( DEFAULT_ADMIN_PASSWORD );
        config.setLdapApiService( LdapApiServiceFactory.getSingleton() );

        LdapNetworkConnection connection = new LdapNetworkConnection( config );
        connection.bind( DEFAULT_ADMIN_DN, DEFAULT_ADMIN_PASSWORD );
        return connection;
    }


    public ApacheDsContainer withLdifClasspathResource( String resourcePath )
    {
        String fileName = resourcePath.substring( resourcePath.lastIndexOf( '/' ) + 1 );
        return withCopyFileToContainer( MountableFile.forClasspathResource( resourcePath ),
            DEFAULT_LDIF_DIRECTORY + "/" + fileName );
    }


    public ApacheDsContainer withLdifFile( String path )
    {
        String fileName = path.substring( Math.max( path.lastIndexOf( '/' ), path.lastIndexOf( '\\' ) ) + 1 );
        return withCopyFileToContainer( MountableFile.forHostPath( path ), DEFAULT_LDIF_DIRECTORY + "/" + fileName );
    }


    public ApacheDsContainer withActiveDirectoryFixture()
    {
        return withLdifClasspathResource( ACTIVE_DIRECTORY_SCHEMA_RESOURCE )
            .withLdifClasspathResource( ACTIVE_DIRECTORY_FIXTURE_RESOURCE );
    }


    public Entry lookupRootDse() throws Exception
    {
        try ( LdapNetworkConnection connection = createAdminConnection() )
        {
            return connection.lookup( Dn.ROOT_DSE );
        }
    }
}
