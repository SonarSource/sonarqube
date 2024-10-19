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
package org.sonar.db.alm.integration.pat;

import java.util.function.Consumer;
import org.sonar.db.DbTester;
import org.sonar.db.alm.pat.AlmPatDto;

import static java.util.Arrays.stream;
import static org.sonar.db.alm.integration.pat.AlmPatsTesting.newAlmPatDto;

public class AlmPatsDbTester {

  private final DbTester db;

  public AlmPatsDbTester(DbTester db) {
    this.db = db;
  }

  @SafeVarargs
  public final AlmPatDto insert(Consumer<AlmPatDto>... populators) {
    return insert(newAlmPatDto(), populators);
  }

  private AlmPatDto insert(AlmPatDto dto, Consumer<AlmPatDto>[] populators) {
    stream(populators).forEach(p -> p.accept(dto));
    db.getDbClient().almPatDao().insert(db.getSession(), dto, null, null);
    db.commit();
    return dto;
  }

}
