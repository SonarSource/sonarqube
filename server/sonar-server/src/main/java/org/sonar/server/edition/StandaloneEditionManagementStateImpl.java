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
package org.sonar.server.edition;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.picocontainer.Startable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.InternalPropertiesDao;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.AUTOMATIC_IN_PROGRESS;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.AUTOMATIC_READY;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.MANUAL_IN_PROGRESS;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.NONE;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.UNINSTALL_IN_PROGRESS;

public class StandaloneEditionManagementStateImpl implements MutableEditionManagementState, Startable {
  private static final String CURRENT_EDITION_KEY = "currentEditionKey";
  private static final String PENDING_INSTALLATION_STATUS = "pendingInstallStatus";
  private static final String PENDING_EDITION_KEY = "pendingEditionKey";
  private static final String PENDING_LICENSE = "pendingLicense";
  private static final String INSTALL_ERROR_MESSAGE = "installError";

  private final DbClient dbClient;
  private String currentEditionKey;
  private PendingStatus pendingInstallationStatus;
  private String pendingEditionKey;
  private String pendingLicense;
  private String installErrorMessage;

  public StandaloneEditionManagementStateImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      // load current state value
      Map<String, Optional<String>> internalPropertyValues = dbClient.internalPropertiesDao().selectByKeys(dbSession,
        ImmutableSet.of(CURRENT_EDITION_KEY, PENDING_INSTALLATION_STATUS, PENDING_EDITION_KEY, PENDING_LICENSE, INSTALL_ERROR_MESSAGE));
      this.currentEditionKey = internalPropertyValues.getOrDefault(CURRENT_EDITION_KEY, empty())
        .map(StandaloneEditionManagementStateImpl::emptyToNull)
        .orElse(null);
      this.pendingInstallationStatus = internalPropertyValues.getOrDefault(PENDING_INSTALLATION_STATUS, empty())
        .map(StandaloneEditionManagementStateImpl::emptyToNull)
        .map(PendingStatus::valueOf)
        .orElse(NONE);
      this.pendingEditionKey = internalPropertyValues.getOrDefault(PENDING_EDITION_KEY, empty())
        .map(StandaloneEditionManagementStateImpl::emptyToNull)
        .orElse(null);
      this.pendingLicense = internalPropertyValues.getOrDefault(PENDING_LICENSE, empty())
        .map(StandaloneEditionManagementStateImpl::emptyToNull)
        .orElse(null);
      this.installErrorMessage = internalPropertyValues.getOrDefault(INSTALL_ERROR_MESSAGE, empty())
        .map(StandaloneEditionManagementStateImpl::emptyToNull)
        .orElse(null);
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

  @Override
  public Optional<String> getCurrentEditionKey() {
    ensureStarted();
    return Optional.ofNullable(currentEditionKey);
  }

  @Override
  public PendingStatus getPendingInstallationStatus() {
    ensureStarted();
    return pendingInstallationStatus;
  }

  @Override
  public Optional<String> getPendingEditionKey() {
    ensureStarted();
    return Optional.ofNullable(pendingEditionKey);
  }

  @Override
  public Optional<String> getPendingLicense() {
    ensureStarted();
    return Optional.ofNullable(pendingLicense);
  }

  @Override
  public Optional<String> getInstallErrorMessage() {
    ensureStarted();
    return Optional.ofNullable(installErrorMessage);
  }

  @Override
  public synchronized PendingStatus startAutomaticInstall(License license) {
    ensureStarted();
    checkLicense(license);
    changeStatusToFrom(AUTOMATIC_IN_PROGRESS, NONE);
    this.pendingLicense = license.getContent();
    this.pendingEditionKey = license.getEditionKey();
    persistProperties();
    return this.pendingInstallationStatus;
  }

  @Override
  public synchronized PendingStatus startManualInstall(License license) {
    ensureStarted();
    checkLicense(license);
    changeStatusToFrom(MANUAL_IN_PROGRESS, NONE);
    this.pendingLicense = license.getContent();
    this.pendingEditionKey = license.getEditionKey();
    this.pendingInstallationStatus = MANUAL_IN_PROGRESS;
    persistProperties();
    return this.pendingInstallationStatus;
  }

  @Override
  public synchronized PendingStatus newEditionWithoutInstall(String newEditionKey) {
    ensureStarted();
    requireNonNull(newEditionKey, "newEditionKey can't be null");
    checkArgument(!newEditionKey.isEmpty(), "newEditionKey can't be empty");
    changeStatusToFrom(NONE, NONE);
    this.currentEditionKey = newEditionKey;
    persistProperties();
    return this.pendingInstallationStatus;
  }

  @Override
  public synchronized PendingStatus automaticInstallReady() {
    ensureStarted();
    changeStatusToFrom(AUTOMATIC_READY, AUTOMATIC_IN_PROGRESS);
    persistProperties();
    return this.pendingInstallationStatus;
  }

  @Override
  public synchronized PendingStatus finalizeInstallation() {
    ensureStarted();
    changeStatusToFrom(NONE, AUTOMATIC_READY, MANUAL_IN_PROGRESS, UNINSTALL_IN_PROGRESS);

    this.pendingInstallationStatus = NONE;
    this.currentEditionKey = this.pendingEditionKey;
    this.pendingEditionKey = null;
    this.pendingLicense = null;
    persistProperties();
    return this.pendingInstallationStatus;
  }

  @Override
  public synchronized PendingStatus uninstall() {
    ensureStarted();
    changeStatusToFrom(UNINSTALL_IN_PROGRESS, NONE);
    checkState(currentEditionKey != null, "There is no edition currently installed");

    this.pendingInstallationStatus = UNINSTALL_IN_PROGRESS;
    this.pendingEditionKey = null;
    this.pendingLicense = null;
    this.currentEditionKey = null;
    persistProperties();
    return this.pendingInstallationStatus;
  }

  private void ensureStarted() {
    checkState(pendingInstallationStatus != null, "%s is not started", getClass().getSimpleName());
  }

  private void changeStatusToFrom(PendingStatus newStatus, PendingStatus... validPendingStatuses) {
    checkState(Arrays.stream(validPendingStatuses).anyMatch(s -> s == pendingInstallationStatus),
      "Can't move to %s when status is %s (should be any of %s)",
      newStatus, pendingInstallationStatus, Arrays.toString(validPendingStatuses));
    this.pendingInstallationStatus = newStatus;
  }

  private void persistProperties() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      InternalPropertiesDao internalPropertiesDao = dbClient.internalPropertiesDao();
      saveInternalProperty(internalPropertiesDao, dbSession, PENDING_EDITION_KEY, pendingEditionKey);
      saveInternalProperty(internalPropertiesDao, dbSession, PENDING_LICENSE, pendingLicense);
      saveInternalProperty(internalPropertiesDao, dbSession, INSTALL_ERROR_MESSAGE, installErrorMessage);
      saveInternalProperty(internalPropertiesDao, dbSession, CURRENT_EDITION_KEY, currentEditionKey);
      saveInternalProperty(internalPropertiesDao, dbSession, PENDING_INSTALLATION_STATUS, pendingInstallationStatus.name());
      dbSession.commit();
    }
  }

  private static void saveInternalProperty(InternalPropertiesDao dao, DbSession dbSession, String key, @Nullable String value) {
    if (value == null) {
      dao.saveAsEmpty(dbSession, key);
    } else {
      dao.save(dbSession, key, value);
    }
  }

  private static void checkLicense(License license) {
    requireNonNull(license, "license can't be null");
  }

  private static String emptyToNull(String s) {
    return s.isEmpty() ? null : s;
  }
}
