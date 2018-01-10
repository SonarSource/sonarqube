/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.Select.Row;
import org.sonar.server.platform.db.migration.step.Select.RowReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;


public class DataChangeTest {

  private static final int MAX_BATCH_SIZE = 250;

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DataChangeTest.class, "schema.sql");
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() {
    db.executeUpdateSql("truncate table persons");
  }

  @Test
  public void query() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

    final AtomicBoolean executed = new AtomicBoolean(false);
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
  public void read_column_types() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

    final List<Object[]> persons = new ArrayList<>();
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
  public void parameterized_query() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

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
  public void display_current_row_details_if_error_during_get() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Error during processing of row: [id=2]");

    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        context.prepareSelect("select id from persons where id>=?").setLong(1, 2L).get(new RowReader<Long>() {
          @Override
          public Long read(Row row) {
            throw new IllegalStateException("Unexpected error");
          }
        });
      }
    }.execute();

  }

  @Test
  public void display_current_row_details_if_error_during_list() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Error during processing of row: [id=2]");

    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        context.prepareSelect("select id from persons where id>=?").setLong(1, 2L).list(new RowReader<Long>() {
          @Override
          public Long read(Row row) {
            throw new IllegalStateException("Unexpected error");
          }
        });
      }
    }.execute();

  }

  @Test
  public void bad_parameterized_query() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

    final List<Long> ids = new ArrayList<>();
    DataChange change = new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        // parameter value is not set
        ids.addAll(context.prepareSelect("select id from persons where id>=?").list(Select.LONG_READER));
      }
    };

    thrown.expect(SQLException.class);

    change.execute();
  }

  @Test
  public void scroll() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

    final List<Long> ids = new ArrayList<>();
    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        context.prepareSelect("select id from persons order by id desc").scroll(new Select.RowHandler() {
          @Override
          public void handle(Row row) throws SQLException {
            ids.add(row.getNullableLong(1));
          }
        });
      }
    }.execute();
    assertThat(ids).containsExactly(3L, 2L, 1L);
  }

  @Test
  public void insert() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

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

    db.assertDbUnit(getClass(), "insert-result.xml", "persons");
  }

  @Test
  public void batch_insert() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        Upsert upsert = context.prepareUpsert("insert into persons(id,login,age,enabled,coeff) values (?,?,?,?,?)");
        upsert
          .setLong(1, 10L)
          .setString(2, "kurt")
          .setInt(3, 27)
          .setBoolean(4, true)
          .setDouble(5, 2.2)
          .addBatch();
        upsert
          .setLong(1, 11L)
          .setString(2, "courtney")
          .setInt(3, 25)
          .setBoolean(4, false)
          .setDouble(5, 2.3)
          .addBatch();
        upsert.execute().commit().close();
      }
    }.execute();

    db.assertDbUnit(getClass(), "batch-insert-result.xml", "persons");
  }

  @Test
  public void update_null() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

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

    db.assertDbUnit(getClass(), "update-null-result.xml", "persons");
  }

  @Test
  public void mass_batch_insert() throws Exception {
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
            .setDouble(4, i + 0.5)
            .addBatch();
        }
        upsert.execute().commit().close();

      }
    }.execute();

    assertThat(db.countRowsOfTable("persons")).isEqualTo(count);
  }

  @Test
  public void scroll_and_update() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

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

    db.assertDbUnit(getClass(), "scroll-and-update-result.xml", "persons");
  }

  @Test
  public void display_current_row_details_if_error_during_scroll() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Error during processing of row: [id=1]");

    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        final Upsert upsert = context.prepareUpsert("update persons set login=?, age=? where id=?");
        context.prepareSelect("select id from persons").scroll(new Select.RowHandler() {
          @Override
          public void handle(Row row) {
            throw new IllegalStateException("Unexpected error");
          }
        });
        upsert.commit().close();
      }
    }.execute();
  }

  @Test
  public void mass_update() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        MassUpdate massUpdate = context.prepareMassUpdate();
        massUpdate.select("select id from persons where id>=?").setLong(1, 2L);
        massUpdate.update("update persons set login=?, age=? where id=?");
        massUpdate.execute(new MassUpdate.Handler() {
          @Override
          public boolean handle(Row row, SqlStatement update) throws SQLException {
            long id = row.getNullableLong(1);
            update
              .setString(1, "login" + id)
              .setInt(2, 10 + (int) id)
              .setLong(3, id);
            return true;
          }
        });
      }
    }.execute();

    db.assertDbUnit(getClass(), "mass-update-result.xml", "persons");
  }

  @Test
  public void display_current_row_details_if_error_during_mass_update() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Error during processing of row: [id=2]");

    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        MassUpdate massUpdate = context.prepareMassUpdate();
        massUpdate.select("select id from persons where id>=?").setLong(1, 2L);
        massUpdate.update("update persons set login=?, age=? where id=?");
        massUpdate.execute(new MassUpdate.Handler() {
          @Override
          public boolean handle(Row row, SqlStatement update) {
            throw new IllegalStateException("Unexpected error");
          }
        });
      }
    }.execute();
  }

  @Test
  public void mass_update_nothing() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

    new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        MassUpdate massUpdate = context.prepareMassUpdate();
        massUpdate.select("select id from persons where id>=?").setLong(1, 2L);
        massUpdate.update("update persons set login=?, age=? where id=?");
        massUpdate.execute(new MassUpdate.Handler() {
          @Override
          public boolean handle(Row row, SqlStatement update) {
            return false;
          }
        });
      }
    }.execute();

    db.assertDbUnit(getClass(), "persons.xml", "persons");
  }

  @Test
  public void bad_mass_update() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

    DataChange change = new DataChange(db.database()) {
      @Override
      public void execute(Context context) throws SQLException {
        MassUpdate massUpdate = context.prepareMassUpdate();
        massUpdate.select("select id from persons where id>=?").setLong(1, 2L);
        // update is not set
        massUpdate.execute(new MassUpdate.Handler() {
          @Override
          public boolean handle(Row row, SqlStatement update) {
            return false;
          }
        });
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
  public void read_not_null_fields() throws Exception {
    db.prepareDbUnit(getClass(), "persons.xml");

    final List<Object[]> persons = new ArrayList<>();
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
}
