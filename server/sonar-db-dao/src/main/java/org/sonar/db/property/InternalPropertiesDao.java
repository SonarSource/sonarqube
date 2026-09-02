/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db.property;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.PropertyNewValue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;

public class InternalPropertiesDao implements Dao {

  private static final Logger LOG = LoggerFactory.getLogger(InternalPropertiesDao.class);

  /**
   * A common prefix used by locks. {@see InternalPropertiesDao#tryLock}
   */
  private static final String LOCK_PREFIX = "lock.";

  private static final int KEY_MAX_LENGTH = 40;
  public static final int LOCK_NAME_MAX_LENGTH = KEY_MAX_LENGTH - LOCK_PREFIX.length();

  private static final int TEXT_VALUE_MAX_LENGTH = 4000;
  private static final char STAMP_SEPARATOR = ':';
  private static final int MAX_RAISE_ATTEMPTS = 8;
  private static final Optional<String> OPTIONAL_OF_EMPTY_STRING = Optional.of("");

  private final System2 system2;
  private final AuditPersister auditPersister;

  public InternalPropertiesDao(System2 system2, AuditPersister auditPersister) {
    this.system2 = system2;
    this.auditPersister = auditPersister;
  }

  /**
   * Save a property which value is not empty.
   * <p>Value can't be {@code null} but can have any size except 0.</p>
   *
   * @throws IllegalArgumentException if {@code key} or {@code value} is {@code null} or empty.
   *
   * @see #saveAsEmpty(DbSession, String)
   */
  public void save(DbSession dbSession, String key, @Nullable String value) {
    checkKey(key);
    checkArgument(value != null && !value.isEmpty(), "value can't be null nor empty");

    InternalPropertiesMapper mapper = getMapper(dbSession);
    int deletedRows = mapper.deleteByKey(key);
    long now = system2.now();
    if (mustsBeStoredInClob(value)) {
      mapper.insertAsClob(key, value, now);
    } else {
      mapper.insertAsText(key, value, now);
    }

    if (auditPersister.isTrackedProperty(key)) {
      if (deletedRows > 0) {
        auditPersister.updateProperty(dbSession, new PropertyNewValue(key, value), false);
      } else {
        auditPersister.addProperty(dbSession, new PropertyNewValue(key, value), false);
      }
    }
  }

  private static boolean mustsBeStoredInClob(String value) {
    return value.length() > TEXT_VALUE_MAX_LENGTH;
  }

  /**
   * Save a property which value is empty.
   */
  public void saveAsEmpty(DbSession dbSession, String key) {
    checkKey(key);

    InternalPropertiesMapper mapper = getMapper(dbSession);
    int deletedRows = mapper.deleteByKey(key);
    mapper.insertAsEmpty(key, system2.now());

    if (auditPersister.isTrackedProperty(key)) {
      if (deletedRows > 0) {
        auditPersister.updateProperty(dbSession, new PropertyNewValue(key, ""), false);
      } else {
        auditPersister.addProperty(dbSession, new PropertyNewValue(key, ""), false);
      }
    }
  }

  public void delete(DbSession dbSession, String key) {
    int deletedRows = getMapper(dbSession).deleteByKey(key);

    if (deletedRows > 0 && auditPersister.isTrackedProperty(key)) {
      auditPersister.deleteProperty(dbSession, new PropertyNewValue(key), false);
    }
  }

