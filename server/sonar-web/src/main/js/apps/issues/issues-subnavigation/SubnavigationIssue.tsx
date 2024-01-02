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
import {
  BareButton,
  IssueMessageHighlighting,
  SubnavigationItem,
  themeColor,
  themeContrast,
} from 'design-system';
import { noop } from 'lodash';
import * as React from 'react';
import { Issue } from '../../../types/types';
import IssueItemLocationsQuantity from './IssueItemLocationsQuantity';
import IssueLocationsNavigator from './IssueLocationsNavigator';

const HALF_DIVIDER = 2;

export interface ConciseIssueProps {
  issue: Issue;
  onFlowSelect: (index?: number) => void;
  onLocationSelect: (index: number) => void;
  onClick: (issueKey: string) => void;
  selected: boolean;
  selectedFlowIndex: number | undefined;
  selectedLocationIndex: number | undefined;
}

export default function SubnavigationIssue(props: ConciseIssueProps) {
  const { issue, selected, selectedFlowIndex, selectedLocationIndex } = props;
  const element = React.useRef<HTMLLIElement>(null);

  React.useEffect(() => {
    if (selected && element.current) {
      const parent = document.querySelector('nav.issues-nav-bar') as HTMLMenuElement;
      const rect = parent.getBoundingClientRect();
      const offset =
        element.current.offsetTop - rect.height / HALF_DIVIDER + rect.top / HALF_DIVIDER;
      parent.scrollTo({ top: offset, behavior: 'smooth' });
    }
  }, [selected]);

  return (
    <li ref={element}>
      <SubnavigationItem
        active={selected}
        onClick={selected ? noop : props.onClick}
        value={issue.key}
      >
        <div className="sw-w-full">
          <StyledIssueTitle aria-current={selected} className="sw-mb-2">
            <IssueMessageHighlighting
              message={issue.message}
              messageFormattings={issue.messageFormattings}
            />
          </StyledIssueTitle>
          <IssueInfo className="sw-flex sw-justify-between sw-gap-2">
            <IssueItemLocationsQuantity issue={issue} />
          </IssueInfo>
          {selected && (
            <IssueLocationsNavigator
              issue={issue}
              onFlowSelect={props.onFlowSelect}
              onLocationSelect={props.onLocationSelect}
              selectedFlowIndex={selectedFlowIndex}
              selectedLocationIndex={selectedLocationIndex}
            />
          )}
        </div>
      </SubnavigationItem>
    </li>
  );
}

const IssueInfo = styled.div`
  color: ${themeContrast('pageContentLight')};

  .active &,
  :hover & {
    color: ${themeContrast('subnavigation')};
  }
`;

const StyledIssueTitle = styled(BareButton)`
  word-break: break-word;
  &:focus {
    background-color: ${themeColor('subnavigationSelected')};
  }
`;
