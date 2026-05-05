---
name: staff-ship-parallel-qa
description: Execute a staff-level delivery workflow from plan to merged PR with parallel code review and test validation. Use when the user asks to implement a plan, raise test coverage, identify review issues, run reviewer and tester agents in parallel, and complete branch/PR/merge workflow.
---

# Staff Ship With Parallel QA

## When To Use

Apply this skill when the user wants end-to-end delivery, especially requests like:
- "Implement this plan as a staff engineer."
- "Add enough test coverage."
- "Identify review issues."
- "Have one agent review while another tests."
- "Create a separate branch, push, open PR, and merge."

## Non-Negotiable Guardrails

1. Never push directly to `main`/`master`.
2. Never use destructive git operations (`reset --hard`, force push) unless explicitly requested.
3. Do not commit secrets (`.env`, keys, credentials).
4. Do not amend commits unless explicitly requested.
5. If unexpected unrelated local changes appear, stop and ask the user.
6. Reuse existing project patterns/components before introducing new abstractions.
7. Prefer the simplest design that meets current requirements and scale expectations.

## Delivery Workflow

Copy this checklist and execute in order:

```text
Delivery Progress:
- [ ] Confirm scope from the plan and acceptance criteria
- [ ] Create a dedicated feature branch
- [ ] Implement code changes
- [ ] Add or update tests for changed behavior
- [ ] Run reviewer and test agents in parallel
- [ ] Resolve findings and rerun checks
- [ ] Commit with clear message(s)
- [ ] Push branch and create/update PR
- [ ] Merge PR to base branch after green checks
```

## Step 1: Confirm Scope

- Translate the plan into concrete requirements and "done" criteria.
- Note risks, edge cases, and any migration or compatibility concerns.
- If plan details are missing, ask for the minimum needed clarification.

## Step 2: Create Branch

Use a clear branch name from scope, for example:

```bash
git checkout -b feat/<short-scope-name>
```

## Step 3: Implement As Staff Engineer

- Prefer small, coherent commits.
- Keep interfaces stable unless change is required by the plan.
- Add concise comments only around non-obvious logic.
- Preserve existing architecture and conventions.

### Reuse-First Rule

Before writing new code:
1. Search for existing services, utilities, DTOs, validators, mappers, and test helpers that already solve all or part of the problem.
2. Extend existing modules when possible instead of creating parallel implementations.
3. Only introduce new abstractions when reuse would clearly harm correctness, readability, or future maintainability.
4. Document in the final report what was reused and why.

Complexity check:
- Avoid speculative generalization or over-engineering.
- Keep dependency graph shallow and readable.
- Prefer composable, local changes over wide refactors unless required by scope.

## Step 4: Test Coverage Expectations

For every behavior change:
1. Add/extend unit tests for core logic.
2. Add integration/API tests when behavior crosses boundaries.
3. Add regression tests for fixed bugs.
4. Verify failure-path coverage (validation, error handling).

Minimum quality bar:
- New logic has direct tests.
- Critical paths touched by change have assertions.
- No untested bug fix.
- Reused paths have regression coverage for changed behavior.

## Performance And Scalability Checks

For user-facing or high-volume paths touched by changes:
1. Identify hot paths (I/O, DB queries, loops over large collections, serialization/deserialization).
2. Avoid obvious inefficiencies (N+1 queries, repeated parsing, unnecessary allocations, duplicate remote calls).
3. Preserve or improve asymptotic behavior where feasible.
4. Add focused tests/benchmarks/assertions when performance-sensitive logic changes.
5. Prefer proven project mechanisms (caching, batching, pagination, async patterns) over custom infrastructure.

Scalability bar:
- Works for expected data sizes and concurrency levels in this project.
- No single-point bottleneck introduced by new code paths.
- Failure handling remains bounded and observable under load.

## Step 5: Parallel Reviewer + Tester Agents

Launch two subagents in parallel in one message:

1. `generalPurpose` reviewer agent
   - Task: inspect diff for correctness, regressions, security, maintainability, missing tests.
   - Return: prioritized findings with severity (`critical`, `major`, `minor`) and file references.

2. `shell` tester agent
   - Task: run project test commands and report failures/flaky tests/perf outliers.
   - Return: exact failing commands, error summaries, and likely root causes.

After both finish:
- Triage findings.
- Fix clear issues immediately.
- Rerun targeted tests, then full relevant suite.

## Step 6: Review Findings Format

Use this format when reporting issues:

```markdown
## Findings
- [critical] <issue> - impact, evidence, path
- [major] <issue> - impact, evidence, path
- [minor] <issue> - impact, evidence, path

## Test Gaps
- <missing coverage area>
```

If no issues are found, state:
- "No blocking issues found."
- List residual risks and any remaining coverage gaps.

## Step 7: Commit, Push, PR, Merge

1. Stage relevant files only.
2. Commit with "why-focused" message.
3. Push branch to remote.
4. Create PR with summary + test plan.
5. Ensure CI/review checks pass.
6. Merge PR into base branch.
7. Confirm local branch is clean and synced.

Example PR body template:

```markdown
## Summary
- <what changed and why>
- <risk-reduction or design rationale>

## Test Plan
- [ ] Unit tests
- [ ] Integration/API tests
- [ ] Manual sanity checks
```

## Completion Criteria

Only mark complete when all are true:
- Implementation matches plan scope.
- Reviewer findings are resolved or explicitly accepted by user.
- Test suite relevant to changes is green.
- Branch is pushed and PR is merged to target base branch.
- Final report includes: changes made, tests run, issues found/fixed, residual risk.
