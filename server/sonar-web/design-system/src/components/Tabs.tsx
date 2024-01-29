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
import { PropsWithChildren } from 'react';
import tw from 'twin.macro';
import { OPACITY_20_PERCENT, themeBorder, themeColor } from '../helpers';
import { getTabId, getTabPanelId } from '../helpers/tabs';
import { Badge } from './Badge';
import { BareButton } from './buttons';

type TabValueType = string | number | boolean;

export interface TabOption<T extends TabValueType> {
  counter?: number;
  disabled?: boolean;
  label: string | React.ReactNode;
  value: T;
}

export interface TabsProps<T extends TabValueType> {
  className?: string;
  disabled?: boolean;
  label?: string;
  onChange: (value: T) => void;
  options: ReadonlyArray<TabOption<T>>;
  value?: T;
}

export function Tabs<T extends TabValueType>(props: PropsWithChildren<TabsProps<T>>) {
  const { disabled = false, label, options, value, className, children } = props;

  return (
    <TabsContainer className={className}>
      <TabList aria-label={label} role="tablist">
        {options.map((option) => (
          <TabButton
            aria-controls={getTabPanelId(String(option.value))}
            aria-current={option.value === value}
            aria-selected={option.value === value}
            data-value={option.value}
            disabled={disabled || option.disabled}
            id={getTabId(String(option.value))}
            key={option.value.toString()}
            onClick={() => {
              if (option.value !== value) {
                props.onChange(option.value);
              }
            }}
            role="tab"
            selected={option.value === value}
          >
            {option.label}
            {option.counter ? (
              <Badge className="sw-ml-2" variant="counterFailed">
                {option.counter}
              </Badge>
            ) : null}
          </TabButton>
        ))}
      </TabList>
      <RightSection>{children}</RightSection>
    </TabsContainer>
  );
}

const TabsContainer = styled.div`
  ${tw`sw-w-full`};
  ${tw`sw-pl-4`};
  ${tw`sw-flex sw-justify-between`};
  border-bottom: ${themeBorder('default')};
`;

const TabList = styled.div`
  ${tw`sw-inline-flex`};
`;

const TabButton = styled(BareButton)<{ selected: boolean }>`
  ${tw`sw-relative`};
  ${tw`sw-px-3 sw-py-1 sw-mb-[-1px]`};
  ${tw`sw-flex sw-items-center`};
  ${tw`sw-body-sm`};
  ${tw`sw-font-semibold`};
  ${tw`sw-rounded-t-1`};

  height: 34px;
  background: ${(props) => (props.selected ? themeColor('backgroundSecondary') : 'none')};
  color: ${(props) => (props.selected ? themeColor('tabSelected') : themeColor('tab'))};
  border: ${(props) =>
    props.selected ? themeBorder('default') : themeBorder('default', 'transparent')};
  border-bottom: ${(props) =>
    themeBorder('default', props.selected ? 'backgroundSecondary' : undefined)};

  &:hover {
    background: ${themeColor('tabHover')};
  }

  &:focus,
  &:active {
    outline: ${themeBorder('xsActive', 'tabSelected', OPACITY_20_PERCENT)};
    z-index: 1;
  }

  // Active line
  &::after {
    content: '';
    ${tw`sw-absolute`};
    ${tw`sw-rounded-t-1`};
    top: 0;
    left: 0;
    width: calc(100%);
    height: 2px;
    background: ${(props) => (props.selected ? themeColor('tabSelected') : 'none')};
  }
`;

const RightSection = styled.div`
  max-height: 43px;
  ${tw`sw-flex sw-items-center`};
`;
