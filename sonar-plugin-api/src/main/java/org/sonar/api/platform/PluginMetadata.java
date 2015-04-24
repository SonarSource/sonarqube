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
package org.sonar.api.platform;

import javax.annotation.CheckForNull;
import java.io.File;
import java.util.List;

/**
 * @since 2.8
 */
public interface PluginMetadata {
  File getFile();

  List<File> getDeployedFiles();

  String getKey();

  String getName();

  String getMainClass();

  String getDescription();

  String getOrganization();

  String getOrganizationUrl();

  String getLicense();

  String getVersion();

  String getHomepage();

  /**
   * @since 3.6
   */
  String getIssueTrackerUrl();

  boolean isUseChildFirstClassLoader();

  String getBasePlugin();

  /**
   * Always return <code>null</code> since version 5.2
   * @deprecated in 5.2. Concept of parent relationship is removed. See https://jira.codehaus.org/browse/SONAR-6433
   */
  @Deprecated
  @CheckForNull
  String getParent();

  List<String> getRequiredPlugins();

  boolean isCore();

  String getImplementationBuild();
}
