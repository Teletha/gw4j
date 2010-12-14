/*
 * Copyright (C) 2010 Nameless Production Committee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bee.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import bee.Platform;
import bee.Version;

/**
 * @version 2010/09/09 20:10:56
 */
public class Library {

    /** The artifact name. */
    public final String artifact;

    /** The group name. */
    public final String group;

    /** The version identifier. */
    public final Version version;

    /**
     * @param artifact
     * @param group
     * @param version
     */
    public Library(String artifact, String group, String version) {
        this.artifact = artifact;
        this.group = group;
        this.version = new Version(version);
    }

    public File getJar() {
        return get(".jar");
    }

    /**
     * Load library from the specified repositories.
     * 
     * @param repositories
     * @return
     */
    public File load(List<Repository> repositories) {
        // Locate local jar file.
        File file = new File(Platform.Repository, toPath("jar"));

        // Check exstence.
        if (!file.exists()) {

        }

        // API defintion
        return file;
    }

    /**
     * <p>
     * Locate jar file in your internal repository.
     * </p>
     * 
     * @return A location.
     */
    File toInternal(String extension) {
        return new File(Platform.Repository, toPath(extension));
    }

    /**
     * <p>
     * Locate jar file in the central repository.
     * </p>
     * 
     * @return A location.
     */
    URL toExternal(String extension) {
        return toExternal(extension, Repository.builtin.get(0));
    }

    /**
     * <p>
     * Locate jar file in the specified external repository.
     * </p>
     * 
     * @param repository A target repository.
     * @return A location.
     */
    URL toExternal(String extension, Repository repository) {
        try {
            return new URL(repository.url, toPath(extension));
        } catch (MalformedURLException e) {
            // If this exception will be thrown, it is bug of this program. So we must rethrow the
            // wrapped error in here.
            throw new Error(e);
        }
    }

    /**
     * <p>
     * Helper method to build path string.
     * </p>
     * 
     * @param extension An extension value.
     * @return A path string.
     */
    String toPath(String extension) {
        StringBuilder builder = new StringBuilder();
        builder.append(group).append('/');
        builder.append(artifact).append('/');
        builder.append(version).append('/');
        builder.append(artifact).append('-').append(version).append(extension);

        return builder.toString();
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifact == null) ? 0 : artifact.hashCode());
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Library other = (Library) obj;
        if (artifact == null) {
            if (other.artifact != null) return false;
        } else if (!artifact.equals(other.artifact)) return false;
        if (group == null) {
            if (other.group != null) return false;
        } else if (!group.equals(other.group)) return false;
        if (version == null) {
            if (other.version != null) return false;
        } else if (!version.equals(other.version)) return false;
        return true;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(artifact).append('-').append(version);

        return builder.toString();
    }
}
