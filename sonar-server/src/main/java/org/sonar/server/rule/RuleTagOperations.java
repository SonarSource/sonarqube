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
package org.sonar.server.rule;

import org.apache.ibatis.session.SqlSession;
import org.elasticsearch.common.collect.Sets;
import org.sonar.api.ServerExtension;
import org.sonar.api.server.rule.RuleTagFormat;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.rule.RuleTagDao;
import org.sonar.core.rule.RuleTagDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;

import java.util.Set;

public class RuleTagOperations implements ServerExtension {

  private final RuleTagDao ruleTagDao;
  private final ESRuleTags esRuleTags;

  public RuleTagOperations(RuleTagDao ruleTagDao, ESRuleTags esRuleTags) {
    this.ruleTagDao = ruleTagDao;
    this.esRuleTags = esRuleTags;
  }

  public RuleTagDto create(String tag, UserSession userSession) {
    checkPermission(userSession);
    validateTagFormat(tag);
    checkDuplicateTag(tag);

    RuleTagDto newTag = new RuleTagDto().setTag(tag);
    ruleTagDao.insert(newTag);
    esRuleTags.put(newTag);
    return newTag;
  }

  private void checkDuplicateTag(String tag) {
    if (ruleTagDao.selectId(tag) != null) {
      throw new BadRequestException(String.format("Tag %s already exists", tag));
    }
  }

  private void validateTagFormat(String tag) {
    try {
      RuleTagFormat.validate(tag);
    } catch(IllegalArgumentException iae) {
      throw new BadRequestException(iae.getMessage());
    }
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  public void deleteUnusedTags(SqlSession session) {
    Set<String> deleted = Sets.newHashSet();
    for (RuleTagDto unused: ruleTagDao.selectUnused(session)) {
      deleted.add(unused.getTag());
      ruleTagDao.delete(unused.getId(), session);
    }
    if (!deleted.isEmpty()) {
      esRuleTags.delete(deleted.toArray(new String[0]));
    }
  }
}
