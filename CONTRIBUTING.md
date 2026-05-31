# Contributing to Polychro

> We welcome **all** contributions to Polychro, from the smallest to the largest — they all make a positive impact. This guide applies to both human developers and AI-assisted coding agents.

---

## TL;DR

1. Open an **Issue** before starting work
2. Fork the repo and branch from `main` (`feat/`, `fix/`, `chore/`)
3. Keep PRs **atomic**, rebased on `main`, with CI green
4. A **maintainer** will review and merge your PR
5. All contributions are accepted under the [Apache 2.0 License](https://github.com/naftiko/polychro/blob/main/LICENSE.md)

---

## Local Bootstrap

Before contributing, ensure your local environment has at least JDK 21 and Maven.

**Required:** JDK 21, Maven 3.9+

```bash
java -version    # must be 21+
mvn -version     # must be 3.9+
```

---

## Bugs & Features

- Report bugs and suggest features in the [Issue Tracker](https://github.com/naftiko/polychro/issues)
- Please **search existing issues** before creating a new one to avoid duplicates
- When opening an issue, select the appropriate **template** — GitHub will guide you through the required fields:
  - **Bug Report** — for unexpected behavior or broken functionality
  - **Feature Request** — for new capabilities or improvements
- Discuss the issue directly in the thread before starting implementation

---

## Code Contributions

### 1. Fork & branch

- Fork the repository and create a branch from `main`
- Follow branch naming conventions:

| Prefix | Purpose |
|--------|-------------------------------|
| `feat/` | New feature or capability |
| `fix/` | Bug fix |
| `chore/` | Maintenance, deps, refactoring |

### 2. Develop

- Follow [Conventional Commits](https://www.conventionalcommits.org/) for commit messages:

        feat: add JSON Schema validation
        fix: handle null pointer in rule evaluation
        chore: bump dependencies

- **Rebase on `main`** before requesting a review — we maintain a linear history (no merge commits)
- Use `git push --force-with-lease` (never `--force`)
- Run the test suite before opening your PR:

        mvn clean test --no-transfer-progress

### 3. Open a Pull Request

Submit your work via a [Pull Request](https://github.com/naftiko/polychro/pulls):

- [ ] Link the related Issue
- [ ] Ensure **CI is green** (build, tests)
- [ ] Keep it **small and focused** — one concern per PR

### 4. Review & merge

- A maintainer will be assigned automatically, or you can mention `@naftiko/core`
- Expect feedback within a few business days
- Address requested changes by pushing new commits
- Once approved, a **maintainer will handle the merge** — you don't need to do anything else

> 💡 **First-time contributors**: don't hesitate to open a draft PR early if you want guidance before your work is complete.

---

## For AI Agents

This section provides **machine-readable guidance** for AI coding agents contributing to this repository.

### Repository context

- **Language**: Java 21 (Maven multi-module build)
- **Description**: Polyglot spec linting engine — validates YAML, JSON, XML, Markdown, and HTML specs against schemas and rulesets through a composable pipeline (well-formedness → schema-model → ruleset → format-aware)
- **Modules**: `polychro-api`, `polychro-core`, `polychro-wellformedness`, `polychro-json-schema`, `polychro-json-structure`, `polychro-ruleset`, `polychro-markdown`, `polychro-html`, `polychro-cli`, plus Go / Node.js / Python SDKs
- **Documentation**: the [Naftiko Shipyard](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/) hosts the full Polychro documentation (Features, Architecture, CLI, Guides, Tutorial, FAQ)

### Agent contribution rules

- Follow **all human contribution rules** above — no exceptions
- Branch, commit, and PR naming conventions are mandatory
- PRs must pass all CI checks
- Keep changes **atomic**: one logical change per PR
- Always include a clear PR description explaining the problem and solution
- Do **not** modify CI/CD workflows, security configs, or branch protection rules

### Key files for agent context

| File / Path | Purpose |
|---|---|
| `polychro-api/` | SPI contracts — `Validator`, `Diagnostic`, `Document` |
| `polychro-core/` | Pipeline orchestrator (`Linter`, formatters) |
| `polychro-wellformedness/` | Well-formedness validation |
| `polychro-json-schema/` | JSON Schema validation engine |
| `polychro-json-structure/` | JSON / YAML structure validation |
| `polychro-ruleset/` | Spectral-format ruleset model and evaluation |
| `polychro-rulesets/` | Built-in rulesets (`governance`, `ai-safety`, `security`) |
| `polychro-markdown/` | Markdown structural validation |
| `polychro-html/` | HTML structure / accessibility / security validation |
| `polychro-cli/` | Picocli CLI (`lint`, `serve`) |
| `pom.xml` | Parent POM with module declarations |

---

## License

All contributions are accepted under the [Apache 2.0 License](https://github.com/naftiko/polychro/blob/main/LICENSE.md).

> ⚠️ You must ensure you have **full rights** on the code you are submitting, for example from your employer.
