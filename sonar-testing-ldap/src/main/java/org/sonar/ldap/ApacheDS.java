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
package org.sonar.ldap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.ldif.ChangeType;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.FileUtils;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.sasl.MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.plain.PlainMechanismHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.client.KrbConfigKey;
import org.apache.kerby.kerberos.kerb.identity.backend.BackendConfig;
import org.apache.kerby.kerberos.kerb.server.KdcConfigKey;
import org.apache.kerby.kerberos.kerb.server.KdcServer;
import org.apache.mina.util.AvailablePortFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ApacheDS {
  private static final Logger LOG = LoggerFactory.getLogger(ApacheDS.class);
  private static final String HOSTNAME_LOCALHOST = "localhost";
  private final String realm;
  private final String baseDn;
  private int ldapPort;

  private DirectoryService directoryService;
  private LdapServer ldapServer;
  private KdcServer kdcServer;

  private ApacheDS(String realm, String baseDn) {
    this.realm = realm;
    this.baseDn = baseDn;
    ldapServer = new LdapServer();
  }

  public static ApacheDS start(String realm, String baseDn, String workDir, Integer port) throws Exception {
    return new ApacheDS(realm, baseDn)
      .startDirectoryService(workDir)
      .startLdapServer(port == null ? AvailablePortFinder.getNextAvailable(1024) : port)
      .startKdcServer()
      .activateNis();
  }

  public static ApacheDS start(String realm, String baseDn, String workDir) throws Exception {
    return start(realm, baseDn, workDir + realm, null);
  }

  public static ApacheDS start(String realm, String baseDn) throws Exception {
    return start(realm, baseDn, "target/ldap-work/" + realm, null);
  }

  public void stop() {
    try {
      kdcServer.stop();
      kdcServer = null;
      ldapServer.stop();
      ldapServer = null;
      directoryService.shutdown();
      directoryService = null;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public String getUrl() {
    return "ldap://localhost:" + ldapServer.getPort();
  }

  /**
   * Stream will be closed automatically.
   */
  public void importLdif(InputStream is) throws Exception {
    try (LdifReader reader = new LdifReader(is)) {
      CoreSession coreSession = directoryService.getAdminSession();
      // see LdifFileLoader
      for (LdifEntry ldifEntry : reader) {
        String ldif = ldifEntry.toString();
        LOG.info(ldif);
        if (ChangeType.Add == ldifEntry.getChangeType() || /* assume "add" by default */ ChangeType.None == ldifEntry.getChangeType()) {
          coreSession.add(new DefaultEntry(coreSession.getDirectoryService().getSchemaManager(), ldifEntry.getEntry()));
        } else if (ChangeType.Modify == ldifEntry.getChangeType()) {
          coreSession.modify(ldifEntry.getDn(), ldifEntry.getModifications());
        } else if (ChangeType.Delete == ldifEntry.getChangeType()) {
          coreSession.delete(ldifEntry.getDn());
        } else {
          throw new IllegalStateException();
        }
      }
    }
  }

  public void disableAnonymousAccess() {
    directoryService.setAllowAnonymousAccess(false);
  }

  public void enableAnonymousAccess() {
    directoryService.setAllowAnonymousAccess(true);
  }

  private ApacheDS startDirectoryService(String workDirStr) throws Exception {
    DefaultDirectoryServiceFactory factory = new DefaultDirectoryServiceFactory();
    factory.init(realm);

    directoryService = factory.getDirectoryService();
    directoryService.getChangeLog().setEnabled(false);
    directoryService.setShutdownHookEnabled(false);
    directoryService.setAllowAnonymousAccess(true);

    File workDir = new File(workDirStr);
    if (workDir.exists()) {
      FileUtils.deleteDirectory(workDir);
    }
    InstanceLayout instanceLayout = new InstanceLayout(workDir);
    directoryService.setInstanceLayout(instanceLayout);

    AvlPartition partition = new AvlPartition(directoryService.getSchemaManager());
    partition.setId("Test");
    partition.setSuffixDn(new Dn(directoryService.getSchemaManager(), baseDn));
    partition.addIndexedAttributes(
      new AvlIndex<>("ou"),
      new AvlIndex<>("uid"),
      new AvlIndex<>("dc"),
      new AvlIndex<>("objectClass"));
    partition.initialize();
    directoryService.addPartition(partition);
    directoryService.addLast(new KeyDerivationInterceptor());

    directoryService.shutdown();
    directoryService.startup();

    return this;
  }

  private ApacheDS startLdapServer(int port) throws Exception {
    this.ldapPort = port;
    ldapServer.setTransports(new TcpTransport(port));
    ldapServer.setDirectoryService(directoryService);

    // Setup SASL mechanisms
    Map<String, MechanismHandler> mechanismHandlerMap = new HashMap<>();
    mechanismHandlerMap.put(SupportedSaslMechanisms.PLAIN, new PlainMechanismHandler());
    mechanismHandlerMap.put(SupportedSaslMechanisms.CRAM_MD5, new CramMd5MechanismHandler());
    mechanismHandlerMap.put(SupportedSaslMechanisms.DIGEST_MD5, new DigestMd5MechanismHandler());
    mechanismHandlerMap.put(SupportedSaslMechanisms.GSSAPI, new GssapiMechanismHandler());
    ldapServer.setSaslMechanismHandlers(mechanismHandlerMap);

    ldapServer.setSaslHost(HOSTNAME_LOCALHOST);
    ldapServer.setSaslRealms(Collections.singletonList(realm));
    // TODO ldapServer.setSaslPrincipal();
    // The base DN containing users that can be SASL authenticated.
    ldapServer.setSearchBaseDn(baseDn);

    ldapServer.start();

    return this;
  }


  private ApacheDS startKdcServer() throws IOException, KrbException {
    int port = AvailablePortFinder.getNextAvailable(6088);

    File krbConf = new File("target/krb5.conf");
    FileUtils.writeStringToFile(krbConf, ""
        + "[libdefaults]\n"
        + "    default_realm = EXAMPLE.ORG\n"
        + "\n"
        + "[realms]\n"
        + "    EXAMPLE.ORG = {\n"
        + "        kdc = localhost:" + port + "\n"
        + "    }\n"
        + "\n"
        + "[domain_realm]\n"
        + "    .example.org = EXAMPLE.ORG\n"
        + "    example.org = EXAMPLE.ORG\n",
      StandardCharsets.UTF_8.name());

    kdcServer = new KdcServer(krbConf);
    kdcServer.setKdcRealm("EXAMPLE.ORG");
    kdcServer.getKdcConfig().setBoolean(KrbConfigKey.PA_ENC_TIMESTAMP_REQUIRED, false);

    BackendConfig backendConfig = kdcServer.getBackendConfig();
    backendConfig.setString("host", HOSTNAME_LOCALHOST);
    backendConfig.setString("base_dn", baseDn);
    backendConfig.setInt("port", this.ldapPort);
    backendConfig.setString(KdcConfigKey.KDC_IDENTITY_BACKEND,
      "org.apache.kerby.kerberos.kdc.identitybackend.LdapIdentityBackend");

    kdcServer.setAllowUdp(true);
    kdcServer.setAllowTcp(false);
    kdcServer.setKdcUdpPort(port);
    kdcServer.setKdcHost(HOSTNAME_LOCALHOST);

    kdcServer.init();
    kdcServer.start();
    return this;
  }

  /**
   * This seems to be required for objectClass posixGroup.
   */
  private ApacheDS activateNis() throws Exception {
    directoryService.getAdminSession().modify(
      new Dn("cn=nis,ou=schema"),
      new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "m-disabled", "FALSE"));
    return this;
  }

}
