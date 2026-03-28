"""
Multi-Agent System for Android development.

Agents:
  1. ProductManager  - Collects user input, writes requirements, asks for validation
  2. TicketWriter    - Breaks requirements into actionable tickets
  3. UIUXDesigner    - Designs UI/UX based on tickets
  4. Architect       - Plans technical architecture
  5. Developer       - Implements the feature
  6. QA              - Writes tests to ensure behaviour

Usage:
    pip install anthropic
    export ANTHROPIC_API_KEY=sk-ant-...
    python agents/multi_agent.py
"""

import os
import json
import textwrap
from dataclasses import dataclass, field
from typing import Optional
import anthropic

client = anthropic.Anthropic()
MODEL = "claude-opus-4-6"


# ---------------------------------------------------------------------------
# Base Agent
# ---------------------------------------------------------------------------

@dataclass
class Message:
    role: str
    content: str


class Agent:
    """A single-turn or multi-turn agent with a fixed system prompt."""

    name: str
    system: str

    def run(self, prompt: str, history: Optional[list[Message]] = None) -> str:
        messages = [{"role": m.role, "content": m.content} for m in (history or [])]
        messages.append({"role": "user", "content": prompt})

        response = client.messages.create(
            model=MODEL,
            max_tokens=8096,
            thinking={"type": "adaptive"},
            system=self.system,
            messages=messages,
        )
        return next(b.text for b in response.content if b.type == "text")


# ---------------------------------------------------------------------------
# Specialized Agents
# ---------------------------------------------------------------------------

class ProductManager(Agent):
    name = "ProductManager"
    system = textwrap.dedent("""\
        You are a senior Product Manager for a mobile Android app.

        Your job:
        1. Receive raw user input (ideas, pain-points, goals).
        2. Clarify ambiguities by asking targeted follow-up questions (one at a time).
        3. Once you have enough context, produce a structured Product Requirements Document (PRD):
           - Problem statement
           - Goals & success metrics
           - User stories (As a <user>, I want <goal>, So that <benefit>)
           - Out of scope
           - Open questions

        Always end your PRD with the exact line:
            READY_FOR_REVIEW

        Be concise. Avoid padding.
    """)

    def gather_requirements(self, raw_input: str) -> str:
        """Iteratively refine requirements, asking the user questions until the PRD is ready."""
        history: list[Message] = []
        prompt = raw_input

        print("\n[ProductManager] Analysing your request…\n")

        while True:
            reply = self.run(prompt, history)
            history.append(Message("user", prompt))
            history.append(Message("assistant", reply))

            if "READY_FOR_REVIEW" in reply:
                prd = reply.replace("READY_FOR_REVIEW", "").strip()
                return prd

            # The PM has a follow-up question
            print(f"[ProductManager]\n{reply}\n")
            prompt = input("Your answer: ").strip()
            if not prompt:
                prompt = "(no additional information)"


class TicketWriter(Agent):
    name = "TicketWriter"
    system = textwrap.dedent("""\
        You are a technical Product Owner who writes clear, actionable tickets for a software team.

        Given a PRD, produce a JSON array of tickets. Each ticket must have:
          - id: "TICKET-001", "TICKET-002", …
          - title: short imperative sentence
          - type: "feature" | "design" | "architecture" | "test"
          - description: what needs to be done (2–5 sentences)
          - acceptance_criteria: bullet list of verifiable conditions
          - assigned_to: one of "UIUXDesigner" | "Architect" | "Developer" | "QA"
          - depends_on: list of ticket ids this ticket depends on (may be empty)

        Return ONLY valid JSON — no markdown fences, no extra text.
    """)

    def write_tickets(self, prd: str) -> list[dict]:
        raw = self.run(f"PRD:\n\n{prd}")
        return json.loads(raw)


class UIUXDesigner(Agent):
    name = "UIUXDesigner"
    system = textwrap.dedent("""\
        You are a senior UI/UX Designer specialising in Android Material 3 apps.

        Given a ticket, produce a detailed design specification:
          - Screen layout description (components, hierarchy)
          - Key user interactions and transitions
          - Accessibility considerations (content descriptions, contrast, touch targets)
          - Material 3 component recommendations
          - Any assets or icons needed

        Be precise enough that a developer can implement the screen without further design input.
        Use Markdown for formatting.
    """)

    def design(self, ticket: dict) -> str:
        prompt = f"Ticket:\n{json.dumps(ticket, indent=2)}"
        return self.run(prompt)


