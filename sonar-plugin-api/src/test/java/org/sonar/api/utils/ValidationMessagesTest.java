/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.utils;

import org.junit.Test;
import org.slf4j.Logger;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.junit.internal.matchers.StringContains.containsString;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ValidationMessagesTest {

  @Test
  public void emptyMessages() {
    ValidationMessages messages = ValidationMessages.create();
    assertThat(messages.hasErrors(), is(false));
    assertThat(messages.hasWarnings(), is(false));
    assertThat(messages.hasInfos(), is(false));

    Logger logger = mock(Logger.class);
    messages.log(logger);
    verify(logger, never()).error(anyString());
    verify(logger, never()).warn(anyString());
    verify(logger, never()).info(anyString());
  }

  @Test
  public void addError() {
    ValidationMessages messages = ValidationMessages.create();
    messages.addErrorText("my error");
    assertThat(messages.hasErrors(), is(true));
    assertThat(messages.hasWarnings(), is(false));
    assertThat(messages.hasInfos(), is(false));
    assertThat(messages.getErrors().size(), is(1));
    assertThat(messages.getErrors(), hasItem("my error"));
    assertThat(messages.toString(), containsString("my error"));
    
    Logger logger = mock(Logger.class);
    messages.log(logger);
    verify(logger, times(1)).error("my error");
    verify(logger, never()).warn(anyString());
    verify(logger, never()).info(anyString());
  }
}
