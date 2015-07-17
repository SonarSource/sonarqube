/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.db.version.v51;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.CheckForNull;
import org.sonar.api.utils.System2;
import org.sonar.core.util.ProgressLogger;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.Select;
import org.sonar.db.version.Upsert;
import org.sonar.db.version.UpsertImpl;

import static com.google.common.collect.Lists.newArrayList;

public class CopyScmAccountsFromAuthorsToUsers extends BaseDataChange {

  private static final char SCM_ACCOUNTS_SEPARATOR = '\n';

  private final System2 system;
  private final AtomicLong counter = new AtomicLong(0L);

  public CopyScmAccountsFromAuthorsToUsers(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  public void execute(final Context context) throws SQLException {
    ProgressLogger progress = ProgressLogger.create(getClass(), counter);
    progress.start();
    final Long now = system.now();

    try {
      final Multimap<Long, String> authorsByPersonId = ArrayListMultimap.create();
      context.prepareSelect("SELECT a.person_id, a.login FROM authors a," +
        "  (SELECT person_id, COUNT(*) AS nb FROM authors GROUP BY person_id HAVING COUNT(*) > 1) group_by_person" +
        "     WHERE a.person_id = group_by_person.person_id "
        ).scroll(new AuthorsByPersonIdHandler(authorsByPersonId));

      Upsert update = context.prepareUpsert("UPDATE users SET scm_accounts = ?, updated_at = ? WHERE id = ?");
      for (Long personId : authorsByPersonId.keySet()) {
        List<String> authors = newArrayList(authorsByPersonId.get(personId));
        List<User> users = selectUsersFromLoginOrEmail(context, authors);
        if (users.size() == 1) {
          User user = users.get(0);
          if (authors.contains(user.login)) {
            authors.remove(user.login);
          }
          if (authors.contains(user.email)) {
            authors.remove(user.email);
          }
          if (!authors.isEmpty()) {
            update
              .setString(1, encodeScmAccounts(authors))
              .setLong(2, now)
              .setLong(3, user.id)
              .addBatch();
            counter.getAndIncrement();
          }
        }
      }
      if (((UpsertImpl) update).getBatchCount() > 0L) {
        update.execute().commit();
      }
      update.close();

      progress.log();
    } finally {
      progress.stop();
    }
  }

  private List<User> selectUsersFromLoginOrEmail(Context context, Collection<String> authors) throws SQLException {
    final List<User> users = newArrayList();
    StringBuilder sql = new StringBuilder("SELECT u.id, u.login, u.email, u.scm_accounts FROM users u WHERE u.active=? AND (");
    for (int i = 0; i < authors.size(); i++) {
      if (i < authors.size() - 1) {
        sql.append("u.login=? OR u.email=? OR ");
      } else {
        sql.append("u.login=? OR u.email=?)");
      }
    }
    Select select = context.prepareSelect(sql.toString());
    select.setBoolean(1, true);
    int currentIndex = 1;
    for (String author : authors) {
      currentIndex++;
      select.setString(currentIndex, author);
      currentIndex++;
      select.setString(currentIndex, author);
    }

    select.scroll(new UsersHandler(users));
    return users;
  }

  @CheckForNull
  private static String encodeScmAccounts(List<String> scmAccounts) {
    if (scmAccounts.isEmpty()) {
      return null;
    }
    return SCM_ACCOUNTS_SEPARATOR + Joiner.on(SCM_ACCOUNTS_SEPARATOR).join(scmAccounts) + SCM_ACCOUNTS_SEPARATOR;
  }

  private static class User {
    Long id;
    String login;
    String email;
    String scmAccounts;

    User(Long id, String login, String email, String scmAccounts) {
      this.id = id;
      this.login = login;
      this.email = email;
      this.scmAccounts = scmAccounts;
    }
  }

  private static class AuthorsByPersonIdHandler implements Select.RowHandler {

    private final Multimap<Long, String> authorsByPersonId;

    private AuthorsByPersonIdHandler(Multimap<Long, String> authorsByPersonId) {
      this.authorsByPersonId = authorsByPersonId;
    }

    @Override
    public void handle(Select.Row row) throws SQLException {
      authorsByPersonId.put(row.getNullableLong(1), row.getNullableString(2));
    }
  }

  private static class UsersHandler implements Select.RowHandler {

    private final List<User> users;

    private UsersHandler(List<User> users) {
      this.users = users;
    }

    @Override
    public void handle(Select.Row row) throws SQLException {
      users.add(new User(row.getNullableLong(1), row.getNullableString(2), row.getNullableString(3), row.getNullableString(4)));
    }
  }
}
