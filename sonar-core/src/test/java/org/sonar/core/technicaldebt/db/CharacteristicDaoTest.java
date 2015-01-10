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

package org.sonar.core.technicaldebt.db;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class CharacteristicDaoTest extends AbstractDaoTestCase {

  private static final String[] EXCLUDED_COLUMNS = new String[]{"id", "root_id", "rule_id", "function_key", "factor_unit", "factor_value", "offset_unit", "offset_value"};

  CharacteristicDao dao;

  @Before
  public void createDao() {
    dao = new CharacteristicDao(getMyBatis());
  }

  @Test
  public void select_enabled_characteristics() {
    setupData("shared");

    List<CharacteristicDto> dtos = dao.selectEnabledCharacteristics();

    assertThat(dtos).hasSize(2);

    CharacteristicDto rootCharacteristic = dtos.get(0);
    assertThat(rootCharacteristic.getId()).isEqualTo(1);
    assertThat(rootCharacteristic.getKey()).isEqualTo("PORTABILITY");
    assertThat(rootCharacteristic.getName()).isEqualTo("Portability");
    assertThat(rootCharacteristic.getParentId()).isNull();
    assertThat(rootCharacteristic.getOrder()).isEqualTo(1);
    assertThat(rootCharacteristic.isEnabled()).isTrue();
    assertThat(rootCharacteristic.getCreatedAt()).isNotNull();
    assertThat(rootCharacteristic.getUpdatedAt()).isNotNull();

    CharacteristicDto characteristic = dtos.get(1);
    assertThat(characteristic.getId()).isEqualTo(2);
    assertThat(characteristic.getKey()).isEqualTo("COMPILER_RELATED_PORTABILITY");
    assertThat(characteristic.getName()).isEqualTo("Compiler related portability");
    assertThat(characteristic.getParentId()).isEqualTo(1);
    assertThat(characteristic.getOrder()).isNull();
    assertThat(characteristic.isEnabled()).isTrue();
    assertThat(characteristic.getCreatedAt()).isNotNull();
    assertThat(characteristic.getUpdatedAt()).isNotNull();
  }

  @Test
  public void select_characteristics() {
    setupData("shared");

    assertThat(dao.selectCharacteristics()).hasSize(4);
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
  public void select_sub_characteristics_by_parent_id() {
    setupData("select_sub_characteristics_by_parent_id");

    assertThat(dao.selectCharacteristicsByParentId(1)).hasSize(2);
    assertThat(dao.selectCharacteristicsByParentId(55)).isEmpty();
  }

  @Test
  public void select_characteristics_by_ids() {
    setupData("shared");

    assertThat(dao.selectCharacteristicsByIds(newArrayList(1, 2))).hasSize(2);
    assertThat(dao.selectCharacteristicsByIds(newArrayList(1))).hasSize(1);

    // Disabled characteristics are not returned
    assertThat(dao.selectCharacteristicsByIds(newArrayList(4, 5))).isEmpty();
  }

  @Test
  public void select_characteristic_by_key() {
    setupData("shared");

    CharacteristicDto dto = dao.selectByKey("COMPILER_RELATED_PORTABILITY");
    assertThat(dto).isNotNull();
    assertThat(dto.getId()).isEqualTo(2);
    assertThat(dto.getParentId()).isEqualTo(1);

    dto = dao.selectByKey("PORTABILITY");
    assertThat(dto).isNotNull();
    assertThat(dto.getId()).isEqualTo(1);
    assertThat(dto.getParentId()).isNull();

    assertThat(dao.selectByKey("UNKNOWN")).isNull();
  }

  @Test
  public void select_characteristic_by_name() {
    setupData("shared");

    assertThat(dao.selectByName("Portability")).isNotNull();
    assertThat(dao.selectByName("Compiler related portability")).isNotNull();
    assertThat(dao.selectByName("Unknown")).isNull();
  }

  @Test
  public void select_characteristic_by_id() {
    setupData("shared");

    assertThat(dao.selectById(2)).isNotNull();
    assertThat(dao.selectById(1)).isNotNull();

    assertThat(dao.selectById(10)).isNull();
  }

  @Test
  public void select_max_characteristic_order() {
    setupData("shared");

    assertThat(dao.selectMaxCharacteristicOrder()).isEqualTo(1);
  }

  @Test
  public void select_max_characteristic_order_when_characteristics_are_all_disabled() {
    setupData("select_max_characteristic_order_when_characteristics_are_all_disabled");

    assertThat(dao.selectMaxCharacteristicOrder()).isEqualTo(0);
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
      .setUpdatedAt(DateUtils.parseDate("2014-03-19"))
      .setEnabled(false);

    dao.update(dto);

    checkTables("update_characteristic", EXCLUDED_COLUMNS, "characteristics");
  }

}
