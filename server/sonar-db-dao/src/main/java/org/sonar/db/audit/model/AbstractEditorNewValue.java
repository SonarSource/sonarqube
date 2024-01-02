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
package org.sonar.db.audit.model;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public abstract class AbstractEditorNewValue extends NewValue {

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @Nullable
  protected String qualityGateUuid;
  @Nullable
  protected String qualityGateName;
  @Nullable
  protected String qualityProfileUuid;
  @Nullable
  protected String qualityProfileName;

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @CheckForNull
  public String getQualityGateUuid() {
    return this.qualityGateUuid;
  }

  @CheckForNull
  public String getQualityGateName() {
    return this.qualityGateName;
  }

  @CheckForNull
  public String getQualityProfileUuid() {
    return this.qualityProfileUuid;
  }

  @CheckForNull
  public String getQualityProfileName() {
    return this.qualityProfileName;
  }
}
