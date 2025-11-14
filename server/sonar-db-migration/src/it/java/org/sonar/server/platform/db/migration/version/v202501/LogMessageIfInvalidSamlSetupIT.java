/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202501;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v202501.LogMessageIfInvalidSamlSetup.SAML_ENABLED;
import static org.sonar.server.platform.db.migration.version.v202501.LogMessageIfInvalidSamlSetup.SERVICE_PROVIDER_CERTIFICATE;
import static org.sonar.server.platform.db.migration.version.v202501.LogMessageIfInvalidSamlSetup.SERVICE_PROVIDER_PRIVATE_KEY;
import static org.sonar.server.platform.db.migration.version.v202501.LogMessageIfInvalidSamlSetup.SIGN_REQUESTS_ENABLED;


class LogMessageIfInvalidSamlSetupIT {

  private static final String CERTIFICATE = "certificate";
  private static final String PRIVATE_KEY = "private_key";
  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(LogMessageIfInvalidSamlSetup.class);

  @RegisterExtension
  public final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final DataChange underTest = new LogMessageIfInvalidSamlSetup(db.database());

  @BeforeEach
  void setUp() {
    enableSaml();
  }

  @Test
  void execute_givenSamlIsNotEnabled_doesNothing() throws Exception {
    disableSaml();
    underTest.execute();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void execute_givenSamlIsNotEnabledAndCertificateAndPrivateKey_doesNothing() throws Exception {
    disableSaml();
    defineCertificate();
    definePrivateKey();

    underTest.execute();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void execute_whenSignatureNotEnabledAndNoCertificateAndNoPrivateKey_doesNothing() throws Exception {
    underTest.execute();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void execute_whenSignatureNotEnableAndCertificateAndPrivateKey_doesNothing() throws Exception {
    defineCertificate();
    definePrivateKey();
    underTest.execute();

    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void execute_whenSignatureNotEnabledAndCertificateAndNoPrivateKey_doesNothing() throws Exception {
    defineCertificate();
    underTest.execute();

    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void execute_whenSignatureEnabledAndCertificateAndPrivateKey_doesNothing() throws Exception {
    enableSamlSigneRequest();
    defineCertificate();
    definePrivateKey();
    underTest.execute();

    assertThat(logTester.logs()).isEmpty();
  }


  @Test
  void execute_whenSignatureNotEnabledAndPrivateKeyAndNoCertificate_warns() throws Exception {
    definePrivateKey();
    underTest.execute();
    assertThat(logTester.logs()).containsExactly("We detected an invalid SAML configuration that will prevent users to login with SAML: Service provider certificate is needed to decrypt SAML response.");
  }

  @Test
  void execute_whenSignatureEnabledAndCertificateAndNoPrivateKey_doesNothing() throws Exception {
    enableSamlSigneRequest();
    defineCertificate();
    underTest.execute();

    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void execute_whenSignatureEnabledAndNoCertificateAndPrivateKey_doesNothing() throws Exception {
    enableSamlSigneRequest();
    definePrivateKey();
    underTest.execute();

    assertThat(logTester.logs()).isEmpty();
  }

  private void enableSaml() {
    enableProperty(SAML_ENABLED);
  }

  private void enableSamlSigneRequest() {
    enableProperty(SIGN_REQUESTS_ENABLED);
  }

  private void defineCertificate() {
    defineProperty(SERVICE_PROVIDER_CERTIFICATE, CERTIFICATE);
  }

  private void definePrivateKey() {
    defineProperty(SERVICE_PROVIDER_PRIVATE_KEY, PRIVATE_KEY);
  }

  private void enableProperty(String property) {
    defineProperty(property, "true");
  }

  private void defineProperty(String property, String value) {
    db.executeInsert("properties", "prop_key", property, "is_empty", false,
      "text_value", value, "uuid", property, "created_at", 1_000_000_000_000L);
  }

  private void disableSaml(){
    db.executeUpdateSql("delete from properties where prop_key = '%s'".formatted(SAML_ENABLED));
  }

}
