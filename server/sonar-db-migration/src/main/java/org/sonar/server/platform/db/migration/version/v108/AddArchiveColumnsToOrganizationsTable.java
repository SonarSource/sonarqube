package org.sonar.server.platform.db.migration.version.v108;

import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.BooleanColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import java.sql.SQLException;

import static org.sonar.db.DatabaseUtils.tableColumnExists;

public class AddArchiveColumnsToOrganizationsTable extends DdlChange {

    public static final String TABLE_NAME = "organizations";
    public static final String IS_ARCHIVED_COLUMN = "is_archived";
    public static final String ARCHIVED_AT_COLUMN = "archived_at";

    public AddArchiveColumnsToOrganizationsTable(Database db) {
        super(db);
    }

    @Override
    public void execute(Context context) throws SQLException {
        try (var connection = getDatabase().getDataSource().getConnection()) {
            if (!tableColumnExists(connection, TABLE_NAME, IS_ARCHIVED_COLUMN)) {
                var isArchivedColumn = BooleanColumnDef.newBooleanColumnDefBuilder().setColumnName(IS_ARCHIVED_COLUMN).setIsNullable(true).build();
                context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME).addColumn(isArchivedColumn).build());
            }
            if (!tableColumnExists(connection, TABLE_NAME, ARCHIVED_AT_COLUMN)) {
                var archivedAtColumn = BigIntegerColumnDef.newBigIntegerColumnDefBuilder().setColumnName(ARCHIVED_AT_COLUMN).setIsNullable(true).build();
                context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME).addColumn(archivedAtColumn).build());
            }
        }
    }
}
