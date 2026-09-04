#!/usr/bin/env bash
# Text-only scan enforcing SPEC.md P1 / CLAUDE.md 禁令 2:
# domain/**/*.kt must never import android.* or androidx.* (kotlin.*/kotlinx.* are fine).
# Mirrors the spirit of the web version's scripts/check-domain-purity.mjs.
set -euo pipefail

DOMAIN_DIR="app/src/main/kotlin/com/jojomango/expensetracker/domain"
VIOLATIONS=0

if [[ ! -d "$DOMAIN_DIR" ]]; then
    echo "domain-purity: directory not found: $DOMAIN_DIR" >&2
    exit 1
fi

while IFS= read -r -d '' file; do
    while IFS= read -r line; do
        echo "domain-purity VIOLATION: $file: $line"
        VIOLATIONS=$((VIOLATIONS + 1))
    done < <(grep -nE '^\s*import\s+android(x)?\.' "$file" || true)
done < <(find "$DOMAIN_DIR" -type f -name '*.kt' -print0)

if [[ "$VIOLATIONS" -gt 0 ]]; then
    echo "domain-purity: $VIOLATIONS violation(s) found in $DOMAIN_DIR" >&2
    exit 1
fi

echo "domain-purity: clean ($DOMAIN_DIR)"
