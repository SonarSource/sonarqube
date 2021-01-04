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
package org.sonar.auth.ldap;

import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * @author Evgeny Mandrikov
 */
public class LdapContextFactory {

  private static final Logger LOG = Loggers.get(LdapContextFactory.class);

  // visible for testing
  static final String AUTH_METHOD_SIMPLE = "simple";
  static final String AUTH_METHOD_GSSAPI = "GSSAPI";
  static final String AUTH_METHOD_DIGEST_MD5 = "DIGEST-MD5";
  static final String AUTH_METHOD_CRAM_MD5 = "CRAM-MD5";

  private static final String REFERRALS_FOLLOW_MODE = "follow";
  private static final String REFERRALS_IGNORE_MODE = "ignore";

  private static final String DEFAULT_AUTHENTICATION = AUTH_METHOD_SIMPLE;
  private static final String DEFAULT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";

  /**
   * The Sun LDAP property used to enable connection pooling. This is used in the default implementation to enable
   * LDAP connection pooling.
   */
  private static final String SUN_CONNECTION_POOLING_PROPERTY = "com.sun.jndi.ldap.connect.pool";

  private static final String SASL_REALM_PROPERTY = "java.naming.security.sasl.realm";

  private final String providerUrl;
  private final boolean startTLS;
  private final String authentication;
  private final String factory;
  private final String username;
  private final String password;
  private final String realm;
  private final String referral;

  public LdapContextFactory(org.sonar.api.config.Configuration config, String settingsPrefix, String ldapUrl) {
    this.authentication = StringUtils.defaultString(config.get(settingsPrefix + ".authentication").orElse(null), DEFAULT_AUTHENTICATION);
    this.factory = StringUtils.defaultString(config.get(settingsPrefix + ".contextFactoryClass").orElse(null), DEFAULT_FACTORY);
    this.realm = config.get(settingsPrefix + ".realm").orElse(null);
    this.providerUrl = ldapUrl;
    this.startTLS = config.getBoolean(settingsPrefix + ".StartTLS").orElse(false);
    this.username = config.get(settingsPrefix + ".bindDn").orElse(null);
    this.password = config.get(settingsPrefix + ".bindPassword").orElse(null);
    this.referral = getReferralsMode(config, settingsPrefix + ".followReferrals");
  }

  /**
   * Returns {@code InitialDirContext} for Bind user.
   */
  public InitialDirContext createBindContext() throws NamingException {
    if (isGssapi()) {
      return createInitialDirContextUsingGssapi(username, password);
    } else {
      return createInitialDirContext(username, password, true);
    }
  }

  /**
   * Returns {@code InitialDirContext} for specified user.
   * Note that pooling intentionally disabled by this method.
   */
  public InitialDirContext createUserContext(String principal, String credentials) throws NamingException {
    return createInitialDirContext(principal, credentials, false);
  }

