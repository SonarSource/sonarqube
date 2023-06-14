/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import { DiscreetSelect } from 'design-system';
import * as React from 'react';
import { setIssueSeverity } from '../../../api/issues';
import { SEVERITIES } from '../../../helpers/constants';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { IssueResponse, IssueSeverity as IssueSeverityType } from '../../../types/issues';
import { Issue, RawQuery } from '../../../types/types';
import IssueSeverityIcon from '../../icon-mappers/IssueSeverityIcon';

interface Props {
  canSetSeverity: boolean;
  isOpen: boolean;
  issue: Pick<Issue, 'severity'>;
  togglePopup: (popup: string, show?: boolean) => void;
  setIssueProperty: (
    property: keyof Issue,
    popup: string,
    apiCall: (query: RawQuery) => Promise<IssueResponse>,
    value: string
  ) => void;
}

export default class IssueSeverity extends React.PureComponent<Props> {
  setSeverity = ({ value }: { value: string }) => {
    this.props.setIssueProperty('severity', 'set-severity', setIssueSeverity, value);
    this.toggleSetSeverity(false);
  };

  toggleSetSeverity = (open: boolean) => {
    this.props.togglePopup('set-severity', open);
  };

  handleClose = () => {
    this.toggleSetSeverity(false);
  };

  render() {
    const { issue } = this.props;

    const typesOptions = SEVERITIES.map((severity) => ({
      label: translate('severity', severity),
      value: severity,
      Icon: <IssueSeverityIcon severity={severity} aria-hidden />,
    }));

    if (this.props.canSetSeverity) {
      return (
        <DiscreetSelect
          aria-label={translateWithParameters(
            'issue.severity.severity_x_click_to_change',
            translate('severity', issue.severity)
          )}
          menuIsOpen={this.props.isOpen && this.props.canSetSeverity}
          className="it__issue-severity"
          options={typesOptions}
          onMenuClose={this.handleClose}
          onMenuOpen={() => this.toggleSetSeverity(true)}
          setValue={this.setSeverity}
          value={issue.severity}
        />
      );
    }

    return (
      <span className="sw-flex sw-items-center sw-gap-1">
        <IssueSeverityIcon severity={issue.severity as IssueSeverityType} aria-hidden />

        {translate('severity', issue.severity)}
      </span>
    );
  }
}
