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
package org.sonar.server.util;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.server.exceptions.BadRequestException;

import static java.lang.String.format;

@ServerSide
public class TypeValidations {

  private final List<TypeValidation> typeValidationList;

  public TypeValidations(List<TypeValidation> typeValidationList) {
    this.typeValidationList = typeValidationList;
  }

  public void validate(List<String> values, String type, List<String> options) {
    TypeValidation typeValidation = findByKey(type);
    for (String value : values) {
      typeValidation.validate(value, options);
    }
  }

  public void validate(String value, String type, @Nullable List<String> options) {
    TypeValidation typeValidation = findByKey(type);
    typeValidation.validate(value, options);
  }

  private TypeValidation findByKey(String key) {
    return typeValidationList.stream().filter(input -> input.key().equals(key)).findFirst()
      .orElseThrow(() -> BadRequestException.create(format("Type '%s' is not valid.", key)));
  }
}
