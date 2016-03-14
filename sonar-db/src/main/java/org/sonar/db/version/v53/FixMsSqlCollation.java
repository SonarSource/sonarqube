/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v53;

import com.google.common.annotations.VisibleForTesting;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.version.AlterColumnsTypeBuilder;
import org.sonar.db.version.DdlChange;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.db.version.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.db.version.ColumnDefValidation.validateColumnName;
import static org.sonar.db.version.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Fix the collation of all columns on MsSQL
 */
public class FixMsSqlCollation extends DdlChange {

  private static final Logger LOGGER = Loggers.get(FixMsSqlCollation.class);

  private final Database db;

  public FixMsSqlCollation(Database db) {
    super(db);
    this.db = db;
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (!getDatabase().getDialect().getId().equals(MsSql.ID)) {
      return;
    }

    // characteristics
    new UpdateTableCollation(context, db, "characteristics")
      .addVarcharColumn("kee", 100)
      .addVarcharColumn("name", 100)
      .addVarcharColumn("function_key", 100)
      .addVarcharColumn("factor_unit", 100)
      .addVarcharColumn("offset_unit", 100)
      .execute();

    // rules_parameters
    new UpdateTableCollation(context, db, "rules_parameters")
      .addNotNullableVarcharColumn("name", 128)
      .addNotNullableVarcharColumn("param_type", 512)
      .addVarcharColumn("default_value", 4000)
      .addVarcharColumn("description", 4000)
      .execute();

    // rules_profiles
    new UpdateTableCollation(context, db, "rules_profiles")
      .addUniqueIndex("uniq_qprof_key", "kee")
      .addNotNullableVarcharColumn("name", 100)
      .addVarcharColumn("language", 20)
      .addNotNullableVarcharColumn("kee", 255)
      .addVarcharColumn("parent_kee", 255)
      .addVarcharColumn("rules_updated_at", 100)
      .execute();

    // project_qprofiles
    new UpdateTableCollation(context, db, "project_qprofiles")
      .addUniqueIndex("uniq_project_qprofiles", "project_uuid", "profile_key")
      .addNotNullableVarcharColumn("project_uuid", 50)
      .addNotNullableVarcharColumn("profile_key", 255)
      .execute();

    // widgets
    new UpdateTableCollation(context, db, "widgets")
      .addIndex("widgets_widgetkey", "widget_key")
      .addNotNullableVarcharColumn("widget_key", 256)
      .addVarcharColumn("name", 256)
      .addVarcharColumn("description", 1000)
      .execute();

    // groups
    new UpdateTableCollation(context, db, "groups")
      .addVarcharColumn("name", 500)
      .addVarcharColumn("description", 200)
      .execute();

    // snapshots
    new UpdateTableCollation(context, db, "snapshots")
      .addIndex("snapshots_qualifier", "qualifier")
      .addVarcharColumn("scope", 3)
      .addVarcharColumn("qualifier", 10)
      .addVarcharColumn("version", 500)
      .addVarcharColumn("path", 500)
      .execute();

    // schema_migrations
    new UpdateTableCollation(context, db, "schema_migrations")
      .addIndex("unique_schema_migrations", "version")
      .addNotNullableVarcharColumn("version", 256)
      .execute();

    // group_roles
    new UpdateTableCollation(context, db, "group_roles")
      .addUniqueIndex("uniq_group_roles", "group_id", "resource_id", "role")
      .addIndex("group_roles_role", "role")
      .addNotNullableVarcharColumn("role", 64)
      .execute();

    // Rules
    new UpdateTableCollation(context, db, "rules")
      .addUniqueIndex("rules_repo_key", "plugin_name", "plugin_rule_key")
      .addNotNullableVarcharColumn("plugin_rule_key", 200)
      .addNotNullableVarcharColumn("plugin_name", 255)
      .addClobColumn("description")
      .addVarcharColumn("description_format", 20)
      .addVarcharColumn("plugin_config_key", 500)
      .addVarcharColumn("name", 200)
      .addVarcharColumn("status", 40)
      .addVarcharColumn("language", 20)
      .addClobColumn("note_data")
      .addVarcharColumn("note_user_login", 255)
      .addVarcharColumn("remediation_function", 20)
      .addVarcharColumn("default_remediation_function", 20)
      .addVarcharColumn("remediation_coeff", 20)
      .addVarcharColumn("default_remediation_coeff", 20)
      .addVarcharColumn("remediation_offset", 20)
      .addVarcharColumn("default_remediation_offset", 20)
      .addVarcharColumn("effort_to_fix_description", 4000)
      .addVarcharColumn("tags", 4000)
      .addVarcharColumn("system_tags", 4000)
      .execute();

    // widget_properties
    new UpdateTableCollation(context, db, "widget_properties")
      .addVarcharColumn("kee", 100)
      .addVarcharColumn("text_value", 4000)
      .execute();

    // events
    new UpdateTableCollation(context, db, "events")
      .addIndex("events_component_uuid", "component_uuid")
      .addVarcharColumn("name", 400)
      .addVarcharColumn("component_uuid", 50)
      .addVarcharColumn("category", 50)
      .addVarcharColumn("description", 4000)
      .addVarcharColumn("event_data", 4000)
      .execute();

    // quality_gates
    new UpdateTableCollation(context, db, "quality_gates")
      .addUniqueIndex("uniq_quality_gates", "name")
      .addVarcharColumn("name", 100)
      .execute();

    // quality_gate_conditions
    new UpdateTableCollation(context, db, "quality_gate_conditions")
      .addVarcharColumn("operator", 3)
      .addVarcharColumn("value_error", 64)
      .addVarcharColumn("value_warning", 64)
      .execute();

    // properties
    new UpdateTableCollation(context, db, "properties")
      .addIndex("properties_key", "prop_key")
      .addVarcharColumn("prop_key", 512)
      .addClobColumn("text_value")
      .execute();

    // project_links
    new UpdateTableCollation(context, db, "project_links")
      .addVarcharColumn("component_uuid", 50)
      .addVarcharColumn("link_type", 20)
      .addVarcharColumn("name", 128)
      .addNotNullableVarcharColumn("href", 2048)
      .execute();

    // duplications_index
    new UpdateTableCollation(context, db, "duplications_index")
      .addIndex("duplications_index_hash", "hash")
      .addNotNullableVarcharColumn("hash", 50)
      .execute();

    // project_measures
    new UpdateTableCollation(context, db, "project_measures")
      .addVarcharColumn("text_value", 4000)
      .addVarcharColumn("alert_status", 5)
      .addVarcharColumn("alert_text", 4000)
      .addVarcharColumn("url", 2000)
      .addVarcharColumn("description", 4000)
      .execute();

    // projects
    new UpdateTableCollation(context, db, "projects")
      .addUniqueIndex("projects_kee", "kee")
      .addUniqueIndex("projects_uuid", "uuid")
      .addIndex("projects_project_uuid", "project_uuid")
      .addIndex("projects_module_uuid", "module_uuid")
      .addIndex("projects_qualifier", "qualifier")
      .addVarcharColumn("kee", 400)
      .addVarcharColumn("uuid", 50)
      .addVarcharColumn("project_uuid", 50)
      .addVarcharColumn("module_uuid", 50)
      .addVarcharColumn("module_uuid_path", 4000)
      .addVarcharColumn("name", 256)
      .addVarcharColumn("description", 2000)
      .addVarcharColumn("scope", 3)
      .addVarcharColumn("qualifier", 10)
      .addVarcharColumn("deprecated_kee", 400)
      .addVarcharColumn("path", 2000)
      .addVarcharColumn("language", 20)
      .addVarcharColumn("long_name", 256)
      .execute();

    // manual_measures
    new UpdateTableCollation(context, db, "manual_measures")
      .addIndex("manual_measures_component_uuid", "component_uuid")
      .addVarcharColumn("component_uuid", 50)
      .addVarcharColumn("text_value", 4000)
      .addVarcharColumn("user_login", 255)
      .addVarcharColumn("description", 4000)
      .execute();

    // active_rules
    new UpdateTableCollation(context, db, "active_rules")
      .addVarcharColumn("inheritance", 10)
      .execute();

    // user_roles
    new UpdateTableCollation(context, db, "user_roles")
      .addNotNullableVarcharColumn("role", 64)
      .execute();

    // active_rule_parameters
    new UpdateTableCollation(context, db, "active_rule_parameters")
      .addVarcharColumn("rules_parameter_key", 128)
      .addVarcharColumn("value", 4000)
      .execute();

    // users
    new UpdateTableCollation(context, db, "users")
      .addUniqueIndex("users_login", "login")
      .addVarcharColumn("login", 255)
      .addVarcharColumn("name", 200)
      .addVarcharColumn("email", 100)
      .addVarcharColumn("crypted_password", 40)
      .addVarcharColumn("salt", 40)
      .addVarcharColumn("remember_token", 500)
      .addVarcharColumn("scm_accounts", 4000)
      .execute();

    // dashboards
    new UpdateTableCollation(context, db, "dashboards")
      .addVarcharColumn("name", 256)
      .addVarcharColumn("description", 1000)
      .addVarcharColumn("column_layout", 20)
      .execute();

    // metrics
    new UpdateTableCollation(context, db, "metrics")
      .addUniqueIndex("metrics_unique_name", "name")
      .addNotNullableVarcharColumn("name", 64)
      .addVarcharColumn("description", 255)
      .addVarcharColumn("domain", 64)
      .addVarcharColumn("short_name", 64)
      .addVarcharColumn("val_type", 8)
      .execute();

    // loaded_templates
    new UpdateTableCollation(context, db, "loaded_templates")
      .addVarcharColumn("kee", 200)
      .addVarcharColumn("template_type", 15)
      .execute();

    // resource_index
    new UpdateTableCollation(context, db, "resource_index")
      .addIndex("resource_index_key", "kee")
      .addNotNullableVarcharColumn("kee", 400)
      .addNotNullableVarcharColumn("qualifier", 10)
      .execute();

    // action_plans
    new UpdateTableCollation(context, db, "action_plans")
      .addVarcharColumn("kee", 100)
      .addVarcharColumn("name", 200)
      .addVarcharColumn("description", 1000)
      .addVarcharColumn("user_login", 255)
      .addVarcharColumn("status", 10)
      .execute();

    // authors
    new UpdateTableCollation(context, db, "authors")
      .addUniqueIndex("uniq_author_logins", "login")
      .addVarcharColumn("login", 100)
      .execute();

    // measure_filters
    new UpdateTableCollation(context, db, "measure_filters")
      .addIndex("measure_filters_name", "name")
      .addNotNullableVarcharColumn("name", 100)
      .addVarcharColumn("description", 4000)
      .addClobColumn("data")
      .execute();

    // issues
    new UpdateTableCollation(context, db, "issues")
      .addUniqueIndex("issues_kee", "kee")
      .addIndex("issues_component_uuid", "component_uuid")
      .addIndex("issues_project_uuid", "project_uuid")
      .addIndex("issues_severity", "severity")
      .addIndex("issues_status", "status")
      .addIndex("issues_resolution", "resolution")
      .addIndex("issues_assignee", "assignee")
      .addIndex("issues_action_plan_key", "action_plan_key")
      .addNotNullableVarcharColumn("kee", 50)
      .addVarcharColumn("component_uuid", 50)
      .addVarcharColumn("project_uuid", 50)
      .addVarcharColumn("severity", 10)
      .addVarcharColumn("message", 4000)
      .addVarcharColumn("status", 20)
      .addVarcharColumn("resolution", 20)
      .addVarcharColumn("checksum", 1000)
      .addVarcharColumn("reporter", 255)
      .addVarcharColumn("assignee", 255)
      .addVarcharColumn("author_login", 255)
      .addVarcharColumn("action_plan_key", 50)
      .addVarcharColumn("issue_attributes", 4000)
      .addVarcharColumn("tags", 4000)
      .execute();

    // issue_changes
    new UpdateTableCollation(context, db, "issue_changes")
      .addIndex("issue_changes_kee", "kee")
      .addIndex("issue_changes_issue_key", "issue_key")
      .addVarcharColumn("kee", 50)
      .addNotNullableVarcharColumn("issue_key", 50)
      .addVarcharColumn("user_login", 255)
      .addVarcharColumn("change_type", 40)
      .addClobColumn("change_data")
      .execute();

    // issue_filters
    new UpdateTableCollation(context, db, "issue_filters")
      .addIndex("issue_filters_name", "name")
      .addNotNullableVarcharColumn("name", 100)
      .addVarcharColumn("user_login", 255)
      .addVarcharColumn("description", 4000)
      .addClobColumn("data")
      .execute();

    // issue_filter_favourites
    new UpdateTableCollation(context, db, "issue_filter_favourites")
      .addIndex("issue_filter_favs_user", "user_login")
      .addNotNullableVarcharColumn("user_login", 255)
      .execute();

    // permission_templates
    new UpdateTableCollation(context, db, "permission_templates")
      .addNotNullableVarcharColumn("name", 100)
      .addNotNullableVarcharColumn("kee", 100)
      .addVarcharColumn("description", 4000)
      .addVarcharColumn("key_pattern", 500)
      .execute();

    // perm_templates_users
    new UpdateTableCollation(context, db, "perm_templates_users")
      .addNotNullableVarcharColumn("permission_reference", 64)
      .execute();

    // perm_templates_groups
    new UpdateTableCollation(context, db, "perm_templates_groups")
      .addNotNullableVarcharColumn("permission_reference", 64)
      .execute();

    // activities
    new UpdateTableCollation(context, db, "activities")
      .addUniqueIndex("activities_log_key", "log_key")
      .addVarcharColumn("log_key", 250)
      .addVarcharColumn("user_login", 255)
      .addVarcharColumn("log_type", 250)
      .addVarcharColumn("log_action", 250)
      .addVarcharColumn("log_message", 250)
      .addClobColumn("data_field")
      .execute();

    // file_sources
    new UpdateTableCollation(context, db, "file_sources")
      .addIndex("file_sources_project_uuid", "project_uuid")
      .addUniqueIndex("file_sources_uuid_type", "file_uuid", "data_type")
      .addNotNullableVarcharColumn("project_uuid", 50)
      .addNotNullableVarcharColumn("file_uuid", 50)
      .addClobColumn("line_hashes")
      .addVarcharColumn("data_type", 20)
      .addVarcharColumn("data_hash", 50)
      .addVarcharColumn("src_hash", 50)
      .addVarcharColumn("revision", 100)
      .execute();

    // ce_queue
    new UpdateTableCollation(context, db, "ce_queue")
      .addUniqueIndex("ce_queue_uuid", "uuid")
      .addNotNullableVarcharColumn("uuid", 40)
      .addNotNullableVarcharColumn("task_type", 15)
      .addVarcharColumn("component_uuid", 40)
      .addNotNullableVarcharColumn("status", 15)
      .addVarcharColumn("submitter_login", 255)
      .execute();

    // ce_activity
    new UpdateTableCollation(context, db, "ce_activity")
      .addUniqueIndex("ce_activity_uuid", "uuid")
      .addIndex("ce_activity_component_uuid", "component_uuid")
      .addNotNullableVarcharColumn("uuid", 40)
      .addNotNullableVarcharColumn("task_type", 15)
      .addVarcharColumn("component_uuid", 40)
      .addNotNullableVarcharColumn("status", 15)
      .addNotNullableVarcharColumn("is_last_key", 55)
      .addVarcharColumn("submitter_login", 255)
      .execute();
  }

