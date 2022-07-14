/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

export default function LeastTrustPrinciple() {
  return (
    <>
      <h3>Least Trust Principle</h3>
      <p>Applications must treat all third-party data as attacker-controlled data. </p>
      <p>
        First, the application must determine where the third-party data originates and treat that
        data source as an attack vector.
      </p>

      <p>
        Then, the application must validate the attacker-controlled data against predefined formats,
        such as:
      </p>
      <ul>
        <li>Character sets</li>
        <li>Sizes</li>
        <li>Types</li>
        <li>Or any strict schema</li>
      </ul>

      <p>
        Next, the code must sanitize the data before performing mission-critical operations on the
        attacker-controlled data. The code must know in which contexts the intercepted data is used
        and act accordingly.
      </p>
    </>
  );
}
