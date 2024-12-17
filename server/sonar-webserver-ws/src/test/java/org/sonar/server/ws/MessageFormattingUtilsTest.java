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

import org.junit.Test;
import org.sonar.db.protobuf.DbIssues;
import org.sonarqube.ws.Common;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageFormattingUtilsTest {

  @Test
  public void nullFormattingShouldBeEmptyList() {
    assertThat(MessageFormattingUtils.dbMessageFormattingToWs(null)).isEmpty();
  }

  @Test
  public void nullFormattingListShouldBeEmptyList() {
    assertThat(MessageFormattingUtils.dbMessageFormattingListToWs(null)).isEmpty();
  }

  @Test
  public void singleEntryShouldBeIdentical() {
    DbIssues.MessageFormattings formattings = DbIssues.MessageFormattings.newBuilder()
      .addMessageFormatting(DbIssues.MessageFormatting.newBuilder()
        .setStart(0)
        .setEnd(4)
        .setType(DbIssues.MessageFormattingType.CODE)
        .build())
      .build();

    assertThat(MessageFormattingUtils.dbMessageFormattingToWs(
      formattings).get(0))
        .extracting(Common.MessageFormatting::getStart, Common.MessageFormatting::getEnd, Common.MessageFormatting::getType)
        .containsExactly(0, 4, Common.MessageFormattingType.CODE);
  }

}
