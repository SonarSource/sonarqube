/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.dismissmessage.ws;

import javax.annotation.Nullable;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.server.ws.WsAction;

public interface DismissMessageWsAction extends WsAction {
  String PARAM_PROJECT_KEY = "projectKey";
  String PARAM_MESSAGE_TYPE = "messageType";

  static MessageType parseMessageType(String messageType) throws IllegalArgumentException {
    try {
      return MessageType.valueOf(messageType);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid message type: " + messageType);
    }
  }

  static void verifyProjectKeyAndMessageType(@Nullable String projectKey, MessageType type) {
    switch (type) {
      case GLOBAL_NCD_90, GLOBAL_NCD_PAGE_90 -> {
        if (projectKey != null) {
          throw new IllegalArgumentException("The 'projectKey' parameter is not expected for message type: " + type);
        }
      }
      case PROJECT_NCD_90, PROJECT_NCD_PAGE_90, BRANCH_NCD_90, UNRESOLVED_FINDINGS_IN_AI_GENERATED_CODE -> {
        if(projectKey == null) {
          throw new IllegalArgumentException("The 'projectKey' parameter is missing for message type: " + type);
        }
      }
      default -> throw new IllegalArgumentException("Unexpected message type: " + type);
    }
  }
}
