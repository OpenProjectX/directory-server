<!--
SPDX-License-Identifier: Apache-2.0

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Threat Model — Apache Directory (umbrella)

## §1 Header

- **Project:** Apache Directory — a family of identity-infrastructure components: an **LDAP v3 directory
  server** (ApacheDS), the **LDAP API** client/codec library, a **Kerberos** implementation (Kerby, incl.
  KDC + token/PKINIT preauth), the **Fortress** RBAC/ARBAC authorization system (core + REST endpoint +
  web console + container realm), a **SCIM 2.0** provisioning implementation (SCIMple), an **MVCC B-tree**
  storage engine (Mavibot), and the **Studio** desktop LDAP client *(documented — project; per-repo READMEs)*.
- **Scope of this umbrella:** the **shared trust model** across the server-side identity components, with
  **per-domain addenda** where the surface diverges (LDAP bind/ACI, Kerberos KDC crypto, RBAC policy, SCIM
  protocol, MVCC storage). The Studio desktop client is modelled lightly as a *client* (§3).
- **Repos covered (confirmed scope):** `directory-server`, `directory-ldap-api`, `directory-kerby`,
  `directory-fortress-core`, `directory-fortress-enmasse`, `directory-fortress-commander`,
  `directory-fortress-realm`, `directory-scimple`, `directory-mavibot`, `directory-studio`.
