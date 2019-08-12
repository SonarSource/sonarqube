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
package org.sonar.server.util;

import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.PropertyType;

import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public class BooleanTypeValidation implements TypeValidation {

  @Override
  public String key() {
    return PropertyType.BOOLEAN.name();
  }

  @Override
  public void validate(String value, @Nullable List<String> options) {
    checkRequest(StringUtils.equalsIgnoreCase(value, "true") || StringUtils.equalsIgnoreCase(value, "false"),
      "Value '%s' must be one of \"true\" or \"false\".", value);
  }

}
