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
package org.sonar.server.exceptions;

  import com.google.common.base.MoreObjects;
  import java.util.List;

  import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
  import static java.util.Collections.singletonList;

/**
 * Provided request is not valid due to Permission template matching key collision.
 */
public class TemplateMatchingKeyException extends ServerException {
  private final transient List<String> errors;

  public TemplateMatchingKeyException(String errorMessage) {
    super(HTTP_BAD_REQUEST, errorMessage);
    this.errors = singletonList(errorMessage);
  }

  public List<String> errors() {
    return this.errors;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("errors", this.errors())
      .toString();
  }

}


