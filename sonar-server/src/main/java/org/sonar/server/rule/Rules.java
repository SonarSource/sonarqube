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
import com.google.common.collect.ImmutableList;
import org.sonar.api.ServerExtension;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.paging.PagedResult;
import org.sonar.server.qualityprofile.QProfileValidations;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.RubyUtils;
import org.sonar.server.util.Validation;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class Rules implements ServerExtension {

  private final RuleDao ruleDao;
  private final RuleOperations ruleOperations;
  private final RuleRegistry ruleRegistry;

  public Rules(RuleDao ruleDao, RuleOperations ruleOperations, RuleRegistry ruleRegistry) {
    this.ruleOperations = ruleOperations;
    this.ruleDao = ruleDao;
    this.ruleRegistry = ruleRegistry;
  }

  /**
   * Update a rule : Only sub characteristic or remediation function can be updated
   */
  public void updateRule(RuleOperations.RuleChange ruleChange) {
    ruleOperations.updateRule(ruleChange, UserSession.get());
  }

  public void updateRuleNote(int ruleId, String note) {
    RuleDto rule = findRuleNotNull(ruleId);
    String sanitizedNote = Strings.emptyToNull(note);
    if (sanitizedNote != null) {
      ruleOperations.updateRuleNote(rule, note, UserSession.get());
    } else {
      ruleOperations.deleteRuleNote(rule, UserSession.get());
    }
  }

  /**
   * Create custom rule
   */
  public Integer createCustomRule(int ruleId, @Nullable String name, @Nullable String severity, @Nullable String description, Map<String, String> paramsByKey) {
    RuleDto rule = findRuleNotNull(ruleId);
    validateRule(null, name, severity, description);
    RuleDto newRule = ruleOperations.createCustomRule(rule, name, severity, description, paramsByKey, UserSession.get());
    return newRule.getId();
  }

  /**
   * Update custom rule
   */
  public void updateCustomRule(int ruleId, @Nullable String name, @Nullable String severity, @Nullable String description, Map<String, String> paramsByKey) {
    RuleDto rule = findRuleNotNull(ruleId);
    validateRuleParent(rule);
    validateRule(ruleId, name, severity, description);
    ruleOperations.updateCustomRule(rule, name, severity, description, paramsByKey, UserSession.get());
  }

  /**
   * Delete custom rule
   */
  public void deleteCustomRule(int ruleId) {
    RuleDto rule = findRuleNotNull(ruleId);
    validateRuleParent(rule);
    ruleOperations.deleteCustomRule(rule, UserSession.get());
  }

  public void updateRuleTags(int ruleId, Object tags) {
    RuleDto rule = findRuleNotNull(ruleId);
    List<String> newTags = RubyUtils.toStrings(tags);
    if (newTags == null) {
      newTags = ImmutableList.of();
    }
    ruleOperations.updateRuleTags(rule, newTags, UserSession.get());
  }

  public Rule findByKey(RuleKey key) {
    return ruleRegistry.findByKey(key);
  }

  public PagedResult<Rule> find(RuleQuery query) {
    return ruleRegistry.find(query);
  }

  //
  // Rule validation
  //

  private void validateRule(@Nullable Integer updatingRuleId, @Nullable String name, @Nullable String severity, @Nullable String description) {
    List<BadRequestException.Message> messages = newArrayList();
    if (Strings.isNullOrEmpty(name)) {
      messages.add(BadRequestException.Message.ofL10n(Validation.CANT_BE_EMPTY_MESSAGE, "Name"));
    } else {
      checkRuleNotAlreadyExists(updatingRuleId, name, messages);
    }
    if (Strings.isNullOrEmpty(description)) {
      messages.add(BadRequestException.Message.ofL10n(Validation.CANT_BE_EMPTY_MESSAGE, "Description"));
    }
    if (Strings.isNullOrEmpty(severity)) {
      messages.add(BadRequestException.Message.ofL10n(Validation.CANT_BE_EMPTY_MESSAGE, "Severity"));
    }
    if (!messages.isEmpty()) {
      throw BadRequestException.of(messages);
    }
  }

  private void validateRuleParent(RuleDto rule) {
    if (rule.getParentId() == null) {
      throw new NotFoundException("Unknown rule");
    }
  }

  private RuleDto findRuleNotNull(int ruleId) {
    RuleDto rule = ruleDao.selectById(ruleId);
    QProfileValidations.checkRuleIsNotNull(rule);
    return rule;
  }


  private void checkRuleNotAlreadyExists(@Nullable Integer updatingRuleId, String name, List<BadRequestException.Message> messages) {
    RuleDto existingRule = ruleDao.selectByName(name);
    boolean isModifyingCurrentRule = updatingRuleId != null && existingRule != null && existingRule.getId().equals(updatingRuleId);
    if (!isModifyingCurrentRule && existingRule != null) {
      messages.add(BadRequestException.Message.ofL10n(Validation.IS_ALREADY_USED_MESSAGE, "Name"));
    }
  }
}
