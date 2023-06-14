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
import styled from '@emotion/styled';
import classNames from 'classnames';
import { Checkbox, themeBorder, themeColor } from 'design-system';
import * as React from 'react';
import { deleteIssueComment, editIssueComment } from '../../../api/issues';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { Issue } from '../../../types/types';
import { updateIssue } from '../actions';
import IssueActionsBar from './IssueActionsBar';
import IssueTitleBar from './IssueTitleBar';

interface Props {
  branchLike?: BranchLike;
  checked?: boolean;
  currentPopup?: string;
  displayWhyIsThisAnIssue?: boolean;
  issue: Issue;
  onAssign: (login: string) => void;
  onChange: (issue: Issue) => void;
  onCheck?: (issue: string) => void;
  onSelect: (issueKey: string) => void;
  onClick?: (issueKey: string) => void;
  selected: boolean;
  togglePopup: (popup: string, show: boolean | void) => void;
}

export default class IssueView extends React.PureComponent<Props> {
  nodeRef: HTMLLIElement | null = null;

  componentDidMount() {
    const { selected } = this.props;
    if (this.nodeRef && selected) {
      this.nodeRef.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
    }
  }

  componentDidUpdate(prevProps: Props) {
    const { selected } = this.props;
    if (!prevProps.selected && selected && this.nodeRef) {
      this.nodeRef.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
    }
  }

  handleCheck = () => {
    if (this.props.onCheck) {
      this.props.onCheck(this.props.issue.key);
    }
  };

  editComment = (comment: string, text: string) => {
    updateIssue(this.props.onChange, editIssueComment({ comment, text }));
  };

  deleteComment = (comment: string) => {
    updateIssue(this.props.onChange, deleteIssueComment({ comment }));
  };

  render() {
    const { issue, branchLike, checked, currentPopup, displayWhyIsThisAnIssue } = this.props;

    const hasCheckbox = this.props.onCheck != null;

    const issueClass = classNames(
      'it__issue-item sw-py-3 sw-flex sw-items-center sw-justify-between sw-w-full ',
      {
        'no-click': this.props.onClick === undefined,
        selected: this.props.selected,
      }
    );

    return (
      <IssueItem
        onClick={() => this.props.onSelect(issue.key)}
        className={issueClass}
        role="region"
        aria-label={issue.message}
        ref={(node) => (this.nodeRef = node)}
      >
        <div className="sw-flex sw-w-full sw-px-2 sw-gap-4">
          {hasCheckbox && (
            <Checkbox
              checked={checked ?? false}
              onCheck={this.handleCheck}
              label={translateWithParameters('issues.action_select.label', issue.message)}
              title={translate('issues.action_select')}
            />
          )}
          <div className="sw-flex sw-flex-col sw-grow sw-gap-2">
            <IssueTitleBar
              currentPopup={currentPopup}
              branchLike={branchLike}
              displayWhyIsThisAnIssue={displayWhyIsThisAnIssue}
              issue={issue}
              onChange={this.props.onChange}
              togglePopup={this.props.togglePopup}
            />
            <IssueActionsBar
              currentPopup={currentPopup}
              issue={issue}
              onAssign={this.props.onAssign}
              onChange={this.props.onChange}
              togglePopup={this.props.togglePopup}
              showComments
            />
          </div>
        </div>
      </IssueItem>
    );
  }
}

const IssueItem = styled.li`
  box-sizing: border-box;
  border: ${themeBorder('default', 'transparent')};
  border-top: ${themeBorder('default')};
  outline: none;

  &:last-child {
    border-bottom: ${themeBorder('default')};
  }

  &.selected {
    border: ${themeBorder('default', 'tableRowSelected')};
  }

  &:hover {
    background: ${themeColor('tableRowHover')};
  }
`;
