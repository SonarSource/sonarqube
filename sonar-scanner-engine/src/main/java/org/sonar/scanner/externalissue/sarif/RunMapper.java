/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.util.Optional;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.sarif.Result;
import org.sonar.core.sarif.Run;

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
    String driverName = getToolDriverNameOrThrow(run);
    return run.getResults().stream()
      .map(result -> toNewExternalIssue(driverName, result))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(toList());
  }

  private static String getToolDriverNameOrThrow(Run run) {
    checkArgument(run.getTool() != null
        && run.getTool().getDriver() != null
        && run.getTool().getDriver().getName() != null,
      "The run does not have a tool driver name defined.");
    return run.getTool().getDriver().getName();
  }

  private Optional<NewExternalIssue> toNewExternalIssue(String driverName, Result result) {
    try {
      return Optional.of(resultMapper.mapResult(driverName, result));
    } catch (Exception exception) {
      LOG.warn("Failed to import an issue raised by tool {}, error: {}", driverName, exception.getMessage());
      return Optional.empty();
    }
  }

}
