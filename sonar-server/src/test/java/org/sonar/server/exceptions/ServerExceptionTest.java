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

package org.sonar.server.exceptions;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class ServerExceptionTest {

  @Test
  public void should_create_exception_with_status(){
    ServerException exception = new ServerException(400);
    assertThat(exception.httpCode()).isEqualTo(400);
  }

  @Test
  public void should_create_exception_with_status_and_message(){
    ServerException exception = new ServerException(404, "Not found");
    assertThat(exception.httpCode()).isEqualTo(404);
    assertThat(exception.getMessage()).isEqualTo("Not found");
  }

  @Test
  public void should_create_exception_with_status_and_l10n_message_with_param(){
    ServerException exception = new ServerException(404, null, "key", new String[]{"value"});
    assertThat(exception.httpCode()).isEqualTo(404);
    assertThat(exception.l10nKey()).isEqualTo("key");
    assertThat(exception.l10nParams()).containsOnly("value");
  }

  @Test
  public void should_create_exception_with_status_and_l10n_message_without_param(){
    ServerException exception = new ServerException(404, null, "key", null);
    assertThat(exception.httpCode()).isEqualTo(404);
    assertThat(exception.l10nKey()).isEqualTo("key");
    assertThat(exception.l10nParams()).isEmpty();
  }
}
