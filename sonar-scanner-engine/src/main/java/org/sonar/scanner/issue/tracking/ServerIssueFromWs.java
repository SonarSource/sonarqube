/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.issue.tracking;

import java.util.Date;
import javax.annotation.CheckForNull;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.issue.tracking.Trackable;

import static org.apache.commons.lang.StringUtils.trim;

public class ServerIssueFromWs implements Trackable {

  private org.sonar.scanner.protocol.input.ScannerInput.ServerIssue dto;

  public ServerIssueFromWs(org.sonar.scanner.protocol.input.ScannerInput.ServerIssue dto) {
    this.dto = dto;
  }

  public org.sonar.scanner.protocol.input.ScannerInput.ServerIssue getDto() {
    return dto;
  }

  public String key() {
    return dto.getKey();
  }

  @Override
  public RuleKey getRuleKey() {
    return RuleKey.of(dto.getRuleRepository(), dto.getRuleKey());
  }

  @Override
  @CheckForNull
  public String getLineHash() {
    return dto.hasChecksum() ? dto.getChecksum() : null;
  }

  @Override
  @CheckForNull
  public Integer getLine() {
    return dto.hasLine() ? dto.getLine() : null;
  }

  @Override
  public String getMessage() {
    return dto.hasMsg() ? trim(dto.getMsg()) : "";
  }

  @Override
  public String getStatus() {
    return dto.getStatus();
  }

  @Override
  public Date getCreationDate() {
    return DateUtils.longToDate(dto.getCreationDate());
  }

}