class Architect(Agent):
    name = "Architect"
    system = textwrap.dedent("""\
        You are a senior Android architect with deep expertise in:
          - Jetpack Compose + Material 3
          - Clean Architecture (Domain / Data / Presentation layers)
          - Kotlin Coroutines & Flow
          - Dependency injection (Hilt)
          - Room, DataStore, Retrofit

        Given a ticket and optional design spec, produce:
          - Module/package structure changes
          - New classes/interfaces with their responsibilities
          - Data flow diagram (text-based)
          - Key design patterns to apply
          - Risks and mitigations

        Use Markdown for formatting.
    """)

    def plan(self, ticket: dict, design_spec: Optional[str] = None) -> str:
        parts = [f"Ticket:\n{json.dumps(ticket, indent=2)}"]
        if design_spec:
            parts.append(f"Design Spec:\n{design_spec}")
        return self.run("\n\n".join(parts))


class Developer(Agent):
    name = "Developer"
    system = textwrap.dedent("""\
        You are a senior Android developer who writes idiomatic, production-quality Kotlin.

        Standards you always follow:
          - Jetpack Compose for all UI (no XML layouts)
          - Material 3 components and theming
          - Clean Architecture: ViewModels in presentation, UseCases in domain, Repositories in data
          - Kotlin Coroutines/Flow for async work
          - Hilt for dependency injection
          - Meaningful names, small functions, no magic numbers
          - KDoc for public APIs

        Given a ticket, design spec and architecture plan, produce:
          - All new/modified Kotlin files with full content
          - File path for each file (e.g. app/src/main/java/fr/mandarine/diceroller/…)
          - Brief explanation of each change

        Use Markdown code blocks with the file path as the header.
    """)

    def implement(self, ticket: dict, design_spec: Optional[str], arch_plan: Optional[str]) -> str:
        parts = [f"Ticket:\n{json.dumps(ticket, indent=2)}"]
        if design_spec:
            parts.append(f"Design Spec:\n{design_spec}")
        if arch_plan:
            parts.append(f"Architecture Plan:\n{arch_plan}")
        return self.run("\n\n".join(parts))


class QA(Agent):
    name = "QA"
    system = textwrap.dedent("""\
        You are a senior QA Engineer for Android apps.

        Given a ticket and its implementation, write comprehensive tests:
          - Unit tests (JUnit 5 + MockK) for ViewModels and UseCases
          - Integration tests for Repositories (Room in-memory DB)
          - Compose UI tests (ComposeTestRule) for key user flows
          - Edge cases and negative paths

        Standards:
          - AAA pattern (Arrange / Act / Assert)
          - One assertion per test (preferred)
          - Descriptive test names: `givenX_whenY_thenZ`
          - No flaky Thread.sleep — use TestCoroutineScheduler / advanceUntilIdle

        Produce full Kotlin test files with file paths.
        Use Markdown code blocks with the file path as the header.
    """)

    def write_tests(self, ticket: dict, implementation: str) -> str:
        parts = [
            f"Ticket:\n{json.dumps(ticket, indent=2)}",
            f"Implementation:\n{implementation}",
        ]
        return self.run("\n\n".join(parts))


# ---------------------------------------------------------------------------
# Orchestrator
# ---------------------------------------------------------------------------

@dataclass
class TicketResult:
    ticket: dict
    design_spec: Optional[str] = None
    arch_plan: Optional[str] = None
    implementation: Optional[str] = None
    tests: Optional[str] = None


