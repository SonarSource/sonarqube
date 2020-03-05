/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.db.dialect;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.Version;

import static com.google.common.base.Preconditions.checkState;

public class CockroachDB extends AbstractDialect {

    public static final String ID = "cockroachdb";
    @VisibleForTesting
    static final List<String> INIT_STATEMENTS = ImmutableList.of("SET standard_conforming_strings=on");
    private static final Version MIN_SUPPORTED_VERSION = Version.create(19, 1, 0);

    private boolean initialized = false;

    public CockroachDB() {
        // CockroachDB currently uses the same driver as PostgreSQL.
        super(ID, "org.postgresql.Driver", "true", "false", "SELECT 1");
    }

    @Override
    public boolean matchesJdbcUrl(String jdbcConnectionURL) {
        return StringUtils.startsWithIgnoreCase(jdbcConnectionURL, "jdbc:cockroachdb:");
    }

    @Override
    public List<String> getConnectionInitStatements() {
        return INIT_STATEMENTS;
    }

    @Override
    public boolean supportsMigration() {
        return true;
    }

    @Override
    public boolean supportsUpsert() {
        checkState(initialized, "onInit() must be called before calling supportsUpsert()");
        return true;
    }

    @Override
    public void init(DatabaseMetaData metaData) throws SQLException {
        checkState(!initialized, "onInit() must be called once");
        checkDbVersion(metaData, MIN_SUPPORTED_VERSION);
        initialized = true;
    }
}
