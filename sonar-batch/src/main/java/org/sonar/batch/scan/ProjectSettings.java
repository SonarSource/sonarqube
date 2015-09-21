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

import org.sonar.batch.repository.ProjectRepositories;

import org.sonar.batch.analysis.DefaultAnalysisMode;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.bootstrap.DroppedPropertyChecker;
import org.sonar.batch.bootstrap.GlobalSettings;

public class ProjectSettings extends Settings {

  /**
   * A map of dropped properties as key and specific message to display for that property
   * (what will happen, what should the user do, ...) as a value
   */
  private static final Map<String, String> DROPPED_PROPERTIES = ImmutableMap.of(
    "sonar.qualitygate", "It will be ignored."
    );

  private final GlobalSettings globalSettings;
  private final ProjectRepositories projectRepositories;
  private final DefaultAnalysisMode mode;

  public ProjectSettings(ProjectReactor reactor, GlobalSettings globalSettings, PropertyDefinitions propertyDefinitions,
    ProjectRepositories projectRepositories, DefaultAnalysisMode mode) {
    super(propertyDefinitions);
    this.mode = mode;
    getEncryption().setPathToSecretKey(globalSettings.getString(CoreProperties.ENCRYPTION_SECRET_KEY_PATH));
    this.globalSettings = globalSettings;
    this.projectRepositories = projectRepositories;
    init(reactor);
    new DroppedPropertyChecker(this, DROPPED_PROPERTIES).checkDroppedProperties();
  }

  private void init(ProjectReactor reactor) {
    addProperties(globalSettings.getProperties());

    addProperties(projectRepositories.settings(reactor.getRoot().getKeyWithBranch()));

    addProperties(reactor.getRoot().properties());
  }

  @Override
  protected void doOnGetProperties(String key) {
    if (mode.isIssues() && key.endsWith(".secured") && !key.contains(".license")) {
      throw MessageException.of("Access to the secured property '" + key
        + "' is not possible in issues mode. The SonarQube plugin which requires this property must be deactivated in issues mode.");
    }
  }
}
