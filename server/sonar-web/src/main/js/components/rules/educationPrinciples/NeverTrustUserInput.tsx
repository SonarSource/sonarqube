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
import * as React from 'react';

export default function NeverTrustUserInput() {
  return (
    <>
      <h3>Never Trust User Input</h3>
      <p>
        Applications must treat all user input and, more generally, all third-party data as
        attacker-controlled data.
      </p>
      <p>
        The application must determine where the third-party data comes from and treat that data
        source as an attack vector. Two rules apply:
      </p>

      <p>
        First, before using it in the application&apos;s business logic, the application must
        validate the attacker-controlled data against predefined formats, such as:
      </p>
      <ul>
        <li>Character sets</li>
        <li>Sizes</li>
        <li>Types</li>
        <li>Or any strict schema</li>
      </ul>

      <p>
        Second, the application must sanitize string data before inserting it into interpreted
        contexts (client-side code, file paths, SQL queries). Unsanitized code can corrupt the
        application&apos;s logic.
      </p>
    </>
  );
}
