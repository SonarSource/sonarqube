/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.batch;

import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.Plugins;
import org.sonar.api.resources.Project;

/**
 * This class implements the management of the code coverage engine if there are several.
 * It is a pre-implementation for Sensors and Decorators
 *
 * @since 1.10
 */
public abstract class AbstractCoverageExtension implements BatchExtension {

  /**
   * The plugin key to retrieve the coverage engine to be used
   */
  public static final String PARAM_PLUGIN = "sonar.core.codeCoveragePlugin";

  /**
   * The default value for the code coverage plugin
   */
  public static final String DEFAULT_PLUGIN = "cobertura";

  private final Plugins plugins;

  /**
   * Default constructor
   * @param plugins the list of plugins available
   */
  public AbstractCoverageExtension(Plugins plugins) {
    this.plugins = plugins;
  }

  /**
   * Whether to implement the extension on the project
   */
  public boolean shouldExecuteOnProject(Project project) {
    return project.getAnalysisType().isDynamic(true) && isSelectedPlugin(project);
  }

  protected boolean isSelectedPlugin(Project project) {
    String[] selectedPluginKeys = project.getConfiguration().getStringArray(PARAM_PLUGIN);
    if (selectedPluginKeys.length == 0) {
      selectedPluginKeys = new String[]{DEFAULT_PLUGIN};
    }
    String pluginKey = plugins.getPluginKeyByExtension(getClass());
    return ArrayUtils.contains(selectedPluginKeys, pluginKey);
  }
}
