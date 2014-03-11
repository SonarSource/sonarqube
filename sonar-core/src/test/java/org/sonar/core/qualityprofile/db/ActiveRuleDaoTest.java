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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class ActiveRuleDaoTest extends AbstractDaoTestCase {

  ActiveRuleDao dao;

  @Before
  public void createDao() {
    dao = new ActiveRuleDao(getMyBatis());
  }

  @Test
  public void insert() {
    setupData("empty");

    ActiveRuleDto dto = new ActiveRuleDto()
      .setProfileId(1)
      .setRuleId(10)
      .setSeverity(Severity.MAJOR)
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
      .setSeverity(Severity.BLOCKER)
      .setInheritance(null)
      .setNoteData("text");

    dao.update(dto);

    checkTables("update", "active_rules");
  }

  @Test
  public void delete() {
    setupData("shared");

    dao.delete(1);

    checkTables("delete", "active_rules");
  }

  @Test
  public void delete_from_rule() {
    setupData("shared");

    dao.deleteFromRule(11);

    checkTables("delete_from_rule", "active_rules");
  }

  @Test
  public void delete_from_profile() {
    setupData("shared");

    dao.deleteFromProfile(2);

    checkTables("delete_from_profile", "active_rules");
  }

  @Test
  public void select_by_id() {
    setupData("shared");

    ActiveRuleDto result = dao.selectById(1);
    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getProfileId()).isEqualTo(1);
    assertThat(result.getRulId()).isEqualTo(10);
    assertThat(result.getSeverityString()).isEqualTo(Severity.MAJOR);
    assertThat(result.getInheritance()).isEqualTo("INHERITED");
    assertThat(result.getNoteData()).isEqualTo("some note");
    assertThat(result.getNoteUserLogin()).isEqualTo("henry");
    assertThat(result.getNoteCreatedAt()).isEqualTo(DateUtils.parseDate("2013-12-18"));
    assertThat(result.getNoteUpdatedAt()).isEqualTo(DateUtils.parseDate("2013-12-18"));
  }

  @Test
  public void select_by_ids() {
    setupData("shared");

    List<ActiveRuleDto> result = dao.selectByIds(ImmutableList.of(1));
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getParentId()).isEqualTo(2);

    assertThat(dao.selectByIds(ImmutableList.of(1, 2))).hasSize(2);
  }

  @Test
  public void select_by_profile_and_rule() {
    setupData("shared");

    ActiveRuleDto result = dao.selectByProfileAndRule(1, 10);
    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getProfileId()).isEqualTo(1);
    assertThat(result.getRulId()).isEqualTo(10);
    assertThat(result.getSeverityString()).isEqualTo(Severity.MAJOR);
    assertThat(result.getInheritance()).isEqualTo("INHERITED");
    assertThat(result.getNoteData()).isEqualTo("some note");
    assertThat(result.getNoteUserLogin()).isEqualTo("henry");
    assertThat(result.getNoteCreatedAt()).isEqualTo(DateUtils.parseDate("2013-12-18"));
    assertThat(result.getNoteUpdatedAt()).isEqualTo(DateUtils.parseDate("2013-12-18"));
  }

  @Test
  public void select_by_rule() {
    setupData("shared");

    List<ActiveRuleDto> result = dao.selectByRuleId(10);
    assertThat(result).hasSize(2);
  }

  @Test
  public void select_by_profile() {
    setupData("shared");

    List<ActiveRuleDto> result = dao.selectByProfileId(2);
    assertThat(result).hasSize(2);
  }

  @Test
  public void select_all() {
    setupData("shared");

    List<ActiveRuleDto> result = dao.selectAll();
    assertThat(result).hasSize(3);

    assertThat(find(1, result).getParentId()).isEqualTo(2);
    assertThat(find(2, result).getParentId()).isNull();
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

    checkTables("insert_parameter", "active_rule_parameters");
  }

  @Test
  public void update_parameter() {
    setupData("shared");

    ActiveRuleParamDto dto = new ActiveRuleParamDto()
      .setId(1)
      .setActiveRuleId(2)
      .setRulesParameterId(3)
      .setKey("newMax")
      .setValue("30");

    dao.update(dto);

    checkTables("update_parameter", "active_rule_parameters");
  }

  @Test
  public void delete_parameters() {
    setupData("shared");

    dao.deleteParameters(1);

    checkTables("delete_parameters", "active_rule_parameters");
  }

  @Test
  public void delete_parameter() {
    setupData("shared");

    dao.deleteParameter(1);

    checkTables("delete_parameter", "active_rule_parameters");
  }

  @Test
  public void delete_parameters_from_profile_id() {
    setupData("delete_parameters_from_profile_id");

    dao.deleteParametersFromProfile(2);

    checkTables("delete_parameters_from_profile_id", "active_rule_parameters");
  }

  @Test
  public void select_param_by_id() {
    setupData("shared");

    ActiveRuleParamDto result = dao.selectParamById(1);
    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getActiveRuleId()).isEqualTo(1);
    assertThat(result.getRulesParameterId()).isEqualTo(1);
    assertThat(result.getKey()).isEqualTo("max");
    assertThat(result.getValue()).isEqualTo("20");
  }

  @Test
  public void select_params_by_active_rule_id() {
    setupData("shared");

    assertThat(dao.selectParamsByActiveRuleId(1)).hasSize(2);
    assertThat(dao.selectParamsByActiveRuleId(2)).hasSize(1);
  }

  @Test
  public void select_param_by_active_rule_and_key() {
    setupData("shared");

    ActiveRuleParamDto result = dao.selectParamByActiveRuleAndKey(1, "max");
    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getActiveRuleId()).isEqualTo(1);
    assertThat(result.getRulesParameterId()).isEqualTo(1);
    assertThat(result.getKey()).isEqualTo("max");
    assertThat(result.getValue()).isEqualTo("20");
  }

  @Test
  public void select_params_by_active_rule_ids() {
    setupData("shared");

    assertThat(dao.selectParamsByActiveRuleIds(ImmutableList.of(1))).hasSize(2);
    assertThat(dao.selectParamsByActiveRuleIds(ImmutableList.of(2))).hasSize(1);
    assertThat(dao.selectParamsByActiveRuleIds(ImmutableList.of(1, 2))).hasSize(3);
  }

  @Test
  public void select_params_by_profile_id() {
    setupData("shared");

    assertThat(dao.selectParamsByProfileId(1)).hasSize(2);
  }

  @Test
  public void select_all_params() {
    setupData("shared");

    assertThat(dao.selectAllParams()).hasSize(3);
  }

  private ActiveRuleDto find(final Integer id, List<ActiveRuleDto> dtos){
    return Iterables.find(dtos, new Predicate<ActiveRuleDto>(){
      @Override
      public boolean apply(ActiveRuleDto input) {
        return input.getId().equals(id);
      }
    });
  }

}
