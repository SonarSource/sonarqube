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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.sarif.DefaultConfiguration;
import org.sonar.core.sarif.Extension;
import org.sonar.core.sarif.Result;
import org.sonar.core.sarif.Rule;
import org.sonar.core.sarif.Run;
import org.sonar.core.sarif.Tool;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;
import static org.sonar.scanner.externalissue.sarif.ResultMapper.DEFAULT_IMPACT_SEVERITY;
import static org.sonar.scanner.externalissue.sarif.ResultMapper.DEFAULT_SEVERITY;

public class RulesSeverityDetector {
  private static final Logger LOG = LoggerFactory.getLogger(RulesSeverityDetector.class);
  private static final String UNSUPPORTED_RULE_SEVERITIES_WARNING = "Unable to detect rules severity for issue detected by tool {}, falling back to default rule severity: {}";

  private RulesSeverityDetector() {}

  public static Map<String, String> detectRulesSeverities(Run run, String driverName) {
    Map<String, String> resultDefinedRuleSeverities = getResultDefinedRuleSeverities(run);

    if (!resultDefinedRuleSeverities.isEmpty()) {
      return resultDefinedRuleSeverities;
    }

    Map<String, String> driverDefinedRuleSeverities = getDriverDefinedRuleSeverities(run);

    if (!driverDefinedRuleSeverities.isEmpty()) {
      return driverDefinedRuleSeverities;
    }

    Map<String, String> extensionDefinedRuleSeverities = getExtensionsDefinedRuleSeverities(run);

    if (!extensionDefinedRuleSeverities.isEmpty()) {
      return extensionDefinedRuleSeverities;
    }

    LOG.warn(UNSUPPORTED_RULE_SEVERITIES_WARNING, driverName, DEFAULT_SEVERITY.name());
    return emptyMap();
  }

  public static Map<String, String> detectRulesSeveritiesForNewTaxonomy(Run run, String driverName) {
    Map<String, String> driverDefinedRuleSeverities = getDriverDefinedRuleSeverities(run);

    if (!driverDefinedRuleSeverities.isEmpty()) {
      return driverDefinedRuleSeverities;
    }

    Map<String, String> extensionDefinedRuleSeverities = getExtensionsDefinedRuleSeverities(run);

    if (!extensionDefinedRuleSeverities.isEmpty()) {
      return extensionDefinedRuleSeverities;
    }

    LOG.warn(UNSUPPORTED_RULE_SEVERITIES_WARNING, driverName, DEFAULT_IMPACT_SEVERITY.name());
    return emptyMap();
  }

  private static Map<String, String> getResultDefinedRuleSeverities(Run run) {
    Predicate<Result> hasResultDefinedLevel = result -> Optional.ofNullable(result).map(Result::getLevel).isPresent();

    return run.getResults()
      .stream()
      .filter(hasResultDefinedLevel)
      .collect(toMap(Result::getRuleId, Result::getLevel, (x, y) -> y));
  }

  private static Map<String, String> getDriverDefinedRuleSeverities(Run run) {
    return run.getTool().getDriver().getRules()
      .stream()
      .filter(RulesSeverityDetector::hasRuleDefinedLevel)
      .collect(toMap(Rule::getId, x -> x.getDefaultConfiguration().getLevel()));
  }

  private static Map<String, String> getExtensionsDefinedRuleSeverities(Run run) {
    return getExtensions(run)
      .stream()
      .map(Extension::getRules)
      .filter(Objects::nonNull)
      .flatMap(Collection::stream)
      .filter(RulesSeverityDetector::hasRuleDefinedLevel)
      .collect(toMap(Rule::getId, rule -> rule.getDefaultConfiguration().getLevel()));
  }

  private static Set<Extension> getExtensions(Run run) {
    return Optional.of(run)
      .map(Run::getTool)
      .map(Tool::getExtensions)
      .orElse(emptySet());
  }

  private static boolean hasRuleDefinedLevel(@Nullable Rule rule) {
    return Optional.ofNullable(rule)
      .map(Rule::getDefaultConfiguration)
      .map(DefaultConfiguration::getLevel)
      .isPresent();
  }

}
