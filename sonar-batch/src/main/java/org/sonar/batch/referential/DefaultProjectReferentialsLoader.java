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
package org.sonar.batch.referential;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.protocol.input.ActiveRule;
import org.sonar.batch.protocol.input.ProjectReferentials;
import org.sonar.batch.protocol.input.QProfile;
import org.sonar.batch.rule.ModuleQProfiles;
import org.sonar.core.UtcDateUtils;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;

import javax.annotation.CheckForNull;

/**
 * TODO This is currently implemented by accessing DB but should be replaced by WS call
 */
public class DefaultProjectReferentialsLoader implements ProjectReferentialsLoader {

  private static final String ENABLED = "enabled";

  private final QualityProfileDao qualityProfileDao;
  private final ActiveRuleDao activeRuleDao;
  private final RuleFinder ruleFinder;

  public DefaultProjectReferentialsLoader(QualityProfileDao qualityProfileDao,
    ActiveRuleDao activeRuleDao, RuleFinder ruleFinder) {
    this.qualityProfileDao = qualityProfileDao;
    this.activeRuleDao = activeRuleDao;
    this.ruleFinder = ruleFinder;
  }

  @Override
  public ProjectReferentials load(ProjectReactor reactor, Settings settings, Languages languages) {
    ProjectReferentials ref = new ProjectReferentials();

    String defaultName = settings.getString(ModuleQProfiles.SONAR_PROFILE_PROP);

    for (Language language : languages.all()) {
      org.sonar.batch.protocol.input.QProfile profile = null;
      if (StringUtils.isNotBlank(defaultName)) {
        profile = loadDefaultQProfile(defaultName, language.getKey());
      }
      if (profile == null) {
        profile = loadQProfile(settings, language.getKey());
      }
      if (profile != null) {
        ref.addQProfile(profile);
      }
    }

    for (QProfile qProfile : ref.qProfiles()) {
      ListMultimap<Integer, ActiveRuleParamDto> paramDtosByActiveRuleId = ArrayListMultimap.create();
      for (ActiveRuleParamDto dto : activeRuleDao.selectParamsByProfileKey(qProfile.key())) {
        paramDtosByActiveRuleId.put(dto.getActiveRuleId(), dto);
      }

      for (ActiveRuleDto activeDto : activeRuleDao.selectByProfileKey(qProfile.key())) {
        Rule rule = ruleFinder.findById(activeDto.getRuleId());
        if (rule != null) {
          String internalKey;
          Rule template = rule.getTemplate();
          if (template != null) {
            internalKey = template.getConfigKey();
          } else {
            internalKey = rule.getConfigKey();
          }
          ActiveRule activeRule = new ActiveRule(rule.ruleKey().repository(), rule.ruleKey().rule(), rule.getName(), activeDto.getSeverityString(), internalKey, rule.getLanguage());

          // load parameter values
          for (ActiveRuleParamDto paramDto : paramDtosByActiveRuleId.get(activeDto.getId())) {
            activeRule.params().put(paramDto.getKey(), paramDto.getValue());
          }

          // load default values
          for (RuleParam param : rule.getParams()) {
            if (!activeRule.params().containsKey(param.getKey())) {
              activeRule.params().put(param.getKey(), param.getDefaultValue());
            }
          }

          ref.addActiveRule(activeRule);
        }
      }
    }

    return ref;
  }

  @CheckForNull
  private QProfile loadQProfile(Settings settings, String language) {
    String profileName = settings.getString("sonar.profile." + language);
    if (profileName != null) {
      QProfile dto = get(language, profileName);
      if (dto == null) {
        throw MessageException.of(String.format("Quality profile not found : '%s' on language '%s'", profileName, language));
      }
      return dto;
    }
    return null;
  }

  @CheckForNull
  private QProfile loadDefaultQProfile(String profileName, String language) {
    return get(language, profileName);
  }

  public QProfile get(String language, String name) {
    QualityProfileDto dto = qualityProfileDao.getByNameAndLanguage(name, language);
    if (dto == null) {
      return null;
    }
    return new org.sonar.batch.protocol.input.QProfile(dto.getKey(), dto.getName(), dto.getLanguage(), UtcDateUtils.parseDateTime(dto.getRulesUpdatedAt()));
  }
}
