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
package org.sonar.server.email.ws;

import com.google.common.base.Throwables;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.mail.EmailException;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.user.UserSession;

public class SendAction implements EmailsWsAction {

  private static final String PARAM_TO = "to";
  private static final String PARAM_SUBJECT = "subject";
  private static final String PARAM_MESSAGE = "message";

  private final UserSession userSession;
  private final EmailNotificationChannel emailNotificationChannel;

  public SendAction(UserSession userSession, EmailNotificationChannel emailNotificationChannel) {
    this.userSession = userSession;
    this.emailNotificationChannel = emailNotificationChannel;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("send")
      .setDescription("Test email configuration by sending an email<br>" +
        "Requires 'Administer System' permission.")
      .setSince("6.1")
      .setInternal(true)
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_TO)
      .setDescription("Email address")
      .setExampleValue("john@doo.com")
      .setRequired(true);

    action.createParam(PARAM_SUBJECT)
      .setDescription("Subject of the email")
      .setExampleValue("Test Message from SonarQube");

    action.createParam(PARAM_MESSAGE)
      .setDescription("Content of the email")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();
    try {
      emailNotificationChannel.sendTestEmail(request.mandatoryParam(PARAM_TO), request.param(PARAM_SUBJECT), request.mandatoryParam(PARAM_MESSAGE));
    } catch (EmailException emailException) {
      throw createBadRequestException(emailException);
    }
    response.noContent();
  }

  private static BadRequestException createBadRequestException(EmailException emailException) {
    List<String> messages = Throwables.getCausalChain(emailException)
      .stream()
      .map(Throwable::getMessage)
      .collect(Collectors.toList());
    Collections.reverse(messages);
    return BadRequestException.create(messages);
  }

}
