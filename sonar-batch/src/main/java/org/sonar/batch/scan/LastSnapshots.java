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
package org.sonar.batch.scan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.core.source.db.SnapshotSourceDao;

import javax.annotation.CheckForNull;

public class LastSnapshots implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(LastSnapshots.class);

  private final AnalysisMode analysisMode;
  private final ServerClient server;
  private final SnapshotSourceDao sourceDao;

  public LastSnapshots(AnalysisMode analysisMode, SnapshotSourceDao dao, ServerClient server) {
    this.analysisMode = analysisMode;
    this.sourceDao = dao;
    this.server = server;
  }

  public String getSource(Resource resource) {
    String source = "";
    if (ResourceUtils.isFile(resource)) {
      if (analysisMode.isPreview()) {
        source = loadSourceFromWs(resource);
      } else {
        source = loadSourceFromDb(resource);
      }
    }
    return source;
  }

  private String loadSourceFromWs(Resource resource) {
    TimeProfiler profiler = new TimeProfiler(LOG).start("Load previous source code of: " + resource.getEffectiveKey()).setLevelToDebug();
    try {
      return server.request("/api/sources?resource=" + resource.getEffectiveKey() + "&format=txt", "GET", false, analysisMode.getPreviewReadTimeoutSec() * 1000);
    } catch (HttpDownloader.HttpException he) {
      if (he.getResponseCode() == 404) {
        return "";
      }
      throw he;
    } finally {
      profiler.stop();
    }
  }

  @CheckForNull
  private String loadSourceFromDb(Resource resource) {
    return sourceDao.selectSnapshotSourceByComponentKey(resource.getEffectiveKey());
  }
}
