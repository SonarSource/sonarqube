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
import ChangelogPopup from '../popups/ChangelogPopup';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import Toggler from '../../../components/controls/Toggler';
import Tooltip from '../../../components/controls/Tooltip';
import { Button } from '../../../components/ui/buttons';
/*:: import type { Issue } from '../types'; */

/*::
type Props = {
  isOpen: boolean,
  issue: Issue,
  creationDate: string,
  togglePopup: (string, boolean | void) => void,
  onFail: Error => void
};
*/

export default class IssueChangelog extends React.PureComponent {
  /*:: props: Props; */

  toggleChangelog = (open /*: boolean | void */) => {
    this.props.togglePopup('changelog', open);
  };

  handleClick = () => {
    this.toggleChangelog();
  };

  handleClose = () => {
    this.toggleChangelog(false);
  };

  render() {
    return (
      <div className="dropdown">
        <Toggler
          onRequestClose={this.handleClose}
          open={this.props.isOpen}
          overlay={<ChangelogPopup issue={this.props.issue} onFail={this.props.onFail} />}>
          <Tooltip
            mouseEnterDelay={0.5}
            overlay={<DateTimeFormatter date={this.props.creationDate} />}>
            <Button
              className="button-link issue-action issue-action-with-options js-issue-show-changelog"
              onClick={this.handleClick}>
              <span className="issue-meta-label">
                <DateFromNow date={this.props.creationDate} />
              </span>
              <i className="icon-dropdown little-spacer-left" />
            </Button>
          </Tooltip>
        </Toggler>
      </div>
    );
  }
}
