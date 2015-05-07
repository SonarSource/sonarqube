/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.server.util;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.sonar.api.ServerSide;
import org.sonar.server.exceptions.BadRequestException;

import javax.annotation.Nullable;

import java.util.List;

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

  private TypeValidation findByKey(final String key) {
    TypeValidation typeValidation = Iterables.find(typeValidationList, new Predicate<TypeValidation>() {
      @Override
      public boolean apply(TypeValidation input) {
        return input.key().equals(key);
      }
    }, null);
    if (typeValidation == null) {
      throw new BadRequestException(String.format("Type '%s' is not valid.", key));
    }
    return typeValidation;
  }
}
