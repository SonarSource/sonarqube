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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.server.ws.MessageFormattingUtils;
import org.sonarqube.ws.Common;

import static java.util.Optional.ofNullable;

public class TextRangeResponseFormatter {

  public void formatTextRange(IssueDto dto, Consumer<Common.TextRange> rangeConsumer) {
    DbIssues.Locations locations = dto.parseLocations();
    if (locations == null) {
      return;
    }
    formatTextRange(locations, rangeConsumer);
  }

  public void formatTextRange(DbIssues.Locations locations, Consumer<Common.TextRange> rangeConsumer) {
    if (locations.hasTextRange()) {
      DbCommons.TextRange textRange = locations.getTextRange();
      rangeConsumer.accept(convertTextRange(textRange).build());
    }
  }

  public List<Common.Flow> formatFlows(DbIssues.Locations locations, String issueComponent, Map<String, ComponentDto> componentsByUuid) {
    return locations.getFlowList().stream().map(flow -> {
      Common.Flow.Builder targetFlow = Common.Flow.newBuilder();
      for (DbIssues.Location flowLocation : flow.getLocationList()) {
        targetFlow.addLocations(formatLocation(flowLocation, issueComponent, componentsByUuid));
      }
      if (flow.hasDescription()) {
        targetFlow.setDescription(flow.getDescription());
      }
      if (flow.hasType()) {
        convertFlowType(flow.getType()).ifPresent(targetFlow::setType);
      }
      return targetFlow.build();
    }).toList();
  }

  private static Optional<Common.FlowType> convertFlowType(DbIssues.FlowType flowType) {
    switch (flowType) {
      case DATA:
        return Optional.of(Common.FlowType.DATA);
      case EXECUTION:
        return Optional.of(Common.FlowType.EXECUTION);
      case UNDEFINED:
        // we should only get this value if no type was set (since it's the default value of the enum), in which case this method shouldn't be called.
      default:
        throw new IllegalArgumentException("Unrecognized flow type: " + flowType);
    }
  }

  public Common.Location formatLocation(DbIssues.Location source, String issueComponent, Map<String, ComponentDto> componentsByUuid) {
    Common.Location.Builder target = Common.Location.newBuilder();
    if (source.hasMsg()) {
      target.setMsg(source.getMsg());
      target.addAllMsgFormattings(MessageFormattingUtils.dbMessageFormattingListToWs(source.getMsgFormattingList()));
    }
    if (source.hasTextRange()) {
      DbCommons.TextRange sourceRange = source.getTextRange();
      Common.TextRange.Builder targetRange = convertTextRange(sourceRange);
      target.setTextRange(targetRange);
    }
    if (source.hasComponentId()) {
      ofNullable(componentsByUuid.get(source.getComponentId())).ifPresent(c -> target.setComponent(c.getKey()));
    } else {
      target.setComponent(issueComponent);
    }
    return target.build();
  }

  private static Common.TextRange.Builder convertTextRange(DbCommons.TextRange sourceRange) {
    Common.TextRange.Builder targetRange = Common.TextRange.newBuilder();
    if (sourceRange.hasStartLine()) {
      targetRange.setStartLine(sourceRange.getStartLine());
    }
    if (sourceRange.hasStartOffset()) {
      targetRange.setStartOffset(sourceRange.getStartOffset());
    }
    if (sourceRange.hasEndLine()) {
      targetRange.setEndLine(sourceRange.getEndLine());
    }
    if (sourceRange.hasEndOffset()) {
      targetRange.setEndOffset(sourceRange.getEndOffset());
    }
    return targetRange;
  }
}
