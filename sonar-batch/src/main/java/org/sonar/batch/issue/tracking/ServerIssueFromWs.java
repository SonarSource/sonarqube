/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.issue.tracking;

import org.sonar.api.rule.RuleKey;

public class ServerIssueFromWs implements ServerIssue {

  private org.sonar.batch.protocol.input.BatchInput.ServerIssue dto;

  public ServerIssueFromWs(org.sonar.batch.protocol.input.BatchInput.ServerIssue dto) {
    this.dto = dto;
  }

  public org.sonar.batch.protocol.input.BatchInput.ServerIssue getDto() {
    return dto;
  }

  @Override
  public String key() {
    return dto.getKey();
  }

  @Override
  public RuleKey ruleKey() {
    return RuleKey.of(dto.getRuleRepository(), dto.getRuleKey());
  }

  @Override
  public String checksum() {
    return dto.hasChecksum() ? dto.getChecksum() : null;
  }

  @Override
  public Integer line() {
    return dto.hasLine() ? dto.getLine() : null;
  }

  @Override
  public String message() {
    return dto.hasMsg() ? dto.getMsg() : null;
  }

}
