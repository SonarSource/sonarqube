/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { PopupPlacement, Tags } from 'design-system';
import * as React from 'react';
import { setIssueTags } from '../../../api/issues';
import { translate } from '../../../helpers/l10n';
import { Issue } from '../../../types/types';
import Tooltip from '../../controls/Tooltip';
import { updateIssue } from '../actions';
import IssueTagsPopup from '../popups/IssueTagsPopup';

interface Props {
  canSetTags: boolean;
  issue: Pick<Issue, 'key' | 'tags'>;
  onChange: (issue: Issue) => void;
  togglePopup: (popup: string, show?: boolean) => void;
  open?: boolean;
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
      issue as Issue,
      newIssue as Issue
    );
  };

  handleClose = () => {
    this.toggleSetTags(false);
  };

  render() {
    const { issue, open } = this.props;
    const { tags = [] } = issue;

    return (
      <Tags
        allowUpdate={this.props.canSetTags}
        ariaTagsListLabel={translate('issue.tags')}
        className="js-issue-edit-tags"
        emptyText={translate('issue.no_tag')}
        menuId="issue-tags-menu"
        overlay={<IssueTagsPopup selectedTags={tags} setTags={this.setTags} />}
        popupPlacement={PopupPlacement.Bottom}
        tags={tags}
        tagsToDisplay={2}
        tooltip={Tooltip}
        open={open}
        onClose={this.handleClose}
      />
    );
  }
}
