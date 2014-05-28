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

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.sonar.api.ServerComponent;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;

import java.util.Collections;
import java.util.Date;

public class RuleUpdater implements ServerComponent {

  private final DbClient dbClient;
  private final System2 system;

  public RuleUpdater(DbClient dbClient, System2 system) {
    this.dbClient = dbClient;
    this.system = system;
  }

  public boolean update(RuleUpdate update, UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    if (update.isEmpty()) {
      return false;
    }

    DbSession dbSession = dbClient.openSession(false);
    try {
      Context context = newContext(update);
      // validate only the changes, not all the rule fields
      apply(update, context, userSession);
      dbClient.ruleDao().update(dbSession, context.rule);
      dbSession.commit();
      return true;

    } finally {
      dbSession.close();
    }
  }

  /**
   * Load all the DTOs required for validating changes and updating rule
   */
  private Context newContext(RuleUpdate change) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Context context = new Context();
      context.rule = dbClient.ruleDao().getNonNullByKey(dbSession, change.getRuleKey());

      if (change.getDebtSubCharacteristicKey() != null &&
        !change.getDebtSubCharacteristicKey().equals(RuleUpdate.DEFAULT_DEBT_CHARACTERISTIC)) {
        CharacteristicDto characteristicDto = dbClient.debtCharacteristicDao().selectByKey(change.getDebtSubCharacteristicKey(), dbSession);
        if (characteristicDto == null) {
          throw new IllegalArgumentException("Unknown debt sub-characteristic: " + change.getDebtSubCharacteristicKey());
        }
        if (!characteristicDto.isEnabled()) {
          throw new IllegalArgumentException("Debt sub-characteristic is disabled: " + change.getDebtSubCharacteristicKey());
        }
        if (characteristicDto.getParentId() == null) {
          throw new IllegalArgumentException("Not a sub-characteristic: " + change.getDebtSubCharacteristicKey());
        }
        context.newCharacteristic = characteristicDto;
      }
      return context;

    } finally {
      dbSession.close();
    }
  }

  private void apply(RuleUpdate update, Context context, UserSession userSession) {
    if (update.isChangeMarkdownNote()) {
      updateMarkdownNote(update, context, userSession);
    }
    if (update.isChangeTags()) {
      updateTags(update, context);
    }
    if (update.isChangeDebtSubCharacteristic()) {
      updateDebtSubCharacteristic(update, context);
    }
    // order is important -> sub-characteristic must be set
    if (update.isChangeDebtRemediationFunction()) {
      updateDebtRemediationFunction(update, context);
    }
  }

  private void updateTags(RuleUpdate update, Context context) {
    if (update.getTags() == null || update.getTags().isEmpty()) {
      context.rule.setTags(Collections.<String>emptySet());
    } else {
      RuleTagHelper.applyTags(context.rule, update.getTags());
    }
  }

  private void updateDebtSubCharacteristic(RuleUpdate update, Context context) {
    if (update.getDebtSubCharacteristicKey() == null) {
      // set to "none"
      Integer id = context.rule.getDefaultSubCharacteristicId() != null ? RuleDto.DISABLED_CHARACTERISTIC_ID : null;
      context.rule.setSubCharacteristicId(id);
      context.rule.setRemediationFunction(null);
      context.rule.setRemediationCoefficient(null);
      context.rule.setRemediationOffset(null);

    } else if (update.getDebtSubCharacteristicKey().equals(RuleUpdate.DEFAULT_DEBT_CHARACTERISTIC)) {
      // reset to default
      context.rule.setSubCharacteristicId(null);
      context.rule.setRemediationFunction(null);
      context.rule.setRemediationCoefficient(null);
      context.rule.setRemediationOffset(null);

    } else {
      if (ObjectUtils.equals(context.newCharacteristic.getId(), context.rule.getDefaultSubCharacteristicId())) {
        // reset to default -> compatibility with SQALE
        context.rule.setSubCharacteristicId(null);
        context.rule.setRemediationFunction(null);
        context.rule.setRemediationCoefficient(null);
        context.rule.setRemediationOffset(null);
      } else {
        // override default
        context.rule.setSubCharacteristicId(context.newCharacteristic.getId());
      }
    }
  }

  private void updateDebtRemediationFunction(RuleUpdate update, Context context) {
    boolean noChar =
      (context.rule.getDefaultSubCharacteristicId() == null && context.rule.getSubCharacteristicId() == null) ||
        (context.rule.getSubCharacteristicId() != null && context.rule.getSubCharacteristicId().intValue() == RuleDto.DISABLED_CHARACTERISTIC_ID);

    if (noChar || update.getDebtRemediationFunction()==null) {
      context.rule.setRemediationFunction(null);
      context.rule.setRemediationCoefficient(null);
      context.rule.setRemediationOffset(null);
    } else {
      if (isSameAsDefaultFunction(update.getDebtRemediationFunction(), context.rule)) {
        // reset to default
        context.rule.setRemediationFunction(null);
        context.rule.setRemediationCoefficient(null);
        context.rule.setRemediationOffset(null);
      } else {
        context.rule.setRemediationFunction(update.getDebtRemediationFunction().type().name());
        context.rule.setRemediationCoefficient(update.getDebtRemediationFunction().coefficient());
        context.rule.setRemediationOffset(update.getDebtRemediationFunction().offset());
      }
    }
  }

  private void updateMarkdownNote(RuleUpdate update, Context context, UserSession userSession) {
    if (StringUtils.isBlank(update.getMarkdownNote())) {
      context.rule.setNoteData(null);
      context.rule.setNoteCreatedAt(null);
      context.rule.setNoteUpdatedAt(null);
      context.rule.setNoteUserLogin(null);
    } else {
      Date now = new Date(system.now());
      context.rule.setNoteData(update.getMarkdownNote());
      context.rule.setNoteCreatedAt(context.rule.getNoteCreatedAt() != null ? context.rule.getNoteCreatedAt() : now);
      context.rule.setNoteUpdatedAt(now);
      context.rule.setNoteUserLogin(userSession.login());
    }
  }

  private static boolean isSameAsDefaultFunction(DebtRemediationFunction fn, RuleDto rule) {
    return new EqualsBuilder()
      .append(fn.type().name(), rule.getDefaultRemediationFunction())
      .append(fn.coefficient(), rule.getDefaultRemediationCoefficient())
      .append(fn.offset(), rule.getDefaultRemediationOffset())
      .isEquals();
  }

  /**
   * Data loaded before update
   */
  private static class Context {
    private RuleDto rule;
    private CharacteristicDto newCharacteristic;
  }

}
