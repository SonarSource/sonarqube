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
import classNames from 'classnames';
import * as React from 'react';
import { colors } from '../../app/theme';
import { Issue } from '../../types/types';
import IssueTypeIcon from '../icons/IssueTypeIcon';
import './Issue.css';
import { IssueMessageHighlighting } from './IssueMessageHighlighting';

export interface IssueMessageBoxProps {
  selected: boolean;
  issue: Issue;
  onClick: (issueKey: string) => void;
}

export function IssueMessageBox(props: IssueMessageBoxProps, ref: React.ForwardedRef<any>) {
  const { issue, selected } = props;

  return (
    <div
      className={classNames('issue-message-box display-flex-row display-flex-center padded-right', {
        'selected big-padded-top big-padded-bottom text-bold': selected,
        'secondary-issue padded-top padded-bottom': !selected,
      })}
      key={issue.key}
      onClick={() => props.onClick(issue.key)}
      role="region"
      ref={ref}
      aria-label={issue.message}
    >
      <IssueTypeIcon
        className="big-spacer-right spacer-left"
        fill={colors.baseFontColor}
        query={issue.type}
      />
      <IssueMessageHighlighting
        message={issue.message}
        messageFormattings={issue.messageFormattings}
      />
    </div>
  );
}

export default React.forwardRef(IssueMessageBox);