- **Modelled against:** each repo's default branch HEAD (2026-05-31).
- **Status:** **DRAFT — v0, not yet reviewed by the Directory PMC.** Produced by the ASF Security team via the
  `threat-model-producer` rubric (<https://gist.github.com/potiuk/da14a826283038ddfe38cc9fe6310573>).
- **Reporting / version-binding / legend** as in the sibling models. **Draft confidence:** ~18 documented /
  0 maintainer / ~70 inferred. Each *(inferred)* routes to §14 (grouped by domain).

These components share one shape: a **network service that authenticates a remote principal and then
authorizes its requests over a wire protocol** (LDAP, Kerberos, SCIM/HTTP, Fortress REST). The umbrella
captures that shared boundary; the addenda capture each protocol's specific attack surface.

## §2 Scope and intended use

Caller roles (common across the stack):

- **Unauthenticated network client** — a peer speaking LDAP/Kerberos/HTTP before authenticating; untrusted.
- **Authenticated principal** — bound LDAP user, Kerberos principal, or authenticated SCIM/REST caller;
  trusted up to its directory/RBAC permissions.
- **Administrator** — a principal with directory-admin / Kerberos-admin / Fortress-superuser rights.
- **Operator / deployer** — controls server config, schema, ACIs, the Kerberos realm + keytabs, the RBAC
  policy data, TLS material, and the backing store. **Trusted; out of model as adversary (§3).**

**Component-family table (repo → domain → model placement):**

| Repo | Domain / role | Trust surface | In runtime model? |
| --- | --- | --- | --- |
| `directory-server` (ApacheDS) | LDAP v3 server (bind, search, modify, ACI), embeds Kerberos/DNS | LDAP/Kerberos network protocol | **Yes — host of this umbrella** |
| `directory-ldap-api` | LDAP protocol API + ASN.1/BER codec (client & server) | parses untrusted LDAP PDUs | **Yes** |
| `directory-kerby` | Kerberos: KDC (AS/TGS), client, crypto, PKINIT/token preauth | Kerberos network protocol; crypto | **Yes (addendum K)** |
| `directory-fortress-core` | RBAC/ARBAC policy engine over LDAP | API + policy decisions | **Yes (addendum F)** |
| `directory-fortress-enmasse` | Fortress REST service (RBAC over HTTP) | HTTP/REST | **Yes (addendum F)** |
| `directory-fortress-commander` | Fortress web admin console | HTTP/web UI | **Yes (addendum F)** |
| `directory-fortress-realm` | Fortress container security realm (Tomcat/etc.) | servlet auth integration | **Yes (addendum F)** |
| `directory-scimple` | SCIM 2.0 provisioning protocol | HTTP/JSON | **Yes (addendum S)** |
| `directory-mavibot` | MVCC B-tree storage engine (a backend) | in-process; on-disk data | **Yes (addendum M)** |
| `directory-studio` | Eclipse-based LDAP **client** tool (desktop) | parses server responses; local | Light — client tool (§3) |

## §3 Out of scope (explicit non-goals)

- **The operator/deployer as adversary**, and pure misconfiguration (weak ACIs, an open anonymous bind left
  enabled, a Kerberos realm with weak crypto enabled, RBAC policy mistakes, TLS disabled). Each component
  provides controls; configuring them is the operator's job (§10/§11) *(inferred)*.
- **The backing data stores and external systems** the components integrate with (an external LDAP, an
  external KDC, the OS keytab/credential cache), except as these components consume them.
- **`directory-studio` as a server** — it is a desktop client; its in-model surface is *parsing untrusted
  responses* from a directory it connects to, not serving requests. Findings requiring a malicious server
  the user chose to connect to are low-priority client-side hardening, not server-side `VALID`.
- **Examples, samples, integration-test harnesses, and build tooling** across all repos *(inferred)*.
- **Cryptographic-primitive internals** (the JCE provider) except as the components select and use them.

## §4 Trust boundaries and data flow

The shared boundary is **the wire-protocol request before authentication+authorization completes**. Per domain:

- **LDAP (server, ldap-api):** the BER/ASN.1 decoder is the first thing untrusted bytes hit — a classic
  parser/DoS surface. After a **bind** establishes identity (simple/SASL), **ACIs** authorize each
  operation; anonymous/unauthenticated operations are a configurable surface *(inferred — addendum L)*.
- **Kerberos (kerby):** AS-REQ/TGS-REQ from any network peer reach the KDC; pre-authentication, ticket
  issuance, encryption-type negotiation, and PKINIT/token preauth are the trust transitions; the KDC's
  master key and per-principal keys are the crown jewels *(inferred — addendum K)*.
- **Fortress (core/enmasse/commander/realm):** RBAC/ARBAC **policy decisions** are the boundary — a request
  is authorized only if the activated roles grant the permission; the REST/web tiers add HTTP auth, CSRF,
  and injection surfaces *(inferred — addendum F)*.
- **SCIM (scimple):** HTTP/JSON provisioning requests (create/modify/delete users/groups) must be
  authenticated and authorized; the filter/PATCH parsers are an injection/DoS surface *(inferred — addendum S)*.
- **Mavibot (storage):** consumes data from the server layer; its trust input is the on-disk B-tree and the
  records the server hands it — corruption-resistance and bounds, not a network boundary *(inferred — addendum M)*.

**Reachability precondition (shared):** a finding is in-model on a **server inbound** path if reachable from
an unauthenticated or under-privileged remote principal *before* the component's auth/authorization gate;
a finding that requires admin rights or a misconfiguration is `OUT-OF-MODEL` (§7/§5a).

## §5 Assumptions about the environment

- JVM services; operator-managed config, schema, ACIs, Kerberos realm/keytabs, RBAC policy, and TLS material.
- Backing storage (Mavibot or another partition backend; LDAP for Fortress policy) is on trusted disk.
- Clocks are synchronized within the Kerberos skew window *(inferred — addendum K)*.
- TLS/StartTLS, SASL, and Kerberos crypto are available and operator-configured *(inferred)*.
- **What the stack does to its host (*(inferred)* — wave-2):** binds protocol ports; reads/writes its data
  partitions and keytabs; performs crypto; the Fortress tiers open HTTP listeners and bind to a backing LDAP.
  Not assumed to execute host commands.

## §5a Build-time and configuration variants

| Knob (per domain; defaults *(inferred)*) | Effect | Ruling needed |
| --- | --- | --- |
| LDAP **anonymous bind / unauthenticated access** | Whether unauthenticated reads/ops are allowed | **Open (wave-1, L):** default on/off; supported posture? |
| LDAP **ACI default** & admin-limits | Default authorization posture | **Open (L)** |
| Kerberos **allowed enctypes** (incl. legacy RC4/DES) | Crypto strength of tickets | **Open (wave-1, K):** are weak enctypes disabled by default? |
| Kerberos **PKINIT / token-preauth** enabled | Adds preauth attack surface | Open (K) |
| Fortress **REST/console auth + TLS** | Whether RBAC management is authenticated/encrypted | **Open (wave-1, F)** |
| SCIM **auth + filter/PATCH limits** | Auth + injection/DoS posture | **Open (wave-1, S)** |
| TLS/StartTLS enforcement (all protocols) | Transport confidentiality/integrity | Operator (§10) |

## §6 Assumptions about inputs (shared trust table)

| Entry point | Parameter | Attacker-controllable? | Caller/operator must enforce |
| --- | --- | --- | --- |
| LDAP listener | BER-encoded PDU (bind, search filter, DN, controls) | **yes** | codec bounds; filter complexity limits; ACI authz |
| Kerberos KDC | AS-REQ/TGS-REQ (princ names, enctypes, preauth, PA-DATA) | **yes** | enctype policy; preauth; replay window |
| SCIM endpoint | HTTP method, JSON body, SCIM filter, PATCH ops | **yes** | authn/authz; filter/PATCH bounds; JSON limits |
| Fortress REST/console | HTTP request, RBAC operands, form fields | **yes** | authn; RBAC decision; CSRF; injection guards |
| LDAP-API codec | server/client LDAP messages | **yes** (both directions) | robust decode; no OOB/DoS |
| Mavibot | record bytes from the server; on-disk pages | from a **trusted** server layer | bounds; corruption handling |
| config / schema / ACI / keytab / policy | all | **no — operator-trusted** | never sourced from a request |

## §7 Adversary model

- **Primary adversary:** an untrusted network client of any listener (LDAP, Kerberos, SCIM, Fortress REST/UI)
  — sending crafted PDUs, malformed/oversized requests, weak-enctype or replayed Kerberos requests,
  injection payloads (LDAP filter, SCIM filter, SQL/LDAP injection via the management tiers), or
  authentication-bypass attempts.
- **Secondary:** an **under-privileged authenticated principal** attempting to exceed its ACIs / RBAC roles
  (privilege escalation); a malicious LDAP server vs. a Studio/ldap-api **client**.
- **Goals:** authenticate as another principal / bypass bind; read or modify directory entries, Kerberos
  principals, or RBAC policy beyond authorization; forge or replay Kerberos tickets; crash/DoS a service;
  corrupt stored data.
- **Out of model:** the operator; anyone holding the KDC master key, admin DN, Fortress superuser, keytabs,
  or filesystem access; the JCE/OS internals.

## §8 Security properties the project provides (shared + per-domain)

*(All *(inferred)* pending §14; symptom + severity per the rubric.)*

1. **Authentication integrity (shared).** Bind / Kerberos preauth / SCIM+REST auth verify the principal before
   privileged operations *(inferred)*. *Symptom:* auth bypass. *Severity:* critical.
2. **Authorization enforcement (shared).** LDAP **ACIs**, Fortress **RBAC/ARBAC**, and SCIM authz confine a
   principal to permitted operations *(documented — Fortress is an RBAC engine; ACI in ApacheDS; specifics
   inferred)*. *Symptom:* read/modify beyond authorization; role/permission escalation. *Severity:* critical.
3. **Robust protocol parsing (shared).** The LDAP BER codec, Kerberos ASN.1, and SCIM JSON/filter parsers
   reject malformed/oversized input cleanly rather than crashing or corrupting memory *(inferred)*. *Symptom:*
   crash/hang/OOB/DoS from crafted input. *Severity:* high–critical.
4. **(K) Kerberos ticket integrity & replay resistance.** Tickets are bound to the issuing key and protected
   by timestamps/replay caches; weak enctypes are restrictable *(inferred)*. *Symptom:* forged/replayed
   ticket; downgrade to weak crypto. *Severity:* critical.
5. **(M) Storage integrity.** Mavibot's MVCC keeps readers consistent and resists corruption from concurrent
   writes / crash *(inferred — MVCC design)*. *Symptom:* reader sees torn/inconsistent state; corruption.
   *Severity:* high.
6. **Transport security support.** TLS/StartTLS and SASL confidentiality/integrity when configured *(inferred)*.
   *Symptom:* MITM/downgrade where protection was expected. *Severity:* high.

## §9 Security properties the project does NOT provide

- **No security without configuration** — anonymous LDAP, weak Kerberos enctypes, or an unauthenticated
  Fortress REST/console expose the service to the extent the operator left them open *(inferred)*.
- **No defence against the operator / key-holder** (KDC master key, admin DN, Fortress superuser) — §3.
- **No application-level authorization beyond the configured ACIs/RBAC policy.**
- **(client) ldap-api / Studio do not protect the user against a hostile server they chose to connect to**
  beyond robust parsing.

**False friends:**

- *A successful **anonymous bind** looks like "it works" but is unauthenticated access* — many reports are
  really an operator leaving anonymous access on.
- *Kerberos "encryption" of a ticket is integrity/authenticity, not a guarantee against a weak **enctype**
  the realm still permits* — a permitted RC4/DES enctype is a downgrade surface.
- *An RBAC **role assignment** is not active permission until the session **activates** the role* (RBAC vs.
  ARBAC, separation-of-duty constraints) — naive "user has role" checks miss SoD/DSD.
- *An LDAP **search filter** is a query language, not inert data* — filter injection via unsanitized input at
  a downstream app is the LDAP analogue of SQL injection.

**Well-known attack classes to keep in view:** LDAP/SCIM **filter injection**; **anonymous-bind** exposure;
Kerberos **enctype downgrade**, **replay**, **Kerberoasting** (weak service keys), **PKINIT** edge cases;
**ASN.1/BER parser DoS**; **XXE/JSON-bomb** in SCIM/REST; **CSRF/XSS** in the Fortress console; **SoD/DSD**
policy bypass in RBAC; **password-policy / lockout** bypass.

## §10 Downstream (operator) responsibilities

- **LDAP:** disable anonymous/unauthenticated access unless intended; set restrictive ACIs and admin limits;
  enforce TLS/StartTLS; bound search filter complexity and result sizes.
- **Kerberos:** disable weak enctypes (RC4/DES); require preauth; protect the KDC master key + keytabs; keep
  clocks synced and the replay cache on.
- **Fortress:** authenticate and TLS-protect the REST endpoint and web console; protect the policy LDAP;
  configure SoD/DSD constraints deliberately.
- **SCIM:** require authentication/authorization on every endpoint; bound filter/PATCH/JSON size.
- **Storage:** protect the data partitions/keytabs at the filesystem layer; back up.
- Track ASF advisories and stay on supported lines.

## §11 Known misuse patterns

- Leaving anonymous LDAP bind/read enabled on an exposed server.
- Permitting legacy Kerberos enctypes for "compatibility".
- Exposing the Fortress REST endpoint or console without authentication/TLS.
- Building LDAP/SCIM filters by concatenating untrusted input in a downstream app (filter injection).
- Treating an RBAC role *assignment* as an active permission, ignoring activation/SoD.

## §11a Known non-findings (recurring false positives)

*(v0 seed — the PMC (LDAP/Kerberos/RBAC experts) will own the authoritative list — §14.)*

- **"Anonymous bind allowed" / "weak enctype available"** against a default/sample config — operator posture
  (§5a/§10); `OUT-OF-MODEL: non-default-build` unless the *default* is the insecure one (then `VALID` — §14).
- **"Admin can do X"** — the admin/superuser is trusted (§7); an authorized admin action is not a finding.
- **LDAP/SCIM filter injection attributed to a downstream caller** concatenating input — not a server bug (§9).
- **Findings in examples / integration tests / build tooling** across repos — out of scope (§3).
- **Studio parsing a response from a server the user connected to** — client-side hardening, not server `VALID` (§3).
- **Mavibot internal invariants** not reachable from server-accepted input — out of the input surface (§6).

## §12 Conditions that would change this model

- A change to a default auth/anonymous/enctype/anonymous-REST posture in any component.
- A new protocol surface, a new Kerberos preauth mechanism, or a new SCIM/REST endpoint enabled by default.
- A change to ACI/RBAC default semantics or to Mavibot's consistency guarantees.
- Promotion of `directory-studio` to a server role.
- Any report not cleanly routable to a §13 disposition.

## §13 Triage dispositions

| Disposition | Meaning | Licensed by |
| --- | --- | --- |
| `VALID` | Violates a claimed property via an in-scope adversary/input in a default/secure config. | §8, §6, §7 |
| `VALID-HARDENING` | No §8 property broken, but a §11 misuse is easy enough to warrant a safer default/guard. | §11 |
| `OUT-OF-MODEL: trusted-input` | Requires control of config / schema / ACI / policy / keytab. | §6 |
| `OUT-OF-MODEL: adversary-not-in-scope` | Requires operator / admin / key-holder capability. | §7, §3 |
| `OUT-OF-MODEL: unsupported-component` | Lands in examples, tests, build tooling, or Studio-as-server. | §3 |
| `OUT-OF-MODEL: non-default-build` | Only when an insecure non-default option was enabled. | §5a |
| `BY-DESIGN: property-disclaimed` | Concerns a §9-disclaimed property (no security without config; client ≠ server). | §9 |
| `KNOWN-NON-FINDING` | Matches a §11a entry. | §11a |
| `MODEL-GAP` | Routes to none of the above → revise the model. | §12 |

## §14 Open questions for the maintainers (grouped by domain)

**Wave 1 — insecure-default rulings across the stack (decide VALID-vs-misconfig):**
1. **(LDAP)** Is **anonymous bind / unauthenticated access** off by default, and is leaving it on an operator
   choice (→ misconfig) rather than a default we ship? *Proposed:* off by default; on = operator choice.
2. **(Kerberos)** Are **weak enctypes (RC4/DES)** disabled by default and is **preauth required** by default?
   *Proposed:* weak enctypes off; preauth required.
3. **(Fortress/SCIM)** Do the **REST endpoint, web console, and SCIM endpoints require authentication (+ TLS)**
   by default? *Proposed:* yes; unauthenticated exposure is operator misconfig.

**Wave 2 — authorization & protocol parsing (§8):**
4. **(LDAP)** What enforces **ACI** authorization and are there bounds on **search-filter complexity / result
   size** to prevent DoS? *Proposed:* ACI subsystem enforces; admin limits bound cost.
5. **(RBAC)** Does Fortress enforce **role activation + SoD/DSD** (not just assignment) on every decision?
   *Proposed:* yes; activation + SoD enforced.
6. **(codec)** Are the **LDAP BER and Kerberos ASN.1 decoders** hardened against malformed/oversized PDUs
   (depth/length caps)? *Proposed:* yes, bounded.

**Wave 3 — crypto, storage, §11a (§8/§9/§11a):**
7. **(Kerberos)** Replay protection + master-key handling assumptions; any known PKINIT/token-preauth caveats?
8. **(Mavibot)** What consistency/corruption guarantees does the MVCC engine make under crash/concurrent write?
9. **(all)** From the long advisory history, what do scanners most often (re)report that the PMC considers a
   **non-finding** today? (Seeds §11a per domain.)

**Meta:**
10. Confirm the umbrella shape: this document in `apache/directory-server`, the other nine repos carrying an
    `AGENTS.md`→`SECURITY.md`→(this umbrella) discoverability pointer, with the per-domain addenda above
    expanded as the PMC answers. Should **Kerby / Fortress / SCIMple** instead each carry their *own* full
    model (they are semi-independent products)? *Proposed:* umbrella + pointers now; split out later if the
    PMC prefers.

## §15 Machine-readable companion

Deferred for v0; a `threat-model.yaml` per domain can later encode the §6 trust tables, §2/§3 scoping, §8
property rows, §9 false friends, §11a non-findings, and §13 dispositions.
