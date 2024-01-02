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
import { ButtonLink } from '../../../components/controls/buttons';
import Toggler from '../../../components/controls/Toggler';
import DropdownIcon from '../../../components/icons/DropdownIcon';
import { Issue } from '../../../types/types';
import DateFromNow from '../../intl/DateFromNow';
import ChangelogPopup from '../popups/ChangelogPopup';

interface Props {
  isOpen: boolean;
  issue: Pick<Issue, 'author' | 'creationDate' | 'key'>;
  creationDate: string;
  togglePopup: (popup: string, show?: boolean) => void;
}

export default class IssueChangelog extends React.PureComponent<Props> {
  toggleChangelog = (open?: boolean) => {
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
          overlay={<ChangelogPopup issue={this.props.issue} />}
        >
          <ButtonLink
            aria-expanded={this.props.isOpen}
            className="issue-action issue-action-with-options js-issue-show-changelog"
            onClick={this.handleClick}
          >
            <span className="issue-meta-label">
              <DateFromNow date={this.props.creationDate} />
            </span>
            <DropdownIcon className="little-spacer-left" />
          </ButtonLink>
        </Toggler>
      </div>
    );
  }
}
