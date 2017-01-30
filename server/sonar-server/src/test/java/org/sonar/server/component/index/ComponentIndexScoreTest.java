/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.component.index;

import org.junit.Test;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;

public class ComponentIndexScoreTest extends ComponentIndexTest {

  @Test
  public void should_prefer_components_without_prefix() {
    assertResultOrder("File.java",
      "File.java",
      "MyFile.java");
  }

  @Test
  public void should_prefer_components_without_suffix() {
    assertResultOrder("File",
      "File",
      "Filex");
  }

  @Test
  public void should_prefer_key_matching_over_name_matching() {
    ComponentDto project1 = indexProject("quality", "SonarQube");
    ComponentDto project2 = indexProject("sonarqube", "Quality Product");

    assertExactResults("sonarqube", project2, project1);
  }

  @Test
  public void scoring_test_DbTester() {
    features.set(ComponentIndexSearchFeature.PARTIAL);

    ComponentDto project = indexProject("key-1", "Quality Product");

    index(ComponentTesting.newFileDto(project)
      .setName("DbTester.java")
      .setKey("java/org/example/DbTester.java")
      .setUuid("UUID-DbTester"));

    index(ComponentTesting.newFileDto(project)
      .setName("WebhookDbTesting.java")
      .setKey("java/org/example/WebhookDbTesting.java")
      .setUuid("UUID-WebhookDbTesting"));

    assertSearch("dbt").containsExactly(

      "UUID-DbTester",
      "UUID-WebhookDbTesting"

    );
  }
}