  private InitialDirContext createInitialDirContext(String principal, String credentials, boolean pooling) throws NamingException {
    final InitialLdapContext ctx;
    if (startTLS) {
      // Note that pooling is not enabled for such connections, because "Stop TLS" is not performed.
      Properties env = new Properties();
      env.put(Context.INITIAL_CONTEXT_FACTORY, factory);
      env.put(Context.PROVIDER_URL, providerUrl);
      env.put(Context.REFERRAL, referral);
      // At this point env should not contain properties SECURITY_AUTHENTICATION, SECURITY_PRINCIPAL and SECURITY_CREDENTIALS to avoid
      // "bind" operation prior to StartTLS:
      ctx = new InitialLdapContext(env, null);
      // http://docs.oracle.com/javase/jndi/tutorial/ldap/ext/starttls.html
      StartTlsResponse tls = (StartTlsResponse) ctx.extendedOperation(new StartTlsRequest());
      try {
        tls.negotiate();
      } catch (IOException e) {
        NamingException ex = new NamingException("StartTLS failed");
        ex.initCause(e);
        throw ex;
      }
      // Explicitly initiate "bind" operation:
      ctx.addToEnvironment(Context.SECURITY_AUTHENTICATION, authentication);
      if (principal != null) {
        ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, principal);
      }
      if (credentials != null) {
        ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, credentials);
      }
      ctx.reconnect(null);
    } else {
      ctx = new InitialLdapContext(getEnvironment(principal, credentials, pooling), null);
    }
    return ctx;
  }

  private InitialDirContext createInitialDirContextUsingGssapi(String principal, String credentials) throws NamingException {
    Configuration.setConfiguration(new Krb5LoginConfiguration());
    InitialDirContext initialDirContext;
    try {
      LoginContext lc = new LoginContext(getClass().getName(), new CallbackHandlerImpl(principal, credentials));
      lc.login();
      initialDirContext = Subject.doAs(lc.getSubject(), new PrivilegedExceptionAction<InitialDirContext>() {
        @Override
        public InitialDirContext run() throws NamingException {
          Properties env = new Properties();
          env.put(Context.INITIAL_CONTEXT_FACTORY, factory);
          env.put(Context.PROVIDER_URL, providerUrl);
          env.put(Context.REFERRAL, referral);
          return new InitialLdapContext(env, null);
        }
      });
    } catch (LoginException | PrivilegedActionException e) {
      NamingException namingException = new NamingException(e.getMessage());
      namingException.initCause(e);
      throw namingException;
    }
    return initialDirContext;
  }

  private Properties getEnvironment(@Nullable String principal, @Nullable String credentials, boolean pooling) {
    Properties env = new Properties();
    env.put(Context.SECURITY_AUTHENTICATION, authentication);
    if (realm != null) {
      env.put(SASL_REALM_PROPERTY, realm);
    }
    if (pooling) {
      // Enable connection pooling
      env.put(SUN_CONNECTION_POOLING_PROPERTY, "true");
    }
    env.put(Context.INITIAL_CONTEXT_FACTORY, factory);
    env.put(Context.PROVIDER_URL, providerUrl);
    env.put(Context.REFERRAL, referral);
    if (principal != null) {
      env.put(Context.SECURITY_PRINCIPAL, principal);
    }
    // Note: debug is intentionally was placed here - in order to not expose password in log
    LOG.debug("Initializing LDAP context {}", env);
    if (credentials != null) {
      env.put(Context.SECURITY_CREDENTIALS, credentials);
    }
    return env;
  }

  public boolean isSasl() {
    return AUTH_METHOD_DIGEST_MD5.equals(authentication) ||
      AUTH_METHOD_CRAM_MD5.equals(authentication) ||
      AUTH_METHOD_GSSAPI.equals(authentication);
  }

  public boolean isGssapi() {
    return AUTH_METHOD_GSSAPI.equals(authentication);
  }

  /**
   * Tests connection.
   *
   * @throws LdapException if unable to open connection
   */
  public void testConnection() {
    if (StringUtils.isBlank(username) && isSasl()) {
      throw new IllegalArgumentException("When using SASL - property ldap.bindDn is required");
    }
    try {
      createBindContext();
      LOG.info("Test LDAP connection on {}: OK", providerUrl);
    } catch (NamingException e) {
      LOG.info("Test LDAP connection: FAIL");
      throw new LdapException("Unable to open LDAP connection", e);
    }
  }

  public String getProviderUrl() {
    return providerUrl;
  }

  public String getReferral() {
    return referral;
  }

  private static String getReferralsMode(org.sonar.api.config.Configuration config, String followReferralsSettingKey) {
    // By default follow referrals
    return config.getBoolean(followReferralsSettingKey).orElse(true) ? REFERRALS_FOLLOW_MODE : REFERRALS_IGNORE_MODE;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
      "url=" + providerUrl +
      ", authentication=" + authentication +
      ", factory=" + factory +
      ", bindDn=" + username +
      ", realm=" + realm +
      ", referral=" + referral +
      "}";
  }

}
