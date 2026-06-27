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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.StringReader;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;


class ApacheDsContainerTest
{
    @Test
    void canBindToRootDse() throws Exception
    {
        try ( ApacheDsContainer container = startContainer() )
        {
            Entry rootDse = container.lookupRootDse();
            assertNotNull( rootDse );
        }
    }


    @Test
    void canImportLdifEntries() throws Exception
    {
        try ( ApacheDsContainer container = startContainer();
            LdapNetworkConnection connection = container.createAdminConnection() )
        {
            String ldif = "dn: ou=ldif-smoke,ou=system\n"
                + "objectClass: organizationalUnit\n"
                + "objectClass: top\n"
                + "ou: ldif-smoke\n"
                + "\n"
                + "dn: uid=ldif-user,ou=ldif-smoke,ou=system\n"
                + "objectClass: inetOrgPerson\n"
                + "objectClass: organizationalPerson\n"
                + "objectClass: person\n"
                + "objectClass: top\n"
                + "cn: LDIF User\n"
                + "sn: User\n"
                + "uid: ldif-user\n";

            try ( LdifReader reader = new LdifReader( new StringReader( ldif ) ) )
            {
                for ( LdifEntry ldifEntry : reader )
                {
                    connection.add( ldifEntry.getEntry() );
                }
            }

            Entry imported = connection.lookup( "uid=ldif-user,ou=ldif-smoke,ou=system" );
            assertNotNull( imported );
            assertEquals( "LDIF User", imported.get( "cn" ).getString() );
        }
    }


    @Test
    void canLoadLdifEntriesAtStartup() throws Exception
    {
        try ( ApacheDsContainer container = newContainer().withLdifClasspathResource( "startup-import.ldif" ) )
        {
            container.start();

            try ( LdapNetworkConnection connection = container.createAdminConnection() )
            {
                Entry imported = connection.lookup( "uid=startup-user,ou=startup-smoke,ou=system" );
                assertNotNull( imported );
                assertEquals( "Startup User", imported.get( "cn" ).getString() );
            }
        }
    }


    @Test
    void canCreateReadUpdateAndDeleteEntries() throws Exception
    {
        try ( ApacheDsContainer container = startContainer();
            LdapNetworkConnection connection = container.createAdminConnection() )
        {
            connection.add( new DefaultEntry(
                "ou=crud-smoke,ou=system",
                "objectClass: organizationalUnit",
                "objectClass: top",
                "ou: crud-smoke" ) );

            String userDn = "uid=crud-user,ou=crud-smoke,ou=system";
            connection.add( new DefaultEntry(
                userDn,
                "objectClass: inetOrgPerson",
                "objectClass: organizationalPerson",
                "objectClass: person",
                "objectClass: top",
                "cn: CRUD User",
                "sn: User",
                "uid: crud-user" ) );

            Entry created = connection.lookup( userDn );
            assertNotNull( created );
            assertEquals( "CRUD User", created.get( "cn" ).getString() );

            connection.modify( userDn, new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
                "description", "updated by smoke test" ) );

            Entry updated = connection.lookup( userDn );
            assertEquals( "updated by smoke test", updated.get( "description" ).getString() );

            connection.delete( userDn );
            assertFalse( connection.exists( userDn ) );

            connection.delete( "ou=crud-smoke,ou=system" );
            assertFalse( connection.exists( "ou=crud-smoke,ou=system" ) );
        }
    }


    @Test
    void canSearchImportedEntries() throws Exception
    {
        try ( ApacheDsContainer container = startContainer();
            LdapNetworkConnection connection = container.createAdminConnection() )
        {
            connection.add( new DefaultEntry(
                "ou=search-smoke,ou=system",
                "objectClass: organizationalUnit",
                "objectClass: top",
                "ou: search-smoke" ) );

            connection.add( new DefaultEntry(
                "uid=search-user,ou=search-smoke,ou=system",
                "objectClass: inetOrgPerson",
                "objectClass: organizationalPerson",
                "objectClass: person",
                "objectClass: top",
                "cn: Search User",
                "sn: User",
                "uid: search-user" ) );

            try ( EntryCursor cursor = connection.search( "ou=system", "(uid=search-user)",
                SearchScope.SUBTREE, "uid", "cn" ) )
            {
                assertTrue( cursor.next() );
                Entry found = cursor.get();
                assertEquals( "search-user", found.get( "uid" ).getString() );
                assertEquals( "Search User", found.get( "cn" ).getString() );
                assertFalse( cursor.next() );
            }
        }
    }


    private ApacheDsContainer startContainer()
    {
        ApacheDsContainer container = newContainer();
        container.start();
        return container;
    }


    private ApacheDsContainer newContainer()
    {
        assumeTrue( DockerClientFactory.instance().isDockerAvailable(), "Docker is not available" );

        DockerImageName imageName = DockerImageName.parse( System.getProperty( "apacheds.docker.image" ) )
            .asCompatibleSubstituteFor( "apachedirectory/apacheds" );

        return new ApacheDsContainer( imageName )
            .withImagePullPolicy( ignored -> false );
    }
}
