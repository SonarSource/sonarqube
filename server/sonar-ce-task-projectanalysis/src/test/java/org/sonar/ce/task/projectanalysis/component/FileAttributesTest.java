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
package org.sonar.ce.task.projectanalysis.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class FileAttributesTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_production_file() {
    FileAttributes underTest = new FileAttributes(true, "java", 10);

    assertThat(underTest.isUnitTest()).isTrue();
    assertThat(underTest.getLanguageKey()).isEqualTo("java");
    assertThat(underTest.getLines()).isEqualTo(10);
  }

  @Test
  public void create_unit_test() {
    FileAttributes underTest = new FileAttributes(true, "java", 10);

    assertThat(underTest.isUnitTest()).isTrue();
    assertThat(underTest.getLanguageKey()).isEqualTo("java");
    assertThat(underTest.getLines()).isEqualTo(10);
  }

  @Test
  public void create_without_language() {
    FileAttributes underTest = new FileAttributes(true, null, 10);

    assertThat(underTest.isUnitTest()).isTrue();
    assertThat(underTest.getLanguageKey()).isNull();
    assertThat(underTest.getLines()).isEqualTo(10);
  }

  @Test
  public void fail_with_IAE_when_lines_is_0() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Number of lines must be greater than zero");
    new FileAttributes(true, "java", 0);
  }

  @Test
  public void fail_with_IAE_when_lines_is_less_than_0() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Number of lines must be greater than zero");
    new FileAttributes(true, "java", -10);
  }

  @Test
  public void test_toString() {
    assertThat(new FileAttributes(true, "java", 10).toString()).isEqualTo("FileAttributes{languageKey='java', unitTest=true, lines=10}");
    assertThat(new FileAttributes(false, null, 1).toString()).isEqualTo("FileAttributes{languageKey='null', unitTest=false, lines=1}");
  }
}
