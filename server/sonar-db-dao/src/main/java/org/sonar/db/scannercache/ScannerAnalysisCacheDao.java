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
package org.sonar.db.scannercache;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbInputStream;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.dialect.PostgreSql;

public class ScannerAnalysisCacheDao implements Dao {
  private static final Logger LOGGER = LoggerFactory.getLogger(ScannerAnalysisCacheDao.class);
  // PostgreSQL hex encodes BYTEA on output (2 x size in one allocation capped at 1 GiB), so values are read in slices
  // https://www.postgresql.org/docs/current/datatype-binary.html#DATATYPE-BINARY-BYTEA-HEX-FORMAT
  // 16 MiB is an arbitrary slice size, not a PostgreSQL limit: tune it for round trips versus memory per query
  private static final int PG_SLICE_BYTES = 16 * 1024 * 1024;
  // Values up to this size are read in one atomic statement. pgjdbc holds the hex text (2 x size) plus the decoded bytes (1 x size)
  // on the web heap, so keep it a small multiple of the slice size.
  private static final long PG_SINGLE_READ_MAX_BYTES = 32L * 1024 * 1024;
  // PostgreSQL rejects field values of 1 GiB or more; the margin covers the value header
  // https://www.postgresql.org/docs/current/limits.html
  private static final long PG_MAX_STORABLE_BYTES = (1024L * 1024 * 1024) - (1024 * 1024);

  private final int sliceBytes;
  private final long singleReadMaxBytes;
  private final long maxStorableBytes;

  public ScannerAnalysisCacheDao() {
    this(PG_SLICE_BYTES, PG_SINGLE_READ_MAX_BYTES, PG_MAX_STORABLE_BYTES);
  }

  ScannerAnalysisCacheDao(int sliceBytes, long singleReadMaxBytes, long maxStorableBytes) {
    this.sliceBytes = sliceBytes;
    this.singleReadMaxBytes = singleReadMaxBytes;
    this.maxStorableBytes = maxStorableBytes;
  }

  public void removeAll(DbSession session) {
    mapper(session).removeAll();
  }

  public void remove(DbSession session, String branchUuid) {
    mapper(session).remove(branchUuid);
  }

  public void insert(DbSession dbSession, String branchUuid, InputStream data) {
    Connection connection = dbSession.getConnection();
    try (PreparedStatement stmt = connection.prepareStatement(
      "INSERT INTO scanner_analysis_cache (branch_uuid, data) VALUES (?, ?)")) {
      stmt.setString(1, branchUuid);
      stmt.setBinaryStream(2, data);
      stmt.executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to insert cache for branch " + branchUuid, e);
    }
  }

  /**
   * Logs a warning message if the database cannot store a value of this size
   *
   * @return false, without inserting anything, if the database cannot store a value of this size
   */
  public boolean insert(DbSession dbSession, String branchUuid, InputStream data, long size) {
    if (isPostgreSql(dbSession) && size > maxStorableBytes) {
      LOGGER.warn("Analysis cache of branch {} ({} bytes) is too large for the database and was not saved. The next analysis of this branch will run without cache.",
        branchUuid, size);
      return false;
    }
    insert(dbSession, branchUuid, data);
    return true;
  }

  public void cleanOlderThan(DbSession session, int days) {
    long timestamp = LocalDateTime.now(ZoneOffset.UTC).minusDays(days).toInstant(ZoneOffset.UTC).toEpochMilli();
    mapper(session).cleanOlderThan(timestamp);
  }

  @CheckForNull
  public DbInputStream selectData(DbSession dbSession, String branchUuid) {
    return isPostgreSql(dbSession) ? selectDataOnPostgreSql(dbSession, branchUuid) : selectDataAtOnce(dbSession, branchUuid);
  }

  @CheckForNull
  private static DbInputStream selectDataAtOnce(DbSession dbSession, String branchUuid) {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    DbInputStream result = null;
    try {
      stmt = dbSession.getConnection().prepareStatement("SELECT data FROM scanner_analysis_cache WHERE branch_uuid=?");
      stmt.setString(1, branchUuid);
      rs = stmt.executeQuery();
      if (rs.next()) {
        result = new DbInputStream(stmt, rs, rs.getBinaryStream(1));
        return result;
      }
      return null;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to select cache for branch " + branchUuid, e);
    } finally {
      // Not try-with-resources: once returned, the DbInputStream owns rs and stmt and closes them when the caller closes it
      if (result == null) {
        DatabaseUtils.closeQuietly(rs);
        DatabaseUtils.closeQuietly(stmt);
      }
    }
  }

