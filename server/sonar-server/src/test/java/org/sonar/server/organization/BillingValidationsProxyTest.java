/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.sonar.server.organization.BillingValidations.Organization;

public class BillingValidationsProxyTest {

  private static String ORGANIZATION_KEY = "ORGANIZATION_KEY";
  private static String ORGANIZATION_UUID = "ORGANIZATION_UUID";

  private BillingValidationsExtension billingValidationsExtension = mock(BillingValidationsExtension.class);

  private BillingValidationsProxyImpl underTest;

  @Test
  public void checkOnProjectAnalysis_calls_extension_when_available() {
    underTest = new BillingValidationsProxyImpl(billingValidationsExtension);

    Organization organization = new Organization(ORGANIZATION_KEY, ORGANIZATION_UUID);
    underTest.checkOnProjectAnalysis(organization);

    verify(billingValidationsExtension).checkOnProjectAnalysis(organization);
  }

  @Test
  public void checkOnProjectAnalysis_does_nothing_when_no_extension_available() {
    underTest = new BillingValidationsProxyImpl();

    Organization organization = new Organization(ORGANIZATION_KEY, ORGANIZATION_UUID);
    underTest.checkOnProjectAnalysis(organization);

    verifyZeroInteractions(billingValidationsExtension);
  }

}
