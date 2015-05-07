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

package org.sonar.api.technicaldebt.batch;

import org.sonar.api.BatchSide;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;

import javax.annotation.CheckForNull;

import java.util.List;

/**
 * @since 4.1
 * @deprecated since 4.3
 */
@Deprecated
@BatchSide
public interface TechnicalDebtModel {

  @CheckForNull
  Characteristic characteristicById(Integer id);

  @CheckForNull
  Characteristic characteristicByKey(String key);

  /**
   * @deprecated since 4.3. Always return null
   */
  @CheckForNull
  @Deprecated
  Requirement requirementsByRule(RuleKey ruleKey);

  /**
   * @deprecated since 4.3. Always return null
   */
  @CheckForNull
  @Deprecated
  Requirement requirementsById(Integer id);

  /**
   * @deprecated since 4.3. Always return empty list
   */
  @Deprecated
  List<? extends Requirement> requirements();

  /**
   * @since 4.3
   */
  List<DefaultCharacteristic> characteristics();

}
