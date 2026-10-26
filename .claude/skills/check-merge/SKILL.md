---
name: check-merge
description: After a git pull or merge, detect files that were replaced wholesale or reverted to an older version (as happened to getFromInternet.java in merge b19d61c). Use right after pulling/merging, or when the build suddenly fails on code that used to exist.
---

Goal: catch merges that silently threw away one side's work. This is read-only: never reset, checkout or commit without asking.

1. Pick the merge to inspect: `$ARGUMENTS` if a commit was given, otherwise the latest merge commit, found with `git log --merges -1 --format=%H`. If HEAD isn't a merge (fast-forward pull), compare against the previous HEAD from `git reflog -2`.
2. For a merge `M`, list what changed relative to the local side: `git diff --stat M^1 M`. Any file with more than about 50% of its lines changed is a suspect.
3. For each suspect file, check whether it went back to an older version:
   - `git log --format="%h %ad %an %s" --date=short -5 -- <file>` on both parents (`M^1`, `M^2`).
   - If `M`'s version matches an older commit's version of that file, compare with `git diff <older> M -- <file>`; an empty diff means it was reverted. Also check whether functions or classes that exist in `M^1` disappeared in `M`.
   - Pay extra attention to `getFromInternet.java`, `Catalog.java`, `database/*.java`, `ui/*.java`, `res/values*/strings.xml` and `app/build.gradle.kts`.
4. Run `./gradlew assembleDebug -q`. Compile errors about missing classes or methods usually mean one side was dropped.
5. Report each affected file: which side won, which commits were lost, and the exact command to restore it, e.g. `git checkout <commit> -- <file>`. Ask before restoring anything, and say whether the other side's change needs to be re-applied by hand on top.
