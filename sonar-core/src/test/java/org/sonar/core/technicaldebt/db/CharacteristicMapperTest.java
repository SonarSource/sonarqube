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

import org.apache.ibatis.session.SqlSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.MyBatis;

public class CharacteristicMapperTest extends AbstractDaoTestCase {

  private static final String[] EXCLUDED_COLUMNS = new String[]{"id", "depth", "description", "quality_model_id"};

  SqlSession session;
  CharacteristicMapper mapper;

  @Before
  public void setUp() {
    session = getMyBatis().openSession();
    mapper = session.getMapper(CharacteristicMapper.class);
  }

  @After
  public void tearDown() {
    MyBatis.closeQuietly(session);
  }


  @Test
  public void insert_characteristic() throws Exception {
    CharacteristicDto dto = new CharacteristicDto()
      .setKey("COMPILER_RELATED_PORTABILITY")
      .setName("Compiler related portability")
      .setOrder(1)
      .setEnabled(true)
      .setCreatedAt(DateUtils.parseDate("2013-11-20"));

    mapper.insert(dto);
    session.commit();

    checkTables("insert_characteristic", EXCLUDED_COLUMNS, "characteristics");
  }

  @Test
  public void insert_requirement() throws Exception {
    CharacteristicDto dto = new CharacteristicDto()
      .setParentId(2)
      .setRuleId(1)
      .setFunction("linear_offset")
      .setFactorValue(20.0)
      .setFactorUnit("mn")
      .setOffsetValue(30.0)
      .setOffsetUnit("h")
      .setCreatedAt(DateUtils.parseDate("2013-11-20"))
      .setEnabled(true);

    mapper.insert(dto);
    session.commit();

    checkTables("insert_requirement", EXCLUDED_COLUMNS, "characteristics");
  }

  @Test
  public void update_characteristic() throws Exception {
    setupData("update_characteristic");

    CharacteristicDto dto = new CharacteristicDto()
      .setId(1L)
        // The Key should not be changed
      .setKey("NEW_KEY")
      .setName("New name")
      .setOrder(2)
        // Created date should not changed
      .setCreatedAt(DateUtils.parseDate("2013-11-22"))
      .setUpdatedAt(DateUtils.parseDate("2013-11-22"))
      .setEnabled(false);

    mapper.update(dto);
    session.commit();

    checkTables("update_characteristic", EXCLUDED_COLUMNS, "characteristics");
  }

  @Test
  public void update_requirement() throws Exception {
    setupData("update_requirement");

    CharacteristicDto dto = new CharacteristicDto()
      .setId(1L)
      .setParentId(3)
      .setRuleId(2)
      .setFunction("linear")
      .setFactorValue(21.0)
      .setFactorUnit("h")
      .setOffsetValue(null)
      .setOffsetUnit(null)
        // Created date should not changed
      .setCreatedAt(DateUtils.parseDate("2013-11-22"))
      .setUpdatedAt(DateUtils.parseDate("2013-11-22"))
      .setEnabled(false);

    mapper.update(dto);
    session.commit();

    checkTables("update_requirement", EXCLUDED_COLUMNS, "characteristics");
  }

}
