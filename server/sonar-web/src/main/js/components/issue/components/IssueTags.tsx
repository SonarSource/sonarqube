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
import { ButtonLink } from 'sonar-ui-common/components/controls/buttons';
import Toggler from 'sonar-ui-common/components/controls/Toggler';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { setIssueTags } from '../../../api/issues';
import TagsList from '../../tags/TagsList';
import { updateIssue } from '../actions';
import SetIssueTagsPopup from '../popups/SetIssueTagsPopup';

interface Props {
  canSetTags: boolean;
  isOpen: boolean;
  issue: Pick<T.Issue, 'key' | 'projectOrganization' | 'tags'>;
  onChange: (issue: T.Issue) => void;
  togglePopup: (popup: string, show?: boolean) => void;
}

export default class IssueTags extends React.PureComponent<Props> {
  toggleSetTags = (open?: boolean) => {
    this.props.togglePopup('edit-tags', open);
  };

  setTags = (tags: string[]) => {
    const { issue } = this.props;
    const newIssue = { ...issue, tags };
    updateIssue(
      this.props.onChange,
      setIssueTags({ issue: issue.key, tags: tags.join(',') }),
      issue as T.Issue,
      newIssue as T.Issue
    );
  };

  handleClose = () => {
    this.toggleSetTags(false);
  };

  render() {
    const { issue } = this.props;
    const { tags = [] } = issue;

    if (this.props.canSetTags) {
      return (
        <div className="dropdown">
          <Toggler
            onRequestClose={this.handleClose}
            open={this.props.isOpen}
            overlay={
              <SetIssueTagsPopup
                organization={issue.projectOrganization}
                selectedTags={tags}
                setTags={this.setTags}
              />
            }>
            <ButtonLink
              className="issue-action issue-action-with-options js-issue-edit-tags"
              onClick={this.toggleSetTags}>
              <TagsList
                allowUpdate={this.props.canSetTags}
                tags={
                  issue.tags && issue.tags.length > 0 ? issue.tags : [translate('issue.no_tag')]
                }
              />
            </ButtonLink>
          </Toggler>
        </div>
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
