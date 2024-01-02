/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import javax.annotation.Nullable;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.sarif.Driver;
import org.sonar.core.sarif.Result;
import org.sonar.core.sarif.Run;
import org.sonar.core.sarif.Tool;

import static java.util.stream.Collectors.toList;
import static org.sonar.api.utils.Preconditions.checkArgument;

@ScannerSide
public class RunMapper {
  private static final Logger LOG = Loggers.get(RunMapper.class);

  private final ResultMapper resultMapper;

  RunMapper(ResultMapper resultMapper) {
    this.resultMapper = resultMapper;
  }

  List<NewExternalIssue> mapRun(Run run) {
    String driverName = getToolDriverName(run);
    Map<String, String> ruleSeveritiesByRuleId = RulesSeverityDetector.detectRulesSeverities(run, driverName);

    return run.getResults()
      .stream()
      .map(result -> toNewExternalIssue(driverName, ruleSeveritiesByRuleId.get(result.getRuleId()), result))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(toList());
  }

  private static String getToolDriverName(Run run) throws IllegalArgumentException {
    checkArgument(hasToolDriverNameDefined(run), "The run does not have a tool driver name defined.");
    return run.getTool().getDriver().getName();
  }

  private Optional<NewExternalIssue> toNewExternalIssue(String driverName, @Nullable String ruleSeverity, Result result) {
    try {
      return Optional.of(resultMapper.mapResult(driverName, ruleSeverity, result));
    } catch (Exception exception) {
      LOG.warn("Failed to import an issue raised by tool {}, error: {}", driverName, exception.getMessage());
      return Optional.empty();
    }
  }

  private static boolean hasToolDriverNameDefined(Run run) {
    return Optional.ofNullable(run)
      .map(Run::getTool)
      .map(Tool::getDriver)
      .map(Driver::getName)
      .isPresent();
  }
}
