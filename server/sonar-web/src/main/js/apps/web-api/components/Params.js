/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import InternalBadge from './InternalBadge';
import DeprecatedBadge from './DeprecatedBadge';
import type { Param } from '../../../api/web-api';

export default class Params extends React.PureComponent {
  props: {
    showDeprecated: boolean,
    showInternal: boolean,
    params: Array<Param>
  };

  render() {
    const { showDeprecated, showInternal, params } = this.props;
    const displayedParameters = params
      .filter(p => showDeprecated || !p.deprecatedSince)
      .filter(p => showInternal || !p.internal);
    return (
      <div className="web-api-params">
        <table>
          <tbody>
            {displayedParameters.map(param => (
              <tr key={param.key}>
                <td className="markdown" style={{ width: 180 }}>
                  <code>{param.key}</code>

                  {param.internal &&
                    <div className="little-spacer-top">
                      <InternalBadge />
                    </div>}

                  {param.deprecatedSince &&
                    <div className="little-spacer-top">
                      <DeprecatedBadge since={param.deprecatedSince} />
                    </div>}

                  {showDeprecated &&
                    param.deprecatedKey &&
                    <div className="little-spacer-top">
                      <code>{param.deprecatedKey}</code>
                    </div>}

                  {showDeprecated &&
                    param.deprecatedKey &&
                    param.deprecatedKeySince &&
                    <div className="little-spacer-top">
                      <DeprecatedBadge since={param.deprecatedKeySince} />
                    </div>}

                  <div className="note little-spacer-top">
                    {param.required ? 'required' : 'optional'}
                  </div>

                  {param.since &&
                    <div className="note little-spacer-top">
                      since {param.since}
                    </div>}
                </td>

                <td>
                  <div
                    className="markdown"
                    dangerouslySetInnerHTML={{ __html: param.description }}
                  />
                </td>

                <td style={{ width: 250 }}>
                  {param.possibleValues &&
                    <div>
                      <h4>Possible values</h4>
                      <ul className="list-styled">
                        {param.possibleValues.map(value => (
                          <li key={value} className="little-spacer-top">
                            <code>{value}</code>
                          </li>
                        ))}
                      </ul>
                    </div>}

                  {param.defaultValue &&
                    <div className="little-spacer-top">
                      <h4>Default value</h4>
                      <code>{param.defaultValue}</code>
                    </div>}

                  {param.exampleValue &&
                    <div className="little-spacer-top">
                      <h4>Example value</h4>
                      <code>{param.exampleValue}</code>
                    </div>}

                  {param.maxValuesAllowed != null &&
                    <div className="little-spacer-top">
                      <h4>Maximum allowed values</h4>
                      <code>{param.maxValuesAllowed}</code>
                    </div>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }
}
