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
package org.sonar.server.qualityprofile;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.core.util.logs.Profiler;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.DefaultQProfileDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfile;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfileInsert;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfileRepository;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfileUpdate;
import org.sonar.server.qualityprofile.builtin.BuiltInQualityProfilesUpdateListener;
import org.sonar.server.qualityprofile.builtin.QProfileName;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.sonar.server.qualityprofile.ActiveRuleInheritance.NONE;

/**
 * Synchronize Quality profiles during server startup
 */
@ServerSide
public class RegisterQualityProfiles implements Startable {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterQualityProfiles.class);

  private final BuiltInQProfileRepository builtInQProfileRepository;
  private final DbClient dbClient;
  private final BuiltInQProfileInsert builtInQProfileInsert;
  private final BuiltInQProfileUpdate builtInQProfileUpdate;
  private final BuiltInQualityProfilesUpdateListener builtInQualityProfilesNotification;
  private final System2 system2;
  private final Languages languages;

  public RegisterQualityProfiles(BuiltInQProfileRepository builtInQProfileRepository,
    DbClient dbClient, BuiltInQProfileInsert builtInQProfileInsert, BuiltInQProfileUpdate builtInQProfileUpdate,
    BuiltInQualityProfilesUpdateListener builtInQualityProfilesNotification, System2 system2, Languages languages) {
    this.builtInQProfileRepository = builtInQProfileRepository;
    this.dbClient = dbClient;
    this.builtInQProfileInsert = builtInQProfileInsert;
    this.builtInQProfileUpdate = builtInQProfileUpdate;
    this.builtInQualityProfilesNotification = builtInQualityProfilesNotification;
    this.system2 = system2;
    this.languages = languages;
  }

  @Override
  public void start() {
    List<BuiltInQProfile> builtInQProfiles = builtInQProfileRepository.get();
    if (builtInQProfiles.isEmpty()) {
      return;
    }

    Profiler profiler = Profiler.create(LoggerFactory.getLogger(getClass())).startInfo("Register quality profiles");
    try (DbSession dbSession = dbClient.openSession(false);
      DbSession batchDbSession = dbClient.openSession(true)) {
      long startDate = system2.now();

      Map<QProfileName, RulesProfileDto> persistedRuleProfiles = loadPersistedProfiles(dbSession);

      Multimap<QProfileName, ActiveRuleChange> changedProfiles = ArrayListMultimap.create();
      builtInQProfiles.forEach(builtIn -> {
        RulesProfileDto ruleProfile = persistedRuleProfiles.get(builtIn.getQProfileName());
        if (ruleProfile == null) {
          create(dbSession, batchDbSession, builtIn);
        } else {
          List<ActiveRuleChange> changes = update(dbSession, builtIn, ruleProfile);
          changedProfiles.putAll(builtIn.getQProfileName(), changes.stream()
            .filter(change -> {
              String inheritance = change.getActiveRule().getInheritance();
              return inheritance == null || NONE.name().equals(inheritance);
            })
            .toList());
        }
      });
      if (!changedProfiles.isEmpty()) {
        long endDate = system2.now();
        builtInQualityProfilesNotification.onChange(changedProfiles, startDate, endDate);
      }
      ensureBuiltInDefaultQPContainsRules(dbSession);
      unsetBuiltInFlagAndRenameQPWhenPluginUninstalled(dbSession);
      /* TODO */
      // ensureBuiltInAreDefaultQPWhenNoRules(dbSession);

      dbSession.commit();
    }
    profiler.stopDebug();
  }

  @Override
  public void stop() {
    // nothing to do
  }

  private Map<QProfileName, RulesProfileDto> loadPersistedProfiles(DbSession dbSession) {
    return dbClient.qualityProfileDao().selectBuiltInRuleProfiles(dbSession).stream()
      .collect(toMap(rp -> new QProfileName(rp.getLanguage(), rp.getName()), Function.identity()));
  }

  private void create(DbSession dbSession, DbSession batchDbSession, BuiltInQProfile builtIn) {
    LOGGER.info("Register profile {}", builtIn.getQProfileName());

    renameOutdatedProfiles(dbSession, builtIn);

    builtInQProfileInsert.create(dbSession, batchDbSession, builtIn);
  }

  private List<ActiveRuleChange> update(DbSession dbSession, BuiltInQProfile definition, RulesProfileDto dbProfile) {
    LOGGER.info("Update profile {}", definition.getQProfileName());

    return builtInQProfileUpdate.update(dbSession, definition, dbProfile);
  }

  /**
   * The Quality profiles created by users should be renamed when they have the same name
   * as the built-in profile to be persisted.
   * <p>
   * When upgrading from < 6.5 , all existing profiles are considered as "custom" (created
   * by users) because the concept of built-in profile is not persisted. The "Sonar way" profiles
   * are renamed to "Sonar way (outdated copy) in order to avoid conflicts with the new
   * built-in profile "Sonar way", which has probably different configuration.
   */
  private void renameOutdatedProfiles(DbSession dbSession, BuiltInQProfile profile) {
    Collection<String> uuids = dbClient.qualityProfileDao().selectUuidsOfCustomRulesProfiles(dbSession, profile.getLanguage(), profile.getName());
    if (uuids.isEmpty()) {
      return;
    }
    Profiler profiler = Profiler.createIfDebug(LoggerFactory.getLogger(getClass())).start();
    String newName = profile.getName() + " (outdated copy)";
    LOGGER.info("Rename Quality profiles [{}/{}] to [{}]", profile.getLanguage(), profile.getName(), newName);
    dbClient.qualityProfileDao().renameRulesProfilesAndCommit(dbSession, uuids, newName);
    profiler.stopDebug(format("%d Quality profiles renamed to [%s]", uuids.size(), newName));
  }

  /**
   * This method ensure that after plugin removal, we don't end-up in a situation where a custom default quality profile is set,
   * and cannot be deleted because builtIn quality profile cannot be set to default.
   *
   * @see <a href="https://jira.sonarsource.com/browse/SONAR-19478">SONAR-19478</a>
   */
  private void ensureBuiltInAreDefaultQPWhenNoRules(DbSession dbSession) {
    Set<String> activeLanguages = Arrays.stream(languages.all()).map(Language::getKey).collect(toSet());
    Map<String, RulesProfileDto> builtInQProfileByLanguage = dbClient.qualityProfileDao().selectBuiltInRuleProfiles(dbSession).stream()
      .collect(toMap(RulesProfileDto::getLanguage, Function.identity(), (oldValue, newValue) -> oldValue));
    List<QProfileDto> defaultProfileWithNoRules = dbClient.qualityProfileDao().selectDefaultProfilesWithoutActiveRules(dbSession, activeLanguages, false);

    for (QProfileDto defaultProfileWithNoRule : defaultProfileWithNoRules) {
      long rulesCountByLanguage = dbClient.ruleDao().countByLanguage(dbSession, defaultProfileWithNoRule.getLanguage());
      RulesProfileDto builtInQProfile = builtInQProfileByLanguage.get(defaultProfileWithNoRule.getLanguage());
      if (builtInQProfile != null && rulesCountByLanguage == 0) {
//        QProfileDto builtInQualityProfile = dbClient.qualityProfileDao().selectByRuleProfileUuid(dbSession, builtInQProfile.getUuid());
//        if (builtInQualityProfile != null) {
//          reassignDefaultQualityProfile(dbSession, defaultProfileWithNoRule, builtInQualityProfile);
//        }
      }
    }
  }

  /**
   * This method ensure that if a default built-in quality profile does not have any active rules but another built-in one for the same language
   * does have active rules, the last one will be the default one.
   *
   * @see <a href="https://jira.sonarsource.com/browse/SONAR-10363">SONAR-10363</a>
   */
  private void ensureBuiltInDefaultQPContainsRules(DbSession dbSession) {
    Map<String, RulesProfileDto> rulesProfilesByLanguage = dbClient.qualityProfileDao().selectBuiltInRuleProfilesWithActiveRules(dbSession).stream()
      .collect(toMap(RulesProfileDto::getLanguage, Function.identity(), (oldValue, newValue) -> oldValue));

    dbClient.qualityProfileDao().selectDefaultProfilesWithoutActiveRules(dbSession, rulesProfilesByLanguage.keySet(), true)
      .forEach(qp -> {
        RulesProfileDto rulesProfile = rulesProfilesByLanguage.get(qp.getLanguage());
        if (rulesProfile == null) {
          return;
        }

        QProfileDto qualityProfile = dbClient.qualityProfileDao().selectByRuleProfileUuid(dbSession, qp.getOrganizationUuid(), rulesProfile.getUuid());
        if (qualityProfile == null) {
          return;
        }

        reassignDefaultQualityProfile(dbSession, qp, qualityProfile);

        LOGGER.info("Default built-in quality profile for language [{}] has been updated from [{}] to [{}] since previous default does not have active rules.",
          qp.getLanguage(),
          qp.getName(),
          rulesProfile.getName());
      });
  }

  private void reassignDefaultQualityProfile(DbSession dbSession, QProfileDto currentDefaultQualityProfile, QProfileDto newDefaultQualityProfile) {
    /* TODO */
    Set<String> uuids = dbClient.defaultQProfileDao().selectExistingQProfileUuids(dbSession, currentDefaultQualityProfile.getOrganizationUuid(), Collections.singleton(currentDefaultQualityProfile.getKee()));
    dbClient.defaultQProfileDao().deleteByQProfileUuids(dbSession, uuids);
    dbClient.defaultQProfileDao().insertOrUpdate(dbSession, new DefaultQProfileDto()
      .setOrganizationUuid(currentDefaultQualityProfile.getOrganizationUuid())
      .setQProfileUuid(newDefaultQualityProfile.getKee())
      .setLanguage(currentDefaultQualityProfile.getLanguage()));
  }

  public void unsetBuiltInFlagAndRenameQPWhenPluginUninstalled(DbSession dbSession) {
    Set<QProfileName> pluginsBuiltInQProfiles = builtInQProfileRepository.get()
      .stream()
      .map(BuiltInQProfile::getQProfileName)
      .collect(toSet());

    Set<String> qualityProfileLanguages = pluginsBuiltInQProfiles
      .stream()
      .map(QProfileName::getLanguage)
      .collect(toSet());

    dbClient.qualityProfileDao().selectBuiltInRuleProfiles(dbSession)
      .forEach(qProfileDto -> {
        var dbProfileName = QProfileName.createFor(qProfileDto.getLanguage(), qProfileDto.getName());

        // Built-in Quality Profile can be a leftover from plugin which has been removed
        // Rename Quality Profile and unset built-in flag allowing Quality Profile for existing languages to be removed
        // Quality Profiles for languages not existing anymore are marked as 'REMOVED' and won't be seen in UI
        if (!pluginsBuiltInQProfiles.contains(dbProfileName) && qualityProfileLanguages.contains(qProfileDto.getLanguage())) {
          String oldName = qProfileDto.getName();
          String newName = generateNewProfileName(qProfileDto);
          qProfileDto.setName(newName);
          qProfileDto.setIsBuiltIn(false);
          qProfileDto.setIsDefault(false);
          dbClient.qualityProfileDao().update(dbSession, qProfileDto);

          LOGGER.info("Quality profile [{}] for language [{}] is no longer built-in and has been renamed to [{}] "
            + "since it does not have any active rules.",
            oldName,
            dbProfileName.getLanguage(),
            newName);
        }
      });
  }

  /**
   * Abbreviate Quality Profile name if it will be too long with prefix and append suffix
   */
  private String generateNewProfileName(RulesProfileDto qProfileDto) {
    var shortName = StringUtils.abbreviate(qProfileDto.getName(), 40);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd yyyy 'at' hh:mm a")
      .withLocale(Locale.getDefault())
      .withZone(ZoneId.systemDefault());
    var now = formatter.format(Instant.ofEpochMilli(system2.now()));
    String suffix = " (outdated copy since " + now + ")";
    return shortName + suffix;
  }
}
