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
import { PopupPlacement, Tags } from '~design-system';
import { setIssueTags } from '../../../api/issues';
import withComponentContext from '../../../app/components/componentContext/withComponentContext';
import { translate } from '../../../helpers/l10n';
import { ComponentContextShape } from '../../../types/component';
import { Issue } from '../../../types/types';
import Tooltip from '../../controls/Tooltip';
import { updateIssue } from '../actions';
import IssueTagsPopup from '../popups/IssueTagsPopup';

interface Props extends ComponentContextShape {
  canSetTags?: boolean;
  issue: Pick<Issue, 'key' | 'tags'>;
  onChange: (issue: Issue) => void;
  open?: boolean;
  tagsToDisplay?: number;
  togglePopup: (popup: string, show?: boolean) => void;
}

export class IssueTags extends React.PureComponent<Props> {
  toggleSetTags = (open = false) => {
    this.props.togglePopup('edit-tags', open);
  };

  setTags = (tags: string[]) => {
    const { issue } = this.props;
    const newIssue = { ...issue, tags };

    updateIssue(
      this.props.onChange,
      setIssueTags({ issue: issue.key, tags: tags.join(',') }),
      issue as Issue,
      newIssue as Issue,
    );
  };

  handleClose = () => {
    this.toggleSetTags(false);
  };

  render() {
    const { component, issue, open, tagsToDisplay = 2 } = this.props;
    const { tags = [] } = issue;

    return (
      <Tags
        allowUpdate={this.props.canSetTags && !component?.needIssueSync}
        ariaTagsListLabel={translate('issue.tags')}
        className="js-issue-edit-tags sw-typo-sm"
        tagsClassName="sw-typo-sm"
        emptyText={translate('issue.no_tag')}
        menuId="issue-tags-menu"
        onClose={this.handleClose}
        open={open}
        overlay={<IssueTagsPopup selectedTags={tags} setTags={this.setTags} />}
        popupPlacement={PopupPlacement.Bottom}
        tags={tags}
        tagsToDisplay={tagsToDisplay}
        tooltip={Tooltip}
      />
    );
  }
}

export default withComponentContext(IssueTags);
