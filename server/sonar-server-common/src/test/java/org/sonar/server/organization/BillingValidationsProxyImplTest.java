/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

  private static final Organization ORGANIZATION = new Organization("ORGANIZATION_KEY", "ORGANIZATION_UUID", "ORGANIZATION_NAME");

  private BillingValidationsExtension billingValidationsExtension = mock(BillingValidationsExtension.class);

  private BillingValidationsProxyImpl underTest;

  @Test
  public void checkOnProjectAnalysis_calls_extension_when_available() {
    underTest = new BillingValidationsProxyImpl(billingValidationsExtension);

    underTest.checkBeforeProjectAnalysis(ORGANIZATION);

    verify(billingValidationsExtension).checkBeforeProjectAnalysis(ORGANIZATION);
  }

  @Test
  public void checkOnProjectAnalysis_does_nothing_when_no_extension_available() {
    underTest = new BillingValidationsProxyImpl();

    underTest.checkBeforeProjectAnalysis(ORGANIZATION);

    verifyZeroInteractions(billingValidationsExtension);
  }

  @Test
  public void checkCanUpdateProjectsVisibility_calls_extension_when_available() {
    underTest = new BillingValidationsProxyImpl(billingValidationsExtension);

    underTest.checkCanUpdateProjectVisibility(ORGANIZATION, true);

    verify(billingValidationsExtension).checkCanUpdateProjectVisibility(ORGANIZATION, true);
  }

  @Test
  public void checkCanUpdateProjectsVisibility_does_nothing_when_no_extension_available() {
    underTest = new BillingValidationsProxyImpl();

    underTest.checkCanUpdateProjectVisibility(ORGANIZATION, true);

    verifyZeroInteractions(billingValidationsExtension);
  }

  @Test
  public void canUpdateProjectsVisibilityToPrivate_calls_extension_when_available() {
    underTest = new BillingValidationsProxyImpl(billingValidationsExtension);
    when(billingValidationsExtension.canUpdateProjectVisibilityToPrivate(ORGANIZATION)).thenReturn(false);

    boolean result = underTest.canUpdateProjectVisibilityToPrivate(ORGANIZATION);

    assertThat(result).isFalse();
    verify(billingValidationsExtension).canUpdateProjectVisibilityToPrivate(ORGANIZATION);
  }

  @Test
  public void canUpdateProjectsVisibilityToPrivate_return_true_when_no_extension() {
    underTest = new BillingValidationsProxyImpl();

    boolean result = underTest.canUpdateProjectVisibilityToPrivate(ORGANIZATION);

    assertThat(result).isTrue();
    verifyZeroInteractions(billingValidationsExtension);
  }

}
