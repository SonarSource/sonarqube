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
import { translate } from '../../../helpers/l10n';
import { sanitizeString } from '../../../helpers/sanitize';
import { RuleParameter } from '../../../types/types';

interface Props {
  params: RuleParameter[];
}

export default class RuleDetailsParameters extends React.PureComponent<Props> {
  renderParameter = (param: RuleParameter) => (
    <tr className="coding-rules-detail-parameter" key={param.key}>
      <td className="coding-rules-detail-parameter-name">{param.key}</td>
      <td className="coding-rules-detail-parameter-description">
        {param.htmlDesc !== undefined && (
          <p
            // eslint-disable-next-line react/no-danger
            dangerouslySetInnerHTML={{ __html: sanitizeString(param.htmlDesc) }}
          />
        )}
        {param.defaultValue !== undefined && (
          <div className="note spacer-top">
            {translate('coding_rules.parameters.default_value')}
            <br />
            <span className="coding-rules-detail-parameter-value">{param.defaultValue}</span>
          </div>
        )}
      </td>
    </tr>
  );

  render() {
    return (
      <div className="js-rule-parameters">
        <h3 className="coding-rules-detail-title">{translate('coding_rules.parameters')}</h3>
        <table className="coding-rules-detail-parameters">
          <tbody>{this.props.params.map(this.renderParameter)}</tbody>
        </table>
      </div>
    );
  }
}
