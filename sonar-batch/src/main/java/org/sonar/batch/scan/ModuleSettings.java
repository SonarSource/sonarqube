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

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.bootstrap.BatchSettings;
import org.sonar.batch.bootstrap.ServerClient;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static org.sonar.batch.bootstrap.BatchSettings.BATCH_BOOTSTRAP_PROPERTIES_URL;

/**
 * @since 2.12
 */
public class ModuleSettings extends Settings {

  private final Configuration deprecatedCommonsConf;
  private final ServerClient client;
  private AnalysisMode analysisMode;

  public ModuleSettings(BatchSettings batchSettings, ProjectDefinition project, Configuration deprecatedCommonsConf, ServerClient client, AnalysisMode analysisMode) {
    super(batchSettings.getDefinitions());
    this.client = client;
    this.analysisMode = analysisMode;
    getEncryption().setPathToSecretKey(batchSettings.getString(CoreProperties.ENCRYPTION_SECRET_KEY_PATH));

    LoggerFactory.getLogger(ModuleSettings.class).info("Load module settings");
    this.deprecatedCommonsConf = deprecatedCommonsConf;
    init(project, batchSettings);
  }

  private ModuleSettings init(ProjectDefinition project, BatchSettings batchSettings) {
    addProjectProperties(project, batchSettings);
    addBuildProperties(project);
    addEnvironmentVariables();
    addSystemProperties();
    return this;
  }

  private void addProjectProperties(ProjectDefinition project, BatchSettings batchSettings) {
    String branch = batchSettings.getString(CoreProperties.PROJECT_BRANCH_PROPERTY);
    String projectKey = project.getKey();
    if (StringUtils.isNotBlank(branch)) {
      projectKey = String.format("%s:%s", projectKey, branch);
    }
    addProperties(batchSettings.getProperties());
    downloadSettings(projectKey);
  }

  private void downloadSettings(String moduleKey) {
    String url = BATCH_BOOTSTRAP_PROPERTIES_URL + "?project=" + moduleKey + "&dryRun=" + analysisMode.isPreview();
    String jsonText = client.request(url);
    List<Map<String, String>> json = new Gson().fromJson(jsonText, new TypeToken<List<Map<String, String>>>() {
    }.getType());
    for (Map<String, String> jsonProperty : json) {
      String key = jsonProperty.get("k");
      String value = jsonProperty.get("v");
      setProperty(key, value);
    }
  }

  private void addBuildProperties(ProjectDefinition project) {
    List<ProjectDefinition> orderedProjects = getTopDownParentProjects(project);
    for (ProjectDefinition p : orderedProjects) {
      addProperties(p.getProperties());
    }
  }

  /**
   * From root to given project
   */
  static List<ProjectDefinition> getTopDownParentProjects(ProjectDefinition project) {
    List<ProjectDefinition> result = Lists.newArrayList();
    ProjectDefinition p = project;
    while (p != null) {
      result.add(0, p);
      p = p.getParent();
    }
    return result;
  }

  @Override
  protected void doOnSetProperty(String key, @Nullable String value) {
    deprecatedCommonsConf.setProperty(key, value);
  }

  @Override
  protected void doOnRemoveProperty(String key) {
    deprecatedCommonsConf.clearProperty(key);
  }

  @Override
  protected void doOnClearProperties() {
    deprecatedCommonsConf.clear();
  }

  @Override
  protected void doOnGetProperties(String key) {
    if (analysisMode.isPreview() && key.endsWith(".secured") && !key.contains(".license")) {
      throw new SonarException("Access to the secured property '" + key
        + "' is not possible in preview mode. The SonarQube plugin which requires this property must be deactivated in preview mode.");
    }
  }
}
