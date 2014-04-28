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
package org.sonar.server.rule2;

import org.sonar.api.rule.RuleKey;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.cluster.WorkQueue;
import org.sonar.server.db.BaseDao;

import java.util.Collection;

public class RuleDao extends BaseDao<RuleDto, RuleKey> {

  protected RuleDao(WorkQueue workQueue, MyBatis myBatis) {
    super(workQueue, myBatis);
  }

  @Override
  protected String getIndexName() {
    return RuleConstants.INDEX_NAME;
  }

  @Override
  public Collection<RuleKey> insertsSince(Long timestamp) {
    // TODO Auto-generated method stub
    return null;
  }
}
