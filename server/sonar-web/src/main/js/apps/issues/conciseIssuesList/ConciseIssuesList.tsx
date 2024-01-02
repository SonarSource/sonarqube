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
import { Issue } from '../../../types/types';
import ConciseIssue from './ConciseIssue';

export interface ConciseIssuesListProps {
  issues: Issue[];
  onFlowSelect: (index?: number) => void;
  onIssueSelect: (issueKey: string) => void;
  onLocationSelect: (index: number) => void;
  selected: string | undefined;
  selectedFlowIndex: number | undefined;
  selectedLocationIndex: number | undefined;
}

export default function ConciseIssuesList(props: ConciseIssuesListProps) {
  const { issues, selected, selectedFlowIndex, selectedLocationIndex } = props;

  const handleScroll = React.useCallback((element: Element) => {
    if (element) {
      element.scrollIntoView({
        block: 'center',
        behavior: 'smooth',
        inline: 'center',
      });
    }
  }, []);

  return (
    <ul>
      {issues.map((issue, index) => (
        <ConciseIssue
          issue={issue}
          key={issue.key}
          onFlowSelect={props.onFlowSelect}
          onLocationSelect={props.onLocationSelect}
          onSelect={props.onIssueSelect}
          previousIssue={index > 0 ? issues[index - 1] : undefined}
          scroll={handleScroll}
          selected={issue.key === selected}
          selectedFlowIndex={selectedFlowIndex}
          selectedLocationIndex={selectedLocationIndex}
        />
      ))}
    </ul>
  );
}
