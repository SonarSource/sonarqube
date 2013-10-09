/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.scan.filesystem;

import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.PastSnapshotFinder;
import org.sonar.core.source.SnapshotDataType;
import org.sonar.core.source.jdbc.SnapshotDataDao;
import org.sonar.core.source.jdbc.SnapshotDataDto;

import javax.annotation.CheckForNull;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FileHashCache implements BatchComponent, Startable {

  private Map<String, String> currentHashCache = Maps.newHashMap();
  private Map<String, String> previousHashCache = Maps.newHashMap();

  private PathResolver pathResolver;
  private HashBuilder hashBuilder;
  private SnapshotDataDao snapshotDataDao;
  private PastSnapshotFinder pastSnapshotFinder;
  private Snapshot snapshot;
  private ProjectDefinition module;

  public FileHashCache(ProjectDefinition module, PathResolver pathResolver, HashBuilder hashBuilder,
    Snapshot snapshot,
    SnapshotDataDao snapshotDataDao,
    PastSnapshotFinder pastSnapshotFinder) {
    this.module = module;
    this.pathResolver = pathResolver;
    this.hashBuilder = hashBuilder;
    this.snapshot = snapshot;
    this.snapshotDataDao = snapshotDataDao;
    this.pastSnapshotFinder = pastSnapshotFinder;
  }

  @Override
  public void start() {
    // Extract previous checksum of all files of this module and store them in a map
    PastSnapshot pastSnapshot = pastSnapshotFinder.findPreviousAnalysis(snapshot);
    if (pastSnapshot.isRelatedToSnapshot()) {
      Collection<SnapshotDataDto> selectSnapshotData = snapshotDataDao.selectSnapshotData(pastSnapshot.getProjectSnapshot().getId().longValue(),
        Arrays.asList(SnapshotDataType.FILE_HASH.getValue()));
      if (!selectSnapshotData.isEmpty()) {
        SnapshotDataDto snapshotDataDto = selectSnapshotData.iterator().next();
        String data = snapshotDataDto.getData();
        try {
          List<String> lines = IOUtils.readLines(new StringReader(data));
          for (String line : lines) {
            String[] keyValue = StringUtils.split(line, "=");
            if (keyValue.length == 2) {
              previousHashCache.put(keyValue[0], keyValue[1]);
            }
          }
        } catch (IOException e) {
          throw new SonarException("Unable to read previous file hashes", e);
        }
      }
    }
  }

  public String getCurrentHash(File file, Charset sourceCharset) {
    String relativePath = pathResolver.relativePath(module.getBaseDir(), file);
    if (!currentHashCache.containsKey(relativePath)) {
      currentHashCache.put(relativePath, hashBuilder.computeHashNormalizeLineEnds(file, sourceCharset));
    }
    return currentHashCache.get(relativePath);
  }

  @CheckForNull
  public String getPreviousHash(File file) {
    String relativePath = pathResolver.relativePath(module.getBaseDir(), file);
    return previousHashCache.get(relativePath);
  }

  @Override
  public void stop() {
    // Nothing to do
  }
}