  /**
   * @return a Map with an {link Optional<String>} for each String in {@code keys}.
   */
  public Map<String, Optional<String>> selectByKeys(DbSession dbSession, @Nullable Set<String> keys) {
    if (keys == null || keys.isEmpty()) {
      return Collections.emptyMap();
    }
    if (keys.size() == 1) {
      String key = keys.iterator().next();
      return ImmutableMap.of(key, selectByKey(dbSession, key));
    }
    keys.forEach(InternalPropertiesDao::checkKey);

    InternalPropertiesMapper mapper = getMapper(dbSession);
    List<InternalPropertyDto> res = mapper.selectAsText(ImmutableList.copyOf(keys));
    Map<String, Optional<String>> builder = HashMap.newHashMap(keys.size());
    res.forEach(internalPropertyDto -> {
      String key = internalPropertyDto.getKey();
      if (internalPropertyDto.isEmpty()) {
        builder.put(key, OPTIONAL_OF_EMPTY_STRING);
      }
      if (internalPropertyDto.getValue() != null) {
        builder.put(key, Optional.of(internalPropertyDto.getValue()));
      }
    });
    // return Optional.empty() for all keys without a DB entry
    Sets.difference(keys, res.stream().map(InternalPropertyDto::getKey).collect(Collectors.toSet()))
      .forEach(key -> builder.put(key, Optional.empty()));
    // keys for which there isn't a text or empty value found yet
    List<String> keyWithClobValue = ImmutableList.copyOf(Sets.difference(keys, builder.keySet()));
    if (keyWithClobValue.isEmpty()) {
      return ImmutableMap.copyOf(builder);
    }

    // retrieve properties with a clob value
    res = mapper.selectAsClob(keyWithClobValue);
    res.forEach(internalPropertyDto -> builder.put(internalPropertyDto.getKey(), Optional.of(internalPropertyDto.getValue())));

    // return Optional.empty() for all key with a DB entry which neither has text value, nor is empty nor has clob value
    Sets.difference(ImmutableSet.copyOf(keyWithClobValue), builder.keySet()).forEach(key -> builder.put(key, Optional.empty()));

    return ImmutableMap.copyOf(builder);
  }

  /**
   * No streaming of value
   */
  public Optional<String> selectByKey(DbSession dbSession, String key) {
    checkKey(key);

    InternalPropertiesMapper mapper = getMapper(dbSession);
    InternalPropertyDto res = enforceSingleElement(key, mapper.selectAsText(singletonList(key)));
    if (res == null) {
      return Optional.empty();
    }
    if (res.isEmpty()) {
      return OPTIONAL_OF_EMPTY_STRING;
    }
    if (res.getValue() != null) {
      return Optional.of(res.getValue());
    }
    res = enforceSingleElement(key, mapper.selectAsClob(singletonList(key)));
    if (res == null) {
      LOG.debug("Internal property {} has been found in db but has neither text value nor is empty. " +
        "Still it couldn't be retrieved with clob value. Ignoring the property.", key);
      return Optional.empty();
    }
    return Optional.of(res.getValue());
  }

  @CheckForNull
  private static InternalPropertyDto enforceSingleElement(String key, List<InternalPropertyDto> rows) {
    if (rows.isEmpty()) {
      return null;
    }
    int size = rows.size();
    checkState(size <= 1, "%s rows retrieved for single property %s", size, key);
    return rows.getFirst();
  }

  /**
   * Raise a decimal-long text property to {@code value} if it is absent or strictly smaller.
   * Returns the resulting stored value. Concurrent callers observe the maximum, using the same
   * insert-or-{@link InternalPropertiesMapper#replaceValue} compare-and-set as {@link #tryLock}.
   * A non-numeric row (empty, unparseable, clob) is overwritten with {@link #save}.
   *
   * <p>A lost insert (duplicate key) rolls {@code dbSession} back before retrying: PostgreSQL
   * aborts the transaction on the failed statement. Callers that share this session with other
   * uncommitted writes must not use this method.
   */
  public long raiseTextIfGreater(DbSession dbSession, String key, long value) {
    checkKey(key);
    return casRaise(dbSession, key, Long.toString(value), value, current -> {
      Long stored = parseNumeric(current);
      if (stored == null) {
        return new Unreadable();
      }
      return stored >= value ? new Satisfied(stored) : new Stale();
    });
  }

