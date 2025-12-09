/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scm.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;

public class JGitUtils {

  private JGitUtils() {
  }

  public static Repository buildRepository(Path basedir) {
    try {
      Repository repo = GitScmProvider.getVerifiedRepositoryBuilder(basedir).build();
      try (ObjectReader objReader = repo.getObjectDatabase().newReader()) {
        // SONARSCGIT-2 Force initialization of shallow commits to avoid later concurrent modification issue
        objReader.getShallowCommits();
        return repo;
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to open Git repository", e);
    }
  }

  // Return a list of scm ignored paths relative to the baseDir.
  public static List<String> getAllIgnoredPaths(Path baseDir) {
    try (Repository repo = buildRepository(baseDir)) {
      Path workTreePath = repo.getWorkTree().toPath();
      Path baseDirAbs = baseDir.toAbsolutePath().normalize();

      try (Git git = new Git(repo)) {
        return git.status().call().getIgnoredNotInIndex().stream()
          // Convert to absolute path
          .map(filePathStr -> workTreePath.resolve(filePathStr).normalize())
          // Exclude any outside of the baseDir
          .filter(filePath -> filePath.startsWith(baseDirAbs))
          // Make path relative to the baseDir
          .map(baseDir::relativize)
          .map(Path::toString)
          .sorted()
          .toList();
      } catch (GitAPIException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
