# Security Policy

## Reporting a Vulnerability

Apache Directory follows the [Apache Software Foundation security process](https://www.apache.org/security/).
Please report suspected vulnerabilities **privately** to `security@apache.org` (the Directory PMC is
reachable at `private@directory.apache.org`). Do **not** open public GitHub issues or pull requests for
security reports.

## Threat Model

What the Apache Directory components treat as in/out of scope, the security properties they provide and
disclaim (authentication, LDAP ACI / Fortress RBAC authorization, Kerberos ticket integrity, protocol-parser
robustness, storage integrity), the adversary model, and how findings are triaged are documented in the
umbrella [THREAT_MODEL.md](./THREAT_MODEL.md), which carries per-domain addenda for LDAP, Kerberos, RBAC,
SCIM, and MVCC storage.
