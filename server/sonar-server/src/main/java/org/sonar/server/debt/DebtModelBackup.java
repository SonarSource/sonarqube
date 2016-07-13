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
package org.sonar.server.debt;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.debt.DebtModelXMLExporter.RuleDebt;
import org.sonar.server.rule.RuleDefinitionsLoader;
import org.sonar.server.rule.RuleOperations;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;

@ServerSide
public class DebtModelBackup {

  private static final Logger LOG = Loggers.get(DebtModelBackup.class);

  private final DbClient dbClient;
  private final RuleOperations ruleOperations;
  private final DebtRulesXMLImporter rulesXMLImporter;
  private final DebtModelXMLExporter debtModelXMLExporter;
  private final RuleDefinitionsLoader defLoader;
  private final System2 system2;
  private final UserSession userSession;
  private final RuleIndexer ruleIndexer;

  public DebtModelBackup(DbClient dbClient, RuleOperations ruleOperations,
    DebtRulesXMLImporter rulesXMLImporter,
    DebtModelXMLExporter debtModelXMLExporter, RuleDefinitionsLoader defLoader, System2 system2, UserSession userSession, RuleIndexer ruleIndexer) {
    this.dbClient = dbClient;
    this.ruleOperations = ruleOperations;
    this.rulesXMLImporter = rulesXMLImporter;
    this.debtModelXMLExporter = debtModelXMLExporter;
    this.defLoader = defLoader;
    this.system2 = system2;
    this.userSession = userSession;
    this.ruleIndexer = ruleIndexer;
  }

  public String backup() {
    return backupFromLanguage(null);
  }

  public String backup(String languageKey) {
    return backupFromLanguage(languageKey);
  }

