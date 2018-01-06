/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.batch.bootstrap;

import java.util.Properties;
import org.junit.Test;
import org.sonar.api.CoreProperties;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectDefinitionTest {

  @Test
  public void shouldSetKey() {
    ProjectDefinition def = ProjectDefinition.create();
    def.setKey("mykey");
    assertThat(def.getKey()).isEqualTo("mykey");
  }

  @Test
  public void shouldSetVersion() {
    ProjectDefinition def = ProjectDefinition.create();
    def.setVersion("2.0-SNAPSHOT");
    assertThat(def.getVersion()).isEqualTo("2.0-SNAPSHOT");
  }

  @Test
  public void shouldSupportNoVersion() {
    ProjectDefinition def = ProjectDefinition.create();
    def.setVersion(null);
    assertThat(def.getVersion()).isEqualTo("not provided");
    assertThat(def.getOriginalVersion()).isEqualTo("");
  }

  @Test
  public void shouldSetOptionalFields() {
    ProjectDefinition def = ProjectDefinition.create();
    def.setName("myname");
    def.setDescription("desc");
    assertThat(def.getName()).isEqualTo("myname");
    assertThat(def.getDescription()).isEqualTo("desc");
  }

  @Test
  public void shouldSupportDefaultName() {
    ProjectDefinition def = ProjectDefinition.create();
    def.setKey("myKey");
    assertThat(def.getName()).isEqualTo("myKey");
  }

  @Test
  public void shouldGetKeyFromProperties() {
    Properties props = new Properties();
    props.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "foo");
    ProjectDefinition def = ProjectDefinition.create();
    def.setProperties(props);
    assertThat(def.getKey()).isEqualTo("foo");
  }

  @Test
  public void testDefaultValues() {
    ProjectDefinition def = ProjectDefinition.create();
    assertThat(def.sources()).isEmpty();
    assertThat(def.tests()).isEmpty();
  }

  /**
   * See SONAR-2879
   */
  @Test
  public void shouldTrimPaths() {
    ProjectDefinition def = ProjectDefinition.create();
    def.setSources("src1", " src2 ", " with whitespace");
    def.setTests("test1", " test2 ", " with whitespace");

    assertThat(def.sources()).containsOnly("src1", "src2", "with whitespace");
    assertThat(def.tests()).containsOnly("test1", "test2", "with whitespace");
  }

  @Test
  public void shouldManageRelationships() {
    ProjectDefinition root = ProjectDefinition.create();
    ProjectDefinition child = ProjectDefinition.create();
    root.addSubProject(child);

    assertThat(root.getSubProjects()).hasSize(1);
    assertThat(child.getSubProjects()).isEmpty();

    assertThat(root.getParent()).isNull();
    assertThat(child.getParent()).isEqualTo(root);
  }

  @Test
  public void shouldResetSourceDirs() {
    ProjectDefinition root = ProjectDefinition.create();
    root.addSources("src", "src2/main");
    assertThat(root.sources()).hasSize(2);

    root.resetSources();
    assertThat(root.sources()).isEmpty();
  }

  @Test
  public void shouldResetTestDirs() {
    ProjectDefinition root = ProjectDefinition.create();
    root.addTests("src", "src2/test");
    assertThat(root.tests()).hasSize(2);

    root.resetTests();
    assertThat(root.tests()).isEmpty();
  }

}
