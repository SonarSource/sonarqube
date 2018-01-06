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
// @flow
import React from 'react';
import { translate } from '../../../helpers/l10n';

export default class IssueMessage extends React.PureComponent {
  /*:: props: {
    message: string,
    rule: string,
    organization: string
  };
*/

  handleClick = (e /*: MouseEvent */) => {
    e.preventDefault();
    e.stopPropagation();
    const Workspace = require('../../workspace/main').default;
    Workspace.openRule({
      key: this.props.rule,
      organization: this.props.organization
    });
  };

  render() {
    return (
      <div className="issue-message">
        {this.props.message}
        <button
          className="button-link issue-rule icon-ellipsis-h little-spacer-left"
          aria-label={translate('issue.rule_details')}
          onClick={this.handleClick}
        />
      </div>
    );
  }
}
