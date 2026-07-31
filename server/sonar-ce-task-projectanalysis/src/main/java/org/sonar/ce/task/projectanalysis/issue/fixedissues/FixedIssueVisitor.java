/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.issue.fixedissues;

import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.issue.IssueVisitor;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonarsource.history.model.FixedIssueForHistory;

import static java.util.Optional.ofNullable;

public class FixedIssueVisitor extends IssueVisitor {
  private static final Logger LOG = LoggerFactory.getLogger(FixedIssueVisitor.class);

  private final FixedIssueForHistoryRepository fixedIssueForHistoryRepository;

  public FixedIssueVisitor(FixedIssueForHistoryRepository fixedIssueForHistoryRepository) {
    this.fixedIssueForHistoryRepository = fixedIssueForHistoryRepository;
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (issue.isBeingClosed()) {
      if (issue.creationDate() == null) {
        LOG.warn("Skipping ingestion of issue {} because creationDate is null", issue.key());
        return;
      }
      fixedIssueForHistoryRepository.addFixedIssue(toFixedIssueForHistory(issue));
    }
  }

  private static FixedIssueForHistory toFixedIssueForHistory(DefaultIssue issue) {
    return new FixedIssueForHistory(
      issue.key(),
      issue.projectUuid(),
      issue.creationDate().getTime(),
      getDetectionDate(issue),
      getCloseDate(issue),
      issue.type().name(),
      issue.getBranchUuid(),
      issue.severity(),
      effectiveImpacts(issue),
      getPreviousStatus(issue)
    );
  }

  private static Map<String, String> effectiveImpacts(DefaultIssue issue) {
    final Map<SoftwareQuality, Severity> impacts = new EnumMap<>(SoftwareQuality.class);
    impacts.putAll(issue.defaultRuleImpacts());
    impacts.putAll(issue.overriddenImpacts());
    return impacts.entrySet().stream()
      .collect(Collectors.toMap(
        entry -> entry.getKey().name(),
        entry -> entry.getValue().name()
      ));
  }

  private static long getCloseDate(DefaultIssue issue) {
    return ofNullable(issue.closeDate())
      .map(Date::getTime)
      .orElseGet(System::currentTimeMillis);
  }

  private static long getDetectionDate(DefaultIssue issue) {
    return ofNullable(issue.detectionDate())
      .map(Date::getTime)
      .orElse(issue.creationDate().getTime());
  }

  @CheckForNull
  private static String getPreviousStatus(DefaultIssue issue) {
    final @Nullable FieldDiffs change = issue.changes()
      .reversed()
      .stream()
      .filter(c -> c.diffs().containsKey("status"))
      .findFirst()
      .orElse(null);

    if (change == null) {
      return null;
    }

    return (String) change.diffs().get("status").oldValue();
  }
}
