/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.function.Consumer;
import org.apache.commons.lang.math.RandomUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;

import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.db.qualityprofile.ActiveRuleDto.createFor;

public class QualityProfileDbTester {
  private final DbClient dbClient;
  private final DbSession dbSession;

  public QualityProfileDbTester(DbTester db) {
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  /**
   * Create a profile with random field values on the specified organization.
   */
  @SafeVarargs
  public final QualityProfileDto insert(OrganizationDto organization, Consumer<QualityProfileDto>... consumers) {
    QualityProfileDto profile = QualityProfileTesting.newQualityProfileDto()
      // default is not randomized yet in QualityProfileTesting
      .setDefault(RandomUtils.nextBoolean())
      .setOrganizationUuid(organization.getUuid());
    Arrays.stream(consumers).forEach(c -> c.accept(profile));

    dbClient.qualityProfileDao().insert(dbSession, profile);
    dbSession.commit();
    return profile;
  }

  public void insertQualityProfiles(QualityProfileDto qualityProfile, QualityProfileDto... qualityProfiles) {
    dbClient.qualityProfileDao().insert(dbSession, qualityProfile, qualityProfiles);
    dbSession.commit();
  }

  public QualityProfileDto insertQualityProfile(QualityProfileDto qualityProfile) {
    dbClient.qualityProfileDao().insert(dbSession, qualityProfile);
    dbSession.commit();
    return qualityProfile;
  }

  public void insertProjectWithQualityProfileAssociations(ComponentDto project, QualityProfileDto... qualityProfiles) {
    dbClient.componentDao().insert(dbSession, project);
    for (QualityProfileDto qualityProfile : qualityProfiles) {
      dbClient.qualityProfileDao().insertProjectProfileAssociation(project.uuid(), qualityProfile.getKey(), dbSession);
    }
    dbSession.commit();
  }

  public void associateProjectWithQualityProfile(ComponentDto project, QualityProfileDto... qualityProfiles) {
    for (QualityProfileDto qualityProfile : qualityProfiles) {
      dbClient.qualityProfileDao().insertProjectProfileAssociation(project.uuid(), qualityProfile.getKey(), dbSession);
    }
    dbSession.commit();
  }

  @SafeVarargs
  public final ActiveRuleDto activateRule(QualityProfileDto profile, RuleDefinitionDto rule, Consumer<ActiveRuleDto>... consumers) {
    ActiveRuleDto activeRule = createFor(profile, rule).setSeverity(MAJOR);
    for (Consumer<ActiveRuleDto> consumer : consumers) {
      consumer.accept(activeRule);
    }
    dbClient.activeRuleDao().insert(dbSession, activeRule);
    dbSession.commit();
    return activeRule;
  }
}
