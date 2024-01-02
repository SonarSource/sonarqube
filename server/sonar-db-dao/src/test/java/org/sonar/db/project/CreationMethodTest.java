/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.project;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class CreationMethodTest {

  @Parameterized.Parameters(name = "Category:{0}, Is gui Call:{1}, Expected creation method:{2}")
  public static Iterable<Object[]> testData() {
    return Arrays.asList(new Object[][] {
      {CreationMethod.Category.UNKNOWN, true, CreationMethod.UNKNOWN},
      {CreationMethod.Category.UNKNOWN, false, CreationMethod.UNKNOWN},
      {CreationMethod.Category.LOCAL, true, CreationMethod.LOCAL_BROWSER},
      {CreationMethod.Category.LOCAL, false, CreationMethod.LOCAL_API},
      {CreationMethod.Category.ALM_IMPORT, true, CreationMethod.ALM_IMPORT_BROWSER},
      {CreationMethod.Category.ALM_IMPORT, false, CreationMethod.ALM_IMPORT_API},
      {CreationMethod.Category.SCANNER, true, CreationMethod.UNKNOWN},
      {CreationMethod.Category.SCANNER, false, CreationMethod.SCANNER_API},
    });
  }

  private final CreationMethod.Category category;
  private final boolean isCreatedViaBrowser;
  private final CreationMethod expectedCreationMethod;

  public CreationMethodTest(CreationMethod.Category category, boolean isCreatedViaBrowser, CreationMethod expectedCreationMethod) {
    this.category = category;
    this.isCreatedViaBrowser = isCreatedViaBrowser;
    this.expectedCreationMethod = expectedCreationMethod;
  }

  @Test
  public void getCreationMethod_returnsCorrectCreationMethod() {
    CreationMethod creationMethod = CreationMethod.getCreationMethod(category, isCreatedViaBrowser);
    assertThat(creationMethod).isEqualTo(expectedCreationMethod);
  }

}
