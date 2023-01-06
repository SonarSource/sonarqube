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

import java.io.InputStream;
import org.jetbrains.annotations.Nullable;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.springframework.context.annotation.Bean;

public class AnalysisCacheProvider {

  @Bean("ReadCache")
  public ReadCache provideReader(AnalysisCacheEnabled analysisCacheEnabled, AnalysisCacheMemoryStorage storage) {
    if (analysisCacheEnabled.isEnabled()) {
      storage.load();
      return new ReadCacheImpl(storage);
    }
    return new NoOpReadCache();
  }

  @Bean("WriteCache")
  public ScannerWriteCache provideWriter(AnalysisCacheEnabled analysisCacheEnabled, ReadCache readCache, BranchConfiguration branchConfiguration, FileStructure fileStructure) {
    if (analysisCacheEnabled.isEnabled() && !branchConfiguration.isPullRequest()) {
      return new WriteCacheImpl(readCache, fileStructure);
    }
    return new NoOpWriteCache();
  }

  static class NoOpWriteCache implements ScannerWriteCache {
    @Override
    public void write(String s, InputStream inputStream) {
      // no op
    }

    @Override
    public void write(String s, byte[] bytes) {
      // no op
    }

    @Override
    public void copyFromPrevious(String s) {
      // no op
    }

    @Override
    public void close() {
      // no op
    }
  }

  static class NoOpReadCache implements ReadCache {
    @Nullable
    @Override
    public InputStream read(String s) {
      return null;
    }

    @Override
    public boolean contains(String s) {
      return false;
    }
  }
}
