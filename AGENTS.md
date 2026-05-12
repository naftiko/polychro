# Polychro — Agent Guidelines

## Project Context

**Polychro** is a polyglot spec linting engine. Rules are declared in YAML rulesets; validators are
pluggable via a Java SPI. The engine can lint YAML, JSON, Markdown, and OpenAPI documents and
exposes its results as SARIF, JSON, text, or LLM-native (agent) output.

- **Language**: Java 21, Maven build system (multi-module)
- **Wiki**: https://github.com/naftiko/polychro/wiki

### Module map

| Module | Role |
|---|---|
| `polychro-api` | SPI contracts (`Validator`, `ValidatorFactory`, `Diagnostic`, `Document`, `SourceRange`, `ValidatorConfig`) |
| `polychro-core` | Orchestrator (`Linter`, formatters, `DiagnosticFormatter`) |
| `polychro-wellformedness` | Built-in well-formedness validator |
| `polychro-json-schema` | JSON Schema validator |
| `polychro-json-structure` | JSON/YAML structure validator |
| `polychro-markdown` | Markdown validator |
| `polychro-checkov` | Checkov bridge validator |
| `polychro-ruleset` | Ruleset model and loader |
| `polychro-ruleset-polyglot` | Polyglot ruleset support |
| `polychro-rulesets` | Built-in rulesets (`ai-safety`, `governance`, `security`) |
| `polychro-capability` | Naftiko/Ikanos MCP adapter exposing Polychro as an MCP server |
| `polychro-cli` | Picocli CLI (`lint`, `serve` commands) |
| `polychro-github-action` | GitHub Action wrapper |
| `polychro-go` | Go SDK (thin binary wrapper) |
| `polychro-node` | Node.js / TypeScript SDK (thin binary wrapper) |
| `polychro-python` | Python SDK (thin binary wrapper) |
| `polychro-docs` | Wiki sources |

---

## Key Files

| Path | Purpose |
|---|---|
| `polychro-api/src/main/java/io/polychro/spi/` | SPI contracts — source of truth for the plugin API |
| `polychro-core/src/main/java/io/polychro/core/Linter.java` | Orchestrator + Builder |
| `polychro-core/src/main/java/io/polychro/core/AgentFormatter.java` | LLM-native JSON formatter |
| `polychro-capability/src/main/java/io/polychro/capability/PolychroCapability.java` | MCP capability entry point |
| `action.yml` | GitHub Action definition (repo root) |
| `pom.xml` | Parent POM — dependency versions, JaCoCo config, module list |

---

## Build & Test

All commands must be run from the repository root (`polychro/`).

```bash
# Full build + tests (standard workflow — requires JDK 21 + Maven 3.9+)
mvn -B clean verify

# Faster iteration (skip JaCoCo coverage check)
mvn clean test --no-transfer-progress

# Pre-PR validation
mvn -B clean verify
```

**Required:** JDK 21, Maven 3.9+

```bash
java -version    # must be 21+
mvn -version     # must be 3.9+
```

### Polyglot SDKs

```bash
# Go
cd polychro-go && go test ./...

# Node.js / TypeScript
cd polychro-node && npm install && npm test

# Python
cd polychro-python && pip install -e ".[dev]" && pytest
```

### Known Bootstrap Issue — `polychro-capability`

`polychro-capability` depends on `io.ikanos:ikanos:1.0.0-alpha3-SNAPSHOT` (the Naftiko framework,
formerly named `framework`), which is not yet published to GitHub Packages or Maven Central.
Before running `mvn -B clean verify` for the first time, install it locally:

```bash
# From the ikanos repo root (local folder: ../framework/ — pending rename)
cd ../framework
mvn install -DskipTests
cd ../polychro
```

The CI workflow excludes `polychro-capability` and `polychro-cli` (`--pl '!polychro-capability,!polychro-cli'`) until the SNAPSHOT
is published to GitHub Packages (`maven.pkg.github.com/naftiko/ikanos`). Once published,
remove the exclusion and add the `setup-java` `server-id: github-naftiko` credentials block.

