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
package org.sonar.db.qualityprofile;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import org.sonar.api.rule.Severity;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.sonar.db.qualityprofile.ActiveRuleDto.createFor;

public class QualityProfileDbTester {
  private final DbClient dbClient;
  private final DbSession dbSession;

  public QualityProfileDbTester(DbTester dbTester) {
    this.dbClient = dbTester.getDbClient();
    this.dbSession = dbTester.getSession();
  }

  public Optional<QProfileDto> selectByUuid(String uuid) {
    return Optional.ofNullable(dbClient.qualityProfileDao().selectByUuid(dbSession, uuid));
  }

  /**
   * Create a profile with random field values on the specified organization.
   */
  public QProfileDto insert(OrganizationDto organization) {
    return insert(organization, c -> {
    });
  }

  /**
   * Create a profile with random field values on the specified organization.
   */
  public QProfileDto insert(OrganizationDto organization, Consumer<QProfileDto> consumer) {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid());
    consumer.accept(profile);

    dbClient.qualityProfileDao().insert(dbSession, profile);
    dbSession.commit();
    return profile;
  }

  public QualityProfileDbTester insert(QProfileDto profile, QProfileDto... others) {
    dbClient.qualityProfileDao().insert(dbSession, profile);
    Arrays.stream(others).forEach(p -> dbClient.qualityProfileDao().insert(dbSession, p));
    dbSession.commit();
    return this;
  }

  public QualityProfileDbTester associateWithProject(ComponentDto project, QProfileDto profile, QProfileDto... otherProfiles) {
    dbClient.qualityProfileDao().insertProjectProfileAssociation(dbSession, project, profile);
    for (QProfileDto p : otherProfiles) {
      dbClient.qualityProfileDao().insertProjectProfileAssociation(dbSession, project, p);
    }
    dbSession.commit();
    return this;
  }

  public ActiveRuleDto activateRule(QProfileDto profile, RuleDefinitionDto rule) {
    return activateRule(profile, rule, ar -> {
    });
  }

  public ActiveRuleDto activateRule(QProfileDto profile, RuleDefinitionDto rule, Consumer<ActiveRuleDto> consumer) {
    ActiveRuleDto activeRule = createFor(profile, rule)
      .setSeverity(Severity.ALL.get(nextInt(Severity.ALL.size())))
      .setCreatedAt(nextLong())
      .setUpdatedAt(nextLong());
    consumer.accept(activeRule);
    dbClient.activeRuleDao().insert(dbSession, activeRule);
    dbSession.commit();
    return activeRule;
  }

  public QualityProfileDbTester setAsDefault(QProfileDto profile, QProfileDto... others) {
    dbClient.defaultQProfileDao().insertOrUpdate(dbSession, DefaultQProfileDto.from(profile));
    for (QProfileDto other : others) {
      dbClient.defaultQProfileDao().insertOrUpdate(dbSession, DefaultQProfileDto.from(other));
    }
    dbSession.commit();
    return this;
  }

  public void addUserPermission(QProfileDto profile, UserDto user){
    checkArgument(!profile.isBuiltIn(), "Built-In profile cannot be used");
    dbClient.qProfileEditUsersDao().insert(dbSession, new QProfileEditUsersDto()
      .setUuid(Uuids.createFast())
      .setUserId(user.getId())
      .setQProfileUuid(profile.getKee())
    );
    dbSession.commit();
  }

  public void addGroupPermission(QProfileDto profile, GroupDto group){
    checkArgument(!profile.isBuiltIn(), "Built-In profile cannot be used");
    dbClient.qProfileEditGroupsDao().insert(dbSession, new QProfileEditGroupsDto()
      .setUuid(Uuids.createFast())
      .setGroupId(group.getId())
      .setQProfileUuid(profile.getKee())
    );
    dbSession.commit();
  }
}
