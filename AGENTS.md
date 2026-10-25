# Project instructions for Codex

This repository uses `claude-workspace` (project preset). Read `CLAUDE.md`
for the shared project workflow and `.claude/workspace.yaml` for its manifest.
Apply the core behaviors before each matching action, and consult all relevant
skills, not just the first match. User instructions and host instructions take
precedence over skill instructions.

## Skills

The canonical skills, including their references, scripts, data and templates,
live in `.claude/skills/<name>/`. The generated `.agents/skills/<name>/SKILL.md`
files expose their original metadata to Codex and direct it to those sources.
Read the full canonical SKILL.md before using a skill. Prefer this repository's
version over a same-named personal skill when working here.

At session start, read `.claude/skills/using-superpowers/SKILL.md`. For discovery,
inspect the name and description in each `.claude/skills/*/SKILL.md`, including
skills not listed in CLAUDE.md. Load full instructions and references only as
needed. The core skills are `health-review`, `commit-discipline` and `codegraph`.
Their triggers apply continuously; a health review is not required for every edit.

After `claude-workspace sync` or adding/removing skills, run:

```powershell
./scripts/sync-codex-skills.ps1
./scripts/sync-codex-skills.ps1 -Check
```

The script never changes Claude sources or removes stale adapters automatically.

## Tool and path adaptation

- `Read`, `Write`, `Edit`, `Grep`, `Glob`, and `Bash` mean the available file,
  patch, search and command tools. This workspace uses Windows PowerShell;
  translate shell syntax and use `rg` for searches.
- `Skill` means read the canonical SKILL.md and follow it. Claude slash commands
  are workflow names, not automatically registered Codex commands.
- `Task` and subagent instructions map to the available collaboration tools,
  subject to the host's delegation rules. Use only current allowed model IDs;
  do not copy model names or unsupported parameters from old examples.
- `WebSearch`/`WebFetch` map to available search/page retrieval tools. Browser
  and computer actions use available host browser/computer tools and their
  required skills. Do not assume Claude-specific MCP names exist in Codex.
- Resolve skill-local paths from the canonical `.claude/skills/<name>` folder.
  Examples using `~/.claude/skills/<name>` should use this repository's folder
  when that script is supplied here. Do not invent absent scripts.
- Use a working Python runtime; the WindowsApps `python.exe` alias may be
  unusable. The Codex workspace dependency tool can locate bundled Python.
  Node is available. Unix shell scripts require a functioning Bash environment.
- CodeGraph requires connected `codegraph_*` MCP tools and a project index.
  If unavailable, disclose this and use `rg`/file reads for ordinary navigation.
  Installing a skill does not connect an MCP server.
- `.claude/settings.local.json` hooks and `.claude/agents` definitions are not
  Codex registrations. Read agent prompts as references when the relevant
  workflow calls for them. For UI changes, run the Impeccable checks explicitly
  according to its hook reference; do not claim automatic hooks executed.
  On Windows use `.claude/skills/impeccable/scripts/impeccable.cmd`.
- Some visual workflows reference absent `ai-artist`, `ai-multimodal`,
  `chrome-devtools`, or `frontend-design` skills. Use available equivalent
  skills/tools where appropriate and disclose substitutions. Gemini-specific
  scripts need their own packages and API credentials; never infer access
  from the skill's presence or expose credentials.

See `docs/codex-skills.md` for the verification scope and known limitations.
