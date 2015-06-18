/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.bootstrap;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.apache.commons.io.FileUtils;
import org.sonar.api.utils.TempFolder;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.internal.DefaultTempFolder;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

public class TempFolderProvider extends LifecycleProviderAdapter {
  private static final Logger LOG = Loggers.get(TempFolderProvider.class);
  private static final long CLEAN_MAX_AGE = TimeUnit.DAYS.toMillis(21);
  static final String TMP_NAME_PREFIX = ".sonartmp_";

  private DefaultTempFolder tempFolder;

  public TempFolder provide(BootstrapProperties bootstrapProps) {
    if (tempFolder == null) {

      String workingPathName = StringUtils.defaultIfBlank(bootstrapProps.property(CoreProperties.GLOBAL_WORKING_DIRECTORY), CoreProperties.GLOBAL_WORKING_DIRECTORY_DEFAULT_VALUE);
      Path workingPath = Paths.get(workingPathName).normalize();

      if (!workingPath.isAbsolute()) {
        Path home = findHome(bootstrapProps);
        workingPath = home.resolve(workingPath).normalize();
      }

      try {
        cleanTempFolders(workingPath);
      } catch (IOException e) {
        LOG.warn("failed to clean global working directory: " + e.getMessage());
      }

      Path tempDir = workingPath.resolve(TMP_NAME_PREFIX + System.currentTimeMillis());
      try {
        Files.createDirectories(tempDir);
      } catch (IOException e) {
        throw new IllegalStateException("Unable to create root temp directory " + tempDir, e);
      }
      tempFolder = new DefaultTempFolder(tempDir.toFile(), true);
      this.instance = tempFolder;
    }
    return tempFolder;
  }

  private static Path findHome(BootstrapProperties props) {
    String home = props.property("sonar.userHome");
    if (home != null) {
      return Paths.get(home);
    }

    home = System.getenv("SONAR_USER_HOME");

    if (home != null) {
      return Paths.get(home);
    }

    home = System.getProperty("user.home");
    return Paths.get(home, ".sonar");
  }

  private static void cleanTempFolders(Path path) throws IOException {
    if (Files.exists(path)) {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, new CleanFilter())) {
        for (Path p : stream) {
          FileUtils.deleteQuietly(p.toFile());
        }
      }
    }
  }

  private static class CleanFilter implements DirectoryStream.Filter<Path> {
    @Override
    public boolean accept(Path e) throws IOException {
      if (!Files.isDirectory(e)) {
        return false;
      }

      if (!e.getFileName().toString().startsWith(TMP_NAME_PREFIX)) {
        return false;
      }

      long threshold = System.currentTimeMillis() - CLEAN_MAX_AGE;

      // we could also check the timestamp in the name, instead
      BasicFileAttributes attrs;

      try {
        attrs = Files.readAttributes(e, BasicFileAttributes.class);
      } catch (IOException ioe) {
        LOG.warn("couldn't read file attributes for " + e + " : " + ioe.getMessage());
        return false;
      }

      long creationTime = attrs.creationTime().toMillis();
      return creationTime < threshold;
    }
  }
}
