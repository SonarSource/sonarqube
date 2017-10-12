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
import java.util.Map;
import java.util.Optional;
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

public class StandaloneEditionManagementStateImpl implements MutableEditionManagementState, Startable {
  private static final String CURRENT_EDITION_KEY = "sonar.editionManagement.currentEditionKey";
  private static final String PENDING_INSTALLATION_STATUS = "sonar.editionManagement.pendingInstallationStatus";
  private static final String PENDING_EDITION_KEY = "sonar.editionManagement.pendingEditionKey";
  private static final String PENDING_LICENSE = "sonar.editionManagement.pendingLicense";

  private final DbClient dbClient;
  private String currentEditionKey;
  private PendingStatus pendingInstallationStatus = NONE;
  private String pendingEditionKey;
  private String pendingLicense;

  public StandaloneEditionManagementStateImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      // load current state value
      Map<String, Optional<String>> internalPropertyValues = dbClient.internalPropertiesDao().selectByKeys(dbSession,
        ImmutableSet.of(CURRENT_EDITION_KEY, PENDING_INSTALLATION_STATUS, PENDING_EDITION_KEY, PENDING_LICENSE));
      this.currentEditionKey = internalPropertyValues.getOrDefault(CURRENT_EDITION_KEY, empty())
        .map(StandaloneEditionManagementStateImpl::emptyToNull)
        .orElse(null);
      this.pendingInstallationStatus = internalPropertyValues.getOrDefault(PENDING_INSTALLATION_STATUS, empty())
        .map(PendingStatus::valueOf)
        .orElse(NONE);
      this.pendingEditionKey = internalPropertyValues.getOrDefault(PENDING_EDITION_KEY, empty())
        .map(StandaloneEditionManagementStateImpl::emptyToNull)
        .orElse(null);
      this.pendingLicense = internalPropertyValues.getOrDefault(PENDING_LICENSE, empty()).orElse(null);
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

  @Override
  public Optional<String> getCurrentEditionKey() {
    return Optional.ofNullable(currentEditionKey);
  }

  @Override
  public PendingStatus getPendingInstallationStatus() {
    return pendingInstallationStatus;
  }

  @Override
  public Optional<String> getPendingEditionKey() {
    return Optional.ofNullable(pendingEditionKey);
  }

  @Override
  public Optional<String> getPendingLicense() {
    return Optional.ofNullable(pendingLicense);
  }

  @Override
  public synchronized PendingStatus startAutomaticInstall(License license) {
    checkLicense(license);
    changeStatusFromTo(NONE, AUTOMATIC_IN_PROGRESS);
    this.pendingLicense = license.getContent();
    this.pendingEditionKey = license.getEditionKey();
    persistProperties();
    return this.pendingInstallationStatus;
  }

  @Override
  public synchronized PendingStatus startManualInstall(License license) {
    checkLicense(license);
    changeStatusFromTo(NONE, MANUAL_IN_PROGRESS);
    this.pendingLicense = license.getContent();
    this.pendingEditionKey = license.getEditionKey();
    this.pendingInstallationStatus = MANUAL_IN_PROGRESS;
    persistProperties();
    return this.pendingInstallationStatus;
  }

  @Override
  public synchronized PendingStatus newEditionWithoutInstall(String newEditionKey) {
    requireNonNull(newEditionKey, "newEditionKey can't be null");
    checkArgument(!newEditionKey.isEmpty(), "newEditionKey can't be empty");
    changeStatusFromTo(NONE, NONE);
    this.currentEditionKey = newEditionKey;
    persistProperties();
    return this.pendingInstallationStatus;
  }

  @Override
  public synchronized PendingStatus automaticInstallReady() {
    changeStatusFromTo(AUTOMATIC_IN_PROGRESS, AUTOMATIC_READY);
    persistProperties();
    return this.pendingInstallationStatus;
  }

  @Override
  public synchronized PendingStatus finalizeInstallation() {
    checkState(this.pendingInstallationStatus == AUTOMATIC_READY || this.pendingInstallationStatus == MANUAL_IN_PROGRESS,
      "Can't finalize installation when state is %s", this.pendingInstallationStatus);

    this.pendingInstallationStatus = NONE;
    this.currentEditionKey = this.pendingEditionKey;
    this.pendingEditionKey = null;
    this.pendingLicense = null;
    persistProperties();
    return this.pendingInstallationStatus;
  }

  private void changeStatusFromTo(PendingStatus expectedStatus, PendingStatus newStatus) {
    checkState(pendingInstallationStatus == expectedStatus,
      "Can't move to {} when status is {} (should be {})",
      newStatus, pendingInstallationStatus, expectedStatus);
    this.pendingInstallationStatus = newStatus;
  }

  private void persistProperties() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      InternalPropertiesDao internalPropertiesDao = dbClient.internalPropertiesDao();
      if (pendingInstallationStatus == NONE) {
        internalPropertiesDao.saveAsEmpty(dbSession, PENDING_EDITION_KEY);
        internalPropertiesDao.saveAsEmpty(dbSession, PENDING_LICENSE);
      } else {
        internalPropertiesDao.save(dbSession, PENDING_EDITION_KEY, pendingEditionKey);
        internalPropertiesDao.save(dbSession, PENDING_LICENSE, pendingLicense);
      }
      if (currentEditionKey == null) {
        internalPropertiesDao.saveAsEmpty(dbSession, CURRENT_EDITION_KEY);
      } else {
        internalPropertiesDao.save(dbSession, CURRENT_EDITION_KEY, currentEditionKey);
      }
      internalPropertiesDao.save(dbSession, PENDING_INSTALLATION_STATUS, pendingInstallationStatus.name());
      dbSession.commit();
    }
  }

  private static void checkLicense(License license) {
    requireNonNull(license, "license can't be null");
  }

  private static String emptyToNull(String s) {
    return s.isEmpty() ? null : s;
  }
}
