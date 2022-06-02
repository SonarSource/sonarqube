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
import { colors } from '../../app/theme';
import { Issue as TypeIssue } from '../../types/types';
import IssueTypeIcon from '../icons/IssueTypeIcon';
import './Issue.css';

export interface SecondaryIssueProps {
  issue: TypeIssue;
  onClick: (issueKey: string) => void;
}

export default function SecondaryIssue(props: SecondaryIssueProps) {
  const { issue } = props;
  return (
    <div
      className="issue display-flex-row display-flex-center padded-right secondary-issue"
      key={issue.key}
      onClick={() => props.onClick(issue.key)}
      role="region"
      aria-label={issue.message}>
      <IssueTypeIcon
        className="big-spacer-right spacer-left"
        fill={colors.baseFontColor}
        query={issue.type}
      />
      {issue.message}
    </div>
  );
}
