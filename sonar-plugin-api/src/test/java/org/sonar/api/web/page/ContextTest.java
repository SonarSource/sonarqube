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
package org.sonar.api.web.page;

import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ContextTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Context underTest = new Context();

  private Page page = Page.builder("governance/project_export").setName("Project Export").build();

  @Test
  public void no_pages_with_the_same_path() {
    underTest.addPage(page);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Page 'Project Export' cannot be loaded. Another page with key 'governance/project_export' already exists.");

    underTest.addPage(page);
  }

  @Test
  public void ordered_by_name() {
    underTest
      .addPage(Page.builder("K1").setName("N2").build())
      .addPage(Page.builder("K2").setName("N3").build())
      .addPage(Page.builder("K3").setName("N1").build());

    Collection<Page> result = underTest.getPages();

    assertThat(result).extracting(Page::getKey, Page::getName)
      .containsOnly(
        tuple("K3", "N1"),
        tuple("K1", "N2"),
        tuple("K2", "N3"));
  }

  @Test
  public void empty_pages_by_default() {
    Collection<Page> result = underTest.getPages();

    assertThat(result).isEmpty();
  }

}
