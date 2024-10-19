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

import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.sonar.db.project.CreationMethod.Category;

import static org.assertj.core.api.Assertions.assertThat;

class CreationMethodTest {

  static class CreationMethodProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(Arguments.of(Category.UNKNOWN, true, CreationMethod.UNKNOWN),
        Arguments.of(Category.UNKNOWN, false, CreationMethod.UNKNOWN),
        Arguments.of(Category.LOCAL, true, CreationMethod.LOCAL_BROWSER),
        Arguments.of(Category.LOCAL, false, CreationMethod.LOCAL_API),
        Arguments.of(Category.ALM_IMPORT, true, CreationMethod.ALM_IMPORT_BROWSER),
        Arguments.of(Category.ALM_IMPORT, false, CreationMethod.ALM_IMPORT_API),
        Arguments.of(Category.ALM_IMPORT_MONOREPO, true, CreationMethod.ALM_IMPORT_MONOREPO_BROWSER),
        Arguments.of(Category.ALM_IMPORT_MONOREPO, false, CreationMethod.ALM_IMPORT_MONOREPO_API),
        Arguments.of(Category.SCANNER, true, CreationMethod.UNKNOWN),
        Arguments.of(Category.SCANNER, false, CreationMethod.SCANNER_API));
    }
  }

  @ParameterizedTest()
  @ArgumentsSource(CreationMethodProvider.class)
  void getCreationMethod_returnsCorrectCreationMethod(Category category, boolean isCreatedViaBrowser,
    CreationMethod expectedCreationMethod) {
    CreationMethod creationMethod = CreationMethod.getCreationMethod(category, isCreatedViaBrowser);
    assertThat(creationMethod).isEqualTo(expectedCreationMethod);
  }

}
