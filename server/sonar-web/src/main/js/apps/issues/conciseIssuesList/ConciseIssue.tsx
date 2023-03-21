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
import * as React from 'react';
import { Issue } from '../../../types/types';
import ConciseIssueBox from './ConciseIssueBox';
import ConciseIssueComponent from './ConciseIssueComponent';

const HALF_DIVIDER = 2;

export interface ConciseIssueProps {
  issue: Issue;
  onFlowSelect: (index?: number) => void;
  onLocationSelect: (index: number) => void;
  onSelect: (issueKey: string) => void;
  previousIssue: Issue | undefined;
  selected: boolean;
  selectedFlowIndex: number | undefined;
  selectedLocationIndex: number | undefined;
}

export default function ConciseIssue(props: ConciseIssueProps) {
  const { issue, previousIssue, selected, selectedFlowIndex, selectedLocationIndex } = props;
  const element = React.useRef<HTMLLIElement>(null);

  const displayComponent = !previousIssue || previousIssue.component !== issue.component;

  React.useEffect(() => {
    if (selected && element.current) {
      const parent = document.querySelector('.layout-page-side') as HTMLMenuElement;
      const rect = parent.getBoundingClientRect();
      const offset =
        element.current.offsetTop - rect.height / HALF_DIVIDER + rect.top / HALF_DIVIDER;
      parent.scrollTo({ top: offset, behavior: 'smooth' });
    }
  }, [selected]);

  return (
    <>
      {displayComponent && (
        <li>
          <ConciseIssueComponent path={issue.componentLongName} />
        </li>
      )}
      <li ref={element}>
        <ConciseIssueBox
          issue={issue}
          onClick={props.onSelect}
          onFlowSelect={props.onFlowSelect}
          onLocationSelect={props.onLocationSelect}
          selected={selected}
          selectedFlowIndex={selected ? selectedFlowIndex : undefined}
          selectedLocationIndex={selected ? selectedLocationIndex : undefined}
        />
      </li>
    </>
  );
}
