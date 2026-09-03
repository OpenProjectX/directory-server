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

# Fork Maintenance Workflow (OpenProjectX)

This repository is a fork of
[apache/directory-server](https://github.com/apache/directory-server).
This file documents how the fork is maintained. It lives only on the
`fork-config` branch and is merged into `integration`.

**Never upstream this file or anything on `fork-config`.**

## Remotes

| Remote     | URL                                            | Role              |
|------------|------------------------------------------------|-------------------|
| `upstream` | `https://github.com/apache/directory-server.git`      | Apache official repo (read-only for us) |
| `origin`   | `https://github.com/OpenProjectX/directory-server.git` | Our fork          |

## Branch Model

| Branch                | Based on   | Purpose                                        | Upstreamed? |
|-----------------------|------------|------------------------------------------------|-------------|
| `master`              | `upstream/master` | Exact mirror of Apache. Fast-forward only. **Never commit here.** | n/a |
| `feature/<name>`      | `master`   | One upstreamable feature per branch.           | Yes, via PR to Apache |
| `fork-config`         | `master`   | Fork-only changes: this file, release pins, local build tweaks. | **Never** |
| `integration`         | `master`   | Our "product": master + all feature branches + fork-config. Disposable; rebuilt from scratch. | **Never** |
| `backup/<name>`       | any        | Safety snapshots. Delete when no longer needed.| No          |

## The Golden Rules

1. **Never commit directly to `master`.** It only tracks Apache.
2. **The unit of management is the feature branch, not the commit.**
3. **`integration` is a function, not a history:**
   `integration = master + merge(active feature branches) + fork-config`.
   It is rebuilt from scratch on every sync; never maintained, never
   cherry-picked from, never used as the base for a feature branch.
4. **Never base a feature branch on `integration`** — base on `master`,
   otherwise fork-only and unrelated changes leak into your Apache PRs.

## Creating a Feature Branch

```sh
git checkout master
git pull --ff-only upstream master
git checkout -b feature/my-feature master
```

- One logical change per branch. Small branches get upstreamed faster and
  conflict less.
- Keep changes **additive** where possible (new files/modules conflict far
  less than edits to shared files).
- No reformatting, import reordering, or renaming in files the feature does
  not own — cosmetic diffs are the top source of merge conflicts.

## Developing

- Commit early, commit often on the feature branch. History will be
  cleaned up (or squashed by Apache) at upstream time.
- If feature B depends on feature A, base B on A's branch (stacked
  branches). B can only go upstream after A has landed.
- Work from `integration` when you need the full product locally, but
  commit the work to the owning feature branch, then rebuild
  `integration`.

## Syncing with Upstream (do this often — weekly or more)

Small frequent syncs produce small conflicts. Infrequent syncs produce
one giant conflict.

```sh
# 1. Update the mirror
git checkout master
git pull --ff-only upstream master
git push origin master

# 2. Rebuild integration
git checkout -B integration master
git merge --no-edit feature/docker-testcontainers   # each active feature
git merge --no-edit fork-config                      # always last
git push --force-with-lease origin integration
```

`--force-with-lease` is required because the rebuilt `integration` has new
history. It still refuses to overwrite someone else's unexpected push.

Feature branches do not need to be rebased on every sync — the
integration merge absorbs upstream changes. Rebase a feature branch only
when conflicts keep recurring, or right before upstreaming:

```sh
git rebase master feature/my-feature
git push --force-with-lease origin feature/my-feature
```

`git rerere` is enabled in this repository: conflict resolutions are
recorded and replayed automatically on later rebases/merges.

## Deploying

- **Build and deploy only from `integration`.** `master` has no fork
  changes; feature branches are incomplete on their own.
- Docker images are built from `integration`:

  ```sh
  ./mvnw -pl docker-image -am -Pdocker package -DskipTests \
    -Dapacheds.docker.image=apachedirectory/apacheds:local
  ```

- Tag releases of the fork from `integration`, e.g.
  `git tag -a v1.0.0-fork.1 -m "..." <integration-sha>`.

## Upstreaming a Feature to Apache

```sh
git checkout master && git pull --ff-only upstream master
git rebase master feature/my-feature           # linear, current history
git push --force-with-lease origin feature/my-feature
```

Then open a PR from `feature/my-feature` to `apache/directory-server`.

- Expect Apache to **squash or modify** your commits during review. The
  merged upstream commit(s) will not match your local SHAs — that is
  normal.
- **After the PR is merged upstream, do not reconcile your branch with
  theirs. Delete it:**

  ```sh
  git branch -D feature/my-feature
  git push origin --delete feature/my-feature
  # rebuild integration (see above) — the feature now arrives via master
  ```

- Check whether a branch (or parts of it) is already upstream with
  `git cherry upstream/master feature/my-feature`
  (`-` = patch-equivalent exists upstream, `+` = not yet).

## Out-of-Order Upstreaming and Long-Lived Features

Features land upstream in whatever order Apache merges them — this does
not conflict with our commit order, and here is why:

- **Merges operate on tree content, not commit sequence.** Rebuilding
  `integration` merges each still-local feature branch onto the new
  `master`. If feature B was upstreamed last week, its content is already
  in `master`; the rebuild simply no longer merges B's branch.
- **A long-lived feature just stays a branch.** It keeps getting merged
  into every `integration` rebuild until it is upstreamed. Nothing
  special to do.
- **A partially upstreamed branch** (Apache took some commits, e.g. via a
  squashed PR): `git rebase master feature/x` automatically skips
  commits that are patch-equivalent to upstream. What remains is the
  not-yet-merged delta. If the whole branch is upstream, delete it.
- **Never try to keep your original version of an upstreamed feature.**
  Apache's version wins. Delete your branch; the feature lives on in
  `master`.

Mental model: at any moment, your fork = `upstream/master` + the *set* of
active feature branches + `fork-config`. Time and order do not appear in
that equation.

## Troubleshooting

| Situation                    | Fix                                             |
|------------------------------|-------------------------------------------------|
| Rebase/merge going wrong     | `git rebase --abort` / `git merge --abort`      |
| Lost commits                 | `git reflog` — commits survive ~30 days         |
| Repeating the same conflict  | Already handled: `git rerere` is enabled        |
| Need old fork state          | `backup/master-before-upstream-sync` branch     |
| Accidentally committed on master | `git branch feature/x` (rescue), then `git reset --hard upstream/master` |
