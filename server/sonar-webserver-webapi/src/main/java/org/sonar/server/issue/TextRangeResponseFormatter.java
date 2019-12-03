/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import java.util.Map;
import java.util.function.Consumer;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
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

  public Common.Location formatLocation(DbIssues.Location source, String issueComponent, Map<String, ComponentDto> componentsByUuid) {
    Common.Location.Builder target = Common.Location.newBuilder();
    if (source.hasMsg()) {
      target.setMsg(source.getMsg());
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
