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
package org.sonar.batch.rule;

import org.sonar.batch.languages.Language;

import org.sonar.api.batch.rules.QProfile;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.languages.LanguagesReferential;
import org.sonar.batch.rules.QProfilesReferential;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.Map;

/**
 * Lists the Quality profiles enabled on the current module.
 */
public class ModuleQProfiles implements BatchComponent {

  public static final String SONAR_PROFILE_PROP = "sonar.profile";

  private final Map<String, QProfile> byLanguage;

  public ModuleQProfiles(Settings settings, LanguagesReferential languages, QProfilesReferential qProfileRef) {
    ImmutableMap.Builder<String, QProfile> builder = ImmutableMap.builder();
    String defaultName = settings.getString(SONAR_PROFILE_PROP);

    for (Language language : languages.all()) {
      QProfile profile = null;
      if (StringUtils.isNotBlank(defaultName)) {
        profile = loadDefaultQProfile(qProfileRef, defaultName, language.key());
      }
      if (profile == null) {
        profile = loadQProfile(qProfileRef, settings, language.key());
      }
      if (profile != null) {
        builder.put(profile.language(), profile);
      }
    }
    byLanguage = builder.build();
  }

  @CheckForNull
  private QProfile loadQProfile(QProfilesReferential qProfileRef, Settings settings, String language) {
    String profileName = settings.getString("sonar.profile." + language);
    if (profileName != null) {
      QProfile dto = qProfileRef.get(language, profileName);
      if (dto == null) {
        throw MessageException.of(String.format("Quality profile not found : '%s' on language '%s'", profileName, language));
      }
      return dto;
    }
    return null;
  }

  @CheckForNull
  private QProfile loadDefaultQProfile(QProfilesReferential qProfileRef, String profileName, String language) {
    return qProfileRef.get(language, profileName);
  }

  public Collection<QProfile> findAll() {
    return byLanguage.values();
  }

  @CheckForNull
  public QProfile findByLanguage(String language) {
    return byLanguage.get(language);
  }
}
