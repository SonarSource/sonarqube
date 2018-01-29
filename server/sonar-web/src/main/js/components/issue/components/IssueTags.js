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
import SetIssueTagsPopup from '../popups/SetIssueTagsPopup';
import TagsList from '../../../components/tags/TagsList';
import { setIssueTags } from '../../../api/issues';
import { translate } from '../../../helpers/l10n';
/*:: import type { Issue } from '../types'; */

/*::
type Props = {|
  canSetTags: boolean,
  isOpen: boolean,
  issue: Issue,
  onChange: Issue => void,
  onFail: Error => void,
  togglePopup: (string, boolean | void) => void
|};
*/

export default class IssueTags extends React.PureComponent {
  /*:: props: Props; */

  toggleSetTags = (open /*: boolean | void */) => {
    this.props.togglePopup('edit-tags', open);
  };

  setTags = (tags /*: Array<string> */) => {
    const { issue } = this.props;
    const newIssue = { ...issue, tags };
    updateIssue(
      this.props.onChange,
      this.props.onFail,
      setIssueTags({ issue: issue.key, tags: tags.join(',') }),
      issue,
      newIssue
    );
  };

  render() {
    const { issue } = this.props;

    if (this.props.canSetTags) {
      return (
        <BubblePopupHelper
          isOpen={this.props.isOpen}
          position="bottomright"
          togglePopup={this.toggleSetTags}
          popup={
            <SetIssueTagsPopup
              onFail={this.props.onFail}
              organization={issue.projectOrganization}
              selectedTags={issue.tags}
              setTags={this.setTags}
            />
          }>
          <button
            className={'js-issue-edit-tags button-link issue-action issue-action-with-options'}
            onClick={this.toggleSetTags}>
            <TagsList
              allowUpdate={this.props.canSetTags}
              tags={issue.tags && issue.tags.length > 0 ? issue.tags : [translate('issue.no_tag')]}
            />
          </button>
        </BubblePopupHelper>
      );
    } else {
      return (
        <TagsList
          allowUpdate={this.props.canSetTags}
          className="note"
          tags={issue.tags && issue.tags.length > 0 ? issue.tags : [translate('issue.no_tag')]}
        />
      );
    }
  }
}
