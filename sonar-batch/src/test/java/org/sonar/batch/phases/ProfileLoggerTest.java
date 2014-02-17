/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.phases;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.sonar.api.batch.ModuleLanguages;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Language;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.rule.ModuleQProfiles;
import org.sonar.batch.rule.ModuleQProfiles.QProfile;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProfileLoggerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ModuleLanguages languages;
  private ModuleQProfiles profiles;
  private Settings settings;

  @Before
  public void prepare() {
    languages = mock(ModuleLanguages.class);
    List<Language> languageList = Lists.newArrayList();
    languageList.add(new AbstractLanguage("java", "Java") {

      @Override
      public String[] getFileSuffixes() {
        return null;
      }
    });
    languageList.add(new AbstractLanguage("cobol", "Cobol") {

      @Override
      public String[] getFileSuffixes() {
        return null;
      }
    });
    when(languages.languages()).thenReturn(languageList);

    profiles = mock(ModuleQProfiles.class);
    QProfile javaProfile = mock(QProfile.class);
    when(javaProfile.name()).thenReturn("My Java profile");
    when(profiles.findByLanguage("java")).thenReturn(javaProfile);
    QProfile cobolProfile = mock(QProfile.class);
    when(cobolProfile.name()).thenReturn("My Cobol profile");
    when(profiles.findByLanguage("cobol")).thenReturn(cobolProfile);

    settings = new Settings();
  }

  @Test
  public void should_log_all_used_profiles() {

    ProfileLogger profileLogger = new ProfileLogger(settings, languages, profiles);
    Logger logger = mock(Logger.class);
    profileLogger.execute(logger);

    verify(logger).info("Quality profile for {}: {}", "Java", "My Java profile");
    verify(logger).info("Quality profile for {}: {}", "Cobol", "My Cobol profile");
  }

  @Test
  public void should_fail_if_default_profile_not_used() {
    settings.setProperty("sonar.profile", "Unknow");

    ProfileLogger profileLogger = new ProfileLogger(settings, languages, profiles);

    thrown.expect(SonarException.class);
    thrown.expectMessage("sonar.profile was set to 'Unknow' but didn't match any profile for any language. Please check your configuration.");

    profileLogger.execute();

  }

  @Test
  public void should_not_fail_if_default_profile_used_at_least_once() {
    settings.setProperty("sonar.profile", "My Java profile");

    ProfileLogger profileLogger = new ProfileLogger(settings, languages, profiles);

    profileLogger.execute();
  }
}
