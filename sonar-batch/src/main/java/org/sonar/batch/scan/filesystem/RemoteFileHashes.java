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
import org.picocontainer.Startable;
import org.sonar.api.BatchComponent;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.PastSnapshotFinder;
import org.sonar.core.source.SnapshotDataTypes;
import org.sonar.core.source.jdbc.SnapshotDataDao;
import org.sonar.core.source.jdbc.SnapshotDataDto;

import javax.annotation.CheckForNull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class RemoteFileHashes implements BatchComponent, Startable {

  private final SnapshotDataDao dao;
  private final PastSnapshotFinder pastSnapshotFinder;
  private final Snapshot snapshot;

  private Map<String, String> pathToHash = Maps.newHashMap();

  public RemoteFileHashes(Snapshot snapshot, SnapshotDataDao dao, PastSnapshotFinder pastSnapshotFinder) {
    this.snapshot = snapshot;
    this.dao = dao;
    this.pastSnapshotFinder = pastSnapshotFinder;
  }

  @Override
  public void start() {
    // Extract previous checksum of all files of this module and store them in a map
    PastSnapshot pastSnapshot = pastSnapshotFinder.findPreviousAnalysis(snapshot);
    if (pastSnapshot.isRelatedToSnapshot()) {
      Collection<SnapshotDataDto> selectSnapshotData = dao.selectSnapshotData(
        pastSnapshot.getProjectSnapshot().getId().longValue(),
        Arrays.asList(SnapshotDataTypes.FILE_HASHES)
      );
      if (!selectSnapshotData.isEmpty()) {
        SnapshotDataDto snapshotDataDto = selectSnapshotData.iterator().next();
        String data = snapshotDataDto.getData();
        pathToHash = KeyValueFormat.parse(data);
      }
    }
  }

  @CheckForNull
  public String remoteHash(String baseRelativePath) {
    return pathToHash.get(baseRelativePath);
  }

  @Override
  public void stop() {
  }
}
