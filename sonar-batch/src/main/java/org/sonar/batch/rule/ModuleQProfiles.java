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
package org.sonar.batch.rule;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.MessageException;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.Map;

/**
 * Lists the Quality profiles enabled on the current module.
 */
public class ModuleQProfiles implements BatchComponent {

  public static final String SONAR_PROFILE_PROP = "sonar.profile";

  public static class QProfile {
    private final String name, language;
    private final Integer version;
    private final int id;

    public QProfile(QualityProfileDto dto) {
      this.id = dto.getId();
      this.name = dto.getName();
      this.language = dto.getLanguage();
      this.version = dto.getVersion();
    }

    QProfile(int id, String name, String language, Integer version) {
      this.id = id;
      this.name = name;
      this.language = language;
      this.version = version;
    }

    public int id() {
      return id;
    }

    public String name() {
      return name;
    }

    public String language() {
      return language;
    }

    public Integer version() {
      return version;
    }
  }

  private final Map<String, QProfile> byLanguage;

  public ModuleQProfiles(Settings settings, Languages languages, QualityProfileDao dao) {
    ImmutableMap.Builder<String, QProfile> builder = ImmutableMap.builder();
    String defaultName = settings.getString(SONAR_PROFILE_PROP);

    for (Language language : languages.all()) {
      QProfile profile = null;
      if (StringUtils.isNotBlank(defaultName)) {
        profile = loadDefaultQProfile(dao, defaultName, language.getKey());
      }
      if (profile == null) {
        profile = loadQProfile(dao, settings, language.getKey());
      }
      if (profile != null) {
        builder.put(profile.language(), profile);
      }
    }
    byLanguage = builder.build();
  }

  @CheckForNull
  private QProfile loadQProfile(QualityProfileDao dao, Settings settings, String language) {
    String profileName = settings.getString("sonar.profile." + language);
    if (profileName != null) {
      QualityProfileDto dto = dao.selectByNameAndLanguage(profileName, language);
      if (dto == null) {
        throw MessageException.of(String.format("Quality profile not found : '%s' on language '%s'", profileName, language));
      }
      return new QProfile(dto);
    }
    return null;
  }

  @CheckForNull
  private QProfile loadDefaultQProfile(QualityProfileDao dao, String profileName, String language) {
    QualityProfileDto dto = dao.selectByNameAndLanguage(profileName, language);
    if (dto != null) {
      return new QProfile(dto);
    }
    return null;
  }

  public Collection<QProfile> findAll() {
    return byLanguage.values();
  }

  @CheckForNull
  public QProfile findByLanguage(String language) {
    return byLanguage.get(language);
  }
}
