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
import { scrollToElement } from '../../../helpers/scrolling';
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

const DEFAULT_BOTTOM_OFFSET = 100;

export default function ConciseIssuesList(props: ConciseIssuesListProps) {
  const { issues, selected, selectedFlowIndex, selectedLocationIndex } = props;

  const handleScroll = React.useCallback(
    (element: Element, bottomOffset = DEFAULT_BOTTOM_OFFSET) => {
      const scrollableElement = document.querySelector('.layout-page-side');
      if (element && scrollableElement) {
        scrollToElement(element, { topOffset: 150, bottomOffset, parent: scrollableElement });
      }
    },
    []
  );

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