  private String backupFromLanguage(@Nullable String languageKey) {
    checkPermission();

    DbSession session = dbClient.openSession(false);
    try {
      List<RuleDebt> rules = newArrayList();
      for (RuleDto rule : dbClient.ruleDao().selectEnabled(session)) {
        if (languageKey != null && !languageKey.equals(rule.getLanguage())) {
          continue;
        }
        RuleDebt ruleDebt = toRuleDebt(rule);
        if (ruleDebt != null) {
          rules.add(ruleDebt);
        }
      }
      return debtModelXMLExporter.export(rules);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Reset from provided model
   */
  public void reset() {
    checkPermission();

    long updateDate = system2.now();
    DbSession session = dbClient.openSession(false);
    try {
      // Restore rules
      List<RuleDto> ruleDtos = dbClient.ruleDao().selectEnabled(session);
      if (!ruleDtos.isEmpty()) {

        // Load default rule definitions
        RulesDefinition.Context context = defLoader.load();
        List<RulesDefinition.Rule> rules = newArrayList();
        for (RulesDefinition.Repository repoDef : context.repositories()) {
          rules.addAll(repoDef.rules());
        }

        resetRules(ruleDtos, rules, updateDate, session);
      }

      session.commit();
      ruleIndexer.index();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void resetRules(List<RuleDto> ruleDtos, List<RulesDefinition.Rule> rules, long updateDate, DbSession session) {
    for (RuleDto rule : ruleDtos) {
      // Restore default debt definitions

      RulesDefinition.Rule ruleDef;
      Integer ruleTemplateId = rule.getTemplateId();
      if (ruleTemplateId != null) {
        RuleDto templateRule = rule(ruleTemplateId, ruleDtos);
        ruleDef = ruleDef(templateRule.getRepositoryKey(), templateRule.getRuleKey(), rules);
      } else {
        ruleDef = ruleDef(rule.getRepositoryKey(), rule.getRuleKey(), rules);
      }

      if (ruleDef != null) {
        DebtRemediationFunction remediationFunction = ruleDef.debtRemediationFunction();
        boolean hasDebtDefinition = remediationFunction != null;

        rule.setDefaultRemediationFunction(hasDebtDefinition ? remediationFunction.type().name() : null);
        rule.setDefaultRemediationGapMultiplier(hasDebtDefinition ? remediationFunction.gapMultiplier() : null);
        rule.setDefaultRemediationBaseEffort(hasDebtDefinition ? remediationFunction.baseEffort() : null);
      }

      // Reset overridden debt definitions
      rule.setRemediationFunction(null);
      rule.setRemediationGapMultiplier(null);
      rule.setRemediationBaseEffort(null);
      rule.setUpdatedAt(updateDate);
      dbClient.ruleDao().update(session, rule);
    }
  }

  /**
   * Restore model from a given XML model (characteristics and rule debt are restored from XML)
   */
  public ValidationMessages restoreFromXml(String xml) {
    return restoreXmlModel(xml, null);
  }

  /**
   * Restore model from a given XML model and a given language (only debt of rules on given language are restored from XML)
   */
  public ValidationMessages restoreFromXml(String xml, final String languageKey) {
    return restoreXmlModel(xml, languageKey);
  }

  private ValidationMessages restoreXmlModel(String xml, @Nullable final String languageKey) {
    checkPermission();

    ValidationMessages validationMessages = ValidationMessages.create();
    Date updateDate = new Date(system2.now());
    DbSession session = dbClient.openSession(false);
    try {
      restoreRules(rules(languageKey, session), rulesXMLImporter.importXML(xml, validationMessages), validationMessages, updateDate, session);

      session.commit();
      ruleIndexer.index();
    } catch (IllegalArgumentException e) {
      LOG.debug("Error when restoring the model", e);
      validationMessages.addErrorText(e.getMessage());
    } finally {
      MyBatis.closeQuietly(session);
    }
    return validationMessages;
  }

  private void restoreRules(List<RuleDto> rules, List<RuleDebt> ruleDebts,
    ValidationMessages validationMessages, Date updateDate, DbSession session) {
    for (RuleDto rule : rules) {
      RuleDebt ruleDebt = ruleDebt(rule.getRepositoryKey(), rule.getRuleKey(), ruleDebts);
      ruleOperations.updateRule(rule,
        ruleDebt != null ? ruleDebt.function() : null,
        ruleDebt != null ? ruleDebt.coefficient() : null,
        ruleDebt != null ? ruleDebt.offset() : null, session);
      rule.setUpdatedAt(updateDate.getTime());
      ruleDebts.remove(ruleDebt);
    }

    for (RuleDebt ruleDebt : ruleDebts) {
      validationMessages.addWarningText(String.format("The rule '%s' does not exist.", ruleDebt.ruleKey()));
    }
  }

  private List<RuleDto> rules(@Nullable String languageKey, DbSession session) {
    List<RuleDto> rules = dbClient.ruleDao().selectEnabled(session);
    if (languageKey == null) {
      return rules;
    }
    return newArrayList(Iterables.filter(rules, new RuleDtoMatchLanguage(languageKey)));
  }

  @CheckForNull
  private static RuleDebt ruleDebt(String ruleRepo, String ruleKey, List<RuleDebt> ruleDebts) {
    if (ruleDebts.isEmpty()) {
      return null;
    }
    return Iterables.find(ruleDebts, new RuleDebtMatchRuleRepoAndRuleKey(ruleRepo, ruleKey), null);
  }

  private static RuleDto rule(int id, List<RuleDto> rules) {
    return Iterables.find(rules, new RuleDtoMatchId(id));
  }

  @CheckForNull
  private static RulesDefinition.Rule ruleDef(String ruleRepo, String ruleKey, List<RulesDefinition.Rule> rules) {
    return Iterables.find(rules, new RuleDefMatchRuleRepoAndRuleKey(ruleRepo, ruleKey), null);
  }

  @CheckForNull
  private static RuleDebt toRuleDebt(RuleDto rule) {
    RuleDebt ruleDebt = new RuleDebt().setRuleKey(RuleKey.of(rule.getRepositoryKey(), rule.getRuleKey()));
    String overriddenFunction = rule.getRemediationFunction();
    String defaultFunction = rule.getDefaultRemediationFunction();
    if (overriddenFunction != null) {
      ruleDebt.setFunction(overriddenFunction);
      ruleDebt.setCoefficient(rule.getRemediationGapMultiplier());
      ruleDebt.setOffset(rule.getRemediationBaseEffort());
      return ruleDebt;
    } else if (defaultFunction != null) {
      ruleDebt.setFunction(defaultFunction);
      ruleDebt.setCoefficient(rule.getDefaultRemediationGapMultiplier());
      ruleDebt.setOffset(rule.getDefaultRemediationBaseEffort());
      return ruleDebt;
    }
    return null;
  }

  private void checkPermission() {
    userSession.checkPermission(GlobalPermissions.SYSTEM_ADMIN);
  }

  private static class RuleDtoMatchLanguage implements Predicate<RuleDto> {
    private final String languageKey;

    public RuleDtoMatchLanguage(String languageKey) {
      this.languageKey = languageKey;
    }

    @Override
    public boolean apply(@Nonnull RuleDto input) {
      return languageKey.equals(input.getLanguage());
    }
  }

  private static class RuleDebtMatchRuleRepoAndRuleKey implements Predicate<RuleDebt> {

    private final String ruleRepo;
    private final String ruleKey;

    public RuleDebtMatchRuleRepoAndRuleKey(String ruleRepo, String ruleKey) {
      this.ruleRepo = ruleRepo;
      this.ruleKey = ruleKey;
    }

    @Override
    public boolean apply(@Nullable RuleDebt input) {
      return input != null && ruleRepo.equals(input.ruleKey().repository()) && ruleKey.equals(input.ruleKey().rule());
    }
  }

  private static class RuleDtoMatchId implements Predicate<RuleDto> {
    private final int id;

    public RuleDtoMatchId(int id) {
      this.id = id;
    }

    @Override
    public boolean apply(@Nonnull RuleDto input) {
      return id == input.getId();
    }
  }

  private static class RuleDefMatchRuleRepoAndRuleKey implements Predicate<RulesDefinition.Rule> {

    private final String ruleRepo;
    private final String ruleKey;

    public RuleDefMatchRuleRepoAndRuleKey(String ruleRepo, String ruleKey) {
      this.ruleRepo = ruleRepo;
      this.ruleKey = ruleKey;
    }

    @Override
    public boolean apply(@Nonnull RulesDefinition.Rule input) {
      return ruleRepo.equals(input.repository().key()) && ruleKey.equals(input.key());
    }
  }
}
