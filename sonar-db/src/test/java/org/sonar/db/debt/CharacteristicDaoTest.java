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

package org.sonar.db.debt;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class CharacteristicDaoTest {

  private static final String[] EXCLUDED_COLUMNS = new String[] {"id", "root_id", "rule_id", "function_key", "factor_unit", "factor_value", "offset_unit", "offset_value"};

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  CharacteristicDao dao = db.getDbClient().debtCharacteristicDao();

  @Test
  public void select_enabled_characteristics() {
    db.prepareDbUnit(getClass(), "shared.xml");

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
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(dao.selectCharacteristics()).hasSize(4);
  }

  @Test
  public void select_enabled_root_characteristics() {
    db.prepareDbUnit(getClass(), "select_enabled_root_characteristics.xml");

    List<CharacteristicDto> dtos = dao.selectEnabledRootCharacteristics();

    assertThat(dtos).hasSize(1);

    CharacteristicDto rootCharacteristic = dtos.get(0);
    assertThat(rootCharacteristic.getId()).isEqualTo(1);
    assertThat(rootCharacteristic.getKey()).isEqualTo("PORTABILITY");
  }

  @Test
  public void select_enabled_root_characteristics_order_by_characteristic_order() {
    db.prepareDbUnit(getClass(), "select_enabled_root_characteristics_order_by_characteristic_order.xml");

    List<CharacteristicDto> dtos = dao.selectEnabledRootCharacteristics();

    assertThat(dtos).hasSize(3);
    assertThat(dtos.get(0).getKey()).isEqualTo("TESTABILITY");
    assertThat(dtos.get(1).getKey()).isEqualTo("PORTABILITY");
    assertThat(dtos.get(2).getKey()).isEqualTo("MAINTAINABILITY");
  }

  @Test
  public void select_sub_characteristics_by_parent_id() {
    db.prepareDbUnit(getClass(), "select_sub_characteristics_by_parent_id.xml");

    assertThat(dao.selectCharacteristicsByParentId(1)).hasSize(2);
    assertThat(dao.selectCharacteristicsByParentId(55)).isEmpty();
  }

  @Test
  public void select_characteristics_by_ids() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(dao.selectCharacteristicsByIds(newArrayList(1, 2))).hasSize(2);
    assertThat(dao.selectCharacteristicsByIds(newArrayList(1))).hasSize(1);

    // Disabled characteristics are not returned
    assertThat(dao.selectCharacteristicsByIds(newArrayList(4, 5))).isEmpty();
  }

  @Test
  public void select_characteristic_by_key() {
    db.prepareDbUnit(getClass(), "shared.xml");

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
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(dao.selectByName("Portability")).isNotNull();
    assertThat(dao.selectByName("Compiler related portability")).isNotNull();
    assertThat(dao.selectByName("Unknown")).isNull();
  }

  @Test
  public void select_characteristic_by_id() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(dao.selectById(2)).isNotNull();
    assertThat(dao.selectById(1)).isNotNull();

    assertThat(dao.selectById(10)).isNull();
  }

  @Test
  public void select_max_characteristic_order() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(dao.selectMaxCharacteristicOrder()).isEqualTo(1);
  }

  @Test
  public void select_max_characteristic_order_when_characteristics_are_all_disabled() {
    db.prepareDbUnit(getClass(), "select_max_characteristic_order_when_characteristics_are_all_disabled.xml");

    assertThat(dao.selectMaxCharacteristicOrder()).isEqualTo(0);
  }

  @Test
  public void insert_characteristic() {
    db.truncateTables();

    CharacteristicDto dto = new CharacteristicDto()
      .setKey("COMPILER_RELATED_PORTABILITY")
      .setName("Compiler related portability")
      .setOrder(1)
      .setEnabled(true)
      .setCreatedAt(DateUtils.parseDate("2013-11-20"));

    dao.insert(dto);
    db.getSession().commit();

    db.assertDbUnit(getClass(), "insert_characteristic-result.xml", EXCLUDED_COLUMNS, "characteristics");
  }

  @Test
  public void insert_characteristics() {
    db.truncateTables();

    dao.insert(db.getSession(), new CharacteristicDto()
        .setKey("COMPILER_RELATED_PORTABILITY")
        .setName("Compiler related portability")
        .setOrder(1)
        .setEnabled(true)
        .setCreatedAt(DateUtils.parseDate("2013-11-20")),
      new CharacteristicDto()
        .setKey("PORTABILITY")
        .setName("portability")
        .setOrder(2)
        .setEnabled(true)
        .setCreatedAt(DateUtils.parseDate("2013-11-20")));
    db.getSession().commit();

    assertThat(db.countRowsOfTable("characteristics")).isEqualTo(2);
  }

  @Test
  public void update_characteristic() {
    db.prepareDbUnit(getClass(), "update_characteristic.xml");

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
    db.getSession().commit();

    db.assertDbUnit(getClass(), "update_characteristic-result.xml", EXCLUDED_COLUMNS, "characteristics");
  }

}
