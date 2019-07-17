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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { WorkspaceContextShape } from '../../workspace/context';

interface Props {
  engine?: string;
  manualVulnerability: boolean;
  message: string;
  openRule: WorkspaceContextShape['openRule'];
  organization: string;
  rule: string;
}

export default class IssueMessage extends React.PureComponent<Props> {
  handleClick = () => {
    this.props.openRule({
      key: this.props.rule,
      organization: this.props.organization
    });
  };

  render() {
    return (
      <div className="issue-message">
        <span className="little-spacer-right">{this.props.message}</span>
        <Button
          aria-label={translate('issue.rule_details')}
          className="button button-grey button-tiny spacer-right text-top"
          onClick={this.handleClick}>
          {translate('issue.see_rule')}
        </Button>
        {this.props.engine && (
          <Tooltip
            overlay={translateWithParameters('issue.from_external_rule_engine', this.props.engine)}>
            <div className="badge spacer-right text-top">{this.props.engine}</div>
          </Tooltip>
        )}
        {this.props.manualVulnerability && (
          <Tooltip overlay={translate('issue.manual_vulnerability.description')}>
            <div className="badge spacer-right text-top">
              {translate('issue.manual_vulnerability')}
            </div>
          </Tooltip>
        )}
      </div>
    );
  }
}
