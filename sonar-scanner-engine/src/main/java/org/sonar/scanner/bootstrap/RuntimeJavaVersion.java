/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.core.documentation.DocumentationLinkGenerator;

public class RuntimeJavaVersion {
  private static final Logger LOG = LoggerFactory.getLogger(RuntimeJavaVersion.class);
  public static final String LOG_MESSAGE = "SonarScanner will require Java 17 to run, starting in SonarQube 10.4";
  public static final String WARNING_MESSAGE_TEMPLATE = "SonarScanner will require Java 17 to run, starting in SonarQube 10.4. Please upgrade the" +
    " version of Java that executes the scanner and refer to <a href=\"{}/\" target=\"_blank\">the documentation</a> if needed.";

  private final DocumentationLinkGenerator documentationLinkGenerator;
  private final AnalysisWarnings analysisWarnings;

  public RuntimeJavaVersion(DocumentationLinkGenerator documentationLinkGenerator, AnalysisWarnings analysisWarnings){
    this.documentationLinkGenerator = documentationLinkGenerator;
    this.analysisWarnings = analysisWarnings;
  }

  public void checkJavaVersion() {
    Runtime.Version version = Runtime.version();
    if (version.compareTo(Runtime.Version.parse("17")) < 0) {
      LOG.warn(LOG_MESSAGE);
      String documentationLink = documentationLinkGenerator.getDocumentationLink("/analyzing-source-code/scanner-environment");
      analysisWarnings.addUnique(WARNING_MESSAGE_TEMPLATE.replace("{}", documentationLink));
    }
  }
}
