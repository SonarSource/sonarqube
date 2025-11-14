/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.sarif.pojo.ReportingConfiguration;
import org.sonar.sarif.pojo.ReportingDescriptor;
import org.sonar.sarif.pojo.Result;
import org.sonar.sarif.pojo.Run;
import org.sonar.sarif.pojo.Tool;
import org.sonar.sarif.pojo.ToolComponent;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;
import static org.sonar.scanner.externalissue.sarif.ResultMapper.DEFAULT_IMPACT_SEVERITY;
import static org.sonar.scanner.externalissue.sarif.ResultMapper.DEFAULT_SEVERITY;

public class RulesSeverityDetector {
  private static final Logger LOG = LoggerFactory.getLogger(RulesSeverityDetector.class);
  private static final String UNSUPPORTED_RULE_SEVERITIES_WARNING = "Unable to detect rules severity for issue detected by tool {}, falling back to default rule severity: {}";

  private RulesSeverityDetector() {}

  public static Map<String, Result.Level> detectRulesSeverities(Run run, String driverName) {
    Map<String, Result.Level> resultDefinedRuleSeverities = getResultDefinedRuleSeverities(run);

    if (!resultDefinedRuleSeverities.isEmpty()) {
      return resultDefinedRuleSeverities;
    }

    Map<String, Result.Level> driverDefinedRuleSeverities = getDriverDefinedRuleSeverities(run);

    if (!driverDefinedRuleSeverities.isEmpty()) {
      return driverDefinedRuleSeverities;
    }

    Map<String, Result.Level> extensionDefinedRuleSeverities = getExtensionsDefinedRuleSeverities(run);

    if (!extensionDefinedRuleSeverities.isEmpty()) {
      return extensionDefinedRuleSeverities;
    }

    LOG.warn(UNSUPPORTED_RULE_SEVERITIES_WARNING, driverName, DEFAULT_SEVERITY.name());
    return emptyMap();
  }

  public static Map<String, Result.Level> detectRulesSeveritiesForNewTaxonomy(Run run, String driverName) {
    Map<String, Result.Level> driverDefinedRuleSeverities = getDriverDefinedRuleSeverities(run);

    if (!driverDefinedRuleSeverities.isEmpty()) {
      return driverDefinedRuleSeverities;
    }

    Map<String, Result.Level> extensionDefinedRuleSeverities = getExtensionsDefinedRuleSeverities(run);

    if (!extensionDefinedRuleSeverities.isEmpty()) {
      return extensionDefinedRuleSeverities;
    }

    LOG.warn(UNSUPPORTED_RULE_SEVERITIES_WARNING, driverName, DEFAULT_IMPACT_SEVERITY.name());
    return emptyMap();
  }

  private static Map<String, Result.Level> getResultDefinedRuleSeverities(Run run) {
    Predicate<Result> hasResultDefinedLevel = result -> Optional.ofNullable(result).map(Result::getLevel).isPresent();

    return run.getResults()
      .stream()
      .filter(hasResultDefinedLevel)
      .collect(toMap(Result::getRuleId, Result::getLevel, (x, y) -> y));
  }

  private static Map<String, Result.Level> getDriverDefinedRuleSeverities(Run run) {
    Set<ReportingDescriptor> rules = run.getTool().getDriver().getRules();
    if (rules == null) {
      return emptyMap();
    }
    return rules.stream()
      .filter(RulesSeverityDetector::hasRuleDefinedLevel)
      .collect(toMap(ReportingDescriptor::getId, x -> Result.Level.valueOf(x.getDefaultConfiguration().getLevel().name())));
  }

  private static Map<String, Result.Level> getExtensionsDefinedRuleSeverities(Run run) {
    return getExtensions(run)
      .stream()
      .map(ToolComponent::getRules)
      .filter(Objects::nonNull)
      .flatMap(Collection::stream)
      .filter(RulesSeverityDetector::hasRuleDefinedLevel)
      .collect(toMap(ReportingDescriptor::getId, rule -> Result.Level.valueOf(rule.getDefaultConfiguration().getLevel().name())));
  }

  private static Set<ToolComponent> getExtensions(Run run) {
    return Optional.of(run)
      .map(Run::getTool)
      .map(Tool::getExtensions)
      .orElse(emptySet());
  }

  private static boolean hasRuleDefinedLevel(@Nullable ReportingDescriptor rule) {
    return Optional.ofNullable(rule)
      .map(ReportingDescriptor::getDefaultConfiguration)
      .map(ReportingConfiguration::getLevel)
      .isPresent();
  }

}
