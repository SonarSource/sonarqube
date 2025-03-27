/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.issue;

import java.util.Collection;
import java.util.Map;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.core.rule.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.rule.internal.ImpactMapper;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.rule.ImpactSeverityMapper;
import org.sonar.core.rule.RuleTypeMapper;
import org.sonar.server.issue.workflow.IsUnResolved;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sonar.db.permission.ProjectPermission.ISSUE_ADMIN;

@ServerSide
public class SetSeverityAction extends Action {

  public static final String SET_SEVERITY_KEY = "set_severity";
  public static final String SEVERITY_PARAMETER = "severity";

  private final IssueFieldsSetter issueUpdater;
  private final UserSession userSession;

  public SetSeverityAction(IssueFieldsSetter issueUpdater, UserSession userSession) {
    super(SET_SEVERITY_KEY);
    this.issueUpdater = issueUpdater;
    this.userSession = userSession;
    super.setConditions(new IsUnResolved(), this::isCurrentUserIssueAdminAndNotSecurityHotspot);
  }

  private boolean isCurrentUserIssueAdminAndNotSecurityHotspot(Issue issue) {
    DefaultIssue defaultIssue = (DefaultIssue) issue;
    return (defaultIssue.type() != RuleType.SECURITY_HOTSPOT && userSession.hasComponentUuidPermission(ISSUE_ADMIN, issue.projectUuid()));
  }

  @Override
  public boolean verify(Map<String, Object> properties, Collection<DefaultIssue> issues, UserSession userSession) {
    verifySeverityParameter(properties);
    return true;
  }

  @Override
  public boolean execute(Map<String, Object> properties, Context context) {
    String severity = verifySeverityParameter(properties);
    boolean updated = issueUpdater.setManualSeverity(context.issue(), severity, context.issueChangeContext());

    SoftwareQuality softwareQuality = ImpactMapper.convertToSoftwareQuality(RuleTypeMapper.toApiRuleType(context.issue().type()));
    if (updated
      && context.issueDto().getEffectiveImpacts().containsKey(softwareQuality)) {
      createImpactsIfMissing(context.issue(), context.issueDto().getEffectiveImpacts());
      issueUpdater.setImpactManualSeverity(context.issue(), softwareQuality, ImpactSeverityMapper.mapImpactSeverity(severity), context.issueChangeContext());
    }
    return updated;
  }

  private static void createImpactsIfMissing(DefaultIssue issue, Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> effectiveImpacts) {
    if (issue.impacts().isEmpty()) {
      issue.replaceImpacts(effectiveImpacts);
      issue.setChanged(true);
    }
  }

  @Override
  public boolean shouldRefreshMeasures() {
    return true;
  }

  private static String verifySeverityParameter(Map<String, Object> properties) {
    String param = (String) properties.get(SEVERITY_PARAMETER);
    checkArgument(!isNullOrEmpty(param), "Missing parameter : '%s'", SEVERITY_PARAMETER);
    return param;
  }
}