  @CheckForNull
  private Long insertTextOrRetry(DbSession dbSession, InternalPropertiesMapper mapper, String key, long value, String asText) {
    try {
      mapper.insertAsText(key, asText, system2.now());
      return value;
    } catch (Exception e) {
      dbSession.rollback();
      if (selectByKey(dbSession, key).isEmpty()) {
        throw new IllegalStateException("Failed to insert internal property: " + key, e);
      }
      LOG.debug("Lost insert race for internal property {}", key, e);
      return null;
    }
  }

  /**
   * Read, decide, and write only if nothing changed underneath, retrying on contention. Shared by every
   * compare-and-set raise on this DAO so the retry and failure semantics are defined once; the callers differ only
   * in how they read the stored value.
   */
  private long casRaise(DbSession dbSession, String key, String encoded, long value, Function<String, Stored> interpret) {
    InternalPropertiesMapper mapper = getMapper(dbSession);
    for (int attempt = 0; attempt < MAX_RAISE_ATTEMPTS; attempt++) {
      Optional<String> current = selectByKey(dbSession, key);
      if (current.isEmpty()) {
        Long inserted = insertTextOrRetry(dbSession, mapper, key, value, encoded);
        if (inserted != null) {
          return inserted;
        }
        continue;
      }
      Stored stored = interpret.apply(current.get());
      if (stored instanceof Satisfied(long satisfied)) {
        return satisfied;
      }
      if (stored instanceof Unreadable) {
        // Not in the expected format (empty, unparseable, or clob-backed), so there is nothing to compare
        // against and the compare-and-set could never match. Overwrite it outright.
        save(dbSession, key, encoded);
        return value;
      }
      if (mapper.replaceValue(key, current.get(), encoded) == 1) {
        return value;
      }
    }
    return afterAttemptsExhausted(dbSession, key, value, interpret);
  }

  /** A writer that beat us to it may already have stored something that satisfies the request. */
  private long afterAttemptsExhausted(DbSession dbSession, String key, long value,
    Function<String, Stored> interpret) {
    Optional<String> current = selectByKey(dbSession, key);
    if (current.isPresent() && interpret.apply(current.get()) instanceof Satisfied(long satisfied)) {
      return satisfied;
    }
    throw new IllegalStateException("Failed to raise internal property " + key + " to " + value
      + " after " + MAX_RAISE_ATTEMPTS + " attempts (stored=" + current.orElse(null) + ")");
  }

  /** What the currently stored value means for a pending raise. */
  private sealed interface Stored {
  }

  /** Already at or above what was asked for: no write needed. */
  private record Satisfied(long value) implements Stored {
  }

  /** Understood, but lower or from an earlier window: compare-and-set over it. */
  private record Stale() implements Stored {
  }

  /** Not in the expected format, so there is nothing to compare against. */
  private record Unreadable() implements Stored {
  }

  /**
   * Raise a window-stamped maximum. Keeps the highest {@code value} seen while {@code stamp} is unchanged, and
   * starts again from {@code value} when the stamp differs — so a window rolls lazily on the next write, with
   * nothing scheduled to reset it. Returns the maximum in force after the call.
   *
   * <p>The pair is stored as {@code <stamp>:<value>} in a single row, so a reader can never see a stamp from one
   * window beside a maximum from another. Uses the same insert-or-{@link InternalPropertiesMapper#replaceValue}
   * compare-and-set as {@link #raiseTextIfGreater}; an unparseable row is overwritten with {@link #save}.
   *
   * <p>Deliberately unaudited, as {@link #raiseTextIfGreater} is: both are internal counters, never a tracked
   * property.
   *
   * <p>A lost insert (duplicate key) rolls {@code dbSession} back before retrying, so callers must not share this
   * session with other uncommitted writes.
   */
  public long raiseStampedMax(DbSession dbSession, String key, String stamp, long value) {
    checkKey(key);
    checkArgument(!stamp.isEmpty() && stamp.indexOf(STAMP_SEPARATOR) < 0,
      "stamp can't be empty nor contain '%s'", STAMP_SEPARATOR);
    String encoded = stamp + STAMP_SEPARATOR + value;
    checkArgument(encoded.length() <= TEXT_VALUE_MAX_LENGTH, "stamped value is too long to compare-and-set");
    return casRaise(dbSession, key, encoded, value, current -> parseStamped(current)
      .map(stored -> stored.stamp().equals(stamp) && stored.max() >= value
        ? (Stored) new Satisfied(stored.max())
        : new Stale())
      .orElseGet(Unreadable::new));
  }

