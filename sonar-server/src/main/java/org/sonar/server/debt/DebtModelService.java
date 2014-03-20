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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.DebtModel;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.Validation;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Used through ruby code <pre>Internal.debt</pre>
 * Also used by SQALE plugin.
 */
public class DebtModelService implements DebtModel {

  private final MyBatis mybatis;
  private final CharacteristicDao dao;
  private final RuleDao ruleDao;
  private final System2 system2;

  public DebtModelService(MyBatis mybatis, CharacteristicDao dao, RuleDao ruleDao) {
    this(mybatis, dao, ruleDao, System2.INSTANCE);
  }

  @VisibleForTesting
  DebtModelService(MyBatis mybatis, CharacteristicDao dao, RuleDao ruleDao, System2 system2) {
    this.mybatis = mybatis;
    this.dao = dao;
    this.ruleDao = ruleDao;
    this.system2 = system2;
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

  public DebtCharacteristic create(String name, @Nullable Integer parentId) {
    checkPermission();

    SqlSession session = mybatis.openSession();
    try {
      checkNotAlreadyExists(name, session);

      CharacteristicDto newCharacteristic = new CharacteristicDto()
        .setKey(name.toUpperCase().replace(" ", "_"))
        .setName(name)
        .setEnabled(true);

      // New sub characteristic
      if (parentId != null) {
        CharacteristicDto parent = findCharacteristic(parentId, session);
        if (parent.getParentId() != null) {
          throw new BadRequestException("A sub characteristic can not have a sub characteristic as parent.");
        }
        newCharacteristic.setParentId(parent.getId());
      } else {
        // New root characteristic
        newCharacteristic.setOrder(dao.selectMaxCharacteristicOrder(session) + 1);
      }
      dao.insert(newCharacteristic, session);
      session.commit();
      return toCharacteristic(newCharacteristic);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public DebtCharacteristic rename(int characteristicId, String newName) {
    checkPermission();

    SqlSession session = mybatis.openSession();
    try {
      checkNotAlreadyExists(newName, session);

      CharacteristicDto dto = findCharacteristic(characteristicId, session);
      if (!dto.getName().equals(newName)) {
        dto.setName(newName);
        dao.update(dto, session);
        session.commit();
      }
      return toCharacteristic(dto);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public DebtCharacteristic moveUp(int characteristicId) {
    return move(characteristicId, true);
  }

  public DebtCharacteristic moveDown(int characteristicId) {
    return move(characteristicId, false);
  }

  private DebtCharacteristic move(int characteristicId, boolean moveUpOrDown) {
    checkPermission();

    SqlSession session = mybatis.openSession();
    try {
      CharacteristicDto dto = findCharacteristic(characteristicId, session);
      int currentOrder = dto.getOrder();
      CharacteristicDto dtoToSwitchOrderWith = moveUpOrDown ? dao.selectPrevious(currentOrder, session) : dao.selectNext(currentOrder, session);

      // Do nothing when characteristic is already to the new location
      if (dtoToSwitchOrderWith == null) {
        return toCharacteristic(dto);
      }
      int nextOrder = dtoToSwitchOrderWith.getOrder();
      dtoToSwitchOrderWith.setOrder(currentOrder);
      dao.update(dtoToSwitchOrderWith, session);

      dto.setOrder(nextOrder);
      dao.update(dto, session);

      session.commit();
      return toCharacteristic(dto);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Disable characteristic and sub characteristic or only sub characteristic.
   * Will also update every rules linked to sub characteristics by setting characteristic id to -1 and remove function, factor and offset.
   */
  public void delete(int characteristicOrSubCharactteristicId) {
    checkPermission();

    SqlSession session = mybatis.openBatchSession();
    try {
      Date now = new Date(system2.now());

      CharacteristicDto characteristicOrSubCharacteristicDto = findCharacteristic(characteristicOrSubCharactteristicId, session);
      List<RuleDto> ruleDtos = ruleDao.selectByCharacteristicOrSubCharacteristicId(characteristicOrSubCharacteristicDto.getId(), session);
      for (RuleDto ruleDto : ruleDtos) {
        ruleDto.setCharacteristicId(RuleDto.DISABLED_CHARACTERISTIC_ID);
        ruleDto.setRemediationFunction(null);
        ruleDto.setRemediationFactor(null);
        ruleDto.setRemediationOffset(null);
        ruleDto.setUpdatedAt(now);
        ruleDao.update(ruleDto, session);

        // TODO update rules from E/S
      }

      if (characteristicOrSubCharacteristicDto.getParentId() == null) {
        List<CharacteristicDto> dtos = dao.selectCharacteristicsByParentId(characteristicOrSubCharacteristicDto.getId(), session);
        for (CharacteristicDto subCharacteristicDto : dtos) {
          subCharacteristicDto.setEnabled(false);
          subCharacteristicDto.setUpdatedAt(now);
          dao.update(subCharacteristicDto, session);
        }
      }
      characteristicOrSubCharacteristicDto.setEnabled(false);
      characteristicOrSubCharacteristicDto.setUpdatedAt(now);
      dao.update(characteristicOrSubCharacteristicDto, session);

      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private CharacteristicDto findCharacteristic(Integer id, SqlSession session) {
    CharacteristicDto dto = dao.selectById(id, session);
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

  private void checkPermission() {
    UserSession.get().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
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
