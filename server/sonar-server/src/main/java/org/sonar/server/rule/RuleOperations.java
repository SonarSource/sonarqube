/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rule;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.user.UserSession;

/**
 * @deprecated to be dropped in 4.4
 */
@Deprecated
@ServerSide
public class RuleOperations {

  private final RuleIndexer ruleIndexer;
  private final DbClient dbClient;

  public RuleOperations(RuleIndexer ruleIndexer, DbClient dbClient) {
    this.ruleIndexer = ruleIndexer;
    this.dbClient = dbClient;
  }

  public void updateRule(RuleChange ruleChange, UserSession userSession) {
    checkPermission(userSession);
    DbSession session = dbClient.openSession(false);
    try {
      RuleDto ruleDto = dbClient.ruleDao().selectOrFailByKey(session, ruleChange.ruleKey());
      boolean needUpdate = updateRule(ruleDto, ruleChange.debtRemediationFunction(), ruleChange.debtRemediationCoefficient(),
        ruleChange.debtRemediationOffset(),
        session);
      if (needUpdate) {
        ruleIndexer.index();
        session.commit();
      }
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(e.getMessage());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public boolean updateRule(RuleDto ruleDto, @Nullable String newFunction,
    @Nullable String newCoefficient, @Nullable String newOffset, DbSession session) {
    boolean needUpdate = false;

    // A sub-characteristic and a remediation function is given -> update rule debt
    if (newFunction != null) {
      // New values are the same as the default values -> set overridden values to null
      if (isRuleDebtSameAsDefaultValues(ruleDto, newFunction, newCoefficient, newOffset)) {
        ruleDto.setRemediationFunction(null);
        ruleDto.setRemediationGapMultiplier(null);
        ruleDto.setRemediationBaseEffort(null);
        needUpdate = true;

        // New values are not the same as the overridden values -> update overridden values with new values
      } else if (!isRuleDebtSameAsOverriddenValues(ruleDto, newFunction, newCoefficient, newOffset)) {
        DefaultDebtRemediationFunction debtRemediationFunction = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.valueOf(newFunction), newCoefficient, newOffset);
        ruleDto.setRemediationFunction(debtRemediationFunction.type().name());
        ruleDto.setRemediationGapMultiplier(debtRemediationFunction.gapMultiplier());
        ruleDto.setRemediationBaseEffort(debtRemediationFunction.baseEffort());
        needUpdate = true;
      }

      // No sub-characteristic is given -> disable rule debt if not already disabled
    } else {
      // Rule characteristic is not already disabled -> update it
      ruleDto.setRemediationFunction(null);
      ruleDto.setRemediationGapMultiplier(null);
      ruleDto.setRemediationBaseEffort(null);
      needUpdate = true;
    }

    if (needUpdate) {
      dbClient.ruleDao().update(session, ruleDto);
    }
    return needUpdate;
  }

  private static boolean isRuleDebtSameAsDefaultValues(RuleDto ruleDto, @Nullable String newFunction,
    @Nullable String newCoefficient, @Nullable String newOffset) {
    return isSameRemediationFunction(newFunction, newCoefficient, newOffset, ruleDto.getDefaultRemediationFunction(), ruleDto.getDefaultRemediationGapMultiplier(),
      ruleDto.getDefaultRemediationBaseEffort());
  }

  private static boolean isRuleDebtSameAsOverriddenValues(RuleDto ruleDto, @Nullable String newFunction,
    @Nullable String newCoefficient, @Nullable String newOffset) {
    return isSameRemediationFunction(newFunction, newCoefficient, newOffset, ruleDto.getRemediationFunction(), ruleDto.getRemediationGapMultiplier(),
      ruleDto.getRemediationBaseEffort());
  }

  private static boolean isSameRemediationFunction(@Nullable String newFunction, @Nullable String newCoefficient, @Nullable String newOffset,
    String oldFunction, @Nullable String oldCoefficient, @Nullable String oldOffset) {
    return new EqualsBuilder()
      .append(oldFunction, newFunction)
      .append(oldCoefficient, newCoefficient)
      .append(oldOffset, newOffset)
      .isEquals();
  }

  private static void checkPermission(UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  public static class RuleChange {
    private RuleKey ruleKey;
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
