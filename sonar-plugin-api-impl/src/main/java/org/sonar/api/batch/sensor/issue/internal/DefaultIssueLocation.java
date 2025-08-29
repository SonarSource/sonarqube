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
package org.sonar.api.batch.sensor.issue.internal;

import java.util.ArrayList;
import java.util.List;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.batch.sensor.issue.MessageFormatting;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.issue.NewMessageFormatting;
import org.sonar.api.issue.Issue;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.commons.lang3.Strings.CS;
import static org.sonar.api.utils.Preconditions.checkArgument;
import static org.sonar.api.utils.Preconditions.checkState;

public class DefaultIssueLocation implements NewIssueLocation, IssueLocation {

  private static final String NULL_CHARACTER = "\u0000";
  private static final String NULL_REPLACEMENT = "[NULL]";

  private InputComponent component;
  private TextRange textRange;
  private String message;
  private final List<MessageFormatting> messageFormattings = new ArrayList<>();

  @Override
  public DefaultIssueLocation on(InputComponent component) {
    checkArgument(component != null, "Component can't be null");
    checkState(this.component == null, "on() already called");
    this.component = component;
    return this;
  }

  @Override
  public DefaultIssueLocation at(TextRange location) {
    checkState(this.component != null, "at() should be called after on()");
    checkState(this.component.isFile(), "at() should be called only for an InputFile.");
    DefaultInputFile file = (DefaultInputFile) this.component;
    file.validate(location);
    this.textRange = location;
    return this;
  }

  @Override
  public DefaultIssueLocation message(String message) {
    validateMessage(message);
    String sanitizedMessage = sanitizeNulls(message);
    this.message = abbreviate(trim(sanitizedMessage), Issue.MESSAGE_MAX_SIZE);
    return this;
  }

  @Override
  public DefaultIssueLocation message(String message, List<NewMessageFormatting> newMessageFormattings) {
    validateMessage(message);
    validateFormattings(newMessageFormattings, message);
    String sanitizedMessage = sanitizeNulls(message);
    this.message = abbreviate(sanitizedMessage, Issue.MESSAGE_MAX_SIZE);

    for (NewMessageFormatting newMessageFormatting : newMessageFormattings) {
      DefaultMessageFormatting messageFormatting = (DefaultMessageFormatting) newMessageFormatting;
      if (messageFormatting.start() >  Issue.MESSAGE_MAX_SIZE) {
        continue;
      }
      if (messageFormatting.end() > Issue.MESSAGE_MAX_SIZE) {
        messageFormatting = new DefaultMessageFormatting()
          .start(messageFormatting.start())
          .end( Issue.MESSAGE_MAX_SIZE)
          .type(messageFormatting.type());
      }
      messageFormattings.add(messageFormatting);
    }
    return this;
  }

  private static void validateFormattings(List<NewMessageFormatting> newMessageFormattings, String message) {
    checkArgument(newMessageFormattings != null, "messageFormattings can't be null");
    newMessageFormattings.stream()
      .map(DefaultMessageFormatting.class::cast)
      .forEach(e -> e.validate(message));
  }

  private static void validateMessage(String message) {
    requireNonNull(message, "Message can't be null");
  }

  private static String sanitizeNulls(String message) {
    return CS.replace(message, NULL_CHARACTER, NULL_REPLACEMENT);
  }

  @Override
  public NewMessageFormatting newMessageFormatting() {
    return new DefaultMessageFormatting();
  }

  @Override
  public InputComponent inputComponent() {
    return this.component;
  }

  @Override
  public TextRange textRange() {
    return textRange;
  }

  @Override
  public String message() {
    return this.message;
  }

  @Override
  public List<MessageFormatting> messageFormattings() {
    return this.messageFormattings;
  }

}
