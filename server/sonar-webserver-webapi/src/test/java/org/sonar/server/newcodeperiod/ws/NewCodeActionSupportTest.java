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
package org.sonar.server.newcodeperiod.ws;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.newcodeperiod.ws.NewCodeActionSupport.getDocumentationUrl;

public class NewCodeActionSupportTest {
  private static final String LATEST = "https://docs.sonarqube.org/latest/project-administration/defining-new-code/";

  @Test
  public void check_documentation_url_generation() {
    assertThat(getDocumentationUrl("10.1-SNAPSHOT", "project-administration/defining-new-code/")).isEqualTo(LATEST);
    assertThat(getDocumentationUrl("10.1.0.654666-SNAPSHOT", "project-administration/defining-new-code/")).isEqualTo(LATEST);
    assertThat(getDocumentationUrl("X.X.10.WHATEVER", "project-administration/defining-new-code/")).isEqualTo(LATEST);
    assertThat(getDocumentationUrl("9.9.0.65466", "another/path/")).isEqualTo("https://docs.sonarqube.org/9.9/another/path/");
    assertThat(getDocumentationUrl("9.8.0", "yet/another/path/")).isEqualTo("https://docs.sonarqube.org/9.8/yet/another/path/");
    assertThat(getDocumentationUrl("100.999.0", "yet/another/path/")).isEqualTo("https://docs.sonarqube.org/100.999/yet/another/path/");
  }

}
