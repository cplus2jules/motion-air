# Contributing to Motion Air

[Leer en español](CONTRIBUTING.es.md) · [Back to setup](README.md)

You can help by testing the app on your phone, reporting a confusing screen, improving the instructions, translating, or sending a code change. You do not need to write code to contribute.

## Choose where to start

| I want to... | Open this |
| --- | --- |
| Report something that does not work | [Bug report](https://github.com/cplus2jules/motion-air/issues/new?template=01-bug-report.yml) |
| Suggest a feature | [Feature request](https://github.com/cplus2jules/motion-air/issues/new?template=02-feature-request.yml) |
| Ask for help | [Discussions Q&A](https://github.com/cplus2jules/motion-air/discussions/categories/q-a) |
| Explore an idea or share feedback | [Discussions](https://github.com/cplus2jules/motion-air/discussions/new/choose) |
| Define work for a contributor or coding agent | [Implementation task](https://github.com/cplus2jules/motion-air/issues/new?template=04-agent-task.yml) |
| Check whether someone already reported it | [Existing issues](https://github.com/cplus2jules/motion-air/issues) |
| Improve code or documentation | [Send a pull request](#send-a-pull-request) |

You can write in English or Spanish. Sign in to GitHub first. This repository is currently private, so you need access to view it and use its forms. Issues and discussions appear under your GitHub account and are visible to people with repository access. The [feedback issue form](https://github.com/cplus2jules/motion-air/issues/new?template=03-feedback.yml) is also available when feedback should be tracked as work.

## Discussions and work for agents

Use **Q&A** for help and mark the answer that solves your question. Use **Ideas** to explore a proposal, **General** for project feedback, and **Show and tell** for something you tried or built. Maintainers can use **Announcements** for updates. Each category has a form; **Polls** uses GitHub's poll editor.

When a discussion leads to a change, open an implementation task with a goal, scope, acceptance criteria and a validation plan. Link the discussion so decisions stay traceable. Maintainers review new tasks under `needs-triage` and apply `ready-for-agent` when the task can be assigned. These labels do not start an agent automatically.

Read [From a discussion to a tested change](docs/agent-workflow.md) for the workflow. Coding agents should read [AGENTS.md](AGENTS.md); GitHub Copilot also has [repository instructions](.github/copilot-instructions.md).

## Report a problem

1. Search the existing issues. If you find the same problem, add your phone model and experience to that issue.
2. Open the bug report form and describe what happened, what you expected, and the steps that caused it.
3. Add your phone model, iOS or Android version, computer system, and Motion Air release if you know them. "Not sure" is fine.
4. Attach a screenshot or the exact error text if it helps. Remove pairing QR codes, pairing codes, passwords and private details first.

For tracking problems, tell us whether buttons work, whether **Enable Motion** is on, and what connection status the app shows. Mention the game and emulator, your Wi-Fi or hotspot, and whether the problem began after locking the phone, switching apps, or reconnecting. You can leave details you do not know blank.

A connected phone, a moving controller in an emulator, and a good score in Just Dance are different results. Say which one you checked. See the [motion testing status](docs/motion-implementation-status.md).

## Help without changing code

- Try the [latest release](https://github.com/cplus2jules/motion-air/releases/latest) and report the phone and computer you used, including what worked.
- Point out the exact setup step where you got stuck, or suggest clearer wording.
- Share screenshots of the current app. Include the platform, app version and language; use names such as `iphone-pairing.png`.
- Improve the English and Spanish instructions together when possible. Keep visible button names accurate; iPhone screenshots currently show an English interface.

Use the feedback form if you want to suggest a text change without editing files yourself.

## Send a pull request

A pull request is a proposed change that the maintainer can review before adding it to the project.

1. For a larger feature or redesign, open an issue first to explain the problem and proposed approach. A small typo fix can go straight to a pull request.
2. Create a branch for your change. If you have write access, create it in this repository. Otherwise, use a fork if the repository allows it. If neither option is available, describe your change in an issue.
3. Make one focused change. For a documentation fix, GitHub's pencil button lets you edit the file in your browser and propose a change.
4. Run the relevant checks below. Review screenshots for UI changes and check links for documentation changes.
5. Open **Pull requests → New pull request**, choose `main` as the base and your branch as the comparison, and fill in the template. Use a draft pull request if work remains.
6. Describe what changed and why. Link the related issue, add screenshots for UI changes, and say what you tested. Reply to review comments and push further edits to the same branch.

For local work, clone the repository or your fork, then create a branch:

```bash
git clone https://github.com/cplus2jules/motion-air.git
cd motion-air
git switch -c your-change-name
```

Replace `your-change-name` with a short description. Use your fork's URL if you are working in a fork. See the [development guide](docs/development.md) for setup and the repository layout.

## Check your change

| Changed area | Useful checks |
| --- | --- |
| README, instructions or screenshots | Open the rendered file on GitHub, follow its links, and check that images load and button names match the app. |
| Computer bridge or launchers | Use Node.js 22 or later, run `npm ci`, then `npm test` and the relevant `npm run test:*` scripts listed in `package.json`. |
| Android app | In `android/`, run `./gradlew assembleDebug testDebugUnitTest lintDebug` with JDK 17 and SDK 35. On Windows use `gradlew.bat`. For pairing or lifecycle changes, also run the [Android emulator check](docs/development.md#android). |
| iPhone app or JoypadCore | Build the **MotionAir** scheme in Xcode and run `swift test --package-path native/Packages/JoypadCore` from the repository root with Xcode's toolchain. See [iPhone development](docs/development.md#iphone). |
| Release scripts or workflows | Run the Python checks in the [development guide](docs/development.md#iphone) and review the [release guide](docs/mobile-releases.md). |

Choose checks that cover the change. If you cannot test a platform, say so in the pull request. Motion changes need real-phone testing as well as automated checks before claiming that gameplay works. Contributors can use Android debug builds and the unsigned iPhone build workflow; release signing keys are not needed for a pull request.

## Working together

Describe the behavior you observed and keep discussion respectful. Give people enough detail to repeat your result. Keep unrelated changes in separate pull requests.

Preserve the Joypad Air attribution and MIT license. Keep intentional compatibility identifiers unless the change includes a migration plan. Do not commit `.local/`, pairing data, passwords, keystores, provisioning profiles, or game files. Use screenshots and logs that show only what the report needs.
