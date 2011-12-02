package org.sonar.persistence;

/**
 * @since 2.13
 */
public final class DatabaseUtils {
  private DatabaseUtils() {
  }

  /**
   * List of all the tables.
   * This list is hardcoded because we didn't succeed in using java.sql.DatabaseMetaData#getTables() in the same way
   * for all the supported databases, particularly due to Oracle results.
   */
  public static final String[] TABLE_NAMES = {
      "active_dashboards",
      "active_filters",
      "active_rules",
      "active_rule_changes",
      "active_rule_parameters",
      "active_rule_param_changes",
      "alerts",
      "characteristics",
      "characteristic_edges",
      "characteristic_properties",
      "criteria",
      "dashboards",
      "dependencies",
      "duplications_index",
      "events",
      "filters",
      "filter_columns",
      "groups",
      "groups_users",
      "group_roles",
      "manual_measures",
      "measure_data",
      "metrics",
      "notifications",
      "projects",
      "project_links",
      "project_measures",
      "properties",
      "quality_models",
      "reviews",
      "review_comments",
      "rules",
      "rules_categories",
      "rules_parameters",
      "rules_profiles",
      "rule_failures",
      "schema_migrations",
      "snapshots",
      "snapshot_sources",
      "users",
      "user_roles",
      "widgets",
      "widget_properties"};
}
