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
package org.sonar.core.properties;

import org.sonar.api.utils.System2;
import org.sonar.db.MyBatis;

/**
 * Kept for backward compatibility of plugins/libs (like sonar-license) that are directly calling classes from the core
 *
 * @deprecated since 5.2, should be replaced by {@link org.sonar.db.property.PropertiesDao}
 */
@Deprecated
public class PropertiesDao extends org.sonar.db.property.PropertiesDao {

  public PropertiesDao(MyBatis mybatis, System2 system2) {
    super(mybatis, system2);
  }

  public void setProperty(PropertyDto property) {
    super.saveProperty(property);
  }
}
