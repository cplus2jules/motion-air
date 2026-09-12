# Motion Air repository instructions

Read [AGENTS.md](../AGENTS.md) for the source map, commands, compatibility rules and delivery checklist. Follow deeper directory instructions when relevant. Read the assigned task's scope, acceptance criteria and validation plan before editing.

Motion Air has native Kotlin Android and Swift iPhone apps, a Node.js ES-module bridge in `server/`, browser UI in `public/`, and Mac/Windows bootstrap scripts in `tools/bootstrap/`. The Expo fallback lives in `app/` and has its own instructions.

Use Node.js 22+ and `npm ci` for bridge development. Android needs JDK 17 and SDK 35. Swift checks need the Xcode toolchain. Run the relevant checks listed in AGENTS.md; report unavailable tools and untested platforms. Documentation-only changes do not need a mobile release.

Preserve legacy pairing identifiers, stored identities and the MIT attribution. Keep motion freshness and reconnect behavior intact. Separate automated delivery checks from physical-phone and in-game evidence. Do not include `.local`, credentials, pairing codes, signing files or game assets in commits or reports.

Use `.github/pull_request_template.md` and connect each acceptance criterion to a result or an explicit gap. The `ready-for-agent` label describes a reviewed task; it does not trigger execution or grant release authorization.
