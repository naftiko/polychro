---
description: "Use when: reviewing a PR, reviewing a pull request, doing a code review, posting inline comments on a PR, reviewing PR #<number>, fixing PR comments, addressing PR feedback, fixing PR #<number>, addressing review comments, fixing Copilot comments, resolving PR threads; OR drafting release notes, writing release notes for a tag (release-notes-drafting skill). Tells the agent how to fetch a shared skill from the agents-shared capability before following it."
name: "agents-shared skill discovery"
---
# Shared agent skills — `agents-shared` capability

The transversal skills served to this repository — `pr-review` and `release-notes-drafting`
(and, over time, others) — are **not** committed to this repository. They are versioned once
in the
[`golden-repo-naftiko`](https://github.com/naftiko/golden-repo-naftiko) golden repo and served
read-only by the `agents-shared` Ikanos capability (Skill Server, `type: skill`).

## How to use a skill

1. **Check for a local copy first.** If `.agents/skills/<skill>/SKILL.md` exists in this
   repository (where `<skill>` is `pr-review` or `release-notes-drafting`), read it and
   follow it — it is a synced, git-ignored copy.

2. **If missing (or the user asks to refresh it), sync it from the Skill Server.**
   The server URL defaults to `http://localhost:9700`; the `AGENTS_SHARED_URL` environment
   variable overrides it. Run the one-liner for the current OS:

   Windows (PowerShell):

   ```powershell
   $u = if ($env:AGENTS_SHARED_URL) { $env:AGENTS_SHARED_URL } else { "http://localhost:9700" }; Invoke-WebRequest "$u/skills/<skill>/download" -OutFile "$env:TEMP\skill.zip"; Expand-Archive "$env:TEMP\skill.zip" -DestinationPath ".agents/skills/<skill>" -Force
   ```

   Linux / macOS (bash):

   ```bash
   u="${AGENTS_SHARED_URL:-http://localhost:9700}"; curl -fsSL "$u/skills/<skill>/download" -o /tmp/skill.zip && mkdir -p .agents/skills/<skill> && unzip -o /tmp/skill.zip -d .agents/skills/<skill>
   ```

3. **Then read `.agents/skills/<skill>/SKILL.md` and follow it** for the requested task.
   Do not improvise the workflow from memory.

## If the Skill Server is not running

The capability runs from a local checkout of `golden-repo-naftiko` via `ikanos serve` — see
`agents-shared/README.md` in that repo for setup (including the absolute `location:`
rewrite) and for a `gh api` fallback that needs no server. Ask the user to start the
server or fetch the skill via the fallback; do not copy the skill into version control.

## Rules

- The synced copy under `.agents/skills/` is **git-ignored** — never commit it.
- Never edit the synced copy; improvements go to `golden-repo-naftiko` (issue-first).
