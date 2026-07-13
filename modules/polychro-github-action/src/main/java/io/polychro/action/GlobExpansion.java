/**
 * Copyright 2026 Naftiko
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.polychro.action;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Expands glob patterns into resolved file paths.
 * Supports multiple patterns separated by newlines or commas.
 */
public final class GlobExpansion {

    private GlobExpansion() {
    }

    /**
     * Expand one or more glob patterns into matching file paths.
     *
     * @param baseDir  the base directory to resolve patterns against
     * @param patterns glob patterns (comma or newline separated)
     * @return sorted list of matching file paths
     */
    public static List<Path> expand(Path baseDir, String patterns) {
        if (patterns == null || patterns.isBlank()) {
            return List.of();
        }

        String[] parts = patterns.split("[,\\n]+");
        List<Path> results = new ArrayList<>();

        for (String raw : parts) {
            String pattern = raw.strip();
            if (pattern.isEmpty()) {
                continue;
            }
            results.addAll(matchGlob(baseDir, pattern));
        }

        return results.stream().sorted().distinct().toList();
    }

    static List<Path> matchGlob(Path baseDir, String pattern) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        List<Path> matches = new ArrayList<>();

        try {
            Files.walkFileTree(baseDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    Path relative = baseDir.relativize(file);
                    if (matcher.matches(relative)) {
                        matches.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk directory: " + baseDir, e);
        }

        return matches;
    }
}
