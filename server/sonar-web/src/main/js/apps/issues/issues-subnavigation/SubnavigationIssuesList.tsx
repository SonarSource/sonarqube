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
import * as React from 'react';
import { themeBorder, themeColor, themeShadow } from '~design-system';
import ListFooter from '../../../components/controls/ListFooter';
import { Issue, Paging } from '../../../types/types';
import SubnavigationIssue from './SubnavigationIssue';
import SubnavigationIssueComponentName from './SubnavigationIssueComponentName';
import SubnavigationIssuesListHeader from './SubnavigationIssuesListHeader';

interface Props {
  fetchMoreIssues: () => void;
  issues: Issue[];
  loading: boolean;
  loadingMore: boolean;
  onFlowSelect: (index?: number) => void;
  onIssueSelect: (issueKey: string) => void;
  onLocationSelect: (index: number) => void;
  paging: Paging | undefined;
  selected: string | undefined;
  selectedFlowIndex: number | undefined;
  selectedLocationIndex: number | undefined;
}

export default function SubnavigationIssuesList(props: Props) {
  const {
    issues,
    loading,
    loadingMore,
    paging,
    selected,
    selectedFlowIndex,
    selectedLocationIndex,
  } = props;

  return (
    <StyledWrapper>
      <SubnavigationIssuesListHeader loading={loading} paging={paging} />
      <StyledList className="sw-overflow-auto sw-flex-auto">
        {issues.map((issue, index) => {
          const previousIssue = index > 0 ? issues[index - 1] : undefined;
          const displayComponentName =
            !previousIssue || previousIssue.component !== issue.component;

          return (
            <React.Fragment key={index}>
              {displayComponentName && (
                <li>
                  <SubnavigationIssueComponentName path={issue.componentLongName} />
                </li>
              )}

              <SubnavigationIssue
                issue={issue}
                onClick={props.onIssueSelect}
                onFlowSelect={props.onFlowSelect}
                onLocationSelect={props.onLocationSelect}
                selected={issue.key === selected}
                selectedFlowIndex={selectedFlowIndex}
                selectedLocationIndex={selectedLocationIndex}
              />
            </React.Fragment>
          );
        })}
      </StyledList>
      {paging && paging.total > 0 && (
        <StyledFooter
          className="sw-my-0 sw-py-4"
          count={issues.length}
          loadMore={props.fetchMoreIssues}
          loading={loadingMore}
          total={paging.total}
        />
      )}
    </StyledWrapper>
  );
}

const StyledList = styled.ul`
  li:not(:last-child) {
    border-bottom: ${themeBorder('default')};
  }
`;

const StyledWrapper = styled.div`
  background-color: ${themeColor('filterbar')};
  height: inherit;
  display: flex;
  flex-direction: column;
`;

const StyledFooter = styled(ListFooter)`
  box-shadow: ${themeShadow('scrolling')};
`;
