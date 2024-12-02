package org.sonar.server.platform.db.migration.version.v107;

import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.ColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import java.sql.Connection;
import java.sql.SQLException;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

public class AddOrganizationColumnInPortfoliosTable extends DdlChange {

    static final String PORTFOLIOS_TABLE_NAME = "portfolios";
    static final String ORGANIZATION_UUID = "organization_uuid";

    public AddOrganizationColumnInPortfoliosTable(Database db) {
        super(db);
    }

    @Override
    public void execute(Context context) throws SQLException {
        try (Connection connection = getDatabase().getDataSource().getConnection()) {
            if (!DatabaseUtils.tableColumnExists(connection, PORTFOLIOS_TABLE_NAME, ORGANIZATION_UUID)) {
                ColumnDef columnDef = VarcharColumnDef.newVarcharColumnDefBuilder()
                        .setColumnName(ORGANIZATION_UUID)
                        .setIsNullable(false)
                        .setLimit(UUID_SIZE)
                        .build();
                context.execute(new AddColumnsBuilder(getDialect(), PORTFOLIOS_TABLE_NAME)
                        .addColumn(columnDef)
                        .build());
            }
        }
    }
}
