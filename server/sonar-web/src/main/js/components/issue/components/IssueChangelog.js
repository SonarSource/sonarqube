/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import moment from 'moment';
import BubblePopupHelper from '../../../components/common/BubblePopupHelper';
import ChangelogPopup from '../popups/ChangelogPopup';
import type { Issue } from '../types';

type Props = {
  isOpen: boolean,
  issue: Issue,
  creationDate: string,
  togglePopup: (string, boolean | void) => void,
  onFail: Error => void
};

export default class IssueChangelog extends React.PureComponent {
  props: Props;

  handleClick = (evt: SyntheticInputEvent) => {
    evt.preventDefault();
    this.toggleChangelog();
  };

  toggleChangelog = (open?: boolean) => {
    this.props.togglePopup('changelog', open);
  };

  render() {
    const momentCreationDate = moment(this.props.creationDate);
    return (
      <BubblePopupHelper
        isOpen={this.props.isOpen}
        position="bottomright"
        togglePopup={this.toggleChangelog}
        popup={<ChangelogPopup issue={this.props.issue} onFail={this.props.onFail} />}>
        <button
          className="button-link issue-action issue-action-with-options js-issue-show-changelog"
          title={momentCreationDate.format('LLL')}
          onClick={this.handleClick}>
          <span className="issue-meta-label">{momentCreationDate.fromNow()}</span>
          <i className="icon-dropdown little-spacer-left" />
        </button>
      </BubblePopupHelper>
    );
  }
}
