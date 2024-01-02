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
package org.sonar.server.ws;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.db.protobuf.DbIssues;
import org.sonarqube.ws.Common;

public class MessageFormattingUtils {

  private MessageFormattingUtils() {
  }

  public static List<Common.MessageFormatting> dbMessageFormattingToWs(@Nullable DbIssues.MessageFormattings dbFormattings) {
    if (dbFormattings == null) {
      return List.of();
    }
    return dbMessageFormattingListToWs(dbFormattings.getMessageFormattingList());
  }

  public static List<Common.MessageFormatting> dbMessageFormattingListToWs(@Nullable List<DbIssues.MessageFormatting> dbFormattings) {
    if (dbFormattings == null) {
      return List.of();
    }
    return dbFormattings.stream()
      .map(f -> Common.MessageFormatting.newBuilder()
        .setStart(f.getStart())
        .setEnd(f.getEnd())
        .setType(Common.MessageFormattingType.valueOf(f.getType().name())).build())
      .toList();
  }

}
