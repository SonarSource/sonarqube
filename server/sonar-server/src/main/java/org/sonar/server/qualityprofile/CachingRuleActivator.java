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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.List;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.TypeValidations;

public class CachingRuleActivator extends RuleActivator {
  private final Cache<String, List<QualityProfileDto>> childrenByParentKey = CacheBuilder.newBuilder()
      .maximumSize(10_000)
      .build();

  public CachingRuleActivator(System2 system2, DbClient db, RuleIndex ruleIndex, CachingRuleActivatorContextFactory contextFactory, TypeValidations typeValidations,
    ActiveRuleIndexer activeRuleIndexer, UserSession userSession) {
    super(system2, db, ruleIndex, contextFactory, typeValidations, activeRuleIndexer, userSession);
  }

  @Override
  protected List<QualityProfileDto> getChildren(DbSession session, String qualityProfileKey) {
    List<QualityProfileDto> res = childrenByParentKey.getIfPresent(qualityProfileKey);
    if (res != null) {
      return res;
    }
    res = super.getChildren(session, qualityProfileKey);
    childrenByParentKey.put(qualityProfileKey, res);
    return res;
  }

}
