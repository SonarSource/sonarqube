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
package com.sonarsource.branch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.MessageException;
import org.sonar.core.config.ScannerProperties;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.scan.BranchConfigurationValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class BranchConfigurationValidatorImplTest {
  private BranchConfigurationValidator validator;

  private GlobalConfiguration settings;

  private List<String> validationMessages = new ArrayList<>();

  @Before
  public void setUp() {
    validationMessages.clear();

    settings = mock(GlobalConfiguration.class);
    when(settings.get(anyString())).thenReturn(Optional.empty());

    validator = new BranchConfigurationValidatorImpl(settings);
  }

  @Test
  public void should_pass_when_no_branch_params_used() {
    validator.validate(validationMessages, null);
    assertThat(validationMessages).isEmpty();
  }

  @Test
  public void should_pass_when_only_new_branch_param_used() {
    String branchName = "dummyValidBranchName";
    when(settings.get(ScannerProperties.BRANCH_NAME)).thenReturn(Optional.of(branchName));
    validator.validate(validationMessages, null);
    assertThat(validationMessages).isEmpty();
  }

  @Test
  public void should_pass_when_only_old_branch_param_used() {
    String branchName = "dummyValidBranchName";
    validator.validate(validationMessages, branchName);
    assertThat(validationMessages).isEmpty();
  }

  @Test
  public void should_fail_when_both_old_and_new_branch_params_used() {
    String branchName = "dummyValidBranchName";
    when(settings.get(ScannerProperties.BRANCH_NAME)).thenReturn(Optional.of(branchName));
    validator.validate(validationMessages, branchName);
    assertThat(validationMessages).isNotEmpty();
  }

  @Test
  public void should_fail_when_branch_name_invalid() {
    String branchName = "invalid branch name $%^";
    when(settings.get(ScannerProperties.BRANCH_NAME)).thenReturn(Optional.of(branchName));
    validator.validate(validationMessages, null);
    assertThat(validationMessages).isNotEmpty();
  }
}
