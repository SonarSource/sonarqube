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
import * as React from 'react';
import { translate } from 'sonar-ui-common/helpers/l10n';

export default function WildcardsHelp() {
  return (
    <div className="huge-spacer-top">
      <h2 className="spacer-bottom">{translate('settings.wildcards')}</h2>
      <p className="spacer-bottom">{translate('settings.wildcards.following_rules_are_applied')}</p>

      <table className="data spacer-bottom">
        <tbody>
          <tr>
            <td>*</td>
            <td>{translate('settings.wildcards.zero_more_char')}</td>
          </tr>
          <tr>
            <td>**</td>
            <td>{translate('settings.wildcards.zero_more_dir')}</td>
          </tr>
          <tr>
            <td>?</td>
            <td>{translate('settings.wildcards.single_char')}</td>
          </tr>
        </tbody>
      </table>

      <table className="data zebra">
        <thead>
          <tr>
            <th>{translate('example')}</th>
            <th>{translate('settings.wildcards.matches')}</th>
            <th>{translate('settings.wildcards.does_no_match')}</th>
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
