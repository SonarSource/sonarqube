/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.auth.saml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SamlAuthenticationStatus {

  private String status = "";

  private Map<String, List<String>> availableAttributes = new HashMap<>();

  private Map<String, Collection<String>> mappedAttributes = new HashMap<>();

  private List<String> errors = new ArrayList<>();

  private List<String> warnings = new ArrayList<>();

  private boolean encryptionEnabled = false;

  private boolean signatureEnabled = false;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Map<String, List<String>> getAvailableAttributes() {
    return availableAttributes;
  }

  public void setAvailableAttributes(Map<String, List<String>> availableAttributes) {
    this.availableAttributes = availableAttributes;
  }

  public Map<String, Collection<String>> getMappedAttributes() {
    return mappedAttributes;
  }

  public void setMappedAttributes(Map<String, Collection<String>> mappedAttributes) {
    this.mappedAttributes = mappedAttributes;
  }

  public List<String> getErrors() {
    return errors;
  }

  public void setErrors(List<String> errors) {
    this.errors = errors;
  }

  public List<String> getWarnings() {
    return warnings;
  }

  public void setWarnings(List<String> warnings) {
    this.warnings = warnings;
  }

  public boolean isEncryptionEnabled() {
    return encryptionEnabled;
  }

  public void setEncryptionEnabled(boolean encryptionEnabled) {
    this.encryptionEnabled = encryptionEnabled;
  }

  public boolean isSignatureEnabled() {
    return signatureEnabled;
  }

  public void setSignatureEnabled(boolean signatureEnabled) {
    this.signatureEnabled = signatureEnabled;
  }
}
