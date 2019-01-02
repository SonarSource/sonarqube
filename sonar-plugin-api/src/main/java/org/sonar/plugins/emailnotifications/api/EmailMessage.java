/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.plugins.emailnotifications.api;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @since 2.10
 */
public class EmailMessage {

  private String from;
  private String to;
  private String subject;
  private String message;
  private String messageId;

  /**
   * @param from full name of user, who initiated this message or null, if message was initiated by Sonar
   */
  public EmailMessage setFrom(String from) {
    this.from = from;
    return this;
  }

  /**
   * @see #setFrom(String)
   */
  public String getFrom() {
    return from;
  }

  /**
   * @param to email address where to send this message
   */
  public EmailMessage setTo(String to) {
    this.to = to;
    return this;
  }

  /**
   * @see #setTo(String)
   */
  public String getTo() {
    return to;
  }

  /**
   * @param subject message subject
   */
  public EmailMessage setSubject(String subject) {
    this.subject = subject;
    return this;
  }

  /**
   * @see #setSubject(String)
   */
  public String getSubject() {
    return subject;
  }

  /**
   * @param message message body
   */
  public EmailMessage setMessage(String message) {
    this.message = message;
    return this;
  }

  /**
   * @see #setMessage(String)
   */
  public String getMessage() {
    return message;
  }

  /**
   * @param messageId id of message for threading
   */
  public EmailMessage setMessageId(String messageId) {
    this.messageId = messageId;
    return this;
  }

  /**
   * @see #setMessageId(String)
   */
  public String getMessageId() {
    return messageId;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

}
