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
import BubblePopupHelper from '../../../components/common/BubblePopupHelper';
import IssueTypeIcon from '../../../components/ui/IssueTypeIcon';
import SetTypePopup from '../popups/SetTypePopup';
import { setIssueType } from '../../../api/issues';
import { translate } from '../../../helpers/l10n';
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

export default class IssueType extends React.PureComponent {
  /*:: props: Props; */

  toggleSetType = (open /*: boolean | void */) => {
    this.props.togglePopup('set-type', open);
  };

  setType = (type /*: string */) =>
    this.props.setIssueProperty('type', 'set-type', setIssueType, type);

  render() {
    const { issue } = this.props;
    if (this.props.canSetSeverity) {
      return (
        <BubblePopupHelper
          isOpen={this.props.isOpen && this.props.canSetSeverity}
          position="bottomleft"
          togglePopup={this.toggleSetType}
          popup={<SetTypePopup issue={issue} onSelect={this.setType} />}>
          <button
            className="button-link issue-action issue-action-with-options js-issue-set-type"
            onClick={this.toggleSetType}>
            <IssueTypeIcon className="little-spacer-right" query={issue.type} />
            {translate('issue.type', issue.type)}
            <i className="little-spacer-left icon-dropdown" />
          </button>
        </BubblePopupHelper>
      );
    } else {
      return (
        <span>
          <IssueTypeIcon className="little-spacer-right" query={issue.type} />
          {translate('issue.type', issue.type)}
        </span>
      );
    }
  }
}
