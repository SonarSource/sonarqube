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
import ConciseIssueBox from './ConciseIssueBox';
import ConciseIssueComponent from './ConciseIssueComponent';

export interface ConciseIssueProps {
  issue: Issue;
  onFlowSelect: (index?: number) => void;
  onLocationSelect: (index: number) => void;
  onSelect: (issueKey: string) => void;
  previousIssue: Issue | undefined;
  scroll: (element: Element) => void;
  selected: boolean;
  selectedFlowIndex: number | undefined;
  selectedLocationIndex: number | undefined;
}

export default function ConciseIssue(props: ConciseIssueProps) {
  const { issue, previousIssue, selected, selectedFlowIndex, selectedLocationIndex } = props;

  const displayComponent = !previousIssue || previousIssue.component !== issue.component;

  return (
    <>
      {displayComponent && (
        <li>
          <ConciseIssueComponent path={issue.componentLongName} />
        </li>
      )}
      <li>
        <ConciseIssueBox
          issue={issue}
          onClick={props.onSelect}
          onFlowSelect={props.onFlowSelect}
          onLocationSelect={props.onLocationSelect}
          scroll={props.scroll}
          selected={selected}
          selectedFlowIndex={selected ? selectedFlowIndex : undefined}
          selectedLocationIndex={selected ? selectedLocationIndex : undefined}
        />
      </li>
    </>
  );
}
