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
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerSide;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.Validation;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;
import java.util.List;

@ServerSide
public class DebtModelOperations {

  private final DbClient dbClient;
  private final System2 system2;

  public DebtModelOperations(DbClient dbClient) {
    this(dbClient, System2.INSTANCE);
  }

  @VisibleForTesting
  DebtModelOperations(DbClient dbClient, System2 system2) {
    this.dbClient = dbClient;
    this.system2 = system2;
  }

  public DebtCharacteristic create(String name, @Nullable Integer parentId) {
    checkPermission();

    SqlSession session = dbClient.openSession(false);
    try {
      checkNotAlreadyExists(name, session);

      CharacteristicDto newCharacteristic = new CharacteristicDto()
        .setKey(name.toUpperCase().replace(" ", "_"))
        .setName(name)
        .setEnabled(true)
        .setCreatedAt(new Date(system2.now()));

      // New sub characteristic
      if (parentId != null) {
        CharacteristicDto parent = findCharacteristic(parentId, session);
        if (parent.getParentId() != null) {
          throw new BadRequestException("A sub characteristic can not have a sub characteristic as parent.");
        }
        newCharacteristic.setParentId(parent.getId());
      } else {
        // New root characteristic
        newCharacteristic.setOrder(dbClient.debtCharacteristicDao().selectMaxCharacteristicOrder(session) + 1);
      }
      dbClient.debtCharacteristicDao().insert(newCharacteristic, session);
      session.commit();
      return toCharacteristic(newCharacteristic);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public DebtCharacteristic rename(int characteristicId, String newName) {
    checkPermission();

    SqlSession session = dbClient.openSession(false);
    try {
      checkNotAlreadyExists(newName, session);

      CharacteristicDto dto = findCharacteristic(characteristicId, session);
      if (!dto.getName().equals(newName)) {
        dto.setName(newName);
        dto.setUpdatedAt(new Date(system2.now()));
        dbClient.debtCharacteristicDao().update(dto, session);
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

    SqlSession session = dbClient.openSession(false);
    try {
      final CharacteristicDto dto = findCharacteristic(characteristicId, session);
      if (dto.getParentId() != null) {
        throw new BadRequestException("Sub characteristics can not be moved.");
      }
      int currentOrder = getOrder(dto);
      CharacteristicDto dtoToSwitchOrderWith = findCharacteristicToSwitchWith(dto, moveUpOrDown, session);

      // Do nothing when characteristic is already to the good location
      if (dtoToSwitchOrderWith == null) {
        return toCharacteristic(dto);
      }

      int nextOrder = getOrder(dtoToSwitchOrderWith);
      dtoToSwitchOrderWith.setOrder(currentOrder);
      dtoToSwitchOrderWith.setUpdatedAt(new Date(system2.now()));
      dbClient.debtCharacteristicDao().update(dtoToSwitchOrderWith, session);

      dto.setOrder(nextOrder);
      dto.setUpdatedAt(new Date(system2.now()));
      dbClient.debtCharacteristicDao().update(dto, session);

      session.commit();
      return toCharacteristic(dto);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private int getOrder(CharacteristicDto characteristicDto) {
    Integer order = characteristicDto.getOrder();
    if (order == null) {
      throw new IllegalArgumentException(String.format("The order of the characteristic '%s' should not be null", characteristicDto.getKey()));
    }
    return order;
  }

  @CheckForNull
  private CharacteristicDto findCharacteristicToSwitchWith(final CharacteristicDto dto, final boolean moveUpOrDown, SqlSession session) {
    // characteristics should be sort by 'order'
    List<CharacteristicDto> rootCharacteristics = dbClient.debtCharacteristicDao().selectEnabledRootCharacteristics(session);
    int currentPosition = Iterables.indexOf(rootCharacteristics, new Predicate<CharacteristicDto>() {
      @Override
      public boolean apply(@Nullable CharacteristicDto input) {
        return input != null && input.getKey().equals(dto.getKey());
      }
    });
    Integer nextPosition = moveUpOrDown ?
      (currentPosition > 0 ? currentPosition - 1 : null) :
      ((currentPosition < rootCharacteristics.size() - 1) ? currentPosition + 1 : null);
    return (nextPosition != null) ? Iterables.get(rootCharacteristics, nextPosition) : null;
  }

  /**
   * Disable characteristic and its sub characteristics or only sub characteristic.
   * Will also update every rules linked to sub characteristics by setting characteristic id to -1 and remove function, coefficient and offset.
   */
  public void delete(int characteristicId) {
    checkPermission();

    Date updateDate = new Date(system2.now());
    DbSession session = dbClient.openSession(true);
    try {
      delete(findCharacteristic(characteristicId, session), updateDate, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Disabled a characteristic or a sub characteristic.
   * If it has already been disabled, do nothing (for instance when call on a list of characteristics and sub-characteristics in random order)
   */
  public void delete(CharacteristicDto characteristicOrSubCharacteristic, Date updateDate, DbSession session) {
    // Do nothing is the characteristic is already disabled
    if (characteristicOrSubCharacteristic.isEnabled()) {
      // When root characteristic, browse sub characteristics and disable rule debt on each sub characteristic then disable it
      if (characteristicOrSubCharacteristic.getParentId() == null) {
        List<CharacteristicDto> subCharacteristics = dbClient.debtCharacteristicDao().selectCharacteristicsByParentId(characteristicOrSubCharacteristic.getId(), session);
        for (CharacteristicDto subCharacteristic : subCharacteristics) {
          disableSubCharacteristic(subCharacteristic, updateDate, session);
        }
        disableCharacteristic(characteristicOrSubCharacteristic, updateDate, session);
      } else {
        // When sub characteristic, disable rule debt on the sub characteristic then disable it
        disableSubCharacteristic(characteristicOrSubCharacteristic, updateDate, session);
      }
    }
  }

  private void disableSubCharacteristic(CharacteristicDto subCharacteristic, Date updateDate, DbSession session) {
    // Disable debt on all rules (even REMOVED ones, in order to have no issue if they are reactivated) linked to the sub characteristic
    disableRulesDebt(dbClient.ruleDao().findRulesByDebtSubCharacteristicId(session, subCharacteristic.getId()), subCharacteristic.getId(), updateDate, session);
    disableCharacteristic(subCharacteristic, updateDate, session);
  }

  private void disableCharacteristic(CharacteristicDto characteristic, Date updateDate, SqlSession session) {
    characteristic.setEnabled(false);
    characteristic.setUpdatedAt(updateDate);
    dbClient.debtCharacteristicDao().update(characteristic, session);
  }

  private void disableRulesDebt(List<RuleDto> ruleDtos, Integer subCharacteristicId, Date updateDate, DbSession session) {
    for (RuleDto ruleDto : ruleDtos) {
      if (subCharacteristicId.equals(ruleDto.getSubCharacteristicId())) {
        ruleDto.setSubCharacteristicId(RuleDto.DISABLED_CHARACTERISTIC_ID);
        ruleDto.setRemediationFunction(null);
        ruleDto.setRemediationCoefficient(null);
        ruleDto.setRemediationOffset(null);
        ruleDto.setUpdatedAt(updateDate);
      }
      if (subCharacteristicId.equals(ruleDto.getDefaultSubCharacteristicId())) {
        ruleDto.setDefaultSubCharacteristicId(null);
        ruleDto.setDefaultRemediationFunction(null);
        ruleDto.setDefaultRemediationCoefficient(null);
        ruleDto.setDefaultRemediationOffset(null);
      }
      dbClient.ruleDao().update(session, ruleDto);
    }
  }

  private CharacteristicDto findCharacteristic(Integer id, SqlSession session) {
    CharacteristicDto dto = dbClient.debtCharacteristicDao().selectById(id, session);
    if (dto == null) {
      throw new NotFoundException(String.format("Characteristic with id %s does not exists.", id));
    }
    return dto;
  }

  private void checkNotAlreadyExists(String name, SqlSession session) {
    if (dbClient.debtCharacteristicDao().selectByName(name, session) != null) {
      throw new BadRequestException(Validation.IS_ALREADY_USED_MESSAGE, name);
    }
  }

  private void checkPermission() {
    UserSession.get().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
  }

  private static DebtCharacteristic toCharacteristic(CharacteristicDto dto) {
    return new DefaultDebtCharacteristic()
      .setId(dto.getId())
      .setKey(dto.getKey())
      .setName(dto.getName())
      .setOrder(dto.getOrder())
      .setParentId(dto.getParentId())
      .setCreatedAt(dto.getCreatedAt())
      .setUpdatedAt(dto.getUpdatedAt());
  }

}
