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
package org.sonar.api.profiles;

import org.junit.Test;
import org.sonar.api.utils.ValidationMessages;

import java.io.Reader;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ProfileImporterTest {

  @Test
  public void testSupportedLanguages() {
    ProfileImporter inmporter = new ProfileImporter("all", "All") {
      @Override
      public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
        return null;
      }
    };
    inmporter.setSupportedLanguages("java", "php");

    assertThat(inmporter.getSupportedLanguages().length, is(2));
    assertThat(inmporter.getSupportedLanguages()[0], is("java"));
    assertThat(inmporter.getSupportedLanguages()[1], is("php"));
  }

  @Test
  public void supportAllLanguages() {
    ProfileImporter importer = new ProfileImporter("all", "All") {
      @Override
      public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
        return null;
      }
    };

    assertThat(importer.getSupportedLanguages().length, is(0));

    importer.setSupportedLanguages();
    assertThat(importer.getSupportedLanguages().length, is(0));
  }
}
