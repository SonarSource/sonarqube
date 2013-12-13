/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.core.qualityprofile.db;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

public class ActiveRuleDaoTest extends AbstractDaoTestCase {

  ActiveRuleDao dao;

  @Before
  public void createDao() {
    dao = new ActiveRuleDao(getMyBatis());
  }

  @Test
  public void insert() {
    ActiveRuleDto dto = new ActiveRuleDto()
      .setProfileId(1)
      .setRuleId(10)
      .setSeverity(2)
      .setInheritance("INHERITED");

    dao.insert(dto);

    checkTables("insert", "active_rules");
  }

  @Test
  public void insert_parameter() {
    ActiveRuleParamDto dto = new ActiveRuleParamDto()
      .setActiveRuleId(1)
      .setRulesParameterId(1)
      .setValue("20");

    dao.insert(dto);

    checkTables("insertParameter", "active_rule_parameters");
  }
}
