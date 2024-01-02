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
import { colors, sizes } from '../../app/theme';

export interface BoxedTabsProps<K extends string | number> {
  className?: string;
  onSelect: (key: K) => void;
  selected?: K;
  tabs: Array<{ key: K; label: React.ReactNode }>;
}

const TabContainer = styled.div`
  display: flex;
  flex-direction: row;
`;

const baseBorder = () => `1px solid ${colors.barBorderColor}`;

const highlightHoverMixin = () => `
  &:hover {
    background-color: ${colors.barBackgroundColorHighlight};
  }
`;

const StyledTab = styled.button<{ active: boolean }>`
  position: relative;
  background-color: ${(props) => (props.active ? 'white' : colors.barBackgroundColor)};
  border-top: ${baseBorder};
  border-left: ${baseBorder};
  border-right: none;
  border-bottom: none;
  margin-bottom: -1px;
  min-width: 128px;
  min-height: 56px;
  ${(props) => !props.active && 'cursor: pointer;'}
  outline: 0;
  padding: calc(2 * ${sizes.gridSize});

  ${(props) => (!props.active ? highlightHoverMixin : null)}

  &:last-child {
    border-right: ${baseBorder};
  }
`;

const ActiveBorder = styled.div<{ active: boolean }>`
  display: ${(props) => (props.active ? 'block' : 'none')};
  background-color: ${colors.blue};
  height: 3px;
  width: 100%;
  position: absolute;
  left: 0;
  top: -1px;
`;

export default function BoxedTabs<K extends string | number>(props: BoxedTabsProps<K>) {
  const { className, tabs, selected } = props;

  return (
    <TabContainer className={className} role="tablist">
      {tabs.map(({ key, label }, i) => (
        <StyledTab
          id={getTabId(key)}
          active={selected === key}
          aria-selected={selected === key}
          aria-controls={getTabPanelId(key)}
          // eslint-disable-next-line react/no-array-index-key
          key={i}
          onClick={() => selected !== key && props.onSelect(key)}
          role="tab"
        >
          <ActiveBorder active={selected === key} />
          {label}
        </StyledTab>
      ))}
    </TabContainer>
  );
}

export function getTabPanelId(key: string | number) {
  return `tabpanel-${key}`;
}

export function getTabId(key: string | number) {
  return `tab-${key}`;
}
