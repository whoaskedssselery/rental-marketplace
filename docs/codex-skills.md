# Claude workspace skills in Codex

The repository has a `claude-workspace` project preset (toolVersion 1.0.3)
and 38 skill directories under `.claude/skills`. The manifest records preset
and upstream sources; it is not an exhaustive directory inventory.

Codex adapters under `.agents/skills` retain each canonical skill's frontmatter
and point to the full original instructions and resources. This avoids copying
scripts or maintaining two divergent instruction bodies. Root `AGENTS.md`
provides persistent workflow and tool mappings. The scope is this repository;
personal/global skills and Claude configuration are unchanged.

Both `.claude/` and `.agents/` are currently excluded by the user's global Git
ignore. These skill installations are local to this checkout. The sync script,
AGENTS.md and this report can be committed normally; another checkout must also
have the canonical Claude workspace installed before running the sync script.
The migration does not override the user's global ignore or stage any files.

Official discovery documentation:
https://learn.chatgpt.com/docs/build-skills

## Verification

Run `./scripts/sync-codex-skills.ps1 -Check` from PowerShell to compare every
adapter with its canonical source metadata and expected forwarding instructions.
It fails for missing, outdated or stale generated adapters. Run without `-Check`
to create/update adapters; remove stale ones only after reviewing them.

Discovery validation is not an end-to-end execution of 38 workflows. Destructive
operations, deployments, commits, remote API generation and browser workflows
must be checked in the context of an actual authorized task. Existing chats may
need a new turn or restart to refresh the host's discovered skill list. The
canonical files can already be read directly in the current chat.

Checks performed on 2026-10-25:

- All 38 generated adapters matched their source frontmatter and forwarding text.
- Python AST parsing passed for all 35 Python files; Node syntax checking passed
  for all 15 `.js`/`.cjs` files. These are syntax checks, not workflow executions.
- UI/UX accessibility search, logo style search and CIP deliverable search returned
  results using bundled Python.
- Impeccable's Windows launcher returned version `4.0.0`.
- The design-token validator ran against its supplied templates and reported no
  token violations. This was a tool smoke test, not an audit of the application.
- The bundled Python has no `google` package, so its Gemini scripts cannot run
  without installing `google-genai`. API credentials and API calls were not tested.

## Known capability limits

| Area | Finding and adaptation |
| --- | --- |
| CodeGraph | CLI is on PATH, but no CodeGraph MCP tools are exposed in this chat or listed in `codex mcp list`; no repository `.codegraph` directory was found. The CodeGraph workflow cannot run until the server and index are configured. |
| Python | The WindowsApps alias fails to start. Use bundled Python located with Codex's workspace dependency tool. |
| Impeccable | Windows launcher and bundled Windows x64 engine exist. Claude PostToolUse/Stop hooks are not registered in Codex; invoke relevant checks explicitly for UI work. |
| Design generation | Logo/CIP/icon scripts include Gemini-specific requirements. Their presence does not establish package, credential or API availability. Native image generation is an alternative where the task permits it. |
| Banner helpers | `ai-artist`, `ai-multimodal`, and `chrome-devtools` referenced by banner-design are absent here. Use available image/browser tools and relevant design skills; the original helper commands cannot run as written. |
| Other skill names | `frontend-design`, `ai-artist`, `ai-multimodal` and `learning-guard` are referenced but not installed as local skills. Do not invent their instructions. |
| Claude agents and tools | Claude agent files are prompts, not Codex agent registrations. Map tools to host equivalents and follow current host permissions and delegation rules. |
| Unix scripts | Shell helpers need a functioning Bash runtime; a `bash.exe` PATH entry alone does not prove availability. |

Instruction-only skills are readable and portable through these mappings.
Tool-dependent branches remain conditional on the capabilities above.
