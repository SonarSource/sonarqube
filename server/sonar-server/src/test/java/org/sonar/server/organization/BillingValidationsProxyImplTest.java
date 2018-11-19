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
package org.sonar.server.organization;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.organization.BillingValidations.Organization;

public class BillingValidationsProxyImplTest {

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

  @Test
  public void checkCanUpdateProjectsVisibility_calls_extension_when_available() {
    underTest = new BillingValidationsProxyImpl(billingValidationsExtension);
    Organization organization = new Organization(ORGANIZATION_KEY, ORGANIZATION_UUID);

    underTest.checkCanUpdateProjectVisibility(organization, true);

    verify(billingValidationsExtension).checkCanUpdateProjectVisibility(organization, true);
  }

  @Test
  public void checkCanUpdateProjectsVisibility_does_nothing_when_no_extension_available() {
    underTest = new BillingValidationsProxyImpl();
    Organization organization = new Organization(ORGANIZATION_KEY, ORGANIZATION_UUID);

    underTest.checkCanUpdateProjectVisibility(organization, true);

    verifyZeroInteractions(billingValidationsExtension);
  }

  @Test
  public void canUpdateProjectsVisibilityToPrivate_calls_extension_when_available() {
    underTest = new BillingValidationsProxyImpl(billingValidationsExtension);
    Organization organization = new Organization(ORGANIZATION_KEY, ORGANIZATION_UUID);
    when(billingValidationsExtension.canUpdateProjectVisibilityToPrivate(organization)).thenReturn(false);

    boolean result = underTest.canUpdateProjectVisibilityToPrivate(organization);

    assertThat(result).isFalse();
    verify(billingValidationsExtension).canUpdateProjectVisibilityToPrivate(organization);
  }

  @Test
  public void canUpdateProjectsVisibilityToPrivate_return_true_when_no_extension() {
    underTest = new BillingValidationsProxyImpl();
    Organization organization = new Organization(ORGANIZATION_KEY, ORGANIZATION_UUID);

    boolean result = underTest.canUpdateProjectVisibilityToPrivate(organization);

    assertThat(result).isTrue();
    verifyZeroInteractions(billingValidationsExtension);
  }

}
