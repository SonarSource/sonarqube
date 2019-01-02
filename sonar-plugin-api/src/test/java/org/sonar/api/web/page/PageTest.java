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
package org.sonar.api.web.page;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.page.Page.Qualifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.page.Page.Qualifier.APP;
import static org.sonar.api.web.page.Page.Qualifier.MODULE;
import static org.sonar.api.web.page.Page.Qualifier.PROJECT;
import static org.sonar.api.web.page.Page.Qualifier.SUB_VIEW;
import static org.sonar.api.web.page.Page.Qualifier.VIEW;
import static org.sonar.api.web.page.Page.Scope.COMPONENT;
import static org.sonar.api.web.page.Page.Scope.GLOBAL;

public class PageTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Page.Builder underTest = Page.builder("governance/project_dump").setName("Project Dump");

  @Test
  public void full_test() {
    Page result = underTest
      .setComponentQualifiers(PROJECT, MODULE)
      .setScope(COMPONENT)
      .setAdmin(true)
      .build();

    assertThat(result.getKey()).isEqualTo("governance/project_dump");
    assertThat(result.getPluginKey()).isEqualTo("governance");
    assertThat(result.getName()).isEqualTo("Project Dump");
    assertThat(result.getComponentQualifiers()).containsOnly(PROJECT, MODULE);
    assertThat(result.getScope()).isEqualTo(COMPONENT);
    assertThat(result.isAdmin()).isTrue();
  }

  @Test
  public void qualifiers_map_to_key() {
    assertThat(Qualifier.PROJECT.getKey()).isEqualTo(org.sonar.api.resources.Qualifiers.PROJECT);
    assertThat(Qualifier.MODULE.getKey()).isEqualTo(org.sonar.api.resources.Qualifiers.MODULE);
    assertThat(Qualifier.VIEW.getKey()).isEqualTo(org.sonar.api.resources.Qualifiers.VIEW);
    assertThat(Qualifier.APP.getKey()).isEqualTo(org.sonar.api.resources.Qualifiers.APP);
    assertThat(Qualifier.SUB_VIEW.getKey()).isEqualTo(org.sonar.api.resources.Qualifiers.SUBVIEW);
  }

  @Test
  public void authorized_qualifiers() {
    Qualifier[] qualifiers = Qualifier.values();

    assertThat(qualifiers).containsExactlyInAnyOrder(PROJECT, MODULE, VIEW, SUB_VIEW, APP);
  }

  @Test
  public void default_values() {
    Page result = underTest.build();

    assertThat(result.getComponentQualifiers()).isEmpty();
    assertThat(result.getScope()).isEqualTo(GLOBAL);
    assertThat(result.isAdmin()).isFalse();
  }

  @Test
  public void all_qualifiers_when_component_page() {
    Page result = underTest.setScope(COMPONENT).build();

    assertThat(result.getComponentQualifiers()).containsOnly(Qualifier.values());
  }

  @Test
  public void qualifiers_from_key() {
    assertThat(Qualifier.fromKey(Qualifiers.PROJECT)).isEqualTo(Qualifier.PROJECT);
    assertThat(Qualifier.fromKey("42")).isNull();
  }

  @Test
  public void fail_if_null_qualifiers() {
    expectedException.expect(NullPointerException.class);

    underTest.setComponentQualifiers((Qualifier[])null).build();
  }

  @Test
  public void fail_if_a_page_has_a_null_key() {
    expectedException.expect(NullPointerException.class);

    Page.builder(null).setName("Say my name").build();
  }

  @Test
  public void fail_if_a_page_has_an_empty_key() {
    expectedException.expect(IllegalArgumentException.class);

    Page.builder("").setName("Say my name").build();
  }

  @Test
  public void fail_if_a_page_has_a_null_name() {
    expectedException.expect(IllegalArgumentException.class);

    Page.builder("governance/project_dump").build();
  }

  @Test
  public void fail_if_a_page_has_an_empty_name() {
    expectedException.expect(IllegalArgumentException.class);

    Page.builder("governance/project_dump").setName("").build();
  }

  @Test
  public void fail_if_qualifiers_without_scope() {
    expectedException.expect(IllegalArgumentException.class);

    underTest.setComponentQualifiers(PROJECT).build();
  }

  @Test
  public void fail_if_key_does_not_contain_a_slash() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Page key [project_dump] is not valid. It must contain a single slash, for example my_plugin/my_page.");

    Page.builder("project_dump").setName("Project Dump").build();
  }

  @Test
  public void fail_if_key_contains_more_than_one_slash() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Page key [governance/project/dump] is not valid. It must contain a single slash, for example my_plugin/my_page.");

    Page.builder("governance/project/dump").setName("Project Dump").build();
  }
}
