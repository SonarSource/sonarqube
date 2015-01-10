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

package org.sonar.core.qualityprofile.db;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ActiveRuleDaoTest extends AbstractDaoTestCase {

  ActiveRuleDao dao;

  @Before
  public void before() {
    dao = new ActiveRuleDao(getMyBatis());
  }

  @Test
  public void select_by_profile() {
    setupData("shared");

    List<ActiveRuleDto> result = dao.selectByProfileKey("parent");
    assertThat(result).hasSize(2);
  }

  @Test
  public void insert_parameter() {
    setupData("empty");

    DbSession session = getMyBatis().openSession(false);
    ActiveRuleParamDto dto = new ActiveRuleParamDto()
      .setActiveRuleId(1)
      .setRulesParameterId(1)
      .setKey("max")
      .setValue("20");
    dao.insert(dto, session);
    session.commit();
    session.close();

    checkTables("insert_parameter", "active_rule_parameters");
  }

  @Test
  public void select_params_by_profile_id() {
    setupData("shared");

    assertThat(dao.selectParamsByProfileKey("child")).hasSize(2);
  }
}
