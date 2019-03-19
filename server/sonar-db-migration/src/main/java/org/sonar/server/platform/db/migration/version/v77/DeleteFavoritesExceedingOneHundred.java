/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v77;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.Upsert;

import static java.util.Arrays.asList;
import static org.sonar.core.util.stream.MoreCollectors.toList;

@SupportsBlueGreen
public class DeleteFavoritesExceedingOneHundred extends DataChange {

  private static final Logger LOG = Loggers.get(DeleteFavoritesExceedingOneHundred.class);

  private static final String FAVOURITE_PROPERTY = "favourite";

  private static final List<String> SORTED_QUALIFIERS = asList("TRK", "VW", "APP", "SVW", "FIL", "UTS");

  public DeleteFavoritesExceedingOneHundred(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    List<Integer> userIdsHavingMoreThanOneHundredFavourites = context.prepareSelect("SELECT user_id FROM " +
      "(SELECT DISTINCT user_id, COUNT(id) AS nb FROM properties WHERE prop_key = ? AND user_id IS NOT NULL GROUP BY user_id) sub " +
      "WHERE sub.nb > 100")
      .setString(1, FAVOURITE_PROPERTY)
      .list(row -> row.getInt(1));
    LOG.info("Deleting favourites exceeding one hundred elements for {} users", userIdsHavingMoreThanOneHundredFavourites.size());
    for (Integer userId : userIdsHavingMoreThanOneHundredFavourites) {
      List<Integer> propertyIdsToKeep = context.prepareSelect("SELECT prop.id, p.qualifier, p.enabled FROM properties prop " +
        "INNER JOIN projects p ON p.id=prop.resource_id " +
        "WHERE prop.prop_key=? AND prop.user_id = ?")
        .setString(1, FAVOURITE_PROPERTY)
        .setInt(2, userId)
        .list(Property::new)
        .stream()
        .sorted()
        .map(Property::getId)
        .limit(100)
        .collect(toList());

      String idsToString = IntStream.range(0, propertyIdsToKeep.size()).mapToObj(i -> "?").collect(Collectors.joining(","));
      Upsert upsert = context.prepareUpsert("DELETE FROM properties WHERE prop_key=? AND user_id=? AND id NOT in (" + idsToString + ")")
        .setString(1, FAVOURITE_PROPERTY)
        .setInt(2, userId);
      int index = 3;
      for (Integer id : propertyIdsToKeep) {
        upsert.setInt(index, id);
        index++;
      }
      upsert.execute().commit();
    }
  }

  private static class Property implements Comparable<Property> {
    private final int id;
    private final String qualifier;
    private final boolean enabled;

    Property(Select.Row row) throws SQLException {
      this.id = row.getInt(1);
      this.qualifier = row.getString(2);
      this.enabled = row.getBoolean(3);
    }

    int getId() {
      return id;
    }

    String getQualifier() {
      return qualifier;
    }

    boolean isEnabled() {
      return enabled;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Property property = (Property) o;
      return id == property.id;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }

    @Override
    public int compareTo(Property o) {
      return Comparator.comparing(Property::isEnabled)
        .reversed()
        .thenComparing(property -> SORTED_QUALIFIERS.indexOf(property.getQualifier()))
        .compare(this, o);
    }
  }

}
