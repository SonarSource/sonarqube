package org.sonar.server.platform.db.migration.version.v99;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class AddOrganizationUuidColumnToAuditsTable extends DdlChange {

    @VisibleForTesting
    static final String TABLE_NAME = "audits";
    @VisibleForTesting
    static final String COLUMN_NAME = "organization_uuid";

    public AddOrganizationUuidColumnToAuditsTable(Database db) {
        super(db);
    }

    @Override
    public void execute(Context context) throws SQLException {
        try (Connection c = getDatabase().getDataSource().getConnection()) {
            if (!DatabaseUtils.tableColumnExists(c, TABLE_NAME, COLUMN_NAME)) {
                context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
                        .addColumn(
                                VarcharColumnDef.newVarcharColumnDefBuilder(COLUMN_NAME)
                                        .setLimit(UUID_SIZE)
                                        .setIsNullable(false)
                                        .build())
                        .build());

                context.execute(new CreateIndexBuilder()
                        .setTable(TABLE_NAME)
                        .setName("audits_organization_created_at")
                        .addColumn(COLUMN_NAME)
                        .addColumn("created_at")
                        .setUnique(false)
                        .build());
            }
        }
    }

}