class Orchestrator:
    """Runs the full pipeline: PM → Ticket Writer → per-ticket parallel roles → output."""

    def __init__(self):
        self.pm = ProductManager()
        self.ticket_writer = TicketWriter()
        self.designer = UIUXDesigner()
        self.architect = Architect()
        self.developer = Developer()
        self.qa = QA()

    def _separator(self, label: str):
        width = 60
        print(f"\n{'='*width}")
        print(f"  {label}")
        print(f"{'='*width}\n")

    def run(self, raw_input: str):
        # ── 1. Product Manager ────────────────────────────────────────────
        self._separator("STEP 1 — Product Manager")
        prd = self.pm.gather_requirements(raw_input)
        print(f"\n[PRD Draft]\n{prd}\n")

        validation = input("Approve PRD? (yes / provide feedback): ").strip()
        if validation.lower() not in ("yes", "y", ""):
            # Feed feedback back to PM for one revision pass
            print("\n[ProductManager] Revising PRD based on your feedback…\n")
            prd = self.pm.run(
                f"Original PRD:\n{prd}\n\nUser feedback:\n{validation}\n\n"
                "Revise the PRD accordingly and end with READY_FOR_REVIEW.",
            ).replace("READY_FOR_REVIEW", "").strip()
            print(f"[Revised PRD]\n{prd}\n")

        # ── 2. Ticket Writer ──────────────────────────────────────────────
        self._separator("STEP 2 — Ticket Writer")
        tickets = self.ticket_writer.write_tickets(prd)
        print(f"Created {len(tickets)} ticket(s):\n")
        for t in tickets:
            print(f"  [{t['id']}] {t['title']}  →  {t['assigned_to']}")

        results: list[TicketResult] = [TicketResult(ticket=t) for t in tickets]

        # ── 3. Process each ticket ────────────────────────────────────────
        for result in results:
            t = result.ticket
            agent = t.get("assigned_to", "Developer")

            self._separator(f"TICKET {t['id']} — {t['title']}  [{agent}]")
            print(json.dumps(t, indent=2))

            if agent == "UIUXDesigner":
                print("\n[UIUXDesigner] Designing…\n")
                result.design_spec = self.designer.design(t)
                print(result.design_spec)

            elif agent == "Architect":
                print("\n[Architect] Planning…\n")
                result.arch_plan = self.architect.plan(t)
                print(result.arch_plan)

            elif agent == "Developer":
                # Look up any prerequisite design/arch results
                design = self._find_output(results, t.get("depends_on", []), "design_spec")
                arch = self._find_output(results, t.get("depends_on", []), "arch_plan")
                print("\n[Developer] Implementing…\n")
                result.implementation = self.developer.implement(t, design, arch)
                print(result.implementation)

            elif agent == "QA":
                impl = self._find_output(results, t.get("depends_on", []), "implementation")
                print("\n[QA] Writing tests…\n")
                result.tests = self.qa.write_tests(t, impl or "")
                print(result.tests)

        # ── 4. Summary ────────────────────────────────────────────────────
        self._separator("COMPLETE — Summary")
        for result in results:
            t = result.ticket
            print(f"[{t['id']}] {t['title']}")
            if result.design_spec:
                print("  ✓ Design spec ready")
            if result.arch_plan:
                print("  ✓ Architecture plan ready")
            if result.implementation:
                print("  ✓ Implementation ready")
            if result.tests:
                print("  ✓ Tests ready")

        return results

    def _find_output(self, results: list[TicketResult], depends_on: list[str], field: str) -> Optional[str]:
        """Find the first non-None output of the given field from depended-on tickets."""
        for dep_id in depends_on:
            for r in results:
                if r.ticket.get("id") == dep_id:
                    value = getattr(r, field, None)
                    if value:
                        return value
        return None


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    print("╔══════════════════════════════════════════════════════════╗")
    print("║          Multi-Agent Development System                  ║")
    print("║  PM → Tickets → Design / Arch → Dev → QA                ║")
    print("╚══════════════════════════════════════════════════════════╝\n")

    print("Describe what you want to build (press Enter twice when done):\n")
    lines = []
    while True:
        line = input()
        if line == "" and lines and lines[-1] == "":
            break
        lines.append(line)

    user_input = "\n".join(lines).strip()
    if not user_input:
        print("No input provided. Exiting.")
        raise SystemExit(1)

    orchestrator = Orchestrator()
    orchestrator.run(user_input)
