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
package org.sonar.server.edition.ws;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.edition.EditionManagementState;
import org.sonar.server.edition.EditionManagementState.PendingStatus;
import org.sonar.server.edition.License;
import org.sonar.server.edition.MutableEditionManagementState;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.license.LicenseCommit;
import org.sonar.server.platform.WebServer;
import org.sonar.server.plugins.edition.EditionInstaller;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Editions;
import org.sonarqube.ws.Editions.StatusResponse;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.AUTOMATIC_IN_PROGRESS;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.NONE;

@RunWith(DataProviderRunner.class)
public class ApplyLicenseActionTest {
  private static final String PARAM_LICENSE = "license";
  private static final String PENDING_EDITION_NAME = "developer-edition";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private EditionInstaller editionInstaller = mock(EditionInstaller.class);
  private MutableEditionManagementState mutableEditionManagementState = mock(MutableEditionManagementState.class);
  private LicenseCommit licenseCommit = mock(LicenseCommit.class);
  private WebServer webServer = mock(WebServer.class);
  private ApplyLicenseAction underTest = new ApplyLicenseAction(userSessionRule, mutableEditionManagementState, editionInstaller,
    webServer, licenseCommit);
  private WsActionTester actionTester = new WsActionTester(underTest);

  @Test
  public void verify_definition() {
    WebService.Action def = actionTester.getDef();

    assertThat(def.key()).isEqualTo("apply_license");
    assertThat(def.since()).isEqualTo("6.7");
    assertThat(def.isPost()).isTrue();
    assertThat(def.isInternal()).isFalse();
    assertThat(def.description()).isNotEmpty();
    assertThat(def.params()).hasSize(1);

    WebService.Param licenseParam = def.param("license");
    assertThat(licenseParam.isRequired()).isTrue();
    assertThat(licenseParam.description()).isNotEmpty();
  }

  @Test
  public void request_fails_if_user_not_logged_in() {
    userSessionRule.anonymous();
    TestRequest request = actionTester.newRequest();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    request.execute();
  }

  @Test
  public void request_fails_if_user_is_not_system_administer() {
    userSessionRule.logIn();
    TestRequest request = actionTester.newRequest();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    request.execute();
  }

  @Test
  public void request_fails_if_license_param_is_not_provided() {
    userSessionRule.logIn().setSystemAdministrator();
    when(mutableEditionManagementState.getPendingInstallationStatus()).thenReturn(NONE);
    TestRequest request = actionTester.newRequest();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'license' parameter is missing");

    request.execute();
  }

  @Test
  @UseDataProvider("notNonePendingInstallationStatuses")
  public void request_fails_with_BadRequestException_is_pendingStatus_is_not_NONE(EditionManagementState.PendingStatus notNone) {
    userSessionRule.logIn().setSystemAdministrator();
    when(mutableEditionManagementState.getCurrentEditionKey()).thenReturn(Optional.empty());
    when(mutableEditionManagementState.getPendingEditionKey()).thenReturn(Optional.empty());
    when(mutableEditionManagementState.getPendingInstallationStatus()).thenReturn(notNone);
    TestRequest request = actionTester.newRequest()
      .setParam(PARAM_LICENSE, "foo");

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Can't apply a license when applying one is already in progress");

    request.execute();
  }

  @Test
  public void request_fails_with_BadRequestException_if_license_is_invalid() {
    userSessionRule.logIn().setSystemAdministrator();
    TestRequest request = actionTester.newRequest()
      .setParam(PARAM_LICENSE, "invalid");
    when(mutableEditionManagementState.getPendingInstallationStatus()).thenReturn(NONE);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The license provided is invalid");
    request.execute();
  }

  @Test
  public void request_fails_with_ISE_if_is_cluster_and_license_plugin_is_not_installed() throws IOException {
    underTest = new ApplyLicenseAction(userSessionRule, mutableEditionManagementState, editionInstaller, webServer, null);
    actionTester = new WsActionTester(underTest);
    userSessionRule.logIn().setSystemAdministrator();

    when(mutableEditionManagementState.getPendingInstallationStatus()).thenReturn(NONE);
    when(webServer.isStandalone()).thenReturn(false);

    TestRequest request = actionTester.newRequest().setParam(PARAM_LICENSE, createLicenseParam("dev", "plugin1"));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("License-manager plugin should be installed");
    request.execute();
  }

