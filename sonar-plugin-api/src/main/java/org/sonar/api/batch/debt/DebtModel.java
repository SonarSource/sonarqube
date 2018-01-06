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
package org.sonar.api.batch.debt;

import java.util.List;
import javax.annotation.CheckForNull;

/**
 * This class can be used to retrieve characteristics or sub-characteristics from the technical debt model during analysis.
 *
 * Unfortunately, this class cannot be used to set characteristic on {@link org.sonar.api.measures.Measure},
 * because the Measure API still uses deprecated {@code org.sonar.api.technicaldebt.batch.Characteristic}.
 *
 * @since 4.3
 * @deprecated since 5.1 debt model will soon be unavailable on batch side
 */
@Deprecated
public interface DebtModel {

  /**
   * Return only characteristics
   */
  List<DebtCharacteristic> characteristics();

  /**
   * Return sub-characteristics of a characteristic
   */
  List<DebtCharacteristic> subCharacteristics(String characteristicKey);

  /**
   * Return characteristics and sub-characteristics
   */
  List<DebtCharacteristic> allCharacteristics();

  /**
   * Return a characteristic or a sub-characteristic by a key
   */
  @CheckForNull
  DebtCharacteristic characteristicByKey(String key);

}
