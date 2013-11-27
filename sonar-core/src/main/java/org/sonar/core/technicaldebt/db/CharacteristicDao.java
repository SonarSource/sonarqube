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
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;

import java.util.List;

public class CharacteristicDao implements BatchComponent, ServerComponent  {

  private final MyBatis mybatis;

  public CharacteristicDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  @CheckForNull
  public List<CharacteristicDto> selectEnabledCharacteristics() {
    SqlSession session = mybatis.openSession();
    CharacteristicMapper mapper = session.getMapper(CharacteristicMapper.class);
    try {
      return mapper.selectEnabledCharacteristics();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(CharacteristicDto dto, SqlSession session) {
    session.getMapper(CharacteristicMapper.class).insert(dto);
  }

  public void insert(CharacteristicDto dto) {
    SqlSession session = mybatis.openSession();
    try {
      insert(dto, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(CharacteristicDto dto, SqlSession session) {
    session.getMapper(CharacteristicMapper.class).update(dto);
  }

  public void update(CharacteristicDto dto) {
    SqlSession session = mybatis.openSession();
    try {
      update(dto, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void disable(Integer id, SqlSession session) {
    session.getMapper(CharacteristicMapper.class).disable(id);
  }

  public void disable(Integer id) {
    SqlSession session = mybatis.openSession();
    try {
      disable(id, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
