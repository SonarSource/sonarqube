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

import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.home.cache.PersistentCache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

public class DefaultProjectCacheStatus implements ProjectCacheStatus {
  private static final String STATUS_PREFIX = "cache-sync-status-";
  private PersistentCache cache;
  private ServerClient client;

  public DefaultProjectCacheStatus(PersistentCache cache, ServerClient client) {
    this.cache = cache;
    this.client = client;
  }

  @Override
  public void save(String projectKey) {
    Date now = new Date();

    try {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      try (ObjectOutputStream objOutput = new ObjectOutputStream(byteOutput)) {
        objOutput.writeObject(now);
      }
      cache.put(getKey(projectKey), byteOutput.toByteArray());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write cache sync status", e);
    }
  }

  @Override
  public void delete(String projectKey) {
    try {
      cache.put(getKey(projectKey), new byte[0]);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to delete cache sync status", e);
    }
  }

  @Override
  public Date getSyncStatus(String projectKey) {
    try {
      byte[] status = cache.get(getKey(projectKey), null);
      if (status == null || status.length == 0) {
        return null;
      }
      ByteArrayInputStream byteInput = new ByteArrayInputStream(status);
      try (ObjectInputStream objInput = new ObjectInputStream(byteInput)) {
        return (Date) objInput.readObject();
      }
    } catch (IOException | ClassNotFoundException e) {
      throw new IllegalStateException("Failed to read cache sync status", e);
    }
  }

  private String getKey(String projectKey) {
    return STATUS_PREFIX + client.getURL() + "-" + projectKey;
  }
}
