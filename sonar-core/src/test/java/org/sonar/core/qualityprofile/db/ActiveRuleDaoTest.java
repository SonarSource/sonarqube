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
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.fest.assertions.Assertions.assertThat;

public class ActiveRuleDaoTest extends AbstractDaoTestCase {

  ActiveRuleDao dao;

  @Before
  public void createDao() {
    dao = new ActiveRuleDao(getMyBatis());
  }

  @Test
  public void select_by_id() {
    setupData("shared");

    ActiveRuleDto result = dao.selectById(1);
    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getProfileId()).isEqualTo(1);
    assertThat(result.getRulId()).isEqualTo(10);
    assertThat(result.getSeverity()).isEqualTo(2);
    assertThat(result.getInheritance()).isEqualTo("INHERITED");
    assertThat(result.getNoteData()).isEqualTo("some note");
    assertThat(result.getNoteUserLogin()).isEqualTo("henry");
    assertThat(result.getNoteCreatedAt()).isEqualTo(DateUtils.parseDate("2013-12-18"));
    assertThat(result.getNoteUpdatedAt()).isEqualTo(DateUtils.parseDate("2013-12-18"));
  }

  @Test
  public void select_by_profile_and_rule() {
    setupData("shared");

    ActiveRuleDto result = dao.selectByProfileAndRule(1, 10);
    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getProfileId()).isEqualTo(1);
    assertThat(result.getRulId()).isEqualTo(10);
    assertThat(result.getSeverity()).isEqualTo(2);
    assertThat(result.getInheritance()).isEqualTo("INHERITED");
    assertThat(result.getNoteData()).isEqualTo("some note");
    assertThat(result.getNoteUserLogin()).isEqualTo("henry");
    assertThat(result.getNoteCreatedAt()).isEqualTo(DateUtils.parseDate("2013-12-18"));
    assertThat(result.getNoteUpdatedAt()).isEqualTo(DateUtils.parseDate("2013-12-18"));
  }

  @Test
  public void insert() {
    setupData("empty");

    ActiveRuleDto dto = new ActiveRuleDto()
      .setProfileId(1)
      .setRuleId(10)
      .setSeverity(2)
      .setInheritance("INHERITED");

    dao.insert(dto);

    checkTables("insert", "active_rules");
  }

  @Test
  public void update() {
    setupData("shared");

    ActiveRuleDto dto = new ActiveRuleDto()
      .setId(1)
      .setProfileId(1)
      .setRuleId(10)
      .setSeverity(4)
      .setInheritance(null)
      .setNoteData("text");

    dao.update(dto);

    checkTables("update", "active_rules");
  }

  @Test
  public void insert_parameter() {
    setupData("empty");

    ActiveRuleParamDto dto = new ActiveRuleParamDto()
      .setActiveRuleId(1)
      .setRulesParameterId(1)
      .setKey("max")
      .setValue("20");

    dao.insert(dto);

    checkTables("insertParameter", "active_rule_parameters");
  }

  @Test
  public void delete() {
    setupData("shared");

    dao.delete(1);

    checkTables("delete", "active_rules");
  }

  @Test
  public void delete_parameters() {
    setupData("shared");

    dao.deleteParameters(1);

    checkTables("delete_parameters", "active_rule_parameters");
  }
}
