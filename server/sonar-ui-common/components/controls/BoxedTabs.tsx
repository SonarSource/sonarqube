/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { styled, themeColor, ThemedProps, themeSize } from '../theme';

export interface BoxedTabsProps<K> {
  className?: string;
  onSelect: (key: K) => void;
  selected?: K;
  tabs: Array<{ key: K; label: React.ReactNode }>;
}

const TabContainer = styled.div`
  display: flex;
  flex-direction: row;
`;

const baseBorder = ({ theme }: ThemedProps) => `1px solid ${theme.colors.barBorderColor}`;

const highlightHoverMixin = ({ theme }: ThemedProps) => `
  &:hover {
    background-color: ${theme.colors.barBackgroundColorHighlight};
  }
`;

const StyledTab = styled.button<{ active: boolean }>`
  position: relative;
  background-color: ${(props) => (props.active ? 'white' : props.theme.colors.barBackgroundColor)};
  border-top: ${baseBorder};
  border-left: ${baseBorder};
  border-right: none;
  border-bottom: none;
  margin-bottom: -1px;
  min-width: 128px;
  min-height: 56px;
  ${(props) => !props.active && 'cursor: pointer;'}
  outline: 0;
  padding: calc(2 * ${themeSize('gridSize')});

  ${(props) => (!props.active ? highlightHoverMixin : null)}

  &:last-child {
    border-right: ${baseBorder};
  }
`;

const ActiveBorder = styled.div<{ active: boolean }>`
  display: ${(props) => (props.active ? 'block' : 'none')};
  background-color: ${themeColor('blue')};
  height: 3px;
  width: 100%;
  position: absolute;
  left: 0;
  top: -1px;
`;

export default function BoxedTabs<K>(props: BoxedTabsProps<K>) {
  const { className, tabs, selected } = props;

  return (
    <TabContainer className={className}>
      {tabs.map(({ key, label }, i) => (
        <StyledTab
          active={selected === key}
          key={i}
          onClick={() => selected !== key && props.onSelect(key)}
          type="button">
          <ActiveBorder active={selected === key} />
          {label}
        </StyledTab>
      ))}
    </TabContainer>
  );
}
