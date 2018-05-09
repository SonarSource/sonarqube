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
import SetSeverityPopup from '../popups/SetSeverityPopup';
import { setIssueSeverity } from '../../../api/issues';
import Toggler from '../../../components/controls/Toggler';
import DropdownIcon from '../../../components/icons-components/DropdownIcon';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import { Button } from '../../../components/ui/buttons';
/*:: import type { Issue } from '../types'; */

/*::
type Props = {
  canSetSeverity: boolean,
  isOpen: boolean,
  issue: Issue,
  setIssueProperty: (string, string, apiCall: (Object) => Promise<*>, string) => void,
  togglePopup: (string, boolean | void) => void
};
*/

export default class IssueSeverity extends React.PureComponent {
  /*:: props: Props; */

  toggleSetSeverity = (open /*: boolean | void */) => {
    this.props.togglePopup('set-severity', open);
  };

  setSeverity = (severity /*: string */) => {
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
            <Button
              className="button-link issue-action issue-action-with-options js-issue-set-severity"
              onClick={this.toggleSetSeverity}>
              <SeverityHelper className="issue-meta-label" severity={issue.severity} />
              <DropdownIcon className="little-spacer-left" />
            </Button>
          </Toggler>
        </div>
      );
    } else {
      return <SeverityHelper className="issue-meta-label" severity={issue.severity} />;
    }
  }
}
