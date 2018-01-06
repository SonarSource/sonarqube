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
import React from 'react';

export default function WildcardsHelp() {
  return (
    <div className="huge-spacer-top">
      <h2 className="spacer-bottom">Wildcards</h2>
      <p className="spacer-bottom">Following rules are applied:</p>

      <table className="data spacer-bottom">
        <tbody>
          <tr>
            <td>*</td>
            <td>Match zero or more characters</td>
          </tr>
          <tr>
            <td>**</td>
            <td>Match zero or more directories</td>
          </tr>
          <tr>
            <td>?</td>
            <td>Match a single character</td>
          </tr>
        </tbody>
      </table>

      <table className="data zebra">
        <thead>
          <tr>
            <th>Example</th>
            <th>Matches</th>
            <th>Does not match</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>**/foo/*.js</td>
            <td>
              <ul>
                <li>src/foo/bar.js</li>
                <li>lib/ui/foo/bar.js</li>
              </ul>
            </td>
            <td>
              <ul>
                <li>src/bar.js</li>
                <li>src/foo2/bar.js</li>
              </ul>
            </td>
          </tr>
          <tr>
            <td>src/foo/*bar*.js</td>
            <td>
              <ul>
                <li>src/foo/bar.js</li>
                <li>src/foo/bar1.js</li>
                <li>src/foo/bar123.js</li>
                <li>src/foo/123bar123.js</li>
              </ul>
            </td>
            <td>
              <ul>
                <li>src/foo/ui/bar.js</li>
                <li>src/bar.js</li>
              </ul>
            </td>
          </tr>
          <tr>
            <td>src/foo/**</td>
            <td>
              <ul>
                <li>src/foo/bar.js</li>
                <li>src/foo/ui/bar.js</li>
              </ul>
            </td>
            <td>
              <ul>
                <li>src/bar/foo/bar.js</li>
                <li>src/bar.js</li>
              </ul>
            </td>
          </tr>
          <tr>
            <td>**/foo?.js</td>
            <td>
              <ul>
                <li>src/foo1.js</li>
                <li>src/bar/foo1.js</li>
              </ul>
            </td>
            <td>
              <ul>
                <li>src/foo.js</li>
                <li>src/foo12.js</li>
                <li>src/12foo3.js</li>
              </ul>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}