  @Test
  public void request_fails_with_ISE_if_no_change_needed_but_license_plugin_is_not_installed() throws IOException {
    underTest = new ApplyLicenseAction(userSessionRule, mutableEditionManagementState, editionInstaller, webServer, null);
    actionTester = new WsActionTester(underTest);
    userSessionRule.logIn().setSystemAdministrator();

    setPendingLicense(NONE);
    when(webServer.isStandalone()).thenReturn(true);

    TestRequest request = actionTester.newRequest().setParam(PARAM_LICENSE, createLicenseParam("dev", "plugin1"));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't decide edition does not require install if LicenseCommit instance is null");
    request.execute();
  }

  @Test
  public void always_apply_license_when_is_cluster() throws IOException {
    userSessionRule.logIn().setSystemAdministrator();
    when(webServer.isStandalone()).thenReturn(false);
    setPendingLicense(NONE);

    TestRequest request = actionTester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_LICENSE, createLicenseParam(PENDING_EDITION_NAME, "plugin1"));

    TestResponse response = request.execute();
    assertResponse(response, PENDING_EDITION_NAME, "", NONE);
    verify(mutableEditionManagementState).newEditionWithoutInstall(PENDING_EDITION_NAME);
    verifyZeroInteractions(editionInstaller);
  }

  @Test
  public void verify_example() throws IOException {
    userSessionRule.logIn().setSystemAdministrator();
    setPendingLicense(AUTOMATIC_IN_PROGRESS, null);

    TestRequest request = actionTester.newRequest()
      .setParam(PARAM_LICENSE, createLicenseParam("dev", "plugin1"));

    JsonAssert.assertJson(request.execute().getInput()).isSimilarTo(actionTester.getDef().responseExampleAsString());
  }

  @Test
  public void apply_without_need_to_install() throws IOException {
    userSessionRule.logIn().setSystemAdministrator();
    setPendingLicense(NONE);
    when(editionInstaller.requiresInstallationChange(singleton("plugin1"))).thenReturn(false);
    String base64License = createLicenseParam(PENDING_EDITION_NAME, "plugin1");
    when(webServer.isStandalone()).thenReturn(true);

    TestRequest request = actionTester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_LICENSE, base64License);

    TestResponse response = request.execute();

    assertResponse(response, PENDING_EDITION_NAME, "", NONE);
    verify(mutableEditionManagementState).newEditionWithoutInstall(PENDING_EDITION_NAME);
    verify(licenseCommit).update(base64License);
  }

  @Test
  public void execute_throws_BadRequestException_if_license_validation_fails_when_there_is_no_need_to_install() throws IOException {
    userSessionRule.logIn().setSystemAdministrator();
    setPendingLicense(NONE, null);
    when(editionInstaller.requiresInstallationChange(singleton("plugin1"))).thenReturn(false);
    String base64License = createLicenseParam(PENDING_EDITION_NAME, "plugin1");
    TestRequest request = actionTester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_LICENSE, base64License);
    IllegalArgumentException fakeValidationError = new IllegalArgumentException("Faking failed validation of license on update call");
    doThrow(fakeValidationError)
      .when(licenseCommit)
      .update(base64License);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(fakeValidationError.getMessage());

    request.execute();
  }

  @Test
  public void apply_offline() throws IOException {
    userSessionRule.logIn().setSystemAdministrator();
    setPendingLicense(PendingStatus.MANUAL_IN_PROGRESS);
    when(editionInstaller.requiresInstallationChange(singleton("plugin1"))).thenReturn(true);
    when(webServer.isStandalone()).thenReturn(true);

    TestRequest request = actionTester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_LICENSE, createLicenseParam(PENDING_EDITION_NAME, "plugin1"));

    TestResponse response = request.execute();

    assertResponse(response, PENDING_EDITION_NAME, "", PendingStatus.MANUAL_IN_PROGRESS);
    verify(mutableEditionManagementState, times(0)).startManualInstall(any(License.class));
    verify(mutableEditionManagementState, times(0)).startAutomaticInstall(any(License.class));
  }

  @Test
  public void apply_successfully_auto_installation() throws IOException {
    userSessionRule.logIn().setSystemAdministrator();
    setPendingLicense(PendingStatus.AUTOMATIC_IN_PROGRESS);
    when(editionInstaller.requiresInstallationChange(singleton("plugin1"))).thenReturn(true);
    when(webServer.isStandalone()).thenReturn(true);

    TestRequest request = actionTester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_LICENSE, createLicenseParam(PENDING_EDITION_NAME, "plugin1"));

    TestResponse response = request.execute();

    assertResponse(response, PENDING_EDITION_NAME, "", PendingStatus.AUTOMATIC_IN_PROGRESS);
    verify(mutableEditionManagementState, times(0)).startAutomaticInstall(any(License.class));
    verify(mutableEditionManagementState, times(0)).startManualInstall(any(License.class));
  }

  @Test
  public void returns_auto_install_fails_instantly() throws IOException {
    userSessionRule.logIn().setSystemAdministrator();
    String errorMessage = "error! an error!";
    setPendingLicense(PendingStatus.NONE, errorMessage);
    when(editionInstaller.requiresInstallationChange(singleton("plugin1"))).thenReturn(true);
    when(mutableEditionManagementState.getInstallErrorMessage()).thenReturn(Optional.of(errorMessage));

    TestRequest request = actionTester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_LICENSE, createLicenseParam(PENDING_EDITION_NAME, "plugin1"));

    TestResponse response = request.execute();

    StatusResponse parsedResponse = Editions.StatusResponse.parseFrom(response.getInputStream());
    assertThat(parsedResponse.getInstallError()).isEqualTo(errorMessage);
  }

  private void assertResponse(TestResponse response, String expectedNextEditionKey, String expectedEditionKey,
    PendingStatus expectedPendingStatus) throws IOException {
    StatusResponse parsedResponse = Editions.StatusResponse.parseFrom(response.getInputStream());
    assertThat(parsedResponse.getCurrentEditionKey()).isEqualTo(expectedEditionKey);
    assertThat(parsedResponse.getNextEditionKey()).isEqualTo(expectedNextEditionKey);
    assertThat(parsedResponse.getInstallationStatus()).isEqualTo(Editions.InstallationStatus.valueOf(expectedPendingStatus.toString()));
  }

  private void setPendingLicense(PendingStatus pendingStatus) {
    setPendingLicense(pendingStatus, null);
  }

  private void setPendingLicense(PendingStatus pendingStatus, @Nullable String errorMessage) {
    when(mutableEditionManagementState.getCurrentEditionKey()).thenReturn(Optional.empty());
    when(mutableEditionManagementState.getPendingEditionKey()).thenReturn(Optional.of(PENDING_EDITION_NAME));
    when(mutableEditionManagementState.getPendingInstallationStatus())
      .thenReturn(NONE)
      .thenReturn(pendingStatus);
    when(mutableEditionManagementState.getInstallErrorMessage()).thenReturn(Optional.ofNullable(errorMessage));
  }

  private static String createLicenseParam(String editionKey, String... pluginKeys) throws IOException {
    Properties props = new Properties();
    props.setProperty("Plugins", String.join(",", pluginKeys));
    props.setProperty("Edition", editionKey);
    StringWriter writer = new StringWriter();
    props.store(writer, "");

    byte[] encoded = Base64.getEncoder().encode(writer.toString().getBytes());
    return new String(encoded, StandardCharsets.UTF_8);
  }

  @DataProvider
  public static Object[][] notNonePendingInstallationStatuses() {
    return Arrays.stream(EditionManagementState.PendingStatus.values())
      .filter(s -> s != NONE)
      .map(s -> new Object[] {s})
      .toArray(Object[][]::new);
  }
}
