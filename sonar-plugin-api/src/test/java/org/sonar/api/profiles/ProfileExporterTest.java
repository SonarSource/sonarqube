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
package org.sonar.api.profiles;

import org.junit.Test;

import java.io.Writer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ProfileExporterTest {

  @Test
  public void testSupportedRepositories() {
    ProfileExporter exporter = new ProfileExporter("all", "All") {
      @Override
      public void exportProfile(RulesProfile profile, Writer writer) {
      }
    };
    exporter.setSupportedLanguages("java", "php");

    assertThat(exporter.getSupportedLanguages().length, is(2));
    assertThat(exporter.getSupportedLanguages()[0], is("java"));
    assertThat(exporter.getSupportedLanguages()[1], is("php"));
  }

  @Test
  public void supportAllRepositories() {
    ProfileExporter exporter = new ProfileExporter("all", "All") {
      @Override
      public void exportProfile(RulesProfile profile, Writer writer) {
      }
    };

    assertThat(exporter.getSupportedLanguages().length, is(0));

    exporter.setSupportedLanguages();
    assertThat(exporter.getSupportedLanguages().length, is(0));
  }
}
