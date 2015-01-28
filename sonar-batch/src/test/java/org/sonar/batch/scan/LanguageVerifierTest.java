/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.scan;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.repository.language.DefaultLanguagesRepository;
import org.sonar.batch.repository.language.LanguagesRepository;

import static org.assertj.core.api.Assertions.assertThat;

public class LanguageVerifierTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private Settings settings = new Settings();
  private LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(Java.INSTANCE));
  private DefaultFileSystem fs;

  @Before
  public void prepare() throws Exception {
    fs = new DefaultFileSystem(temp.newFolder().toPath());
  }

  @Test
  public void language_is_not_set() throws Exception {
    LanguageVerifier verifier = new LanguageVerifier(settings, languages, fs);
    verifier.start();

    // no failure and no language is forced
    assertThat(fs.languages()).isEmpty();

    verifier.stop();
  }

  @Test
  public void language_is_valid() throws Exception {
    settings.setProperty("sonar.language", "java");

    LanguageVerifier verifier = new LanguageVerifier(settings, languages, fs);
    verifier.start();

    // no failure and language is hardly registered
    assertThat(fs.languages()).contains("java");

    verifier.stop();
  }

  @Test
  public void language_is_not_valid() throws Exception {
    thrown.expect(MessageException.class);
    thrown.expectMessage("You must install a plugin that supports the language 'php'");

    settings.setProperty("sonar.language", "php");
    LanguageVerifier verifier = new LanguageVerifier(settings, languages, fs);
    verifier.start();
  }
}
