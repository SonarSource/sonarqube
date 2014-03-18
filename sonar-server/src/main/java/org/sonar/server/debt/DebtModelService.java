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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.DebtModel;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.util.Validation;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Used through ruby code <pre>Internal.debt</pre>
 * Also used by SQALE plugin.
 */
public class DebtModelService implements DebtModel {

  private final MyBatis mybatis;
  private final CharacteristicDao dao;

  public DebtModelService(MyBatis mybatis, CharacteristicDao dao) {
    this.mybatis = mybatis;
    this.dao = dao;
  }

  public List<DebtCharacteristic> rootCharacteristics() {
    return toCharacteristics(dao.selectEnabledRootCharacteristics());
  }

  public List<DebtCharacteristic> characteristics() {
    return toCharacteristics(dao.selectEnabledCharacteristics());
  }

  @CheckForNull
  public DebtCharacteristic characteristicById(int id) {
    CharacteristicDto dto = dao.selectById(id);
    return dto != null ? toCharacteristic(dto) : null;
  }

  public DebtCharacteristic createCharacteristic(String name, @Nullable Integer parentId) {
    SqlSession session = mybatis.openSession();
    try {
      checkNotAlreadyExists(name, session);

      CharacteristicDto newCharacteristic = new CharacteristicDto()
        .setKey(name.toUpperCase().replace(" ", "_"))
        .setName(name)
        .setEnabled(true);

      // New sub characteristic
      if (parentId != null) {
        CharacteristicDto parent = findCharacteristic(parentId);
        if (parent.getParentId() != null) {
          throw new BadRequestException("A sub characteristic can not have a sub characteristic as parent.");
        }
        newCharacteristic.setParentId(parent.getId());
      } else {
        // New root characteristic
        newCharacteristic.setOrder(dao.selectMaxCharacteristicOrder(session)+1);
      }
      dao.insert(newCharacteristic, session);
      session.commit();
      return toCharacteristic(newCharacteristic);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private CharacteristicDto findCharacteristic(Integer id){
    CharacteristicDto dto = dao.selectById(id);
    if (dto == null) {
      throw new NotFoundException(String.format("Characteristic with id %s does not exists.", id));
    }
    return dto;
  }

  private void checkNotAlreadyExists(String name, SqlSession session) {
    if (dao.selectByName(name, session) != null) {
      throw BadRequestException.ofL10n(Validation.IS_ALREADY_USED_MESSAGE, name);
    }
  }

  private static List<DebtCharacteristic> toCharacteristics(Collection<CharacteristicDto> dtos) {
    return newArrayList(Iterables.transform(dtos, new Function<CharacteristicDto, DebtCharacteristic>() {
      @Override
      public DebtCharacteristic apply(CharacteristicDto input) {
        return toCharacteristic(input);
      }
    }));
  }

  private static DebtCharacteristic toCharacteristic(CharacteristicDto dto) {
    return new DefaultDebtCharacteristic()
      .setId(dto.getId())
      .setKey(dto.getKey())
      .setName(dto.getName())
      .setOrder(dto.getOrder())
      .setParentId(dto.getParentId());
  }

}
