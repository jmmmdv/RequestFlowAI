# Git workflow lab

Use short-lived branches and small, explainable commits.

```bash
git switch -c feature/add-assignee
git status
git add src README.md
git diff --staged
git commit -m "feat: add assignee to work items"
git switch main
git merge --no-ff feature/add-assignee
```

Exercises:

1. Inspect `git log --graph --decorate --oneline --all`.
2. Change the same line on two branches and resolve the merge conflict deliberately.
3. Make a safe public undo with `git revert <sha>`; compare it with reset in a disposable branch.
4. Use `git bisect` to locate a deliberately introduced failing test.
5. Tag a tested release with `git tag -a v0.1.0 -m "First learning release"`.

Commit format: `type(scope): imperative summary`, where type is `feat`, `fix`, `test`, `docs`,
`refactor`, `build`, or `chore`. Keep generated files and secrets out of history.
