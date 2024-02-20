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
package org.sonar.auth.github;

import java.util.*;
import org.junit.*;
import org.junit.runner.*;
import static org.junit.Assert.*;


public class GithubBindingTest {

	  @Test
	  public void testGsonApp() {
		GithubBinding.Permissions permission = new GithubBinding.Permissions();
		GithubBinding.GsonApp underTest = new GithubBinding.GsonApp(123, permission);
		assertEquals(underTest.getInstallationsCount(), 123);
		assertEquals(underTest.getPermissions(), permission);
	  }
 
	  @Test
	  public void testGsonInstallations() {
		GithubBinding.GsonInstallations underTest = new GithubBinding.GsonInstallations();
		assertEquals(underTest.getTotalCount(), 0);
		assertEquals(underTest.getInstallations(), null);
	  }
	 
	  @Test
	  public void testGsonInstallation() {
		GithubBinding.Permissions permission = new GithubBinding.Permissions();
		GithubBinding.GsonInstallation.GsonAccount account = new GithubBinding.GsonInstallation.GsonAccount();
		GithubBinding.GsonInstallation underTest = new GithubBinding.GsonInstallation(1, "target", permission, account, "suspended");
		assertEquals(underTest.getId(), 1);
		assertEquals(underTest.getTargetType(), "target");
		assertEquals(underTest.getPermissions(), permission);
		assertEquals(underTest.getAccount(), account);
		assertEquals(underTest.getSuspendedAt(), "suspended");
		assertEquals(account.getId(), 0);
		assertEquals(account.getLogin(), null);
		assertEquals(account.getType(), null);
	  }
	  
	  @Test
	  public void testPermissions() {
		GithubBinding.Permissions permission = new GithubBinding.Permissions("checks", "mridhula", "mridhula@gmail.com", 
				"Hello world", "metadata", "mridhula", "uci");
		
		assertEquals(permission.getMembers(), "mridhula");
		assertEquals(permission.getChecks(), "checks");
		assertEquals(permission.getEmails(), "mridhula@gmail.com");
		assertEquals(permission.getContents(), "Hello world");
		assertEquals(permission.getMetadata(), "metadata");
		assertEquals(permission.getRepoAdministration(), "mridhula");
		assertEquals(permission.getOrgAdministration(), "uci");
		GithubBinding.Permissions.Builder builder = permission.builder();
		builder = builder.setChecks("check");
		builder = builder.setMembers("memeber");
		builder = builder.setEmails("email");
		builder = builder.setContents("content");
		builder = builder.setMetadata("metadata");
		builder = builder.setRepoAdministration("admin");
		builder = builder.setOrgAdministration("admin");
		GithubBinding.Permissions per = builder.build();
		assertEquals(per.getOrgAdministration(), "admin");
		
	  }
  
	  @Test
	  public void testGsonRepositorySearch() {
		GithubBinding.GsonRepositorySearch underTest = new GithubBinding.GsonRepositorySearch();
		assertEquals(underTest.getTotalCount(), 0);
		assertEquals(underTest.getItems(), null);
	  }
	  
	  @Test
	  public void testGsonGithubRepository() {
		GithubBinding.GsonGithubRepository underTest = new GithubBinding.GsonGithubRepository();
		assertThat(underTest.toRepository().getId(), null);
	  }
	  
	  @Test
	  public void testGsonGithubCodeScanningAlert() {
		GithubBinding.GsonGithubCodeScanningAlert underTest = new GithubBinding.GsonGithubCodeScanningAlert();
		assertEquals(underTest.getId(), null);
		assertEquals(underTest.getState(), null);
		assertEquals(underTest.getDismissedReason(), null);
		assertEquals(underTest.getDismissedComment(), null);
		assertEquals(underTest.getTool(), null);
		assertEquals(underTest.getMostRecentInstance(), null);
		assertEquals(underTest.getMessageText(), null);
	  }

	  @Test
	  public void testGsonGithubCodeScanningAlert() {
		GithubBinding.GsonGithubCodeScanningAlert underTest = new GithubBinding.GsonGithubCodeScanningAlert();
		assertEquals(underTest.getId(), null);
		assertEquals(underTest.getState(), null);
		assertEquals(underTest.getDismissedReason(), null);
		assertEquals(underTest.getDismissedComment(), null);
		assertEquals(underTest.getTool(), null);
		assertEquals(underTest.getMostRecentInstance(), null);
		assertEquals(underTest.getMessageText(), null);
	  }
}

