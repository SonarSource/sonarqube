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

import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.WorkUnit;
import org.sonar.api.utils.internal.WorkDuration;

import java.io.Serializable;
import java.util.Date;

/**
 * @since 4.1
 * @deprecated since 4.3
 */
@Deprecated
public interface Requirement extends Serializable {

  Integer id();

  RuleKey ruleKey();

  Characteristic characteristic();

  Characteristic rootCharacteristic();

  String function();

  /**
   * @deprecated since 4.2
   */
  @Deprecated
  WorkUnit factor();

  /**
   * @since 4.2
   */
  int factorValue();

  /**
   * @since 4.2
   */
  WorkDuration.UNIT factorUnit();

  /**
   * @deprecated since 4.2
   */
  @Deprecated
  WorkUnit offset();

  /**
   * @since 4.2
   */
  int offsetValue();

  /**
   * @since 4.2
   */
  WorkDuration.UNIT offsetUnit();

  Date createdAt();

  Date updatedAt();

}
