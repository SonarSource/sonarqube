/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.PropertyType;

import javax.annotation.Nullable;
import java.util.List;

import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public class KeyValueMapValidation implements TypeValidation {

    private static final String KEY_VALUE_DELIMITER = "=";

    @Override
    public String key() {
        return PropertyType.KEY_VALUE_MAP.name();
    }

    @Override
    public void validate(String value, @Nullable List<String> options) {
        String[] properties = value.split("\\s*;\\s*");
        for (String property : properties) {
            String key = property.substring(0, property.indexOf(KEY_VALUE_DELIMITER));
            String val = property.substring(property.indexOf(KEY_VALUE_DELIMITER) + 1);
            checkRequest(!StringUtils.isBlank(key) && !StringUtils.isBlank(val), "Validation failed. Non empty key and value must be provided");
        }
    }

}