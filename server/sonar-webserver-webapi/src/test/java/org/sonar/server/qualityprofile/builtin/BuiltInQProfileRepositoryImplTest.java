/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.qualityprofile.builtin;

import org.junit.jupiter.api.Test;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.db.DbClient;
import org.sonar.server.rule.ServerRuleFinder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuiltInQProfileRepositoryImplTest {
  @Test
  void initializationWithoutQualityProfiles() {
    DbClient dbClient = mock(DbClient.class);
    ServerRuleFinder ruleFinder = mock(ServerRuleFinder.class);
    Languages languages = mock(Languages.class);
    Language java = mock(Language.class);
    Language kotlin = mock(Language.class);

    when(languages.all()).thenReturn(new Language[]{ java, kotlin });
    when(java.getKey()).thenReturn("java");
    when(kotlin.getKey()).thenReturn("kotlin");
    
    BuiltInQProfileRepositoryImpl repository = new BuiltInQProfileRepositoryImpl(dbClient, ruleFinder, languages);

    assertThatCode(repository::initialize).hasMessage("The following languages have no built-in quality profiles: java, kotlin");
  }
}
