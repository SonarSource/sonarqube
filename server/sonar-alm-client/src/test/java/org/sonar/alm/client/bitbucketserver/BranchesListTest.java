/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.alm.client.bitbucketserver;

import java.util.Optional;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchesListTest {

  @Test
  public void findDefaultBranch_givenNoBranches_returnEmptyOptional(){
    BranchesList branchesList = new BranchesList();

    Optional<Branch> defaultBranch = branchesList.findDefaultBranch();

    assertThat(defaultBranch).isNotPresent();
  }

  @Test
  public void findDefaultBranch_givenBranchesWithoutDefaultOne_returnEmptyOptional(){
    BranchesList branchesList = new BranchesList();
    branchesList.addBranch(new Branch("1", false));
    branchesList.addBranch(new Branch("2", false));

    Optional<Branch> defaultBranch = branchesList.findDefaultBranch();

    assertThat(defaultBranch).isNotPresent();
  }

  @Test
  public void findDefaultBranch_givenBranchesWithDefaultOne_returnOptionalWithThisBranch(){
    BranchesList branchesList = new BranchesList();
    branchesList.addBranch(new Branch("1", false));
    branchesList.addBranch(new Branch("2", false));
    branchesList.addBranch(new Branch("default", true));

    Optional<Branch> defaultBranchOptional = branchesList.findDefaultBranch();

    assertThat(defaultBranchOptional).isPresent();
    assertThat(defaultBranchOptional.get().isDefault()).isTrue();
    assertThat(defaultBranchOptional.get().getName()).isEqualTo("default");
  }
}
