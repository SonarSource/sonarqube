/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.server.debt;

import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

/**
 * @since 4.3
 * @deprecated in 5.2. It will be dropped in version 6.0 (see https://jira.sonarsource.com/browse/SONAR-6393)
 */
@ServerSide
@ComputeEngineSide
@Deprecated
public interface DebtModel {

  /**
   * @return an empty list since 5.5.
   */
  List<DebtCharacteristic> allCharacteristics();

  /**
   * @return an empty list since 5.5.
   */
  List<DebtCharacteristic> characteristics();

  /**
   * @return null since 5.5.
   */
  @CheckForNull
  DebtCharacteristic characteristicByKey(String key);

}
