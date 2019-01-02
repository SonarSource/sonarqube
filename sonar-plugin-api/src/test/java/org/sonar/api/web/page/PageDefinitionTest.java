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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Used for the documentation
 */
public class PageDefinitionTest {
  @Test
  public void test_page_definition() {
    PageDefinition underTest = context -> context.addPage(Page.builder("my_plugin/my_page").setName("My Page").build());
    Context context = new Context();

    underTest.define(context);

    assertThat(context.getPages()).extracting(Page::getKey).contains("my_plugin/my_page");
  }

}
