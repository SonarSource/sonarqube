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
import styled from '@emotion/styled';
import classNames from 'classnames';
import { BasicSeparator, Checkbox, themeBorder } from 'design-system';
import * as React from 'react';
import { deleteIssueComment, editIssueComment } from '../../../api/issues';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { IssueActions } from '../../../types/issues';
import { Issue } from '../../../types/types';
import SoftwareImpactPillList from '../../shared/SoftwareImpactPillList';
import { updateIssue } from '../actions';
import IssueActionsBar from './IssueActionsBar';
import IssueMetaBar from './IssueMetaBar';
import IssueTags from './IssueTags';
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
    const canSetTags = issue.actions.includes(IssueActions.SetTags);

    const issueClass = classNames('it__issue-item sw-p-3 sw-mb-4 sw-rounded-1 sw-bg-white', {
      selected: this.props.selected,
    });

    return (
      <IssueItem
        onClick={() => this.props.onSelect(issue.key)}
        className={issueClass}
        role="region"
        aria-label={issue.message}
        ref={(node) => (this.nodeRef = node)}
      >
        <div className="sw-flex sw-gap-3">
          {hasCheckbox && (
            <span className="sw-mt-1/2 sw-ml-1 sw-self-start">
              <Checkbox
                checked={checked ?? false}
                onCheck={this.handleCheck}
                label={translateWithParameters('issues.action_select.label', issue.message)}
                title={translate('issues.action_select')}
              />
            </span>
          )}

          <div className="sw-flex sw-flex-col sw-grow sw-gap-3 sw-min-w-0">
            <IssueTitleBar
              branchLike={branchLike}
              displayWhyIsThisAnIssue={displayWhyIsThisAnIssue}
              issue={issue}
            />

            <div className="sw-mt-1 sw-flex sw-items-start sw-justify-between">
              <SoftwareImpactPillList data-guiding-id="issue-2" softwareImpacts={issue.impacts} />
              <div className="sw-grow-0 sw-whitespace-nowrap">
                <IssueTags
                  issue={issue}
                  onChange={this.props.onChange}
                  togglePopup={this.props.togglePopup}
                  canSetTags={canSetTags}
                  open={currentPopup === 'edit-tags' && canSetTags}
                />
              </div>
            </div>

            <BasicSeparator />

            <div className="sw-flex sw-gap-2 sw-flex-nowrap sw-items-center sw-justify-between">
              <IssueActionsBar
                currentPopup={currentPopup}
                issue={issue}
                onAssign={this.props.onAssign}
                onChange={this.props.onChange}
                togglePopup={this.props.togglePopup}
              />
              <IssueMetaBar issue={issue} />
            </div>
          </div>
        </div>
      </IssueItem>
    );
  }
}

const IssueItem = styled.li`
  outline: ${themeBorder('default', 'almCardBorder')};
  outline-offset: -1px;

  &.selected {
    outline: ${themeBorder('heavy', 'primary')};
    outline-offset: -2px;
  }
`;
