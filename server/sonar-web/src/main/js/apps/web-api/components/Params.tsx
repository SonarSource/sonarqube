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

import {
  ContentCell,
  DarkLabel,
  HtmlFormatter,
  Note,
  SafeHTMLInjection,
  Table,
  TableRow,
} from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { WebApi } from '../../../types/types';
import DeprecatedBadge from './DeprecatedBadge';
import InternalBadge from './InternalBadge';

interface Props {
  params: WebApi.Param[];
  showDeprecated: boolean;
  showInternal: boolean;
}

const TABLE_COLUMNS = ['200', 'auto', '200'];

export default class Params extends React.PureComponent<Props> {
  renderKey(param: WebApi.Param) {
    return (
      <ContentCell>
        <div>
          <HtmlFormatter>
            <code className="sw-code">{param.key}</code>
          </HtmlFormatter>

          {param.internal && (
            <div className="sw-mt-1">
              <InternalBadge />
            </div>
          )}

          {param.deprecatedSince && (
            <div className="sw-mt-1">
              <DeprecatedBadge since={param.deprecatedSince} />
            </div>
          )}

          <Note as="div" className="sw-mt-1">
            {param.required ? 'required' : 'optional'}
          </Note>

          {param.since && (
            <Note as="div" className="sw-mt-1">
              {translateWithParameters('since_x', param.since)}
            </Note>
          )}

          {this.props.showDeprecated && param.deprecatedKey && (
            <div className="sw-ml-2 sw-mt-4">
              <Note as="div" className="sw-mb-1">
                {translate('replaces')}:
              </Note>
              <code className="sw-code">{param.deprecatedKey}</code>
              {param.deprecatedKeySince && (
                <div className="sw-mt-1">
                  <DeprecatedBadge since={param.deprecatedKeySince} />
                </div>
              )}
            </div>
          )}
        </div>
      </ContentCell>
    );
  }

  renderConstraint(param: WebApi.Param, field: keyof WebApi.Param, label: string) {
    const value = param[field];
    if (value !== undefined) {
      return (
        <div className="sw-mt-1">
          <DarkLabel as="div">{translate('api_documentation', label)}</DarkLabel>
          <code className="sw-code">{value}</code>
        </div>
      );
    } else {
      return null;
    }
  }

  render() {
    const { params, showDeprecated, showInternal } = this.props;
    const displayedParameters = params
      .filter((p) => showDeprecated || !p.deprecatedSince)
      .filter((p) => showInternal || !p.internal);
    return (
      <div className="sw-mt-6">
        <Table columnCount={TABLE_COLUMNS.length} columnWidths={TABLE_COLUMNS}>
          {displayedParameters.map((param) => (
            <TableRow key={param.key}>
              {this.renderKey(param)}

              <ContentCell>
                <SafeHTMLInjection htmlAsString={param.description}>
                  <div className="markdown" />
                </SafeHTMLInjection>
              </ContentCell>

              <ContentCell>
                <div>
                  {param.possibleValues && (
                    <div>
                      <DarkLabel as="div">
                        {translate('api_documentation.possible_values')}
                      </DarkLabel>
                      <ul>
                        {param.possibleValues.map((value) => (
                          <li className="sw-mt-1" key={value}>
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
                </div>
              </ContentCell>
            </TableRow>
          ))}
        </Table>
      </div>
    );
  }
}
