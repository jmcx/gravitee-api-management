# Agentic API Platform (Vercel demo) — Design Context

Source: `https://agenticapiplatform.vercel.app/`

## Important note about “source code”

This Vercel deployment does **not** expose a public repository link, and the shipped JS bundle does not contain a usable repo URL. That means we **cannot legitimately “fetch the source code”** from the deployment beyond the compiled/minified frontend assets that browsers already download.

If you want the real source code as Figma Make context, you’ll need **one of**:
- A public Git repo URL for this project, or
- Access granted by the project owner (repo invite / zip / export).

## Included context assets (screenshots)

These are captured to seed a new design:
- `connectors.png` — Catalog → Connectors (card grid with statuses)
- `llms.png` — Catalog → LLMs (table)
- `agents.png` — Catalog → Agents (table)

## UI structure (high-level)

- **Top app bar**
  - Left: Gravitee logo
  - Right: icon buttons (search, notifications), theme toggle, user avatar

- **Left sidebar**
  - Section groups: Catalog / Services / Security / Builders / Developer Portal
  - Catalog sub-items include: Connectors, LLMs, MCP Servers, Agents, APIs, Topics, Scoring/Quality, Federation, Orchestrations, Agentic Lineage
  - Bottom: Organization + Environment selectors, then Settings + Support

- **Main content area**
  - Breadcrumbs, page title + short description
  - Page actions on the right (ex: Import / Add New; Sync All / Add Connector)
  - Filter/search row
  - Primary content: either a **card grid** (Connectors) or **table** (Agents/LLMs)

## Visual style cues

- Clean enterprise UI, light theme (also supports dark theme via toggle)
- **Primary accent** appears to be Gravitee orange for primary CTAs
- Status pills/chips (Connected / Available / Deployed / Active / etc.)
- Lots of whitespace; subtle borders; rounded cards

---

## Figma Make prompt (copy/paste)

Add an agentic chat side window.

### Requirements
- Add a **right-side “Agentic Assistant” panel** that can be opened/closed from the top app bar (new chat icon near search/notifications) and/or a floating toggle button.
- The panel should be **resizable** (drag handle) and support these widths: 360px (compact), 480px (default), 640px (wide).
- When open, the panel should **not cover** the left sidebar; it should push/shrink the main content area or overlay only the main content (choose the best UX and keep it consistent).
- Provide **3 states**:
  - Closed (only toggle icon visible)
  - Open (default)
  - Open + “Action preview” mode (assistant proposes a change with a diff/preview card)

### Panel layout
- Header: “Agentic Assistant”, environment context (Org/Env), model selector (optional), and a close button.
- Body:
  - Conversation stream (assistant + user messages)
  - “Citations / context” area that references the current page (e.g., “Catalog / Agents” or selected row)
- Footer:
  - Message composer with attachments (context chips like “Current page”, “Selected items”, “Filters”)
  - Quick actions relevant to the page (e.g., on Connectors: “Import APIs”, “Connect provider”; on Agents: “Draft new agent”, “Enable tool”)

### Agentic behaviors (UI only)
- The assistant can propose **actions** with confirm/cancel:
  - Create / import / update entities (connectors, LLMs, agents)
  - Suggest filters/search
  - Generate a draft (agent definition) with a review step
- Include “Run” and “Explain” buttons for proposed actions and show progress states.

### Design alignment
- Match the existing visual language: enterprise admin UI, subtle borders, rounded corners, status chips, Gravitee orange primary actions, light/dark theme support.
- Ensure keyboard accessibility and that the panel works across all Catalog pages.

