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

package org.sonar.server.rule;

import com.google.common.base.Strings;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.sonar.api.ServerSide;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @deprecated to be dropped in 4.4
 */
@Deprecated
@ServerSide
public class RuleOperations {

  private final DbClient dbClient;

  public RuleOperations(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public void updateRule(RuleChange ruleChange, UserSession userSession) {
    checkPermission(userSession);
    DbSession session = dbClient.openSession(false);
    try {
      RuleDto ruleDto = dbClient.ruleDao().getNullableByKey(session, ruleChange.ruleKey());
      if (ruleDto == null) {
        throw new NotFoundException(String.format("Unknown rule '%s'", ruleChange.ruleKey()));
      }
      String subCharacteristicKey = ruleChange.debtCharacteristicKey();
      CharacteristicDto subCharacteristic = null;

      // A sub-characteristic is given -> update rule debt if given values are different from overridden ones and from default ones
      if (!Strings.isNullOrEmpty(subCharacteristicKey)) {
        subCharacteristic = dbClient.debtCharacteristicDao().selectByKey(subCharacteristicKey, session);
        if (subCharacteristic == null) {
          throw new NotFoundException(String.format("Unknown sub characteristic '%s'", ruleChange.debtCharacteristicKey()));
        }
      }

      boolean needUpdate = updateRule(ruleDto, subCharacteristic, ruleChange.debtRemediationFunction(), ruleChange.debtRemediationCoefficient(),
        ruleChange.debtRemediationOffset(),
        session);
      if (needUpdate) {
        session.commit();
      }
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(e.getMessage());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public boolean updateRule(RuleDto ruleDto, @Nullable CharacteristicDto newSubCharacteristic, @Nullable String newFunction,
    @Nullable String newCoefficient, @Nullable String newOffset, DbSession session) {
    boolean needUpdate = false;

    // A sub-characteristic and a remediation function is given -> update rule debt
    if (newSubCharacteristic != null && newFunction != null) {
      // New values are the same as the default values -> set overridden values to null
      if (isRuleDebtSameAsDefaultValues(ruleDto, newSubCharacteristic, newFunction, newCoefficient, newOffset)) {
        ruleDto.setSubCharacteristicId(null);
        ruleDto.setRemediationFunction(null);
        ruleDto.setRemediationCoefficient(null);
        ruleDto.setRemediationOffset(null);
        needUpdate = true;

        // New values are not the same as the overridden values -> update overridden values with new values
      } else if (!isRuleDebtSameAsOverriddenValues(ruleDto, newSubCharacteristic, newFunction, newCoefficient, newOffset)) {
        ruleDto.setSubCharacteristicId(newSubCharacteristic.getId());

        DefaultDebtRemediationFunction debtRemediationFunction = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.valueOf(newFunction), newCoefficient, newOffset);
        ruleDto.setRemediationFunction(debtRemediationFunction.type().name());
        ruleDto.setRemediationCoefficient(debtRemediationFunction.coefficient());
        ruleDto.setRemediationOffset(debtRemediationFunction.offset());
        needUpdate = true;
      }

      // No sub-characteristic is given -> disable rule debt if not already disabled
    } else {
      // Rule characteristic is not already disabled -> update it
      if (ruleDto.getSubCharacteristicId() == null || !RuleDto.DISABLED_CHARACTERISTIC_ID.equals(ruleDto.getSubCharacteristicId())) {
        // If default characteristic is not defined, set the overridden characteristic to null in order to be able to track debt plugin
        // update
        ruleDto.setSubCharacteristicId(ruleDto.getDefaultSubCharacteristicId() != null ? RuleDto.DISABLED_CHARACTERISTIC_ID : null);
        ruleDto.setRemediationFunction(null);
        ruleDto.setRemediationCoefficient(null);
        ruleDto.setRemediationOffset(null);
        needUpdate = true;
      }
    }

    if (needUpdate) {
      dbClient.ruleDao().update(session, ruleDto);
    }
    return needUpdate;
  }

  private static boolean isRuleDebtSameAsDefaultValues(RuleDto ruleDto, CharacteristicDto newSubCharacteristic, @Nullable String newFunction,
    @Nullable String newCoefficient, @Nullable String newOffset) {
    return newSubCharacteristic.getId().equals(ruleDto.getDefaultSubCharacteristicId()) &&
      isSameRemediationFunction(newFunction, newCoefficient, newOffset, ruleDto.getDefaultRemediationFunction(), ruleDto.getDefaultRemediationCoefficient(),
        ruleDto.getDefaultRemediationOffset());
  }

  private static boolean isRuleDebtSameAsOverriddenValues(RuleDto ruleDto, CharacteristicDto newSubCharacteristic, @Nullable String newFunction,
    @Nullable String newCoefficient, @Nullable String newOffset) {
    return newSubCharacteristic.getId().equals(ruleDto.getSubCharacteristicId())
      && isSameRemediationFunction(newFunction, newCoefficient, newOffset, ruleDto.getRemediationFunction(), ruleDto.getRemediationCoefficient(), ruleDto.getRemediationOffset());
  }

  private static boolean isSameRemediationFunction(@Nullable String newFunction, @Nullable String newCoefficient, @Nullable String newOffset,
    String oldFunction, @Nullable String oldCoefficient, @Nullable String oldOffset) {
    return new EqualsBuilder()
      .append(oldFunction, newFunction)
      .append(oldCoefficient, newCoefficient)
      .append(oldOffset, newOffset)
      .isEquals();
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  public static class RuleChange {
    private RuleKey ruleKey;
    private String debtCharacteristicKey;
    private String debtRemediationFunction;
    private String debtRemediationCoefficient;
    private String debtRemediationOffset;

    public RuleKey ruleKey() {
      return ruleKey;
    }

    public RuleChange setRuleKey(RuleKey ruleKey) {
      this.ruleKey = ruleKey;
      return this;
    }

    @CheckForNull
    public String debtCharacteristicKey() {
      return debtCharacteristicKey;
    }

    public RuleChange setDebtCharacteristicKey(@Nullable String debtCharacteristicKey) {
      this.debtCharacteristicKey = debtCharacteristicKey;
      return this;
    }

    @CheckForNull
    public String debtRemediationFunction() {
      return debtRemediationFunction;
    }

    public RuleChange setDebtRemediationFunction(@Nullable String debtRemediationFunction) {
      this.debtRemediationFunction = debtRemediationFunction;
      return this;
    }

    @CheckForNull
    public String debtRemediationCoefficient() {
      return debtRemediationCoefficient;
    }

    public RuleChange setDebtRemediationCoefficient(@Nullable String debtRemediationCoefficient) {
      this.debtRemediationCoefficient = debtRemediationCoefficient;
      return this;
    }

    @CheckForNull
    public String debtRemediationOffset() {
      return debtRemediationOffset;
    }

    public RuleChange setDebtRemediationOffset(@Nullable String debtRemediationOffset) {
      this.debtRemediationOffset = debtRemediationOffset;
      return this;
    }
  }
}
