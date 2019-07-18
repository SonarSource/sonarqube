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
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import DeprecatedBadge from './DeprecatedBadge';
import InternalBadge from './InternalBadge';

interface Props {
  params: T.WebApi.Param[];
  showDeprecated: boolean;
  showInternal: boolean;
}

export default class Params extends React.PureComponent<Props> {
  renderKey(param: T.WebApi.Param) {
    return (
      <td className="markdown" style={{ width: 180 }}>
        <code>{param.key}</code>

        {param.internal && (
          <div className="little-spacer-top">
            <InternalBadge />
          </div>
        )}

        {param.deprecatedSince && (
          <div className="little-spacer-top">
            <DeprecatedBadge since={param.deprecatedSince} />
          </div>
        )}

        {this.props.showDeprecated && param.deprecatedKey && (
          <div className="little-spacer-top">
            <code>{param.deprecatedKey}</code>
          </div>
        )}

        {this.props.showDeprecated && param.deprecatedKey && param.deprecatedKeySince && (
          <div className="little-spacer-top">
            <DeprecatedBadge since={param.deprecatedKeySince} />
          </div>
        )}

        <div className="note little-spacer-top">{param.required ? 'required' : 'optional'}</div>

        {param.since && (
          <div className="note little-spacer-top">
            {translateWithParameters('since_x', param.since)}
          </div>
        )}
      </td>
    );
  }

  renderConstraint(param: T.WebApi.Param, field: keyof T.WebApi.Param, label: string) {
    const value = param[field];
    if (value !== undefined) {
      return (
        <div className="little-spacer-top">
          <h4>{translate('api_documentation', label)}</h4>
          <code>{value}</code>
        </div>
      );
    } else {
      return null;
    }
  }

  render() {
    const { params, showDeprecated, showInternal } = this.props;
    const displayedParameters = params
      .filter(p => showDeprecated || !p.deprecatedSince)
      .filter(p => showInternal || !p.internal);
    return (
      <div className="web-api-params">
        <table>
          <tbody>
            {displayedParameters.map(param => (
              <tr key={param.key}>
                {this.renderKey(param)}

                <td>
                  <div
                    className="markdown"
                    // Safe: comes from the backend
                    dangerouslySetInnerHTML={{ __html: param.description }}
                  />
                </td>

                <td style={{ width: 250 }}>
                  {param.possibleValues && (
                    <div>
                      <h4>{translate('api_documentation.possible_values')}</h4>
                      <ul className="list-styled">
                        {param.possibleValues.map(value => (
                          <li className="little-spacer-top" key={value}>
                            <code>{value}</code>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {this.renderConstraint(param, 'defaultValue', 'default_values')}
                  {this.renderConstraint(param, 'exampleValue', 'example_values')}
                  {this.renderConstraint(param, 'maxValuesAllowed', 'max_values')}
                  {this.renderConstraint(param, 'minimumValue', 'min_value')}
                  {this.renderConstraint(param, 'maximumValue', 'max_value')}
                  {this.renderConstraint(param, 'minimumLength', 'min_length')}
                  {this.renderConstraint(param, 'maximumLength', 'max_length')}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }
}
