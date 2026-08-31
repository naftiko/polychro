#!/usr/bin/env python3
"""
Script to remove the "-SNAPSHOT" suffix from the current Polychro engine version:
- polychro's own `pom.xml` `revision` property;
- the `io.ikanos:ikanos-engine` and `io.ikanos:ikanos-spec` dependency versions
  declared in `modules/polychro-capability/pom.xml` (polychro-capability is
  built against a matching Ikanos engine/spec snapshot).

This is the counterpart of `bump-next-version.py`: that script moves the
`revision` (and the two ikanos dependency versions) *forward* to the next
snapshot, this one *drops* the "-SNAPSHOT" suffix from each of them in place
(e.g. "1.0.0-beta5-SNAPSHOT" -> "1.0.0-beta5"). Each target is matched by an
anchored regex scoped to its exact XML structure, never by a blind string
replace, so a version string that happens to appear elsewhere in either file
is never touched.
"""

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from polychro_version import (
    read_pom_revision,
    write_pom_revision,
    read_ikanos_dependency_version,
    write_ikanos_dependency_version,
)


def unbump_pom_revision(pom_path):
    """Removes the "-SNAPSHOT" suffix from the <revision> element in pom.xml.
    Mirrors update_pom_revision's no-op behavior in bump-next-version.py: a
    revision that already has no -SNAPSHOT suffix is a clean no-op (returns
    False), not a failure - the same underlying "nothing to change" situation
    the twin script already treats as success."""
    current_revision = read_pom_revision(pom_path)

    if not current_revision.endswith("-SNAPSHOT"):
        print(
            f"[ok] {pom_path} already at revision {current_revision} (no -SNAPSHOT suffix)",
            file=sys.stderr,
        )
        return False

    new_revision = current_revision[: -len("-SNAPSHOT")]
    write_pom_revision(pom_path, new_revision)
    print(f"[ok] {pom_path}: revision {current_revision} -> {new_revision}", file=sys.stderr)
    return True


def unbump_ikanos_dependency_version(pom_path, artifact_id):
    """Removes the "-SNAPSHOT" suffix from the <version> of the
    io.ikanos:<artifact_id> <dependency> block in pom_path. Mirrors
    unbump_pom_revision's no-op behavior: a version that already has no
    -SNAPSHOT suffix is a clean no-op, not a failure."""
    current_version = read_ikanos_dependency_version(pom_path, artifact_id)

    if not current_version.endswith("-SNAPSHOT"):
        print(
            f"[ok] {pom_path}: io.ikanos:{artifact_id} already at {current_version} "
            f"(no -SNAPSHOT suffix)",
            file=sys.stderr,
        )
        return False

    new_version = current_version[: -len("-SNAPSHOT")]
    write_ikanos_dependency_version(pom_path, artifact_id, new_version)
    print(f"[ok] {pom_path}: io.ikanos:{artifact_id} {current_version} -> {new_version}", file=sys.stderr)
    return True


def main():
    parser = argparse.ArgumentParser(
        description="Remove the -SNAPSHOT suffix from the current Polychro engine version."
    )
    parser.add_argument("--pom", default="pom.xml", help="Path to the root pom.xml")
    parser.add_argument(
        "--capability-pom",
        default="modules/polychro-capability/pom.xml",
        help="Path to modules/polychro-capability/pom.xml",
    )
    args = parser.parse_args()

    print("=" * 60)
    print("Polychro version unbump")
    print("=" * 60)

    unbump_pom_revision(args.pom)
    unbump_ikanos_dependency_version(args.capability_pom, "ikanos-engine")
    unbump_ikanos_dependency_version(args.capability_pom, "ikanos-spec")

    print("=" * 60)


if __name__ == "__main__":
    main()
