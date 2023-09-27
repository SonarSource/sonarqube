/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.scanner.externalissue.sarif;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.core.sarif.Driver;
import org.sonar.core.sarif.Result;
import org.sonar.core.sarif.Rule;
import org.sonar.core.sarif.Run;
import org.sonar.core.sarif.Tool;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.sonar.api.utils.Preconditions.checkArgument;
import static org.sonar.scanner.externalissue.sarif.RulesSeverityDetector.detectRulesSeverities;
import static org.sonar.scanner.externalissue.sarif.RulesSeverityDetector.detectRulesSeveritiesForNewTaxonomy;

@ScannerSide
public class RunMapper {
  private static final Logger LOG = LoggerFactory.getLogger(RunMapper.class);

  private final ResultMapper resultMapper;
  private final RuleMapper ruleMapper;

  RunMapper(ResultMapper resultMapper, RuleMapper ruleMapper) {
    this.resultMapper = resultMapper;
    this.ruleMapper = ruleMapper;
  }

  RunMapperResult mapRun(Run run) {
    if (run.getResults().isEmpty()) {
      return new RunMapperResult();
    }

    String driverName = getToolDriverName(run);
    Map<String, String> ruleSeveritiesByRuleId = detectRulesSeverities(run, driverName);
    Map<String, String> ruleSeveritiesByRuleIdForNewCCT = detectRulesSeveritiesForNewTaxonomy(run, driverName);

    return new RunMapperResult()
      .newAdHocRules(toNewAdHocRules(run, driverName, ruleSeveritiesByRuleId, ruleSeveritiesByRuleIdForNewCCT))
      .newExternalIssues(toNewExternalIssues(run, driverName, ruleSeveritiesByRuleId, ruleSeveritiesByRuleIdForNewCCT));
  }

  private static String getToolDriverName(Run run) throws IllegalArgumentException {
    checkArgument(hasToolDriverNameDefined(run), "The run does not have a tool driver name defined.");
    return run.getTool().getDriver().getName();
  }

  private static boolean hasToolDriverNameDefined(Run run) {
    return Optional.ofNullable(run)
      .map(Run::getTool)
      .map(Tool::getDriver)
      .map(Driver::getName)
      .isPresent();
  }

  private List<NewAdHocRule> toNewAdHocRules(Run run, String driverName, Map<String, String> ruleSeveritiesByRuleId, Map<String, String> ruleSeveritiesByRuleIdForNewCCT) {
    Set<Rule> driverRules = run.getTool().getDriver().getRules();
    Set<Rule> extensionRules = hasExtensions(run.getTool())
      ? run.getTool().getExtensions().stream().flatMap(extension -> extension.getRules().stream()).collect(toSet())
      : Set.of();
    return Stream.concat(driverRules.stream(), extensionRules.stream())
      .distinct()
      .map(rule -> ruleMapper.mapRule(rule, driverName, ruleSeveritiesByRuleId.get(rule.getId()), ruleSeveritiesByRuleIdForNewCCT.get(rule.getId())))
      .collect(toList());
  }

  private static boolean hasExtensions(Tool tool) {
    return tool.getExtensions() != null && !tool.getExtensions().isEmpty();
  }

  private List<NewExternalIssue> toNewExternalIssues(Run run, String driverName, Map<String, String> ruleSeveritiesByRuleId, Map<String, String> ruleSeveritiesByRuleIdForNewCCT) {
    return run.getResults()
      .stream()
      .map(result -> toNewExternalIssue(driverName, ruleSeveritiesByRuleId.get(result.getRuleId()), ruleSeveritiesByRuleIdForNewCCT.get(result.getRuleId()), result))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(toList());
  }

  private Optional<NewExternalIssue> toNewExternalIssue(String driverName, @Nullable String ruleSeverity, @Nullable String ruleSeverityForNewTaxonomy, Result result) {
    try {
      return Optional.of(resultMapper.mapResult(driverName, ruleSeverity, ruleSeverityForNewTaxonomy, result));
    } catch (Exception exception) {
      LOG.warn("Failed to import an issue raised by tool {}, error: {}", driverName, exception.getMessage());
      return Optional.empty();
    }
  }

  static class RunMapperResult {
    private List<NewExternalIssue> newExternalIssues;
    private List<NewAdHocRule> newAdHocRules;
    private boolean success;

    public RunMapperResult() {
      this.newExternalIssues = emptyList();
      this.newAdHocRules = emptyList();
      this.success = true;
    }

    public RunMapperResult newExternalIssues(List<NewExternalIssue> newExternalIssues) {
      this.newExternalIssues = newExternalIssues;
      return this;
    }

    public RunMapperResult newAdHocRules(List<NewAdHocRule> newAdHocRules) {
      this.newAdHocRules = newAdHocRules;
      return this;
    }

    public RunMapperResult success(boolean success) {
      this.success = success;
      return this;
    }

    public List<NewExternalIssue> getNewExternalIssues() {
      return newExternalIssues;
    }

    public List<NewAdHocRule> getNewAdHocRules() {
      return newAdHocRules;
    }

    public boolean isSuccess() {
      return success;
    }
  }

}
