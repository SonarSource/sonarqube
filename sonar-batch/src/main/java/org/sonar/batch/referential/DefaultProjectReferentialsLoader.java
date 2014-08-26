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
package org.sonar.batch.referential;

import com.google.common.base.Charsets;
import com.google.common.io.InputSupplier;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.bootstrap.TaskProperties;
import org.sonar.batch.protocol.input.ProjectReferentials;
import org.sonar.batch.rule.ModuleQProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class DefaultProjectReferentialsLoader implements ProjectReferentialsLoader {

  private static final String BATCH_PROJECT_URL = "/batch/project";

  private final ServerClient serverClient;
  private final AnalysisMode analysisMode;

  public DefaultProjectReferentialsLoader(ServerClient serverClient, AnalysisMode analysisMode) {
    this.serverClient = serverClient;
    this.analysisMode = analysisMode;
  }

  @Override
  public ProjectReferentials load(ProjectReactor reactor, TaskProperties taskProperties) {
    String url = BATCH_PROJECT_URL + "?key=" + reactor.getRoot().getKeyWithBranch();
    if (taskProperties.properties().containsKey(ModuleQProfiles.SONAR_PROFILE_PROP)) {
      try {
        url += "&profile=" + URLEncoder.encode(taskProperties.properties().get(ModuleQProfiles.SONAR_PROFILE_PROP), "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new IllegalStateException("Unable to encode URL", e);
      }
    }
    url += "&preview=" + analysisMode.isPreview();
    InputSupplier<InputStream> jsonStream = serverClient.doRequest(url, null);
    try {
      return ProjectReferentials.fromJson(new InputStreamReader(jsonStream.getInput(), Charsets.UTF_8));
    } catch (IOException e) {
      throw new IllegalStateException("Unable to load project referentials", e);
    }
  }
}
