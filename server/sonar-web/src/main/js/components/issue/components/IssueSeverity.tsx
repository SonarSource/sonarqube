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
import DropdownIcon from '../../icons-components/DropdownIcon';
import SetSeverityPopup from '../popups/SetSeverityPopup';
import SeverityHelper from '../../shared/SeverityHelper';
import Toggler from '../../controls/Toggler';
import { ButtonLink } from '../../ui/buttons';
import { setIssueSeverity, IssueResponse } from '../../../api/issues';
import { RawQuery } from '../../../helpers/query';

interface Props {
  canSetSeverity: boolean;
  isOpen: boolean;
  issue: Pick<T.Issue, 'severity'>;
  setIssueProperty: (
    property: keyof T.Issue,
    popup: string,
    apiCall: (query: RawQuery) => Promise<IssueResponse>,
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
              className="issue-action issue-action-with-options js-issue-set-severity"
              onClick={this.toggleSetSeverity}>
              <SeverityHelper className="issue-meta-label" severity={issue.severity} />
              <DropdownIcon className="little-spacer-left" />
            </ButtonLink>
          </Toggler>
        </div>
      );
    } else {
      return <SeverityHelper className="issue-meta-label" severity={issue.severity} />;
    }
  }
}
