#!/usr/bin/env python3
"""
Shared utilities for Polychro version synchronization.

Factored out of `bump-next-version.py` and `unbump-current-version.py` so the
regex read/write logic for the root `<revision>` and the `io.ikanos:*`
dependency versions in `modules/polychro-capability/pom.xml` lives in exactly
one place, per AGENTS.md's "factor by default, specialize by exception"
guidance - the two scripts previously duplicated the same patterns.
"""

import re
import sys
from pathlib import Path

POM_REVISION_PATTERN = re.compile(r"(<revision>)([^<]*)(</revision>)")


def read_pom_revision(pom_path="pom.xml"):
    """Reads the raw <revision> value from pom.xml, verbatim (no -SNAPSHOT
    stripping/appending). Exits with an error if the element is not found."""
    pom_path = Path(pom_path)
    content = pom_path.read_text(encoding="utf-8")

    match = POM_REVISION_PATTERN.search(content)
    if not match:
        print(f"[error] <revision> element not found in {pom_path}", file=sys.stderr)
        sys.exit(1)

    return match.group(2)


def write_pom_revision(pom_path, new_revision):
    """Writes new_revision into the <revision> element of pom.xml. Returns
    True if the file was changed, False if it was already at new_revision
    (clean no-op). Exits with an error if the element is not found."""
    pom_path = Path(pom_path)
    content = pom_path.read_text(encoding="utf-8")

    updated_content, count = POM_REVISION_PATTERN.subn(
        rf"\g<1>{new_revision}\g<3>", content, count=1
    )
    if count == 0:
        print(f"[error] <revision> element not found in {pom_path}", file=sys.stderr)
        sys.exit(1)

    if updated_content == content:
        return False

    pom_path.write_text(updated_content, encoding="utf-8")
    return True


def _ikanos_dependency_pattern(artifact_id):
    """Anchored on <groupId>io.ikanos</groupId> followed by the exact
    <artifactId>, not on any expected version string, so it works regardless
    of what version is currently declared."""
    return re.compile(
        r"(<groupId>io\.ikanos</groupId>\s*<artifactId>"
        + re.escape(artifact_id)
        + r"</artifactId>\s*<version>)([^<]*)(</version>)"
    )


def read_ikanos_dependency_version(pom_path, artifact_id):
    """Reads the raw <version> value of the io.ikanos:<artifact_id>
    <dependency> block in pom_path. Exits with an error if not found."""
    pom_path = Path(pom_path)
    content = pom_path.read_text(encoding="utf-8")

    match = _ikanos_dependency_pattern(artifact_id).search(content)
    if not match:
        print(
            f"[error] io.ikanos:{artifact_id} dependency not found in {pom_path}",
            file=sys.stderr,
        )
        sys.exit(1)

    return match.group(2)


def write_ikanos_dependency_version(pom_path, artifact_id, new_version):
    """Writes new_version into the <version> of the io.ikanos:<artifact_id>
    <dependency> block in pom_path. Returns True if the file was changed,
    False if it was already at new_version (clean no-op). Exits with an
    error if the dependency block is not found."""
    pom_path = Path(pom_path)
    content = pom_path.read_text(encoding="utf-8")

    updated_content, count = _ikanos_dependency_pattern(artifact_id).subn(
        rf"\g<1>{new_version}\g<3>", content, count=1
    )
    if count == 0:
        print(
            f"[error] io.ikanos:{artifact_id} dependency not found in {pom_path}",
            file=sys.stderr,
        )
        sys.exit(1)

    if updated_content == content:
        return False

    pom_path.write_text(updated_content, encoding="utf-8")
    return True
