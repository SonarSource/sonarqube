/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.step;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.Select.Row;
import org.sonar.server.platform.db.migration.step.Select.RowReader;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;

class DataChangeIT {

  private static final int MAX_BATCH_SIZE = 250;
  private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  @RegisterExtension
  public final CoreDbTester db = CoreDbTester.createForSchema(DataChangeIT.class, "schema.sql");

  @BeforeEach
  public void setUp() {
    db.executeUpdateSql("truncate table persons");
  }

  @Test
  void query() throws Exception {
    insertPersons();

    AtomicBoolean executed = new AtomicBoolean(false);
    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        assertThat(context.prepareSelect("select id from persons order by id desc").list(Select.LONG_READER))
          .containsExactly(3L, 2L, 1L);
        assertThat(context.prepareSelect("select id from persons where id=?").setLong(1, 2L).get(Select.LONG_READER))
          .isEqualTo(2L);
        assertThat(context.prepareSelect("select id from persons where id=?").setLong(1, 12345L).get(Select.LONG_READER))
          .isNull();
        executed.set(true);
      }
    }.execute();
    assertThat(executed.get()).isTrue();
  }

  @Test
  void read_column_types() throws Exception {
    insertPersons();

    List<Object[]> persons = new ArrayList<>();
    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        persons.addAll(context
          .prepareSelect("select id,login,age,enabled,updated_at,coeff from persons where id=2")
          .list(new UserReader()));
      }
    }.execute();
    assertThat(persons).hasSize(1);
    assertThat(persons.get(0)[0]).isEqualTo(2L);
    assertThat(persons.get(0)[1]).isEqualTo("emmerik");
    assertThat(persons.get(0)[2]).isEqualTo(14);
    assertThat(persons.get(0)[3]).isEqualTo(true);
    assertThat(persons.get(0)[4]).isNotNull();
    assertThat(persons.get(0)[5]).isEqualTo(5.2);
  }

  @Test
  void parameterized_query() throws Exception {
    insertPersons();

    final List<Long> ids = new ArrayList<>();
    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        ids.addAll(context.prepareSelect("select id from persons where id>=?").setLong(1, 2L).list(Select.LONG_READER));
      }
    }.execute();
    assertThat(ids).containsOnly(2L, 3L);
  }

  @Test
  void display_current_row_details_if_error_during_get() throws Exception {
    insertPersons();

    assertThatThrownBy(() -> {
      new DataChange(db.database()) {
        @Override
        public void execute(Context context) throws SQLException {
          context.prepareSelect("select id from persons where id>=?").setLong(1, 2L).get((RowReader<Long>) row -> {
            throw new IllegalStateException("Unexpected error");
          });
        }
      }.execute();
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Error during processing of row: [id=2]");
  }

  @Test
  void display_current_row_details_if_error_during_list() throws Exception {
    insertPersons();

    assertThatThrownBy(() -> {
      new DataChange(db.database()) {
        @Override
        public void execute(Context context) throws SQLException {
          context.prepareSelect("select id from persons where id>=?").setLong(1, 2L).list((RowReader<Long>) row -> {
            throw new IllegalStateException("Unexpected error");
          });
        }
      }.execute();
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Error during processing of row: [id=2]");
  }

  @Test
  void bad_parameterized_query() throws Exception {
    insertPersons();

    DataChange change = new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        // parameter value is not set
        context.prepareSelect("select id from persons where id>=?").list(Select.LONG_READER);
      }
    };

    assertThatThrownBy(() -> change.execute())
      .isInstanceOf(SQLException.class);
  }

  @Test
  void scroll() throws Exception {
    insertPersons();

    final List<Long> ids = new ArrayList<>();
    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        context.prepareSelect("select id from persons order by id desc").scroll(row -> ids.add(row.getNullableLong(1)));
      }
    }.execute();
    assertThat(ids).containsExactly(3L, 2L, 1L);
  }

  @Test
  void insert() throws Exception {
    insertPersons();

    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        context.prepareUpsert("insert into persons(id,login,age,enabled,coeff) values (?,?,?,?,?)")
          .setLong(1, 10L)
          .setString(2, "kurt")
          .setInt(3, 27)
          .setBoolean(4, true)
          .setDouble(5, 2.2)
          .execute().commit().close();
      }
    }.execute();

    assertThat(db.select("select id as \"ID\" from persons"))
      .extracting(t -> t.get("ID"))
      .containsOnly(1L, 2L, 3L, 10L);
    assertInitialPersons();
    assertPerson(10L, "kurt", 27L, true, null, 2.2d);
  }

  @Test
  void batch_inserts() throws Exception {
    insertPersons();

    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        Upsert upsert = context.prepareUpsert("insert into persons(id,login,age,enabled,coeff) values (?,?,?,?,?)");
        boolean committed = upsert
          .setLong(1, 10L)
          .setString(2, "kurt")
          .setInt(3, 27)
          .setBoolean(4, true)
          .setDouble(5, 2.2)
          .addBatch();
        assertThat(committed).isFalse();

        committed = upsert
          .setLong(1, 11L)
          .setString(2, "courtney")
          .setInt(3, 25)
          .setBoolean(4, false)
          .setDouble(5, 2.3)
          .addBatch();
        assertThat(committed).isFalse();

        upsert.execute().commit().close();
      }
    }.execute();

    assertThat(db.select("select id as \"ID\" from persons"))
      .extracting(t -> t.get("ID"))
      .containsOnly(1L, 2L, 3L, 10L, 11L);
    assertInitialPersons();
    assertPerson(10L, "kurt", 27L, true, null, 2.2d);
    assertPerson(11L, "courtney", 25L, false, null, 2.3d);
  }

  @Test
  void override_size_of_batch_inserts() throws Exception {
    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        Upsert upsert = context.prepareUpsert("insert into persons(id,login,age,enabled,coeff) values (?,?,?,?,?)")
          .setBatchSize(3);
        long id = 100L;
        assertThat(addBatchInsert(upsert, id++)).isFalse();
        assertThat(addBatchInsert(upsert, id++)).isFalse();
        assertThat(addBatchInsert(upsert, id++)).isTrue();
        assertThat(addBatchInsert(upsert, id++)).isFalse();
        assertThat(addBatchInsert(upsert, id++)).isFalse();
        assertThat(addBatchInsert(upsert, id++)).isTrue();
        assertThat(addBatchInsert(upsert, id)).isFalse();
        upsert.execute().commit().close();
      }
    }.execute();
    assertThat(db.countRowsOfTable("persons")).isEqualTo(7);
    for (int i = 100; i < 107; i++) {
      assertPerson(i, "kurt", 27L, true, null, 2.2d);
    }
  }

  private boolean addBatchInsert(Upsert upsert, long id) throws SQLException {
    return upsert
      .setLong(1, id)
      .setString(2, "kurt")
      .setInt(3, 27)
      .setBoolean(4, true)
      .setDouble(5, 2.2)
      .addBatch();
  }

  @Test
  void update_null() throws Exception {
    insertPersons();

    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        Upsert upsert = context.prepareUpsert("update persons set login=?,age=?,enabled=?, updated_at=?, coeff=? where id=?");
        upsert
          .setString(1, null)
          .setInt(2, null)
          .setBoolean(3, null)
          .setDate(4, null)
          .setDouble(5, null)
          .setLong(6, 2L)
          .execute()
          .commit()
          .close();
      }
    }.execute();

    assertPerson(1L, "barbara", 56L, false, "2014-01-25", 1.5d);
    assertPerson(2L, null, null, null, null, null);
    assertPerson(3L, "morgan", 3L, true, "2014-01-25", 5.4d);
  }

  @Test
  void mass_batch_insert() throws Exception {
    db.executeUpdateSql("truncate table persons");

    final int count = MAX_BATCH_SIZE + 10;
    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        Upsert upsert = context.prepareUpsert("insert into persons(id,login,age,enabled,coeff) values (?,?,?,?,?)");
        for (int i = 0; i < count; i++) {
          upsert
            .setLong(1, 10L + i)
            .setString(2, "login" + i)
            .setInt(3, 10 + i)
            .setBoolean(4, true)
            .setDouble(5, i + 0.5)
            .addBatch();
        }
        upsert.execute().commit().close();

      }
    }.execute();

    assertThat(db.countRowsOfTable("persons")).isEqualTo(count);
  }

  @Test
  void scroll_and_update() throws Exception {
    insertPersons();

    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        final Upsert upsert = context.prepareUpsert("update persons set login=?, age=? where id=?");
        context.prepareSelect("select id from persons").scroll(new Select.RowHandler() {
          @Override
          public void handle(Row row) throws SQLException {
            long id = row.getNullableLong(1);
            upsert.setString(1, "login" + id).setInt(2, 10 + (int) id).setLong(3, id);
            upsert.execute();
          }
        });
        upsert.commit().close();
      }
    }.execute();

    assertPerson(1L, "login1", 11L, false, "2014-01-25", 1.5d);
    assertPerson(2L, "login2", 12L, true, "2014-01-25", 5.2d);
    assertPerson(3L, "login3", 13L, true, "2014-01-25", 5.4d);
  }

  @Test
  void display_current_row_details_if_error_during_scroll() throws Exception {
    insertPersons();

    assertThatThrownBy(() -> {
      new DataChange(db.database()) {
        @Override
        public void execute(Context context) throws SQLException {
          final Upsert upsert = context.prepareUpsert("update persons set login=?, age=? where id=?");
          context.prepareSelect("select id from persons").scroll(row -> {
            throw new IllegalStateException("Unexpected error");
          });
          upsert.commit().close();
        }
      }.execute();
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Error during processing of row: [id=1]");
  }

  @Test
  void mass_update() throws Exception {
    insertPersons();

    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        MassUpdate massUpdate = context.prepareMassUpdate();
        massUpdate.select("select id from persons where id>=?").setLong(1, 2L);
        massUpdate.update("update persons set login=?, age=? where id=?");
        massUpdate.execute((row, update) -> {
          long id = row.getNullableLong(1);
          update
            .setString(1, "login" + id)
            .setInt(2, 10 + (int) id)
            .setLong(3, id);
          return true;
        });
      }
    }.execute();

    assertPerson(1L, "barbara", 56L, false, "2014-01-25", 1.5d);
    assertPerson(2L, "login2", 12L, true, "2014-01-25", 5.2d);
    assertPerson(3L, "login3", 13L, true, "2014-01-25", 5.4d);
  }

  @Test
  void row_splitter_should_split_correctly() throws Exception {
    insertPersons();

    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        MassRowSplitter<PhoneNumberRow> massRowSplitter = context.prepareMassRowSplitter();
        massRowSplitter.select("select id, phone_numbers from persons where id>?").setLong(1, -2L);
        massRowSplitter.splitRow(row -> {
          try {
            int personId = row.getInt(1);
            String phoneNumbers = row.getString(2);
            if (phoneNumbers == null) {
              return emptySet();
            }
            return Arrays.stream(StringUtils.split(phoneNumbers, '\n'))
              .map(number -> new PhoneNumberRow(personId, number))
              .collect(toSet());
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        });

        massRowSplitter.insert("insert into phone_numbers (person_id, phone_number) values (?, ?)");
        massRowSplitter.execute((row, insert) -> {
          insert.setLong(1, row.personId())
            .setString(2, row.phoneNumber());
          return true;
        });
      }
    }.execute();

    Set<PhoneNumberRow> actualRows = getPhoneNumberRows();

    assertThat(actualRows)
      .containsExactlyInAnyOrder(
        new PhoneNumberRow(1, "1"),
        new PhoneNumberRow(1, "32234"),
        new PhoneNumberRow(1, "42343"),
        new PhoneNumberRow(2, "432423")
      );
  }

  private Set<PhoneNumberRow> getPhoneNumberRows() {
    return db
      .select("select person_id as personId, phone_number as phoneNumber from phone_numbers")
      .stream()
      .map(row -> new PhoneNumberRow((long) row.get("PERSONID"), (String) row.get("PHONENUMBER")))
      .collect(toSet());
  }

  private record PhoneNumberRow(long personId, String phoneNumber){}

  @Test
  void display_current_row_details_if_error_during_mass_update() throws Exception {
    insertPersons();

    assertThatThrownBy(() -> {
      new DataChange(db.database()) {
        @Override
        public void execute(Context context) throws SQLException {
          MassUpdate massUpdate = context.prepareMassUpdate();
          massUpdate.select("select id from persons where id>=?").setLong(1, 2L);
          massUpdate.update("update persons set login=?, age=? where id=?");
          massUpdate.execute((row, update) -> {
            throw new IllegalStateException("Unexpected error");
          });
        }
      }.execute();
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Error during processing of row: [id=2]");
  }

  @Test
  void mass_update_nothing() throws Exception {
    insertPersons();

    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        MassUpdate massUpdate = context.prepareMassUpdate();
        massUpdate.select("select id from persons where id>=?").setLong(1, 2L);
        massUpdate.update("update persons set login=?, age=? where id=?");
        massUpdate.execute((row, update) -> false);
      }
    }.execute();

    assertInitialPersons();
  }

  @Test
  void bad_mass_update() throws Exception {
    insertPersons();

    DataChange change = new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        MassUpdate massUpdate = context.prepareMassUpdate();
        massUpdate.select("select id from persons where id>=?").setLong(1, 2L);
        // update is not set
        massUpdate.execute((row, update) -> false);
      }
    };
    try {
      change.execute();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("SELECT or UPDATE requests are not defined");
    }
  }

  @Test
  void read_not_null_fields() throws Exception {
    insertPersons();

    List<Object[]> persons = new ArrayList<>();
    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        persons.addAll(context
          .prepareSelect("select id,login,age,enabled,updated_at,coeff from persons where id=2")
          .list(row -> new Object[] {
            // id, login, age, enabled
            row.getLong(1),
            row.getString(2),
            row.getInt(3),
            row.getBoolean(4),
            row.getDate(5),
            row.getDouble(6),
          }));
      }
    }.execute();
    assertThat(persons).hasSize(1);
    assertThat(persons.get(0)[0]).isEqualTo(2L);
    assertThat(persons.get(0)[1]).isEqualTo("emmerik");
    assertThat(persons.get(0)[2]).isEqualTo(14);
    assertThat(persons.get(0)[3]).isEqualTo(true);
    assertThat(persons.get(0)[4]).isNotNull();
    assertThat(persons.get(0)[5]).isEqualTo(5.2);
  }

  static class UserReader implements RowReader<Object[]> {
    @Override
    public Object[] read(Row row) throws SQLException {
      return new Object[] {
        // id, login, age, enabled
        row.getNullableLong(1),
        row.getNullableString(2),
        row.getNullableInt(3),
        row.getNullableBoolean(4),
        row.getNullableDate(5),
        row.getNullableDouble(6),
      };
    }
  }

  private void insertPersons() throws ParseException {
    insertPerson(1, "barbara", 56, false, "2014-01-25", 1.5d, "\n1\n32234\n42343\n");
    insertPerson(2, "emmerik", 14, true, "2014-01-25", 5.2d, "432423");
    insertPerson(3, "morgan", 3, true, "2014-01-25", 5.4d, null);
  }

  private void assertInitialPersons() throws ParseException {
    assertPerson(1L, "barbara", 56L, false, "2014-01-25", 1.5d);
    assertPerson(2L, "emmerik", 14L, true, "2014-01-25", 5.2d);
    assertPerson(3L, "morgan", 3L, true, "2014-01-25", 5.4d);
  }

  private void insertPerson(int id, String login, int age, boolean enabled, String updatedAt, double coeff, @Nullable String newLineSeparatedPhoneNumbers) throws ParseException {
    db.executeInsert("persons",
      "ID", id,
      "LOGIN", login,
      "AGE", age,
      "ENABLED", enabled,
      "UPDATED_AT", dateFormat.parse(updatedAt),
      "COEFF", coeff,
      "PHONE_NUMBERS", newLineSeparatedPhoneNumbers);
  }

  private void assertPerson(long id, @Nullable String login, @Nullable Long age, @Nullable Boolean enabled, @Nullable String updatedAt, @Nullable Double coeff)
    throws ParseException {
    List<Map<String, Object>> rows = db
      .select("select id as \"ID\", login as \"LOGIN\", age as \"AGE\", enabled as \"ENABLED\", coeff as \"COEFF\", updated_at as \"UPDATED\" from persons where id=" + id);
    assertThat(rows).describedAs("id=" + id).hasSize(1);
    Map<String, Object> row = rows.get(0);
    assertThat(row).containsEntry("ID", id);
    assertThat(row).containsEntry("LOGIN", login);
    assertThat(row).containsEntry("AGE", age);
    assertThat(row).containsEntry("ENABLED", enabled);
    assertThat(row).containsEntry("UPDATED", updatedAt == null ? null : dateFormat.parse(updatedAt));
    assertThat(row).containsEntry("COEFF", coeff);
  }
}
