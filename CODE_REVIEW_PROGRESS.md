# Code Review Remediation — Progress & Handoff

**Status: 11 of 11 PR groups addressed. PR 7 verified against a live Ombi server and
branched (`fix/api-contract`).** See the PR 7 section for per-finding results and two
bonus bugs the live probing surfaced (reported, not fixed in PR 7).

Working through the 39 findings in `/home/apetak/ombi-mobile-code-review-2026-07-10.md`,
grouped into 11 PRs. **One commit per finding; one PR per coherent group.** Each PR is a
separate branch, pushed, PR opened for review (never commit to `main`).

**Environment note:** the authoring session has **no JDK and no Android SDK** — nothing can
be compiled or run here. Every PR is self-reviewed against source and hands off
`./gradlew :app:testDebugUnitTest` + `:app:assembleDebug` to CI/reviewer. State this honestly
in every PR body. Test infra (mockk, coroutines-test, hilt-testing, robolectric) landed in PR 9.

Commit trailer: `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`
PR body trailer: `🤖 Generated with [Claude Code](https://claude.com/claude-code)`

---

## DONE (merged to main)

| PR | Branch | Findings | Merge commit |
|----|--------|----------|--------------|
| PR 2 — network config | `fix/network-config` | #3 timeouts, #6 logging guard (+enabled `buildConfig`), #13 path prefix, #33 throw on blank URL | `071fb83` |
| PR 9 — test infra | `test/infrastructure` | deps + MediaItem mapper tests + RequestsUiState tests | `8d36dd2` |
| PR 1 — startup | `fix/startup-reliability` | #2 async nav loading state, #9 @Volatile URL cache, #36 removed NavViewModel | `b0baa4a` |
| PR 3 — TV cancel | `fix/tv-cancel` | #17 parent-ID + hide button, #7 concurrent guard, **#11 REJECTED** (see below) + regression test | `83a1b61` |
| PR 5 — auth screen state | `fix/auth-screen-state` | #5 login isLoading, #21 clear password, #20 serversetup isSaving + test | `11e053a` |
| PR 6 — request guards | `fix/request-guards` | #15 double-tap, #16 identity check, #18 all-fail error, #34 isLoading finally + test | `80f17a2` |
| PR 4 — auth robustness | `fix/auth-robustness` | #4 Keystore fallback, #14 reactive 401 session flow, #8 logout cancels in-flight via same flow + test | `1f98ac0` |
| PR 8 — security hardening | `fix/security-hardening` | #12 backup exclusion (datastore/), #39 sanitize error bodies, #35 cleartext http warning | `bc47f8f` |
| PR 11 — refactor/dedup | `refactor/dedup-cleanup` | #27 Screen.Main, #26 dead init, #24 TvRequest.statusLabel, #25 RequestStatus enum, #22 shared request mapping (**#23 deferred**) | `5bf6900` |
| PR 10 — perf | `perf/memoization-virtualization` | #29 LazyRow, #30 memoize search filter (remember, not StateFlow), #31 memoize request splits (remember, not ViewModel) | `fe4b09c` |

### Findings resolved via analysis, NOT the review's proposed fix (important)
- **#11 — REJECTED as a false positive.** The review wanted the TV-cancel filter changed to
  `it.parentRequest?.id != null && it.parentRequest.id != parentRequestId`. That is a
  data-loss regression: it drops *every* null-parent child on any cancel. The original filter
  `it.parentRequest?.id != parentRequestId` is correct. Locked in by a regression test in PR 3
  (`RequestsViewModelCancelTest` — "unrelated null-parent requests survive").
- **#34 — supervisorScope omitted.** Repo calls use `runCatching`/return `Result`, never throw,
  so `supervisorScope` is a no-op. Only the `try/finally` (applied) is load-bearing.

---

## TODO (remaining PRs)

### PR 7 — API contract corrections — **✅ VERIFIED & BRANCHED (`fix/api-contract`)**
Probed against live server `https://ombi.petak.family` (Ombi V2) on 2026-08-05. One commit
per finding. Build/tests handed to CI as before (no JDK/Android SDK in the authoring env).

- **#1** `multiSearch` verb — **code was correct, docs were wrong.** Live: `POST` → 200 with
  results; `GET` → **405**. Fixed CLAUDE.md to say `POST` (kept the code as-is).
- **#10** `getTvByMovieDbId` casing — **lowercased `Tv`→`tv`.** Live server accepts both
  casings (both → 200), but aligned to lowercase to match every other route and kill the
  latent case-sensitivity 404 risk.
- **#32** `TvRequestBody.requestAll` default `false`→`true`. Live: `POST /api/v2/requests/tv`
  with `requestAll:true` is the correct whole-show shape (already-requested show correctly
  rejected as a duplicate — no new state created during the smoke test).