  @CheckForNull
  private DbInputStream selectDataOnPostgreSql(DbSession dbSession, String branchUuid) {
    PreparedStatement sizeStmt = null;
    ResultSet rs = null;
    DbInputStream result = null;
    try {
      sizeStmt = dbSession.getConnection().prepareStatement("SELECT octet_length(data), xmin::text FROM scanner_analysis_cache WHERE branch_uuid=?");
      sizeStmt.setString(1, branchUuid);
      rs = sizeStmt.executeQuery();
      if (!rs.next()) {
        return null;
      }
      long storedCacheSize = rs.getLong(1);
      // Small values use the same one-statement read as other databases. If the CE replaces the row after the size query,
      // that read still returns one whole version of the cache, or null if the row was removed.
      if (storedCacheSize <= singleReadMaxBytes) {
        return selectDataAtOnce(dbSession, branchUuid);
      }
      result = new DbInputStream(sizeStmt, rs, new SlicedStream(dbSession.getConnection(), branchUuid, storedCacheSize, rs.getString(2), sliceBytes));
      return result;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to select cache for branch " + branchUuid, e);
    } finally {
      // Same ownership rule; the single read path also lands here and closes the size query
      if (result == null) {
        DatabaseUtils.closeQuietly(rs);
        DatabaseUtils.closeQuietly(sizeStmt);
      }
    }
  }

  private static boolean isPostgreSql(DbSession dbSession) {
    return PostgreSql.ID.equals(dbSession.getConfiguration().getDatabaseId());
  }

  private static ScannerAnalysisCacheMapper mapper(DbSession session) {
    return session.getMapper(ScannerAnalysisCacheMapper.class);
  }

  private static final class SlicedStream extends InputStream {
    private final PreparedStatement sliceStmt;
    private final String branchUuid;
    private final long size;
    private final String rowVersion;
    private final int sliceBytes;
    private long position = 0;
    private byte[] slice = new byte[0];
    private int index = 0;

    private SlicedStream(Connection connection, String branchUuid, long size, String rowVersion, int sliceBytes) throws SQLException {
      this.sliceStmt = connection.prepareStatement("SELECT substring(data FROM ? FOR ?), xmin::text FROM scanner_analysis_cache WHERE branch_uuid=?");
      this.branchUuid = branchUuid;
      this.size = size;
      this.rowVersion = rowVersion;
      this.sliceBytes = sliceBytes;
    }

    @Override
    public void close() {
      DatabaseUtils.closeQuietly(sliceStmt);
    }

    @Override
    public int read() throws IOException {
      if (!ensureData()) {
        return -1;
      }
      int value = slice[index] & 0xFF;
      index++;
      return value;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      Objects.checkFromIndexSize(off, len, b.length);
      if (len == 0) {
        return 0;
      }
      if (!ensureData()) {
        return -1;
      }
      int count = Math.min(len, slice.length - index);
      System.arraycopy(slice, index, b, off, count);
      index += count;
      return count;
    }

    private boolean ensureData() throws IOException {
      if (index < slice.length) {
        return true;
      }
      if (position >= size) {
        return false;
      }
      int length = (int) Math.min(sliceBytes, size - position);
      slice = fetchSlice(length);
      index = 0;
      position += length;
      return true;
    }

    private byte[] fetchSlice(int length) throws IOException {
      try {
        // SQL substring is 1-based
        sliceStmt.setInt(1, Math.toIntExact(position + 1));
        sliceStmt.setInt(2, length);
        sliceStmt.setString(3, branchUuid);
        try (ResultSet rs = sliceStmt.executeQuery()) {
          byte[] bytes = rs.next() ? rs.getBytes(1) : null;
          if (bytes == null || bytes.length != length || !rowVersion.equals(rs.getString(2))) {
            throw new IOException("Cache of branch " + branchUuid + " changed while being read, at offset " + position);
          }
          return bytes;
        }
      } catch (SQLException e) {
        throw new IOException("Fail to read cache of branch " + branchUuid + " at offset " + position, e);
      }
    }
  }
}
