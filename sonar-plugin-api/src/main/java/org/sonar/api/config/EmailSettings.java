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
package org.sonar.api.config;

import java.util.List;
import org.sonar.api.PropertyType;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;

import static java.util.Arrays.asList;
import static org.sonar.api.CoreProperties.CATEGORY_GENERAL;
import static org.sonar.api.CoreProperties.SUBCATEGORY_EMAIL;
import static org.sonar.api.PropertyType.INTEGER;
import static org.sonar.api.PropertyType.SINGLE_SELECT_LIST;

/**
 * @since 3.2
 */
@ServerSide
@ComputeEngineSide
public class EmailSettings {
  public static final String SMTP_HOST = "email.smtp_host.secured";
  public static final String SMTP_HOST_DEFAULT = "";
  public static final String SMTP_PORT = "email.smtp_port.secured";
  public static final String SMTP_PORT_DEFAULT = "25";
  public static final String SMTP_SECURE_CONNECTION = "email.smtp_secure_connection.secured";
  public static final String SMTP_SECURE_CONNECTION_DEFAULT = "";
  public static final String SMTP_USERNAME = "email.smtp_username.secured";
  public static final String SMTP_USERNAME_DEFAULT = "";
  public static final String SMTP_PASSWORD = "email.smtp_password.secured";
  public static final String SMTP_PASSWORD_DEFAULT = "";
  public static final String FROM = "email.from";
  public static final String FROM_DEFAULT = "noreply@nowhere";
  public static final String FROM_NAME = "email.fromName";
  public static final String FROM_NAME_DEFAULT = "SonarQube";
  public static final String PREFIX = "email.prefix";
  public static final String PREFIX_DEFAULT = "[SONARQUBE]";

  private final Configuration config;
  private final Server server;

  public EmailSettings(Configuration config, Server server) {
    this.config = config;
    this.server = server;
  }

  public String getSmtpHost() {
    return get(SMTP_HOST, SMTP_HOST_DEFAULT);
  }

  public int getSmtpPort() {
    return Integer.parseInt(get(SMTP_PORT, SMTP_PORT_DEFAULT));
  }

  public String getSecureConnection() {
    return get(SMTP_SECURE_CONNECTION, SMTP_SECURE_CONNECTION_DEFAULT);
  }

  public String getSmtpUsername() {
    return get(SMTP_USERNAME, SMTP_USERNAME_DEFAULT);
  }

  public String getSmtpPassword() {
    return get(SMTP_PASSWORD, SMTP_PASSWORD_DEFAULT);
  }

  public String getFrom() {
    return get(FROM, FROM_DEFAULT);
  }

  public String getFromName() {
    return get(FROM_NAME, FROM_NAME_DEFAULT);
  }

  public String getPrefix() {
    return get(PREFIX, PREFIX_DEFAULT);
  }

  public String getServerBaseURL() {
    return server.getPublicRootUrl();
  }

  private String get(String key, String defaultValue) {
    return config.get(key).orElse(defaultValue);
  }

  /**
   * @since 6.1
   */
  public static List<PropertyDefinition> definitions() {
    return asList(
      PropertyDefinition.builder(SMTP_HOST)
        .name("SMTP host")
        .description("For example \"smtp.gmail.com\". Leave blank to disable email sending.")
        .defaultValue(SMTP_HOST_DEFAULT)
        .category(CATEGORY_GENERAL)
        .subCategory(SUBCATEGORY_EMAIL)
        .build(),
      PropertyDefinition.builder(SMTP_PORT)
        .name("SMTP port")
        .description("Port number to connect with SMTP server.")
        .defaultValue(SMTP_PORT_DEFAULT)
        .category(CATEGORY_GENERAL)
        .subCategory(SUBCATEGORY_EMAIL)
        .type(INTEGER)
        .build(),
      PropertyDefinition.builder(SMTP_SECURE_CONNECTION)
        .name("Secure connection")
        .description("Type of secure connection. Leave empty to not use secure connection.")
        .defaultValue(SMTP_SECURE_CONNECTION_DEFAULT)
        .category(CATEGORY_GENERAL)
        .subCategory(SUBCATEGORY_EMAIL)
        .type(SINGLE_SELECT_LIST)
        .options("ssl", "starttls")
        .build(),
      PropertyDefinition.builder(SMTP_USERNAME)
        .name("SMTP username")
        .description("Username to use with authenticated SMTP.")
        .defaultValue(SMTP_USERNAME_DEFAULT)
        .category(CATEGORY_GENERAL)
        .subCategory(SUBCATEGORY_EMAIL)
        .build(),
      PropertyDefinition.builder(SMTP_PASSWORD)
        .name("SMTP password")
        .description("Password to use with authenticated SMTP.")
        .defaultValue(SMTP_PASSWORD_DEFAULT)
        .type(PropertyType.PASSWORD)
        .category(CATEGORY_GENERAL)
        .subCategory(SUBCATEGORY_EMAIL)
        .build(),
      PropertyDefinition.builder(FROM)
        .name("From address")
        .description("Emails will come from this address. For example - \"noreply@sonarsource.com\". Note that the mail server may ignore this setting.")
        .defaultValue(FROM_DEFAULT)
        .category(CATEGORY_GENERAL)
        .subCategory(SUBCATEGORY_EMAIL)
        .build(),
      PropertyDefinition.builder(FROM_NAME)
        .name("From name")
        .description("Emails will come from this address name. For example - \"SonarQube\". Note that the mail server may ignore this setting.")
        .defaultValue(FROM_NAME_DEFAULT)
        .category(CATEGORY_GENERAL)
        .subCategory(SUBCATEGORY_EMAIL)
        .build(),
      PropertyDefinition.builder(PREFIX)
        .name("Email prefix")
        .description("Prefix will be prepended to all outgoing email subjects.")
        .defaultValue(PREFIX_DEFAULT)
        .category(CATEGORY_GENERAL)
        .subCategory(SUBCATEGORY_EMAIL)
        .build());
  }
}