  @VisibleForTesting
  static class UpdateTableCollation {
    private final String table;
    private final List<Index> indexes = new ArrayList<>();
    private final AlterColumnsTypeBuilder alterColumnsBuilder;
    private final Context context;

    protected UpdateTableCollation(Context context, Database db, String table) {
      this.context = context;
      this.table = requireNonNull(table);
      this.alterColumnsBuilder = new AlterColumnsTypeBuilder(db.getDialect(), table);
      LOGGER.info("Updating columns from table {}", table);
    }

    public UpdateTableCollation addVarcharColumn(String name, int size) {
      addVarcharColumn(name, size, true);
      return this;
    }

    public UpdateTableCollation addNotNullableVarcharColumn(String name, int size) {
      addVarcharColumn(name, size, false);
      return this;
    }

    private UpdateTableCollation addVarcharColumn(String name, int size, boolean isNullable) {
      alterColumnsBuilder.updateColumn(newVarcharColumnDefBuilder().setColumnName(name).setLimit(size).setIsNullable(isNullable).build());
      return this;
    }

    public UpdateTableCollation addClobColumn(String name) {
      alterColumnsBuilder.updateColumn(newClobColumnDefBuilder().setColumnName(name).build());
      return this;
    }

    public UpdateTableCollation addIndex(String indexName, String... columns) {
      addIndex(indexName, false, columns);
      return this;
    }

