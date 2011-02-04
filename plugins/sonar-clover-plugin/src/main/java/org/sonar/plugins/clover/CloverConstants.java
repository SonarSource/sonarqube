/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.clover;

public interface CloverConstants {

  String LICENSE_PROPERTY = "sonar.clover.license.secured";
  String VERSION_PROPERTY = "sonar.clover.version";
  String DEFAULT_VERSION = "2.6.3";
  String REPORT_PATH_PROPERTY = "sonar.clover.reportPath";

  String MAVEN_GROUP_ID = "com.atlassian.maven.plugins";
  String MAVEN_ARTIFACT_ID = "maven-clover2-plugin";
}
