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
package org.sonar.server.organization;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

public class BillingValidationsProxyImpl implements BillingValidationsProxy {

  @Nullable
  private final BillingValidationsExtension billingValidationsExtension;

  public BillingValidationsProxyImpl() {
    this.billingValidationsExtension = null;
  }

  @Autowired(required = false)
  public BillingValidationsProxyImpl(BillingValidationsExtension e) {
    this.billingValidationsExtension = e;
  }

  @Override
  public void checkBeforeAddMember(Organization organization, User user) {
    if (billingValidationsExtension == null) {
      return;
    }
    billingValidationsExtension.checkBeforeAddMember(organization, user);
  }

  @Override
  public void checkBeforeProjectAnalysis(Organization organization) {
    if (billingValidationsExtension == null) {
      return;
    }
    billingValidationsExtension.checkBeforeProjectAnalysis(organization);
  }

  @Override
  public void checkCanUpdateProjectVisibility(Organization organization, boolean updateToPrivate) {
    if (billingValidationsExtension == null) {
      return;
    }
    billingValidationsExtension.checkCanUpdateProjectVisibility(organization, updateToPrivate);
  }

  @Override
  public boolean canUpdateProjectVisibilityToPrivate(Organization organization) {
    return billingValidationsExtension == null || billingValidationsExtension.canUpdateProjectVisibilityToPrivate(organization);
  }

  @Override
  public void onDelete(Organization organization) {
    if (billingValidationsExtension == null) {
      return;
    }
    billingValidationsExtension.onDelete(organization);
  }
}
