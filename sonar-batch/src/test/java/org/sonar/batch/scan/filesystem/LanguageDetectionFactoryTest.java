/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.scan.filesystem;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Languages;
import org.sonar.batch.repository.language.DefaultLanguagesRepository;
import org.sonar.batch.repository.language.LanguagesRepository;

import static org.assertj.core.api.Assertions.assertThat;

public class LanguageDetectionFactoryTest {
  @Test
  public void testCreate() throws Exception {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(Java.INSTANCE));
    LanguageDetectionFactory factory = new LanguageDetectionFactory(new Settings(), languages);
    LanguageDetection languageDetection = factory.create();
    assertThat(languageDetection).isNotNull();
    assertThat(languageDetection.patternsByLanguage()).hasSize(1);
    assertThat(languageDetection.patternsByLanguage().containsKey("java")).isTrue();
  }
}
