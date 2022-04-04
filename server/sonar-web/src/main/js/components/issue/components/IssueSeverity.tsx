/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { ButtonLink } from 'sonar-ui-common/components/controls/buttons';
import Toggler from 'sonar-ui-common/components/controls/Toggler';
import DropdownIcon from 'sonar-ui-common/components/icons/DropdownIcon';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { setIssueSeverity } from '../../../api/issues';
import { IssueResponse } from '../../../types/issues';
import SeverityHelper from '../../shared/SeverityHelper';
import SetSeverityPopup from '../popups/SetSeverityPopup';

interface Props {
  canSetSeverity: boolean;
  isOpen: boolean;
  issue: Pick<T.Issue, 'severity'>;
  setIssueProperty: (
    property: keyof T.Issue,
    popup: string,
    apiCall: (query: T.RawQuery) => Promise<IssueResponse>,
    value: string
  ) => void;
  togglePopup: (popup: string, show?: boolean) => void;
}

export default class IssueSeverity extends React.PureComponent<Props> {
  toggleSetSeverity = (open?: boolean) => {
    this.props.togglePopup('set-severity', open);
  };

  setSeverity = (severity: string) => {
    this.props.setIssueProperty('severity', 'set-severity', setIssueSeverity, severity);
  };

  handleClose = () => {
    this.toggleSetSeverity(false);
  };

  render() {
    const { issue } = this.props;
    if (this.props.canSetSeverity) {
      return (
        <div className="dropdown">
          <Toggler
            onRequestClose={this.handleClose}
            open={this.props.isOpen && this.props.canSetSeverity}
            overlay={<SetSeverityPopup issue={issue} onSelect={this.setSeverity} />}>
            <ButtonLink
              aria-label={translateWithParameters(
                'issue.severity.severity_x_click_to_change',
                translate('severity', issue.severity)
              )}
              aria-expanded={this.props.isOpen}
              className="issue-action issue-action-with-options js-issue-set-severity"
              onClick={this.toggleSetSeverity}>
              <SeverityHelper className="issue-meta-label" severity={issue.severity} />
              <DropdownIcon className="little-spacer-left" />
            </ButtonLink>
          </Toggler>
        </div>
      );
    }

    return <SeverityHelper className="issue-meta-label" severity={issue.severity} />;
  }
}