  /**
   * The stamped maximum stored under {@code key}, when it belongs to {@code stamp}. Empty when nothing is stored,
   * the row is unparseable, or the stored value is left over from an earlier window — an earlier window says
   * nothing about this one.
   */
  public OptionalLong selectStampedMax(DbSession dbSession, String key, String stamp) {
    checkKey(key);
    return parseStamped(selectByKey(dbSession, key).orElse(null))
      .filter(stored -> stored.stamp().equals(stamp))
      .map(stored -> OptionalLong.of(stored.max()))
      .orElseGet(OptionalLong::empty);
  }

  private static Optional<StampedMax> parseStamped(@Nullable String raw) {
    if (raw == null) {
      return Optional.empty();
    }
    int split = raw.indexOf(STAMP_SEPARATOR);
    if (split <= 0 || split == raw.length() - 1) {
      return Optional.empty();
    }
    try {
      return Optional.of(new StampedMax(raw.substring(0, split), Long.parseLong(raw.substring(split + 1))));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  private record StampedMax(String stamp, long max) {
  }

  /**
   * Try to acquire a lock with the specified name, for specified duration.
   *
   * Returns false if the lock exists with a timestamp > now - duration,
   * or if the atomic replacement of the timestamp fails (another process replaced first).
   *
   * Returns true if the lock does not exist, or if exists with a timestamp <= now - duration,
   * and the atomic replacement of the timestamp succeeds.
   *
   * The lock is considered released when the specified duration has elapsed.
   *
   * @throws IllegalArgumentException if name's length is > {@link #LOCK_NAME_MAX_LENGTH}
   * @throws IllegalArgumentException if maxAgeInSeconds is <= 0
   */
  public boolean tryLock(DbSession dbSession, String name, int maxAgeInSeconds) {
    if (name.isEmpty()) {
      throw new IllegalArgumentException("lock name can't be empty");
    }
    if (name.length() > LOCK_NAME_MAX_LENGTH) {
      throw new IllegalArgumentException("lock name is too long");
    }
    if (maxAgeInSeconds <= 0) {
      throw new IllegalArgumentException("maxAgeInSeconds must be > 0");
    }

    String key = LOCK_PREFIX + name;
    long now = system2.now();

    Optional<String> timestampAsStringOpt = selectByKey(dbSession, key);
    if (timestampAsStringOpt.isEmpty()) {
      return tryInsertText(dbSession, key, String.valueOf(now));
    }

    String oldTimestampString = timestampAsStringOpt.get();
    long oldTimestamp = Long.parseLong(oldTimestampString);
    if (oldTimestamp > now - maxAgeInSeconds * 1000L) {
      return false;
    }

    return getMapper(dbSession).replaceValue(key, oldTimestampString, String.valueOf(now)) == 1;
  }

  @CheckForNull
  private static Long parseNumeric(@Nullable String current) {
    if (current == null || current.isEmpty()) {
      return null;
    }
    try {
      return Long.parseLong(current);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private boolean tryInsertText(DbSession dbSession, String key, String value) {
    try {
      getMapper(dbSession).insertAsText(key, value, system2.now());
      return true;
    } catch (Exception e) {
      LOG.debug("Failed to insert internal property {}", key, e);
      return false;
    }
  }

  private static void checkKey(@Nullable String key) {
    checkArgument(key != null && !key.isEmpty(), "key can't be null nor empty");
  }

  private static InternalPropertiesMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(InternalPropertiesMapper.class);
  }
}