The framework's `maven-shade-plugin` does not fully inline restlet and the MCP SDK into the
published jar, causing `NoClassDefFoundError` at test time (tracked in
[naftiko/framework#433](https://github.com/naftiko/framework/issues/433)).
The workaround (explicit `org.restlet:org.restlet:2.7.0-m2` and
`io.modelcontextprotocol.sdk:mcp-core:1.0.0` dependencies + Talend repository) is already
applied in `polychro-capability/pom.xml` — all 31 `PolychroCapabilityTest` tests pass locally.

**Separate known issue:** `polychro-rulesets` fails the JaCoCo coverage check on `main` — this
is a pre-existing gap unrelated to the framework bootstrap.

---

## Apache License Header

Every Java source file must begin with this exact header:

```java
/**
 * Copyright 2026 Naftiko
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
```

---

## Code Style

**Java** — follows Google Style. Configure VS Code with `Language Support for Java by Red Hat` and
apply settings from [naftiko/code-standards — java](https://github.com/naftiko/code-standards/tree/main/java).

**Method visibility** — prefer package-private (no modifier) over `private` for methods that
implement non-trivial logic. This allows direct unit testing from the same package without
reflection. Reserve `private` for truly internal helpers trivially covered by public API tests.

**Coverage** — JaCoCo 100% line + branch coverage is enforced per module. Every new code path
must be tested.

Never modify CI/CD workflows (`.github/workflows/`), security configs, or branch protection rules.

---

## Test Writing Rules

**Do:**
- Identify the test type before writing any setup code: **unit test** (isolated, in-process, no
  external I/O) or **integration test** (exercises the full linting stack against real fixtures).
  If the type is ambiguous, **ask before writing any code**.
- Test behavior through the public API — assert observable outcomes, not implementation details.
- When a method is not accessible from a test, make it package-private in production code (remove
  `private`) rather than using reflection.
- Write one focused assertion per test, or group only closely related assertions.
- Name tests in the form `methodShouldDoSomethingWhenCondition`.
- Place test fixtures (YAML/JSON/Markdown files) under `src/test/resources/` of the relevant module.

**Don't:**
- Use `getDeclaredMethod` / `setAccessible(true)` to access non-public methods.
- Write tests whose only purpose is to reach a coverage threshold — every test must document a
  real behavior or guard against a real regression.
- Name tests `shouldCoverXxxBranches` or similar — names must describe behavior, not structure.
- Group unrelated scenarios in a single test method.

---

## PR Review Skill

To review a pull request, use the `pr-review` skill from the ikanos repo:

```
../framework/.agents/skills/pr-review/
```

> **Note:** the ikanos repo was renamed from `framework` to `ikanos` on GitHub, but the local
> folder rename is pending — use `../framework/` until further notice.

Run the appropriate context script at the start of every review:
- Windows: `.\.agents\skills\pr-review\pr-context.ps1 -Pr <number>`
- Linux/macOS: `bash .agents/skills/pr-review/pr-context.sh <number>`

Follow `SKILL.md` in that directory for the full step-by-step workflow.

> **TODO:** migrate to a shared Ikanos skill server so this dependency on a sibling repo is removed.

---

## Contribution Workflow

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full workflow. Key rules:

- **Open an Issue before starting work** — for every change (feat, fix, chore, doc). Propose
  opening a GitHub issue and wait for confirmation. The user may explicitly waive the issue step;
  only then proceed without one.
- **All GitHub interactions must be in English** — issues, PR titles/bodies, inline review
  comments, and commit messages.
- Branch from `main`: `feat/`, `fix/`, or `chore/` prefix.
- Use [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `chore:` —
  no scopes for now.
- `AGENTS.md` improvements are `feat:`, not `chore:`.
- Rebase on `main` before PR — linear history, no merge commits.
- One logical change per PR — keep it atomic.
- CI must be green (build + tests).
- Always read the repository templates before creating issues or PRs:
  - Issues: `.github/ISSUE_TEMPLATE/` — use the matching template, fill in all required fields.
  - PRs: `.github/PULL_REQUEST_TEMPLATE.md` — follow the structure exactly, do not improvise.
- Before submitting a PR body, explicitly confirm to the user that the template has been followed
  (section by section) — without necessarily displaying the full body unless asked.
- When creating issues or PRs with multiline bodies via `gh`, **never construct the body as a
  string in the terminal** — always write the body to a temp `.md` file using the file creation
  tool, then pass it via `--body-file "/path/to/file.md"`.
- Do **not** use `git push --force` — use `--force-with-lease`. This applies everywhere:
  feature branches, fix branches, rebases.
- When the user corrects a mistake, note it immediately and propose an `AGENTS.md` update
  if the correction reveals a missing or wrong rule.

---

## Bug Workflow

When you identify a bug, follow these steps **in order**:

### 1. Open an Issue

Create a GitHub issue using the **Bug Report** template. Fill in all required fields: component,
description (actual vs expected), steps to reproduce, root cause if known, proposed fix.

### 2. Create a dedicated branch from up-to-date `main`

If work is in progress on the current branch, stash it first:

```bash
git stash push -m "wip: <description>"
git checkout main
git pull origin main
git checkout -b fix/<short-description>
```

Never start a fix branch from a feature branch or a stale local `main`.

### 3. Write non-regression tests before committing the fix

1. Write the failing test first (only modify `src/test/`).
2. Run `mvn test` — confirm the new test **fails** (proving the bug exists).
3. Implement the fix in `src/main/`.
4. Run `mvn test` — confirm all tests **pass**.

Do not edit production code and test code in the same phase.

---

## Further Considerations

- **Shared skill server**: the `../framework/.agents/skills/pr-review/` reference is a temporary
  workaround. The target architecture is an Ikanos Capability (`agents-shared`) running as an
  offline MCP server, serving skills to any consumer repo without file-system coupling.
- **Local repo rename**: `c:\work\repos\framework\` → `c:\work\repos\ikanos\` pending.
