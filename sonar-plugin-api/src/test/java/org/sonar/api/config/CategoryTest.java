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
package org.sonar.api.config;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CategoryTest {

  @Test
  public void category_key_is_case_insentive() {
    assertThat(new Category("Licenses")).isEqualTo(new Category("licenses"));

    // Just to raise coverage
    assertThat(new Category("Licenses")).isNotEqualTo("Licenses");
  }

  @Test
  public void should_preserve_original_key() {
    assertThat(new Category("Licenses").originalKey()).isEqualTo("Licenses");
  }

  @Test
  public void should_normalize_key() {
    assertThat(new Category("Licenses").key()).isEqualTo("licenses");
  }

  @Test
  public void should_use_original_key() {
    assertThat(new Category("Licenses").toString()).isEqualTo("Licenses");
  }

}
