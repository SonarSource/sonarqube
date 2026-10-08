/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.application.es;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.DoubleUnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.MessageException;

import static java.lang.String.format;

public class EsDiskSpaceChecker {
  private static final Logger LOG = LoggerFactory.getLogger(EsDiskSpaceChecker.class);

  private static final int MIN_FREE_DISK_SPACE_PERCENT = 10;
  private static final long MAX_REQUIRED_FREE_DISK_SPACE_BYTES = 150L * 1024 * 1024 * 1024;

  private final FileStoreResolver fileStoreResolver;

  public EsDiskSpaceChecker() {
    this(Files::getFileStore);
  }

  EsDiskSpaceChecker(FileStoreResolver fileStoreResolver) {
    this.fileStoreResolver = fileStoreResolver;
  }

  public void check(EsInstallation esInstallation) {
    if (!esInstallation.isDiskThresholdEnabled()) {
      return;
    }
    Path dataDirectory = closestExistingPath(esInstallation.getDataDirectory().toPath().toAbsolutePath());
    long totalSpace;
    long usableSpace;
    try {
      FileStore fileStore = fileStoreResolver.resolve(dataDirectory);
      totalSpace = fileStore.getTotalSpace();
      usableSpace = fileStore.getUsableSpace();
    } catch (IOException e) {
      LOG.warn("Unable to check free disk space of Elasticsearch data directory [{}]", dataDirectory, e);
      return;
    }
    if (totalSpace <= 0) {
      return;
    }

    long requiredSpace = Math.min(totalSpace / 100 * MIN_FREE_DISK_SPACE_PERCENT, MAX_REQUIRED_FREE_DISK_SPACE_BYTES);
    if (usableSpace < requiredSpace) {
      throw new MessageException(format(Locale.ENGLISH,
        "Not enough free disk space for Elasticsearch data directory [%s]: %s available out of %s (%.1f%%), at least %s is required. "
          + "Elasticsearch would not be able to allocate its indices. Free up some disk space and restart SonarQube.",
        dataDirectory, toDisplaySize(usableSpace, Math::floor), toDisplaySize(totalSpace, Math::floor),
        floorToOneDecimal(100.0 * usableSpace / totalSpace), toDisplaySize(requiredSpace, Math::ceil)));
    }
  }

  private static Path closestExistingPath(Path path) {
    Path current = path;
    while (current.getParent() != null && !Files.exists(current)) {
      current = current.getParent();
    }
    return current;
  }

  private static String toDisplaySize(long bytes, DoubleUnaryOperator rounding) {
    double megabytes = bytes / (1024.0 * 1024.0);
    if (megabytes < 1024) {
      return format(Locale.ENGLISH, "%.1f MB", rounding.applyAsDouble(megabytes * 10) / 10);
    }
    return format(Locale.ENGLISH, "%.1f GB", rounding.applyAsDouble(megabytes / 1024.0 * 10) / 10);
  }

  private static double floorToOneDecimal(double value) {
    return Math.floor(value * 10) / 10;
  }

  @FunctionalInterface
  interface FileStoreResolver {
    FileStore resolve(Path path) throws IOException;
  }
}
