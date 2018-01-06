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
package org.sonar.server.edition;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
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
  @CheckForNull
  private State state;

  public StandaloneEditionManagementStateImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      // load current state value
      Map<String, Optional<String>> internalPropertyValues = dbClient.internalPropertiesDao().selectByKeys(dbSession,
        ImmutableSet.of(CURRENT_EDITION_KEY, PENDING_INSTALLATION_STATUS, PENDING_EDITION_KEY, PENDING_LICENSE, INSTALL_ERROR_MESSAGE));

      PendingStatus pendingInstallationStatus = internalPropertyValues.getOrDefault(PENDING_INSTALLATION_STATUS, empty())
        .map(StandaloneEditionManagementStateImpl::emptyToNull)
        .map(PendingStatus::valueOf)
        .orElse(NONE);
      State.Builder builder = State.newBuilder(pendingInstallationStatus);
      builder
        .setCurrentEditionKey(internalPropertyValues.getOrDefault(CURRENT_EDITION_KEY, empty())
          .map(StandaloneEditionManagementStateImpl::emptyToNull)
          .orElse(null))
        .setPendingEditionKey(internalPropertyValues.getOrDefault(PENDING_EDITION_KEY, empty())
          .map(StandaloneEditionManagementStateImpl::emptyToNull)
          .orElse(null))
        .setPendingLicense(internalPropertyValues.getOrDefault(PENDING_LICENSE, empty())
          .map(StandaloneEditionManagementStateImpl::emptyToNull)
          .orElse(null))
        .setInstallErrorMessage(internalPropertyValues.getOrDefault(INSTALL_ERROR_MESSAGE, empty())
          .map(StandaloneEditionManagementStateImpl::emptyToNull)
          .orElse(null));
      state = builder.build();
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

  @Override
  public Optional<String> getCurrentEditionKey() {
    ensureStarted();
    return Optional.ofNullable(state.getCurrentEditionKey());
  }

  @Override
  public PendingStatus getPendingInstallationStatus() {
    ensureStarted();
    return state.getPendingInstallationStatus();
  }

  @Override
  public Optional<String> getPendingEditionKey() {
    ensureStarted();
    return Optional.ofNullable(state.getPendingEditionKey());
  }

  @Override
  public Optional<String> getPendingLicense() {
    ensureStarted();
    return Optional.ofNullable(state.getPendingLicense());
  }

  @Override
  public Optional<String> getInstallErrorMessage() {
    ensureStarted();
    return Optional.ofNullable(state.getInstallErrorMessage());
  }

  @Override
  public synchronized PendingStatus startAutomaticInstall(License license) {
    ensureStarted();
    checkLicense(license);
    State newState = changeStatusToFrom(AUTOMATIC_IN_PROGRESS, NONE)
      .setPendingLicense(license.getContent())
      .setPendingEditionKey(license.getEditionKey())
      .clearAutomaticInstallErrorMessage()
      .build();
    persistProperties(newState);
    return newState.getPendingInstallationStatus();
  }

  @Override
  public synchronized PendingStatus startManualInstall(License license) {
    ensureStarted();
    checkLicense(license);
    State newState = changeStatusToFrom(MANUAL_IN_PROGRESS, NONE)
      .setPendingLicense(license.getContent())
      .setPendingEditionKey(license.getEditionKey())
      .clearAutomaticInstallErrorMessage()
      .build();
    persistProperties(newState);
    return newState.getPendingInstallationStatus();
  }

  @Override
  public synchronized PendingStatus newEditionWithoutInstall(String newEditionKey) {
    ensureStarted();
    requireNonNull(newEditionKey, "newEditionKey can't be null");
    checkArgument(!newEditionKey.isEmpty(), "newEditionKey can't be empty");
    State newState = changeStatusToFrom(NONE, NONE)
      .setCurrentEditionKey(newEditionKey)
      .clearAutomaticInstallErrorMessage()
      .build();
    persistProperties(newState);
    return newState.getPendingInstallationStatus();
  }

  @Override
  public synchronized PendingStatus automaticInstallReady() {
    ensureStarted();
    State newState = changeStatusToFrom(AUTOMATIC_READY, AUTOMATIC_IN_PROGRESS)
      .clearAutomaticInstallErrorMessage()
      .build();
    persistProperties(newState);
    return newState.getPendingInstallationStatus();
  }

  @Override
  public synchronized PendingStatus installFailed(@Nullable String errorMessage) {
    ensureStarted();
    State newState = changeStatusToFrom(NONE, AUTOMATIC_IN_PROGRESS, MANUAL_IN_PROGRESS)
      .setInstallErrorMessage(nullableTrimmedEmptyToNull(errorMessage))
      .clearPendingFields()
      .build();
    persistProperties(newState);
    return newState.getPendingInstallationStatus();
  }

  @Override
  public synchronized void clearInstallErrorMessage() {
    ensureStarted();
    State currentState = this.state;
    if (currentState.getInstallErrorMessage() != null) {
      State newState = State.newBuilder(currentState)
        .clearAutomaticInstallErrorMessage()
        .build();
      persistProperties(newState);
    }
  }

  @Override
  public synchronized PendingStatus finalizeInstallation(@Nullable String errorMessage) {
    ensureStarted();
    State newState = changeStatusToFrom(NONE, AUTOMATIC_READY, MANUAL_IN_PROGRESS, UNINSTALL_IN_PROGRESS)
      .commitPendingEditionKey()
      .clearPendingFields()
      .setInstallErrorMessage(nullableTrimmedEmptyToNull(errorMessage))
      .build();
    persistProperties(newState);
    return newState.getPendingInstallationStatus();
  }

  @Override
  public synchronized PendingStatus uninstall() {
    ensureStarted();
    State.Builder builder = changeStatusToFrom(UNINSTALL_IN_PROGRESS, NONE);
    checkState(state.currentEditionKey != null, "There is no edition currently installed");
    State newState = builder
      .clearPendingFields()
      .clearCurrentEditionKey()
      .clearAutomaticInstallErrorMessage()
      .build();
    persistProperties(newState);
    return newState.getPendingInstallationStatus();
  }

  private void ensureStarted() {
    checkState(state != null, "%s is not started", getClass().getSimpleName());
  }

  private State.Builder changeStatusToFrom(PendingStatus newStatus, PendingStatus... validPendingStatuses) {
    State currentState = this.state;
    if (Arrays.stream(validPendingStatuses).noneMatch(s -> s == currentState.getPendingInstallationStatus())) {
      throw new IllegalStateException(String.format("Can't move to %s when status is %s (should be any of %s)",
        newStatus, currentState.getPendingInstallationStatus(), Arrays.toString(validPendingStatuses)));
    }

    return State.newBuilder(currentState, newStatus);
  }

  private void persistProperties(State newState) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      InternalPropertiesDao internalPropertiesDao = dbClient.internalPropertiesDao();
      saveInternalProperty(internalPropertiesDao, dbSession, PENDING_EDITION_KEY, newState.getPendingEditionKey());
      saveInternalProperty(internalPropertiesDao, dbSession, PENDING_LICENSE, newState.getPendingLicense());
      saveInternalProperty(internalPropertiesDao, dbSession, INSTALL_ERROR_MESSAGE, newState.getInstallErrorMessage());
      saveInternalProperty(internalPropertiesDao, dbSession, CURRENT_EDITION_KEY, newState.getCurrentEditionKey());
      saveInternalProperty(internalPropertiesDao, dbSession, PENDING_INSTALLATION_STATUS, newState.getPendingInstallationStatus().name());
      dbSession.commit();
      this.state = newState;
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

  private static String nullableTrimmedEmptyToNull(@Nullable String s) {
    if (s == null) {
      return null;
    }
    String v = s.trim();
    return v.isEmpty() ? null : v;
  }

  private static String emptyToNull(String s) {
    return s.isEmpty() ? null : s;
  }

  @Immutable
  private static final class State {
    private final String currentEditionKey;
    private final PendingStatus pendingInstallationStatus;
    private final String pendingEditionKey;
    private final String pendingLicense;
    private final String installErrorMessage;

    public State(Builder builder) {
      this.currentEditionKey = builder.currentEditionKey;
      this.pendingInstallationStatus = builder.pendingInstallationStatus;
      this.pendingEditionKey = builder.pendingEditionKey;
      this.pendingLicense = builder.pendingLicense;
      this.installErrorMessage = builder.installErrorMessage;
    }

    public String getCurrentEditionKey() {
      return currentEditionKey;
    }

    public PendingStatus getPendingInstallationStatus() {
      return pendingInstallationStatus;
    }

    public String getPendingEditionKey() {
      return pendingEditionKey;
    }

    public String getPendingLicense() {
      return pendingLicense;
    }

    public String getInstallErrorMessage() {
      return installErrorMessage;
    }

    public static Builder newBuilder(PendingStatus pendingInstallationStatus) {
      return new Builder(pendingInstallationStatus);
    }

    public static Builder newBuilder(State from) {
      return newBuilder(from, from.getPendingInstallationStatus());
    }

    public static Builder newBuilder(State from, PendingStatus newStatus) {
      return new Builder(newStatus)
        .setCurrentEditionKey(from.currentEditionKey)
        .setPendingEditionKey(from.pendingEditionKey)
        .setPendingLicense(from.pendingLicense)
        .setInstallErrorMessage(from.installErrorMessage);
    }

    private static class Builder {
      private PendingStatus pendingInstallationStatus;
      private String currentEditionKey;
      private String pendingEditionKey;
      private String pendingLicense;
      private String installErrorMessage;

      private Builder(PendingStatus pendingInstallationStatus) {
        this.pendingInstallationStatus = requireNonNull(pendingInstallationStatus);
      }

      public Builder setCurrentEditionKey(@Nullable String currentEditionKey) {
        this.currentEditionKey = currentEditionKey;
        return this;
      }

      public Builder setPendingEditionKey(@Nullable String pendingEditionKey) {
        this.pendingEditionKey = pendingEditionKey;
        return this;
      }

      public Builder setPendingLicense(@Nullable String pendingLicense) {
        this.pendingLicense = pendingLicense;
        return this;
      }

      public Builder setInstallErrorMessage(@Nullable String installErrorMessage) {
        this.installErrorMessage = installErrorMessage;
        return this;
      }

      public Builder commitPendingEditionKey() {
        this.currentEditionKey = pendingEditionKey;
        return this;
      }

      public Builder clearCurrentEditionKey() {
        this.currentEditionKey = null;
        return this;
      }

      public Builder clearPendingFields() {
        this.pendingEditionKey = null;
        this.pendingLicense = null;
        return this;
      }

      public Builder clearAutomaticInstallErrorMessage() {
        this.installErrorMessage = null;
        return this;
      }

      public State build() {
        return new State(this);
      }
    }
  }
}
