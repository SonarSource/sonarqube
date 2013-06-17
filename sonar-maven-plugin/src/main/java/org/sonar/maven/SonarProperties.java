/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.maven;

public interface SonarProperties {

  String PROJECT_BRANCH_PROPERTY = "sonar.branch";
  String PROJECT_VERSION_PROPERTY = "sonar.projectVersion";
  String PROJECT_KEY_PROPERTY = "sonar.projectKey";
  String PROJECT_NAME_PROPERTY = "sonar.projectName";
  String PROJECT_DESCRIPTION_PROPERTY = "sonar.projectDescription";
  String PROJECT_LANGUAGE_PROPERTY = "sonar.language";
  String ENCODING_PROPERTY = "sonar.sourceEncoding";

  String DRY_RUN_PROPERTY = "sonar.dryRun";
  String DRY_RUN_OUTPUT_PROPERTY = "sonar.dryRun.export.path";
  String REPORT_OUTPUT_PROPERTY = "sonar.report.export.path";

  String SONAR_URL = "sonar.host.url";
  String SONAR_LOGIN = "sonar.login";
  String SONAR_PASSWORD = "sonar.password";
  String PROJECT_BASEDIR = "sonar.projectBaseDir";
  String WORK_DIR = "sonar.working.directory";

  String VERBOSE_PROPERTY = "sonar.verbose";

  char SEPARATOR = ',';

}
