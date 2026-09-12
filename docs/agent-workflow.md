# From a discussion to a tested change

[English contribution guide](../CONTRIBUTING.md) · [Guía en español](../CONTRIBUTING.es.md) · [Agent instructions](../AGENTS.md)

Use **Discussions** to ask questions or explore ideas. Use **Issues** to track a bug or a specific change. The forms accept English and Spanish. A person can fill them in without knowing which source file needs changing.

## Choose a form

| Purpose | Form |
| --- | --- |
| Installation or usage question | [Q&A](https://github.com/cplus2jules/motion-air/discussions/categories/q-a) |
| Idea that still needs discussion | [Ideas](https://github.com/cplus2jules/motion-air/discussions/categories/ideas) |
| General feedback or project conversation | [General](https://github.com/cplus2jules/motion-air/discussions/categories/general) |
| A setup, experiment or contribution to share | [Show and tell](https://github.com/cplus2jules/motion-air/discussions/categories/show-and-tell) |
| Maintainer update | [Announcements](https://github.com/cplus2jules/motion-air/discussions/categories/announcements) |
| Something broken | [Bug report](https://github.com/cplus2jules/motion-air/issues/new?template=01-bug-report.yml) |
| A proposed feature with clear outcomes | [Feature request](https://github.com/cplus2jules/motion-air/issues/new?template=02-feature-request.yml) |
| A scoped implementation task | [Implementation task](https://github.com/cplus2jules/motion-air/issues/new?template=04-agent-task.yml) |

The existing [feedback issue form](https://github.com/cplus2jules/motion-air/issues/new?template=03-feedback.yml) remains available for feedback that should be tracked as work. Polls use GitHub's own poll editor: GitHub does not support category forms for polls. [GitHub's discussion form documentation](https://docs.github.com/en/discussions/managing-discussions-for-your-community/creating-discussion-category-forms).

## Prepare work for a contributor or agent

1. Review the report or discussion. Confirm the observed behavior, affected versions and desired result. Ask about details that would change the implementation; do not guess at them.
2. For a discussion that leads to code work, create an implementation issue and link the original conversation. Record any decision in the task so the implementer does not need to reconstruct it from a long thread.
3. Set a bounded scope. List what is included, what can wait, dependencies and relevant source paths if known.
4. Write observable acceptance criteria and a validation plan. Identify checks requiring Windows, macOS, a simulator, a physical phone or a running game.
5. When those details are sufficient, remove `needs-triage`, add `ready-for-agent`, and assign the task to a contributor or the coding agent you use.
6. Review the resulting pull request against the criteria and evidence. Follow up on untested platforms and unresolved decisions before treating them as complete.

Opening an issue or adding a label does not run an agent automatically. This setup provides templates and instructions for assigned work; it adds no bot that executes arbitrary issue text, posts replies, merges changes or publishes releases.

## Labels

| Label | Meaning |
| --- | --- |
| `needs-triage` | New report or task awaiting review. All issue forms start here. |
| `bug` | Unexpected behavior; applied by the bug form. |
| `enhancement` | Proposed feature; applied by the feature form and Ideas discussions. |
| `question` | Help request; applied by the Q&A discussion form. |
| `agent-task` | Uses the implementation task structure; it still needs review. |
| `ready-for-agent` | A maintainer has checked scope, acceptance criteria and validation. |
| `blocked` | A decision, dependency or environment is preventing progress. Explain the blocker in the issue and remove `ready-for-agent` until it is resolved. |

Existing labels such as `documentation`, `accessibility`, `good first issue` and `help wanted` can describe the work further. Category and platform dropdown answers are recorded in the body; they do not automatically apply additional labels.

## Example acceptance criteria

For a reconnect bug, criteria might be:

- [ ] After returning from the background, the phone can reconnect to the saved computer.
- [ ] Motion stays off until the user enables it again.
- [ ] After enabling motion, the bridge receives fresh samples and the DSU receiver receives increasing timestamps.
- [ ] No held buttons remain after disconnecting.

The validation plan should name the automated checks and the required device tests. Do not substitute a successful network test for an in-game scoring test. Use [AGENTS.md](../AGENTS.md) for repository commands and compatibility details.

## Keep the templates working

Issue forms live in `.github/ISSUE_TEMPLATE/`. Discussion forms live in `.github/DISCUSSION_TEMPLATE/`; each filename must match its category slug, such as `q-a.yml` for Q&A. Keep labels referenced by a form present in the repository. If you rename a discussion category, update its form filename and links together.

GitHub loads these templates from the default branch. Validate YAML and then open the real issue/discussion chooser to check the rendered fields. A form preview is enough to validate layout; do not submit placeholder issues or discussions. PRs use `.github/pull_request_template.md`. [GitHub's form syntax](https://docs.github.com/en/discussions/managing-discussions-for-your-community/syntax-for-discussion-category-forms).

`AGENTS.md` holds the shared source map and checks. `.github/copilot-instructions.md` provides repository instructions for GitHub Copilot and points to that shared guide. Keep the files consistent when paths or build commands change. [GitHub's custom instruction support](https://docs.github.com/en/copilot/reference/custom-instructions-support).

## Español

Usa **Discussions** para preguntas e ideas abiertas, e **Issues** para fallos o cambios concretos. Q&A permite marcar una respuesta como solución. Todos los formularios aceptan español e inglés.

Para preparar una tarea para una persona o agente:

1. Revisa el reporte o la conversación. Aclara los datos que puedan cambiar la solución.
2. Enlaza la conversación original e indica el objetivo, alcance y trabajo que queda fuera.
3. Añade criterios observables y un plan de comprobación. Distingue pruebas automáticas, simulador, teléfono real y juego.
4. Cuando la tarea esté definida, quita `needs-triage`, añade `ready-for-agent` y asígnala. `agent-task` solo indica el tipo de formulario.
5. Si falta una decisión, dependencia o entorno necesario, usa `blocked`, explica el bloqueo y retira `ready-for-agent` hasta resolverlo.
6. Revisa el pull request con los criterios y las evidencias. Lo que no se probó debe quedar indicado.

Las etiquetas no ejecutan agentes automáticamente. Las instrucciones están en [AGENTS.md](../AGENTS.md); la plantilla de pull request pide el contexto, los criterios comprobados y los resultados. No adjuntes credenciales ni códigos de conexión.
