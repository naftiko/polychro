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
package io.polychro.ruleset.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A utility class that allows manipulation of files.
 */
public final class FileUtils {

    private FileUtils() {
        // Utils class
    }

    /**
     * Read a file's content from the file system.
     * @param path  the file's path.
     * @return  the file's content.
     * @throws IOException in case an error occurs while reading the file's content.
     */
    public static String getFileContentFromFileSystem(Path path) throws IOException {
        return Files.readString(path);
    }

    /**
     * Read a file's content from the classpath.
     * @param source    the source's path.
     * @return  the file's content.
     * @throws IOException in case an error occurs while reading the file's content.
     */
    public static String getFileContentFromClasspath(String source) throws IOException {
        try (InputStream is = FileUtils.class.getResourceAsStream(source)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } else {
                throw new IOException("Could not get an input stream to read %s".formatted(source));
            }
        }
    }
}
