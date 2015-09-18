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
package org.sonar.batch.cache;

import org.apache.commons.io.FileUtils;

import org.sonar.home.cache.PersistentCache;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

public class DefaultProjectCacheStatus implements ProjectCacheStatus {
  private static final String STATUS_FILENAME = "cache-sync-status";
  private PersistentCache cache;

  public DefaultProjectCacheStatus(PersistentCache cache) {
    this.cache = cache;
  }

  @Override
  public void save() {
    Date now = new Date();

    try {
      FileOutputStream fos = new FileOutputStream(getStatusFilePath().toFile());
      try (ObjectOutputStream objOutput = new ObjectOutputStream(fos)) {
        objOutput.writeObject(now);
      }

    } catch (IOException e) {
      throw new IllegalStateException("Failed to write cache sync status", e);
    }
  }

  @Override
  public void delete() {
    cache.clear();
    FileUtils.deleteQuietly(getStatusFilePath().toFile());
  }

  @Override
  public Date getSyncStatus() {
    Path p = getStatusFilePath();
    try {
      if (!Files.isRegularFile(p)) {
        return null;
      }
      InputStream is = new FileInputStream(p.toFile());
      try (ObjectInputStream objInput = new ObjectInputStream(is)) {
        return (Date) objInput.readObject();
      }
    } catch (IOException | ClassNotFoundException e) {
      FileUtils.deleteQuietly(p.toFile());
      throw new IllegalStateException("Failed to read cache sync status", e);
    }
  }

  private Path getStatusFilePath() {
    return cache.getDirectory().resolve(STATUS_FILENAME);
  }
}
