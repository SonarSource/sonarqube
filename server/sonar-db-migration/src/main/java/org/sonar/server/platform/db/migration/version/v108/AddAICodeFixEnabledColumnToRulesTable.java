package org.sonar.server.platform.db.migration.version.v108;

import static org.sonar.db.DatabaseUtils.tableColumnExists;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.BooleanColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class AddAICodeFixEnabledColumnToRulesTable extends DdlChange {

    static final String RULES_TABLE_NAME = "rules";
    static final String AI_CODE_FIX_ENABLED_COLUMN_NAME = "ai_code_fix_enabled";

    public AddAICodeFixEnabledColumnToRulesTable(Database db) {
        super(db);
    }

    @Override
    public void execute(Context context) throws SQLException {
        try (var connection = getDatabase().getDataSource().getConnection()) {
            if (!tableColumnExists(connection, RULES_TABLE_NAME, AI_CODE_FIX_ENABLED_COLUMN_NAME)) {
                var columnDef = BooleanColumnDef.newBooleanColumnDefBuilder()
                        .setColumnName(AI_CODE_FIX_ENABLED_COLUMN_NAME).setIsNullable(false).setDefaultValue(false)
                        .build();
                context.execute(new AddColumnsBuilder(getDialect(), RULES_TABLE_NAME).addColumn(columnDef).build());
            }
        }
    }
}




