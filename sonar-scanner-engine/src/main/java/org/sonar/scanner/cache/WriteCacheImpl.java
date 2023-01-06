/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.cache;

import com.google.protobuf.ByteString;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.scanner.protocol.internal.ScannerInternal.SensorCacheEntry;
import org.sonar.scanner.protocol.output.FileStructure;

import static org.sonar.api.utils.Preconditions.checkArgument;
import static org.sonar.api.utils.Preconditions.checkNotNull;

public class WriteCacheImpl implements ScannerWriteCache {
  private final ReadCache readCache;
  private final Set<String> keys = new HashSet<>();
  private final FileStructure fileStructure;

  private OutputStream stream = null;

  public WriteCacheImpl(ReadCache readCache, FileStructure fileStructure) {
    this.readCache = readCache;
    this.fileStructure = fileStructure;
  }

  @Override
  public void write(String key, InputStream data) {
    checkNotNull(data);
    try {
      write(key, data.readAllBytes());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read sensor write cache data", e);
    }
  }

  @Override
  public void write(String key, byte[] data) {
    checkNotNull(data);
    checkKey(key);
    try {
      OutputStream out = getStream();
      toProto(key, data).writeDelimitedTo(out);
      keys.add(key);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write to sensor cache file", e);
    }
  }

  private OutputStream getStream() throws IOException {
    if (stream == null) {
      stream = new GZIPOutputStream(new FileOutputStream(fileStructure.analysisCache()));
    }
    return stream;
  }

  @Override
  public void copyFromPrevious(String key) {
    checkArgument(readCache.contains(key), "Previous cache doesn't contain key '%s'", key);
    write(key, readCache.read(key));
  }

  private static SensorCacheEntry toProto(String key, byte[] data) {
    return SensorCacheEntry.newBuilder()
      .setKey(key)
      .setData(ByteString.copyFrom(data))
      .build();
  }

  @Override
  public void close() {
    if (stream != null) {
      try {
        stream.close();
      } catch (IOException e) {
        throw new IllegalStateException("Failed to close sensor cache file", e);
      }
    }
  }

  private void checkKey(String key) {
    checkNotNull(key);
    checkArgument(!keys.contains(key), "Cache already contains key '%s'", key);
  }
}
