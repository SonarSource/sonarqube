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
package org.sonar.server.debt;

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DebtModelServiceTest {

  @Mock
  CharacteristicDao dao;

  @Mock
  MyBatis mybatis;

  @Mock
  SqlSession session;

  DebtModelService service;

  CharacteristicDto rootCharacteristicDto = new CharacteristicDto()
    .setId(1)
    .setKey("MEMORY_EFFICIENCY")
    .setName("Memory use")
    .setOrder(1);

  CharacteristicDto characteristicDto = new CharacteristicDto()
    .setId(2)
    .setKey("EFFICIENCY")
    .setName("Efficiency")
    .setParentId(1);

  int currentId;

  @Before
  public void setUp() throws Exception {
    currentId = 10;

    // Associate an id when inserting an object to simulate the db id generator
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        CharacteristicDto dto = (CharacteristicDto) args[0];
        dto.setId(++currentId);
        return null;
      }
    }).when(dao).insert(any(CharacteristicDto.class), any(SqlSession.class));

    when(mybatis.openSession()).thenReturn(session);
    service = new DebtModelService(mybatis, dao);
  }

  @Test
  public void find_root_characteristics() {
    when(dao.selectEnabledRootCharacteristics()).thenReturn(newArrayList(rootCharacteristicDto));
    assertThat(service.rootCharacteristics()).hasSize(1);
  }

  @Test
  public void find_characteristics() {
    when(dao.selectEnabledCharacteristics()).thenReturn(newArrayList(rootCharacteristicDto));
    assertThat(service.characteristics()).hasSize(1);
  }

  @Test
  public void find_characteristic_by_id() {
    when(dao.selectById(1)).thenReturn(rootCharacteristicDto);

    DebtCharacteristic characteristic = service.characteristicById(1);
    assertThat(characteristic.id()).isEqualTo(1);
    assertThat(characteristic.key()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(characteristic.name()).isEqualTo("Memory use");
    assertThat(characteristic.order()).isEqualTo(1);
    assertThat(characteristic.parentId()).isNull();

    assertThat(service.characteristicById(111)).isNull();
  }

  @Test
  public void create_sub_characteristic() {
    when(dao.selectById(1)).thenReturn(rootCharacteristicDto);

    DebtCharacteristic result = service.createCharacteristic("Compilation name", 1);

    assertThat(result.id()).isEqualTo(currentId);
    assertThat(result.key()).isEqualTo("COMPILATION_NAME");
    assertThat(result.name()).isEqualTo("Compilation name");
    assertThat(result.parentId()).isEqualTo(1);
  }

  @Test
  public void fail_to_create_sub_characteristic_when_parent_id_is_not_a_root_characteristic() {
    when(dao.selectById(1)).thenReturn(characteristicDto);

    try {
      service.createCharacteristic("Compilation", 1);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("A sub characteristic can not have a sub characteristic as parent.");
    }
  }

  @Test
  public void fail_to_create_sub_characteristic_when_parent_does_not_exists() {
    when(dao.selectById(1)).thenReturn(null);

    try {
      service.createCharacteristic("Compilation", 1);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("Characteristic with id 1 does not exists.");
    }
  }

  @Test
  public void fail_to_create_sub_characteristic_when_name_already_used() {
    when(dao.selectByName("Compilation", session)).thenReturn(new CharacteristicDto());
    when(dao.selectById(1)).thenReturn(rootCharacteristicDto);

    try {
      service.createCharacteristic("Compilation", 1);
      fail();
    } catch (BadRequestException e) {
      assertThat(e.l10nKey()).isEqualTo("errors.is_already_used");
      assertThat(e.l10nParams().iterator().next()).isEqualTo("Compilation");
    }
  }

  @Test
  public void create_characteristic() {
    when(dao.selectMaxCharacteristicOrder(session)).thenReturn(1);

    DebtCharacteristic result = service.createCharacteristic("Portability", null);

    assertThat(result.id()).isEqualTo(currentId);
    assertThat(result.key()).isEqualTo("PORTABILITY");
    assertThat(result.name()).isEqualTo("Portability");
    assertThat(result.order()).isEqualTo(2);
  }

}