- **#37** Dead `@Path` defaults removed from `getMovieRequests`/`getTvRequests`; callers in
  `OmbiRepository` now pass `sort`/`sortOrder` explicitly. Confirmed no server needed.
- **#28** TV poster — **REAL BUG, confirmed and fixed.** The list/discover endpoints (popular,
  trending) return a genuine portrait `posterPath` that the model never captured; code used
  the landscape `backdropPath` everywhere. Added `posterPath` + `images` (TvImages) to
  `SearchTvShowViewModel`; `toMediaItem()` and HomeScreen now prefer `posterPath` →
  `images.original` → `backdropPath`. Expanded `MediaItemMapperTest` to cover all three tiers.

#### Bonus bugs surfaced by live probing — **✅ FIXED** (branch `fix/tv-id-contract`)
Both resolved after further live-server investigation on 2026-08-05, which refined the
original premises. Two commits.

1. **JSON string→number coercion** — `theMovieDbId`/`rating`/`tvDbId` arrive as JSON strings
   (`"60059"`, `"7.3"`, `"273181"`). Investigation concluded this is **not a defect**: Moshi's
   `JsonReader` natively coerces quoted numerics for `Int?`/`Double?` reads, and the app has
   always shipped this (e.g. `RecentlyAddedTv.tvDbId`). Swapping in custom adapters would be
   risky churn for zero benefit. Instead **pinned the behavior** with
   `JsonNumberCoercionTest` (real payloads, NetworkModule-identical Moshi) so a future Moshi
   upgrade that tightened parsing fails in CI instead of silently emptying the lists.
2. **TV ID contract** — the premise ("`getTvByTvDbId` resolves a TVDb id") was itself broken.
   Verified: `SearchTvShowViewModel.id` is the **TMDB id** on every TV endpoint (list items
   with null `theMovieDbId` still carry the TMDB id in `id`); `GET /api/v2/search/tv/{id}` is
   TMDB-keyed (a real TVDb id returns the wrong show); and `recentlyadded/tv` returns
   `theMovieDbId` **directly**. So the resolution step was both broken and unnecessary. Fixed
   the mapper (`theMovieDbId = theMovieDbId ?: id`, `tvDbId` from the real `theTvDbId` field),
   added `theMovieDbId` to `RecentlyAddedTv`, and **removed** `getTvByTvDbId` + the dead
   fallback in `HomeViewModel.requestSelected()`. (Note: the button gate `!available` already
   made the old path unreachable for recently-added TV in practice, but the contract was wrong.)

### #23 — deferred (decided against during PR 11)
Review wanted a shared `MediaRequestState` cluster nested into both Home + Search
UiStates. Rejected: both UiStates carry these fields among many others, so nesting forces
nested `.copy()` at every call site (incl. PR 6's double-tap/identity guards) — a
readability regression for no real gain. #22 already removed the meaningful duplication
(the request-result *logic* → `toRequestOutcome`). Revisit only if a maintainer wants the
holder despite the tradeoff.

### Not yet placed / residual (from review's Residual Risks + Agent-Native sections)
- No pagination (count=30 hardcoded); recently-added has no count param; no Ombi version
  negotiation; sort order sent as Int (may need string) — all need live-server verification.
- Agent-native/MCP layer — out of scope unless user requests.

---

### Design decision made (PR 4, #14)
Chose **reactive** (401 → `sessionEvents` SharedFlow → nav collector) over proactive
`isTokenExpired()`. Reason: catches server-side revocation the clock check misses, and #8
reuses the same flow. `replay = 0` on the flow is deliberate (collector always attached
first; a retained event would bounce a re-logged-in user after rotation).

### Perf deviations (PR 10, #30/#31)
Both done via composable `remember(inputs)` instead of the review's ViewModel/StateFlow
suggestions. #30: a second `combine` StateFlow breaks CLAUDE.md's "single StateFlow<UiState>"
rule. #31: moving the filter predicate into the ViewModel breaks the 12-case
`RequestsUiStateTest`. `remember` gets the same memoization while preserving architecture +
tests. Revisit only if a maintainer prefers the ViewModel-layer version.

## Suggested next order
1. **PR 7** — the only group left; needs a live Ombi server (verb/field checks). Do when one
   is reachable. See its section above for the per-finding detail and which are safe-blind.

## Workflow reminders
- Branch → commit (one per finding) → push → `gh pr create --base main`. Verify claims against
  code before applying; reject/adjust review findings that don't hold up and say so in the PR.
- Add a focused mockk/coroutines-test suite per PR where a ViewModel behavior changed.
- Repo: `github.com/aaronpetak/Ombi-Mobile`. Merged PRs: #19–#28 (all ten remediation groups).
  Only PR 7 (API contract, GitHub PR not yet created) remains, blocked on a live server.
