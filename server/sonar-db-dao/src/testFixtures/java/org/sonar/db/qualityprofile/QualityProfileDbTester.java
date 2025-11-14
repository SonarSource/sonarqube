/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.qualityprofile;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import org.sonar.api.rule.Severity;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.db.qualityprofile.ActiveRuleDto.createFor;

public class QualityProfileDbTester {
  private final Random random = new SecureRandom();
  private final DbClient dbClient;
  private final DbTester db;

  public QualityProfileDbTester(DbTester dbTester) {
    this.dbClient = dbTester.getDbClient();
    this.db = dbTester;
  }

  public Optional<QProfileDto> selectByUuid(String uuid) {
    return Optional.ofNullable(dbClient.qualityProfileDao().selectByUuid(db.getSession(), uuid));
  }

  /**
   * Create a profile with random field values.
   */
  public QProfileDto insert() {
    return insert(c -> {
    });
  }


  /**
   * Create a profile with random field values
   */
  public QProfileDto insert(Consumer<QProfileDto> consumer) {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto();
    consumer.accept(profile);

    dbClient.qualityProfileDao().insert(db.getSession(), profile);
    db.commit();
    return profile;
  }

  public QualityProfileDbTester insert(QProfileDto profile, QProfileDto... others) {
    dbClient.qualityProfileDao().insert(db.getSession(), profile);
    Arrays.stream(others).forEach(p -> dbClient.qualityProfileDao().insert(db.getSession(), p));
    db.commit();
    return this;
  }

  public QualityProfileDbTester associateWithProject(ProjectDto project, QProfileDto profile, QProfileDto... otherProfiles) {
    dbClient.qualityProfileDao().insertProjectProfileAssociation(db.getSession(), project, profile);
    for (QProfileDto p : otherProfiles) {
      dbClient.qualityProfileDao().insertProjectProfileAssociation(db.getSession(), project, p);
    }
    db.commit();
    return this;
  }

  public ActiveRuleDto activateRule(QProfileDto profile, RuleDto rule) {
    return activateRule(profile, rule, ar -> {
    });
  }

  public ActiveRuleDto activateRule(QProfileDto profile, RuleDto rule, Consumer<ActiveRuleDto> consumer) {
    ActiveRuleDto activeRule = createFor(profile, rule)
      .setSeverity(Severity.ALL.get(random.nextInt(Severity.ALL.size())))
      .setImpacts(rule.getDefaultImpactsMap())
      .setPrioritizedRule(random.nextBoolean())
      .setCreatedAt(random.nextLong(Long.MAX_VALUE))
      .setUpdatedAt(random.nextLong(Long.MAX_VALUE));
    consumer.accept(activeRule);
    dbClient.activeRuleDao().insert(db.getSession(), activeRule);
    db.commit();
    return activeRule;
  }

  public QualityProfileDbTester setAsDefault(QProfileDto profile, QProfileDto... others) {
    dbClient.defaultQProfileDao().insertOrUpdate(db.getSession(), DefaultQProfileDto.from(profile));
    for (QProfileDto other : others) {
      dbClient.defaultQProfileDao().insertOrUpdate(db.getSession(), DefaultQProfileDto.from(other));
    }
    db.commit();
    return this;
  }

  public void addUserPermission(QProfileDto profile, UserDto user) {
    checkArgument(!profile.isBuiltIn(), "Built-In profile cannot be used");
    dbClient.qProfileEditUsersDao().insert(db.getSession(), new QProfileEditUsersDto()
        .setUuid(Uuids.createFast())
        .setUserUuid(user.getUuid())
        .setQProfileUuid(profile.getKee()),
      profile.getName(), user.getLogin()
    );
    db.commit();
  }

  public void addGroupPermission(QProfileDto profile, GroupDto group) {
    checkArgument(!profile.isBuiltIn(), "Built-In profile cannot be used");
    dbClient.qProfileEditGroupsDao().insert(db.getSession(), new QProfileEditGroupsDto()
        .setUuid(Uuids.createFast())
        .setGroupUuid(group.getUuid())
        .setQProfileUuid(profile.getKee()),
      profile.getName(), group.getName());
    db.commit();
  }
}
