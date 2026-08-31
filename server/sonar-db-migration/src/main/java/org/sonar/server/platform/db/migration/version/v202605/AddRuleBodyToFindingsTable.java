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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.db.DatabaseUtils.tableColumnExists;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Adds the detection agent's ad-hoc rule body to {@code findings}: the rule's title, its MQR impact
 * severity, and the two markdown halves it generates alongside every finding.
 *
 * <p>The agent regenerates this on every run and nothing on our side kept it, yet an incremental scan
 * has to hand each prior finding back to the agent with its rule block intact — the agent's parser
 * drops a prior whose rule block is missing a field, so an unpersisted body means a baseline quietly
 * shorter than the branch's real one.
 *
 * <p>Typed columns rather than one JSON blob: the four fields are a fixed, flat, all-text shape, so a
 * blob buys no schema flexibility while costing the ability to read them, plus a serialize/parse round
 * trip on every write and every export. The table's existing {@code context} / {@code agent_context}
 * CLOBs are not a counter-example — those hold agent-shaped payloads whose keys are not ours to fix,
 * which is exactly the property these four fields do not have.
 *
 * <p>All four nullable, and in practice all four or none. A row written before these columns existed
 * has no body, and an export has to surface that gap rather than fabricate one; a row with some of the
 * four would be handed back as a prior, dropped by the agent, and shorten the baseline with nothing in
 * the data to say why. SonarCloud pins that down with
 * {@code CHECK (num_nonnulls(...) IN (0, 4))}; this schema carries no CHECK constraints, for the same
 * portability reason as its absent native ENUMs, so the invariant is upheld by the writer instead.
 * That writer is not in this repository: {@code findings} is written only by the hunter capability in
 * {@code sonarqube-unification} ({@code HunterFindingsPersistedTask}, EA-727), which persists the body
 * as a single value or not at all and logs when it drops a partial one. Nothing here reads or writes
 * these columns, so a reader of this class cannot verify the claim from this repository alone.
 *
 * <p>{@code rule_title} is a CLOB rather than a bounded VARCHAR because the agent's title is generated
 * prose with no length contract, and a truncated title is a corrupted prior. The cost is Oracle's:
 * a CLOB cannot be compared, grouped or indexed there, so this column is storage for round-tripping
 * and not a queryable field. If a query on it is ever needed, that is a migration to a bounded
 * VARCHAR plus a length contract with the agent, not a cast at read time.
 *
 * <p>{@code rule_impact_severity} is one value although an MQR rule can carry a severity per software
 * quality. It is the agent's own {@code impact_severity} scalar, copied verbatim
 * ({@code BLOCKER}/{@code HIGH}/{@code MEDIUM}/{@code LOW}/{@code INFO}) — the agent emits exactly
 * one, and the point of these columns is to hand its rule block back unchanged. It is deliberately
 * not a projection of a multi-impact set, and if the agent ever emits several this column is the
 * wrong shape rather than a lossy one.
 */
public class AddRuleBodyToFindingsTable extends DdlChange {

  static final String TABLE_NAME = "findings";

  static final String COLUMN_RULE_TITLE = "rule_title";
  static final String COLUMN_RULE_IMPACT_SEVERITY = "rule_impact_severity";
  static final String COLUMN_RULE_WHY_IS_THIS_AN_ISSUE = "rule_why_is_this_an_issue";
  static final String COLUMN_RULE_HOW_TO_FIX = "rule_how_to_fix";

  static final int RULE_IMPACT_SEVERITY_SIZE = 20;

  public AddRuleBodyToFindingsTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!tableColumnExists(connection, TABLE_NAME, COLUMN_RULE_TITLE)) {
        context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
          // A rule title is short in practice, but it is agent-generated prose with no contractual
          // bound, and the sibling markdown columns are large text anyway.
          .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_RULE_TITLE).setIsNullable(true).build())
          .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_RULE_IMPACT_SEVERITY).setIsNullable(true)
            .setLimit(RULE_IMPACT_SEVERITY_SIZE).build())
          .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_RULE_WHY_IS_THIS_AN_ISSUE).setIsNullable(true).build())
          .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_RULE_HOW_TO_FIX).setIsNullable(true).build())
          .build());
      }
    }
  }
}
