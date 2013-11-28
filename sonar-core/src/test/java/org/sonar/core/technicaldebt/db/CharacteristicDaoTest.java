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

package org.sonar.core.technicaldebt.db;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class CharacteristicDaoTest extends AbstractDaoTestCase {

  private static final String[] EXCLUDED_COLUMNS = new String[]{"id", "depth", "description", "quality_model_id", "created_at", "updated_at"};

  CharacteristicDao dao;

  @Before
  public void createDao() {
    dao = new CharacteristicDao(getMyBatis());
  }

  @Test
  public void select_enabled_characteristics() {
    setupData("shared");

    List<CharacteristicDto> dtos = dao.selectEnabledCharacteristics();

    assertThat(dtos).hasSize(3);

    CharacteristicDto rootCharacteristic = dtos.get(0);
    assertThat(rootCharacteristic.getId()).isEqualTo(1);
    assertThat(rootCharacteristic.getKey()).isEqualTo("PORTABILITY");
    assertThat(rootCharacteristic.getName()).isEqualTo("Portability");
    assertThat(rootCharacteristic.getParentId()).isNull();
    assertThat(rootCharacteristic.getRootId()).isNull();
    assertThat(rootCharacteristic.getRuleId()).isNull();
    assertThat(rootCharacteristic.getOrder()).isEqualTo(1);
    assertThat(rootCharacteristic.isEnabled()).isTrue();
    assertThat(rootCharacteristic.getCreatedAt()).isNotNull();
    assertThat(rootCharacteristic.getUpdatedAt()).isNotNull();

    CharacteristicDto characteristic = dtos.get(1);
    assertThat(characteristic.getId()).isEqualTo(2);
    assertThat(characteristic.getKey()).isEqualTo("COMPILER_RELATED_PORTABILITY");
    assertThat(characteristic.getName()).isEqualTo("Compiler related portability");
    assertThat(characteristic.getParentId()).isEqualTo(1);
    assertThat(characteristic.getRootId()).isEqualTo(1);
    assertThat(characteristic.getRuleId()).isNull();
    assertThat(characteristic.getOrder()).isNull();
    assertThat(characteristic.isEnabled()).isTrue();
    assertThat(characteristic.getCreatedAt()).isNotNull();
    assertThat(characteristic.getUpdatedAt()).isNotNull();

    CharacteristicDto requirement = dtos.get(2);
    assertThat(requirement.getId()).isEqualTo(3);
    assertThat(requirement.getKey()).isNull();
    assertThat(requirement.getName()).isNull();
    assertThat(requirement.getParentId()).isEqualTo(2);
    assertThat(requirement.getRootId()).isEqualTo(1);
    assertThat(requirement.getRuleId()).isEqualTo(1);
    assertThat(requirement.getOrder()).isNull();
    assertThat(requirement.getFunction()).isEqualTo("linear_offset");
    assertThat(requirement.getFactorValue()).isEqualTo(20.0);
    assertThat(requirement.getFactorUnit()).isEqualTo("mn");
    assertThat(requirement.getOffsetValue()).isEqualTo(30.0);
    assertThat(requirement.getOffsetUnit()).isEqualTo("h");
    assertThat(requirement.isEnabled()).isTrue();
    assertThat(requirement.getCreatedAt()).isNotNull();
    assertThat(requirement.getUpdatedAt()).isNull();
  }

  @Test
  public void select_enabled_root_characteristics() {
    setupData("select_enabled_root_characteristics");

    List<CharacteristicDto> dtos = dao.selectEnabledRootCharacteristics();

    assertThat(dtos).hasSize(1);

    CharacteristicDto rootCharacteristic = dtos.get(0);
    assertThat(rootCharacteristic.getId()).isEqualTo(1);
    assertThat(rootCharacteristic.getKey()).isEqualTo("PORTABILITY");
  }

  @Test
  public void select_enabled_root_characteristics_order_by_characteristic_order() {
    setupData("select_enabled_root_characteristics_order_by_characteristic_order");

    List<CharacteristicDto> dtos = dao.selectEnabledRootCharacteristics();

    assertThat(dtos).hasSize(3);
    assertThat(dtos.get(0).getKey()).isEqualTo("TESTABILITY");
    assertThat(dtos.get(1).getKey()).isEqualTo("PORTABILITY");
    assertThat(dtos.get(2).getKey()).isEqualTo("MAINTAINABILITY");
  }

  @Test
  public void select_requirement() {
    setupData("shared");

    CharacteristicDto dto = dao.selectByRuleId(1);

    assertThat(dto).isNotNull();
    assertThat(dto.getId()).isEqualTo(3);
    assertThat(dto.getParentId()).isEqualTo(2);
    assertThat(dto.getRootId()).isEqualTo(1);
  }

  @Test
  public void select_characteristic_by_key() {
    setupData("shared");

    CharacteristicDto dto = dao.selectByKey("COMPILER_RELATED_PORTABILITY");
    assertThat(dto).isNotNull();
    assertThat(dto.getId()).isEqualTo(2);
    assertThat(dto.getParentId()).isEqualTo(1);
    assertThat(dto.getRootId()).isEqualTo(1);

    dto = dao.selectByKey("PORTABILITY");
    assertThat(dto).isNotNull();
    assertThat(dto.getId()).isEqualTo(1);
    assertThat(dto.getParentId()).isNull();
    assertThat(dto.getRootId()).isNull();

    assertThat(dao.selectByKey("UNKNOWN")).isNull();
  }

  @Test
  public void select_characteristic_by_id() {
    setupData("shared");

    assertThat(dao.selectById(2)).isNotNull();
    assertThat(dao.selectById(1)).isNotNull();

    assertThat(dao.selectById(10)).isNull();
  }

  @Test
  public void insert_characteristic() throws Exception {
    CharacteristicDto dto = new CharacteristicDto()
      .setKey("COMPILER_RELATED_PORTABILITY")
      .setName("Compiler related portability")
      .setOrder(1)
      .setEnabled(true)
      .setCreatedAt(DateUtils.parseDate("2013-11-20"));

    dao.insert(dto);

    checkTables("insert_characteristic", EXCLUDED_COLUMNS, "characteristics");
  }

  @Test
  public void insert_requirement() throws Exception {
    CharacteristicDto dto = new CharacteristicDto()
      .setParentId(2)
      .setRootId(1)
      .setRuleId(1)
      .setFunction("linear_offset")
      .setFactorValue(20.0)
      .setFactorUnit("mn")
      .setOffsetValue(30.0)
      .setOffsetUnit("h")
      .setCreatedAt(DateUtils.parseDate("2013-11-20"))
      .setEnabled(true);

    dao.insert(dto);

    checkTables("insert_requirement", EXCLUDED_COLUMNS, "characteristics");
  }

  @Test
  public void update_characteristic() throws Exception {
    setupData("update_characteristic");

    CharacteristicDto dto = new CharacteristicDto()
      .setId(1)
        // The Key should not be changed
      .setKey("NEW_KEY")
      .setName("New name")
      .setOrder(2)
        // Created date should not changed
      .setCreatedAt(DateUtils.parseDate("2013-11-22"))
      .setEnabled(false);

    dao.update(dto);

    checkTables("update_characteristic", new String[]{"id", "depth", "description", "quality_model_id", "updated_at"}, "characteristics");
  }

  @Test
  public void update_requirement() throws Exception {
    setupData("update_requirement");

    CharacteristicDto dto = new CharacteristicDto()
      .setId(1)
      .setParentId(3)
      .setRootId(1)
      .setRuleId(2)
      .setFunction("linear")
      .setFactorValue(21.0)
      .setFactorUnit("h")
      .setOffsetValue(null)
      .setOffsetUnit(null)
        // Created date should not changed
      .setCreatedAt(DateUtils.parseDate("2013-11-22"))
      .setEnabled(false);

    dao.update(dto);

    checkTables("update_requirement", EXCLUDED_COLUMNS, "characteristics");
  }

  @Test
  public void disable() throws Exception {
    setupData("disable");

    dao.disable(1);

    checkTables("disable", EXCLUDED_COLUMNS, "characteristics");
  }

}
