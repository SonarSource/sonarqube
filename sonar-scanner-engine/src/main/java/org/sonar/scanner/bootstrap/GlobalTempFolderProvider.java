/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.ComponentLifecycle;
import org.picocontainer.PicoContainer;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.internal.DefaultTempFolder;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static org.sonar.core.util.FileUtils.deleteQuietly;

public class GlobalTempFolderProvider extends ProviderAdapter implements ComponentLifecycle<TempFolder> {
  private static final Logger LOG = Loggers.get(GlobalTempFolderProvider.class);
  private static final long CLEAN_MAX_AGE = TimeUnit.DAYS.toMillis(21);
  static final String TMP_NAME_PREFIX = ".sonartmp_";
  private boolean started = false;

  private System2 system;
  private DefaultTempFolder tempFolder;

  public GlobalTempFolderProvider() {
    this(new System2());
  }

  GlobalTempFolderProvider(System2 system) {
    this.system = system;
  }

  public TempFolder provide(RawScannerProperties scannerProps) {
    if (tempFolder == null) {

      String workingPathName = StringUtils.defaultIfBlank(scannerProps.property(CoreProperties.GLOBAL_WORKING_DIRECTORY), CoreProperties.GLOBAL_WORKING_DIRECTORY_DEFAULT_VALUE);
      Path workingPath = Paths.get(workingPathName);

      if (!workingPath.isAbsolute()) {
        Path home = findSonarHome(scannerProps);
        workingPath = home.resolve(workingPath).normalize();
      }
      try {
        cleanTempFolders(workingPath);
      } catch (IOException e) {
        LOG.error(String.format("failed to clean global working directory: %s", workingPath), e);
      }
      Path tempDir = createTempFolder(workingPath);
      tempFolder = new DefaultTempFolder(tempDir.toFile(), true);
    }
    return tempFolder;
  }

  private static Path createTempFolder(Path workingPath) {
    try {
      Path realPath = workingPath;
      if (Files.isSymbolicLink(realPath)) {
        realPath = realPath.toRealPath();
      }
      Files.createDirectories(realPath);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create working path: " + workingPath, e);
    }

    try {
      return Files.createTempDirectory(workingPath, TMP_NAME_PREFIX);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create temporary folder in " + workingPath, e);
    }
  }

  private Path findSonarHome(RawScannerProperties props) {
    String home = props.property("sonar.userHome");
    if (home != null) {
      return Paths.get(home).toAbsolutePath();
    }

    home = system.envVariable("SONAR_USER_HOME");

    if (home != null) {
      return Paths.get(home).toAbsolutePath();
    }

    home = system.property("user.home");
    return Paths.get(home, ".sonar").toAbsolutePath();
  }

  private static void cleanTempFolders(Path path) throws IOException {
    if (path.toFile().exists()) {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, new CleanFilter())) {
        for (Path p : stream) {
          deleteQuietly(p.toFile());
        }
      }
    }
  }

  private static class CleanFilter implements DirectoryStream.Filter<Path> {
    @Override
    public boolean accept(Path path) throws IOException {
      if (!path.toFile().exists()) {
        return false;
      }

      if (!path.getFileName().toString().startsWith(TMP_NAME_PREFIX)) {
        return false;
      }

      long threshold = System.currentTimeMillis() - CLEAN_MAX_AGE;

      // we could also check the timestamp in the name, instead
      BasicFileAttributes attrs;

      try {
        attrs = Files.readAttributes(path, BasicFileAttributes.class);
      } catch (IOException ioe) {
        LOG.error(String.format("Couldn't read file attributes for %s : ", path), ioe);
        return false;
      }

      long creationTime = attrs.creationTime().toMillis();
      return creationTime < threshold;
    }
  }

  @Override
  public void start(PicoContainer container) {
    started = true;
  }

  @Override
  public void stop(PicoContainer container) {
    if (tempFolder != null) {
      tempFolder.stop();
    }
  }

  @Override
  public void dispose(PicoContainer container) {
    // nothing to do
  }

  @Override
  public boolean componentHasLifecycle() {
    return true;
  }

  @Override
  public boolean isStarted() {
    return started;
  }
}
