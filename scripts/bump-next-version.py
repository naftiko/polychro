#!/usr/bin/env python3
"""
Script to bump the Polychro engine version:
- polychro's own `pom.xml` `revision` property;
- the `io.ikanos:ikanos-engine` and `io.ikanos:ikanos-spec` dependency versions
  declared in `modules/polychro-capability/pom.xml` (polychro-capability is
  built against a matching Ikanos engine/spec snapshot).

Each target is updated by an anchored regex scoped to its exact XML structure
(mirroring `<groupId>`/`<artifactId>`/`<version>` adjacency), never by a blind
string replace, so a version string that happens to appear elsewhere in either
file is never touched.
"""

import argparse
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from polychro_version import (
    read_pom_revision,
    write_pom_revision,
    read_ikanos_dependency_version,
    write_ikanos_dependency_version,
)

VERSION_PATTERN = re.compile(r"^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$")


def validate_version_format(version, label):
    """Validates that version matches the expected semver-like shape
    (e.g. '1.0.0-beta6') and does not already carry the -SNAPSHOT suffix
    (this script appends it automatically). Exits with an error before any
    file is touched if the format is wrong, catching typos/swapped inputs
    early."""
    if version.endswith("-SNAPSHOT"):
        print(
            f"[error] {label} '{version}' must not include the -SNAPSHOT suffix "
            f"- it is appended automatically",
            file=sys.stderr,
        )
        sys.exit(1)
    if not VERSION_PATTERN.match(version):
        print(
            f"[error] {label} '{version}' does not match the expected format "
            f"<major>.<minor>.<patch>[-<label>] (e.g. 1.0.0-beta6)",
            file=sys.stderr,
        )
        sys.exit(1)


def update_pom_revision(pom_path, new_revision):
    """Updates the <revision> element in pom.xml to new_revision (already
    carrying the -SNAPSHOT suffix)."""
    if read_pom_revision(pom_path) == new_revision:
        print(f"[ok] {pom_path} already at revision {new_revision}", file=sys.stderr)
        return False

    write_pom_revision(pom_path, new_revision)
    print(f"[ok] {pom_path}: revision -> {new_revision}", file=sys.stderr)
    return True


def update_ikanos_dependency_version(pom_path, artifact_id, new_revision):
    """Updates the <version> of the io.ikanos:<artifact_id> <dependency> block
    in pom_path to new_revision."""
    if read_ikanos_dependency_version(pom_path, artifact_id) == new_revision:
        print(f"[ok] {pom_path}: io.ikanos:{artifact_id} already at {new_revision}", file=sys.stderr)
        return False

    write_ikanos_dependency_version(pom_path, artifact_id, new_revision)
    print(f"[ok] {pom_path}: io.ikanos:{artifact_id} -> {new_revision}", file=sys.stderr)
    return True


def main():
    parser = argparse.ArgumentParser(description="Bump the Polychro engine version.")
    parser.add_argument("--pom", default="pom.xml", help="Path to the root pom.xml")
    parser.add_argument(
        "--capability-pom",
        default="modules/polychro-capability/pom.xml",
        help="Path to modules/polychro-capability/pom.xml",
    )
    parser.add_argument("--next-engine-version", required=True, help="Next engine version, e.g. 1.0.0-beta6")
    args = parser.parse_args()

    print("=" * 60)
    print("Polychro version bump")
    print("=" * 60)

    validate_version_format(args.next_engine_version, "--next-engine-version")
    new_revision = f"{args.next_engine_version}-SNAPSHOT"

    update_pom_revision(args.pom, new_revision)
    update_ikanos_dependency_version(args.capability_pom, "ikanos-engine", new_revision)
    update_ikanos_dependency_version(args.capability_pom, "ikanos-spec", new_revision)

    print("=" * 60)


if __name__ == "__main__":
    main()
