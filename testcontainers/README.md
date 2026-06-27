<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# ApacheDS Testcontainers

This module publishes a reusable Testcontainers library for starting ApacheDS
in downstream integration tests.

```java
try ( ApacheDsContainer container = new ApacheDsContainer( "apachedirectory/apacheds:local" ) )
{
    container.start();

    try ( LdapNetworkConnection connection = container.createAdminConnection() )
    {
        Entry rootDse = connection.lookup( Dn.ROOT_DSE );
    }
}
```

## Active Directory Fixture

`withActiveDirectoryFixture()` loads a small LDAP-facing Active Directory
simulation during container startup:

```java
try ( ApacheDsContainer container = new ApacheDsContainer( "apachedirectory/apacheds:local" )
    .withActiveDirectoryFixture() )
{
    container.start();
}
```

The fixture creates `dc=example,dc=com` entries for users, groups, nested
membership, and a computer account. It also loads a small compatibility schema
for common AD attribute names such as `sAMAccountName`, `userPrincipalName`,
`objectSid`, `objectGUID`, `userAccountControl`, `memberOf`, and `groupType`.

This is intended for applications that need to test AD-style LDAP searches and
binds. It is not a Microsoft Active Directory Domain Controller emulator: it
does not implement domain join, Group Policy, DRS replication, Netlogon, SAMR,
LSA, AD Kerberos extensions, or Windows ACL semantics.
