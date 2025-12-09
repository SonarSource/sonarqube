/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.newcodeperiod.ws;

import org.sonar.core.documentation.DocumentationLinkGenerator;

import static org.sonar.server.ws.WsUtils.createHtmlExternalLink;

public class NewCodePeriodsWsUtils {
  public static final String DOCUMENTATION_LINK = "/project-administration/configuring-new-code-calculation#setting-specific-new-code-definition-for-project";

  private NewCodePeriodsWsUtils() {
    // do nothing
  }

  public static String createNewCodePeriodHtmlLink(DocumentationLinkGenerator documentationLinkGenerator) {
    return createHtmlExternalLink(documentationLinkGenerator.getDocumentationLink(DOCUMENTATION_LINK), "new code definition");
  }
}
