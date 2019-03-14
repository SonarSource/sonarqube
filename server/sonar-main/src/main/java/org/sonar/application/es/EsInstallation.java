/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.application.es;

import java.io.File;
import java.util.List;
import java.util.Properties;
import org.sonar.application.command.EsJvmOptions;

public interface EsInstallation {
  File getHomeDirectory();

  List<File> getOutdatedSearchDirectories();

  File getDataDirectory();

  File getConfDirectory();

  File getLogDirectory();

  File getTmpDirectory();

  File getExecutable();

  File getLog4j2PropertiesLocation();

  File getElasticsearchYml();

  File getJvmOptions();

  File getLibDirectory();

  EsJvmOptions getEsJvmOptions();

  EsYmlSettings getEsYmlSettings();

  Properties getLog4j2Properties();

  String getClusterName();

  String getHost();

  int getPort();
}
