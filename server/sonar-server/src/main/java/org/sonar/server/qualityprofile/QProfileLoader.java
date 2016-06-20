/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.FacetValue;

@ServerSide
public class QProfileLoader {

  private final DbClient dbClient;
  private final ActiveRuleIndex activeRuleIndex;
  private final RuleIndex ruleIndex;

  public QProfileLoader(DbClient dbClient, ActiveRuleIndex activeRuleIndex, RuleIndex ruleIndex) {
    this.dbClient = dbClient;
    this.activeRuleIndex = activeRuleIndex;
    this.ruleIndex = ruleIndex;
  }

  /**
   * Returns all Quality profiles as DTOs. This is a temporary solution as long as
   * profiles are not indexed and declared as a business object
   */
  public List<QualityProfileDto> findAll() {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return dbClient.qualityProfileDao().selectAll(dbSession);
    } finally {
      dbSession.close();
    }
  }

  @CheckForNull
  public QualityProfileDto getByKey(String key) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return dbClient.qualityProfileDao().selectByKey(dbSession, key);
    } finally {
      dbSession.close();
    }
  }

  @CheckForNull
  public QualityProfileDto getByLangAndName(String lang, String name) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return dbClient.qualityProfileDao().selectByNameAndLanguage(name, lang, dbSession);
    } finally {
      dbSession.close();
    }
  }

  public Map<String, Multimap<String, FacetValue>> getAllProfileStats() {
    List<String> keys = findAll().stream().map(QualityProfileDto::getKey).collect(Collectors.toList());
    return activeRuleIndex.getStatsByProfileKeys(keys);
  }

  public long countDeprecatedActiveRulesByProfile(String key) {
    return ruleIndex.search(
      new RuleQuery()
        .setQProfileKey(key)
        .setActivation(true)
        .setStatuses(Lists.newArrayList(RuleStatus.DEPRECATED)),
      new SearchOptions().setLimit(0)).getTotal();
  }

}