    public UpdateTableCollation addUniqueIndex(String indexName, String... columns) {
      addIndex(indexName, true, columns);
      return this;
    }

    private UpdateTableCollation addIndex(String indexName, boolean unique, String... columns) {
      indexes.add(new Index(indexName, unique, columns));
      return this;
    }

    public void execute() throws SQLException {
      removeIndexes();
      updateCollation();
      addIndexes();
    }

    private void updateCollation() throws SQLException {
      context.execute(alterColumnsBuilder.build());
    }

    private void removeIndexes() {
      for (Index index : indexes) {
        try {
          context.execute(dropIndex(index));
        } catch (SQLException e) {
          LOGGER.warn("Could not remove index '{}' on table '{}'. It probably doesn't exist, it will be ignored", index, table, e);
        }
      }
    }

    private void addIndexes() throws SQLException {
      for (Index index : indexes) {
        context.execute(createIndex(index));
      }
    }

    private String dropIndex(Index index) {
      return "DROP INDEX " + index.indexName + " ON " + table;
    }

    private String createIndex(Index index) {
      StringBuilder sql = new StringBuilder().append("CREATE ");
      if (index.unique) {
        sql.append("UNIQUE ");
      }
      sql.append("INDEX ").append(index.indexName).append(" ON ").append(table).append("(");
      for (int columnIndex = 0; columnIndex < index.columnNames.length; columnIndex++) {
        sql.append(index.columnNames[columnIndex]);
        if (columnIndex < index.columnNames.length - 1) {
          sql.append(",");
        }
      }
      sql.append(")");

      return sql.toString();
    }

    private static class Index {
      private final String indexName;
      private final boolean unique;
      private final String[] columnNames;

      public Index(String indexName, boolean unique, String[] columnNames) {
        this.indexName = validateColumnName(requireNonNull(indexName));
        this.unique = unique;
        this.columnNames = validateColumns(columnNames);
      }

      private static String[] validateColumns(String[] columnNames) {
        checkState(columnNames.length > 0, "At least one column must be added");
        for (String column : columnNames) {
          validateColumnName(column);
        }
        return columnNames;
      }
    }
  }

}
