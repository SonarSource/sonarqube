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
import PropTypes from 'prop-types';
import Tooltip from '../../controls/Tooltip';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Button } from '../../ui/buttons';

export default class IssueMessage extends React.PureComponent {
  /*:: props: {
    engine?: string;
    message: string,
    organization: string,
    rule: string
  };
  */

  static contextTypes = {
    workspace: PropTypes.object.isRequired
  };

  handleClick = () => {
    this.context.workspace.openRule({
      key: this.props.rule,
      organization: this.props.organization
    });
  };

  render() {
    return (
      <div className="issue-message">
        {this.props.message}
        <Button
          aria-label={translate('issue.rule_details')}
          className="button-link issue-rule icon-ellipsis-h little-spacer-left"
          onClick={this.handleClick}
        />
        {this.props.engine && (
          <Tooltip
            overlay={translateWithParameters('issue.from_external_rule_engine', this.props.engine)}>
            <div className="outline-badge badge-tiny-height spacer-left vertical-text-top">
              {this.props.engine}
            </div>
          </Tooltip>
        )}
      </div>
    );
  }
}
