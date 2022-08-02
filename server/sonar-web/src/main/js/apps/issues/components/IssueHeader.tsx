/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { Link } from 'react-router-dom';
import { deleteIssueComment, editIssueComment, setIssueAssignee } from '../../../api/issues';
import LinkIcon from '../../../components/icons/LinkIcon';
import { updateIssue } from '../../../components/issue/actions';
import IssueActionsBar from '../../../components/issue/components/IssueActionsBar';
import IssueChangelog from '../../../components/issue/components/IssueChangelog';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { isInput, isShortcut } from '../../../helpers/keyboardEventHelpers';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate } from '../../../helpers/l10n';
import { getComponentIssuesUrl, getRuleUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { Issue, RuleDetails } from '../../../types/types';

interface Props {
  issue: Issue;
  ruleDetails: RuleDetails;
  branchLike?: BranchLike;
  onIssueChange: (issue: Issue) => void;
}

interface State {
  issuePopupName?: string;
}

export default class IssueHeader extends React.PureComponent<Props, State> {
  state = { issuePopupName: undefined };

  componentDidMount() {
    document.addEventListener('keydown', this.handleKeyDown, { capture: true });
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.issue.key !== this.props.issue.key) {
      this.setState({ issuePopupName: undefined });
    }
  }

  componentWillUnmount() {
    document.removeEventListener('keydown', this.handleKeyDown, { capture: true });
  }

  handleIssuePopupToggle = (popupName: string, open = true) => {
    const name = open ? popupName : undefined;
    this.setState({ issuePopupName: name });
  };

  deleteComment = (comment: string) => {
    updateIssue(this.props.onIssueChange, deleteIssueComment({ comment }));
  };

  editComment = (comment: string, text: string) => {
    updateIssue(this.props.onIssueChange, editIssueComment({ comment, text }));
  };

  handleAssignement = (login: string) => {
    const { issue } = this.props;
    if (issue.assignee !== login) {
      updateIssue(
        this.props.onIssueChange,
        setIssueAssignee({ issue: issue.key, assignee: login })
      );
    }
    this.handleIssuePopupToggle('assign', false);
  };

  handleKeyDown = (event: KeyboardEvent) => {
    if (isInput(event) || isShortcut(event)) {
      return true;
    } else if (event.key === KeyboardKeys.KeyF) {
      event.preventDefault();
      return this.handleIssuePopupToggle('transition');
    } else if (event.key === KeyboardKeys.KeyA) {
      event.preventDefault();
      return this.handleIssuePopupToggle('assign');
    } else if (event.key === KeyboardKeys.KeyM && this.props.issue.actions.includes('assign')) {
      event.preventDefault();
      return this.handleAssignement('_me');
    } else if (event.key === KeyboardKeys.KeyI) {
      event.preventDefault();
      return this.handleIssuePopupToggle('set-severity');
    } else if (event.key === KeyboardKeys.KeyC) {
      event.preventDefault();
      return this.handleIssuePopupToggle('comment');
    } else if (event.key === KeyboardKeys.KeyT) {
      event.preventDefault();
      return this.handleIssuePopupToggle('edit-tags');
    }
    return true;
  };

  render() {
    const {
      issue,
      ruleDetails: { key, name },
      branchLike
    } = this.props;
    const { issuePopupName } = this.state;
    const issueUrl = getComponentIssuesUrl(issue.project, {
      ...getBranchLikeQuery(branchLike),
      issues: issue.key,
      open: issue.key,
      types: issue.type === 'SECURITY_HOTSPOT' ? issue.type : undefined
    });
    return (
      <>
        <div className="display-flex-center display-flex-space-between big-padded-top">
          <h1 className="text-bold">{issue.message}</h1>
          <div className="issue-meta issue-get-perma-link">
            <Link
              className="js-issue-permalink link-no-underline"
              target="_blank"
              title={translate('permalink')}
              to={issueUrl}>
              {translate('issue.action.permalink')}
              <LinkIcon />
            </Link>
          </div>
        </div>
        <div className="display-flex-center display-flex-space-between spacer-top big-spacer-bottom">
          <div>
            <span className="note padded-right">{name}</span>
            <Link className="small" to={getRuleUrl(key)} target="_blank">
              {key}
            </Link>
          </div>
          <div className="issue-meta-list">
            <div className="issue-meta">
              <IssueChangelog
                creationDate={issue.creationDate}
                isOpen={issuePopupName === 'changelog'}
                issue={issue}
                togglePopup={this.handleIssuePopupToggle}
              />
            </div>
            {issue.textRange != null && (
              <div className="issue-meta">
                <span className="issue-meta-label" title={translate('line_number')}>
                  L{issue.textRange.endLine}
                </span>
              </div>
            )}
          </div>
        </div>
        <IssueActionsBar
          currentPopup={issuePopupName}
          issue={issue}
          onAssign={this.handleAssignement}
          onChange={this.props.onIssueChange}
          togglePopup={this.handleIssuePopupToggle}
          deleteComment={this.deleteComment}
          onEdit={this.editComment}
          showCommentsInPopup={true}
        />
      </>
    );
  }
}
