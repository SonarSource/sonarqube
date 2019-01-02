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
package org.sonar.server.qualityprofile;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.DefaultQProfileDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.sonar.server.qualityprofile.ActiveRuleInheritance.NONE;

/**
 * Synchronize Quality profiles during server startup
 */
@ServerSide
public class RegisterQualityProfiles {

  private static final Logger LOGGER = Loggers.get(RegisterQualityProfiles.class);

  private final BuiltInQProfileRepository builtInQProfileRepository;
  private final DbClient dbClient;
  private final BuiltInQProfileInsert builtInQProfileInsert;
  private final BuiltInQProfileUpdate builtInQProfileUpdate;
  private final BuiltInQualityProfilesUpdateListener builtInQualityProfilesNotification;
  private final System2 system2;

  public RegisterQualityProfiles(BuiltInQProfileRepository builtInQProfileRepository,
    DbClient dbClient, BuiltInQProfileInsert builtInQProfileInsert, BuiltInQProfileUpdate builtInQProfileUpdate,
    BuiltInQualityProfilesUpdateListener builtInQualityProfilesNotification, System2 system2) {
    this.builtInQProfileRepository = builtInQProfileRepository;
    this.dbClient = dbClient;
    this.builtInQProfileInsert = builtInQProfileInsert;
    this.builtInQProfileUpdate = builtInQProfileUpdate;
    this.builtInQualityProfilesNotification = builtInQualityProfilesNotification;
    this.system2 = system2;
  }

  public void start() {
    List<BuiltInQProfile> builtInQProfiles = builtInQProfileRepository.get();
    if (builtInQProfiles.isEmpty()) {
      return;
    }

    Profiler profiler = Profiler.create(Loggers.get(getClass())).startInfo("Register quality profiles");
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
            .collect(MoreCollectors.toList()));
        }
      });
      if (!changedProfiles.isEmpty()) {
        long endDate = system2.now();
        builtInQualityProfilesNotification.onChange(changedProfiles, startDate, endDate);
      }
      ensureBuiltInDefaultQPContainsRules(dbSession);
    }
    profiler.stopDebug();
  }

  private Map<QProfileName, RulesProfileDto> loadPersistedProfiles(DbSession dbSession) {
    return dbClient.qualityProfileDao().selectBuiltInRuleProfiles(dbSession).stream()
      .collect(MoreCollectors.uniqueIndex(rp -> new QProfileName(rp.getLanguage(), rp.getName())));
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
    Profiler profiler = Profiler.createIfDebug(Loggers.get(getClass())).start();
    String newName = profile.getName() + " (outdated copy)";
    LOGGER.info("Rename Quality profiles [{}/{}] to [{}] in {} organizations", profile.getLanguage(), profile.getName(), newName, uuids.size());
    dbClient.qualityProfileDao().renameRulesProfilesAndCommit(dbSession, uuids, newName);
    profiler.stopDebug(format("%d Quality profiles renamed to [%s]", uuids.size(), newName));
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

    dbClient.qualityProfileDao().selectDefaultBuiltInProfilesWithoutActiveRules(dbSession, rulesProfilesByLanguage.keySet())
      .forEach(qp -> {
        RulesProfileDto rulesProfile = rulesProfilesByLanguage.get(qp.getLanguage());
        if (rulesProfile == null) {
          return;
        }

        QProfileDto qualityProfile = dbClient.qualityProfileDao().selectByRuleProfileUuid(dbSession, qp.getOrganizationUuid(), rulesProfile.getKee());
        if (qualityProfile == null) {
          return;
        }

        Set<String> uuids = dbClient.defaultQProfileDao().selectExistingQProfileUuids(dbSession, qp.getOrganizationUuid(), Collections.singleton(qp.getKee()));
        dbClient.defaultQProfileDao().deleteByQProfileUuids(dbSession, uuids);
        dbClient.defaultQProfileDao().insertOrUpdate(dbSession, new DefaultQProfileDto()
          .setQProfileUuid(qualityProfile.getKee())
          .setLanguage(qp.getLanguage())
          .setOrganizationUuid(qp.getOrganizationUuid())
        );

        LOGGER.info("Default built-in quality profile for language [{}] has been updated from [{}] to [{}] since previous default does not have active rules.",
          qp.getLanguage(),
          qp.getName(),
          rulesProfile.getName());
      });

    dbSession.commit();
  }
}
