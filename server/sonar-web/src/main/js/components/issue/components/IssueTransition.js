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
import { updateIssue } from '../actions';
import BubblePopupHelper from '../../../components/common/BubblePopupHelper';
import SetTransitionPopup from '../popups/SetTransitionPopup';
import StatusHelper from '../../../components/shared/StatusHelper';
import { setIssueTransition } from '../../../api/issues';
/*:: import type { Issue } from '../types'; */

/*::
type Props = {
  hasTransitions: boolean,
  isOpen: boolean,
  issue: Issue,
  onChange: Issue => void,
  onFail: Error => void,
  togglePopup: (string, boolean | void) => void
};
*/

export default class IssueTransition extends React.PureComponent {
  /*:: props: Props; */

  setTransition = (transition /*: string */) => {
    updateIssue(
      this.props.onChange,
      this.props.onFail,
      setIssueTransition({ issue: this.props.issue.key, transition })
    );
    this.toggleSetTransition();
  };

  toggleSetTransition = (open /*: boolean | void */) => {
    this.props.togglePopup('transition', open);
  };

  render() {
    const { issue } = this.props;

    if (this.props.hasTransitions) {
      return (
        <BubblePopupHelper
          isOpen={this.props.isOpen && this.props.hasTransitions}
          position="bottomleft"
          togglePopup={this.toggleSetTransition}
          popup={
            <SetTransitionPopup transitions={issue.transitions} onSelect={this.setTransition} />
          }>
          <button
            className="button-link issue-action issue-action-with-options js-issue-transition"
            onClick={this.toggleSetTransition}>
            <StatusHelper
              className="issue-meta-label little-spacer-right"
              status={issue.status}
              resolution={issue.resolution}
            />
            <i className="little-spacer-left icon-dropdown" />
          </button>
        </BubblePopupHelper>
      );
    } else {
      return (
        <StatusHelper
          className="issue-meta-label"
          status={issue.status}
          resolution={issue.resolution}
        />
      );
    }
  }
}
