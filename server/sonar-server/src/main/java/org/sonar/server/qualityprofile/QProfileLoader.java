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
package org.sonar.server.qualityprofile;

import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.search.FacetValue;

@ServerSide
public class QProfileLoader {

  private final DbClient dbClient;
  private final ActiveRuleIndex activeRuleIndex;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public QProfileLoader(DbClient dbClient, ActiveRuleIndex activeRuleIndex, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.activeRuleIndex = activeRuleIndex;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  public Map<String, Multimap<String, FacetValue>> getAllProfileStats() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<String> keys = dbClient.qualityProfileDao().selectAll(dbSession, getDefaultOrganization(dbSession)).stream().map(QualityProfileDto::getKey).collect(Collectors.toList());
      return activeRuleIndex.getStatsByProfileKeys(keys);
    }
  }

  private OrganizationDto getDefaultOrganization(DbSession dbSession) {
    String defaultOrganizationKey = defaultOrganizationProvider.get().getKey();
    return dbClient.organizationDao()
      .selectByKey(dbSession, defaultOrganizationKey)
      .orElseThrow(() -> new IllegalStateException("Cannot find default organization with key "+defaultOrganizationKey));
  }
}
