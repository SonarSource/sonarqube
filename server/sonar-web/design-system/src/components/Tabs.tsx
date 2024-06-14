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
import { FormattedMessage } from 'react-intl';
import tw from 'twin.macro';
import { OPACITY_20_PERCENT, themeBorder, themeColor } from '../helpers';
import { BareButton } from '../sonar-aligned/components/buttons';
import { getTabId, getTabPanelId } from '../sonar-aligned/helpers/tabs';
import { Badge } from './Badge';

type TabValueType = string | number | boolean;

export interface TabOption<T extends TabValueType> {
  counter?: number;
  disabled?: boolean;
  label: string | React.ReactNode;
  value: T;
}

export interface TabsProps<T extends TabValueType> {
  borderColor?: ReturnType<typeof themeBorder>;
  className?: string;
  disabled?: boolean;
  label?: string;
  large?: boolean;
  onChange: (value: T) => void;
  options: ReadonlyArray<TabOption<T>>;
  value?: T;
}

export function Tabs<T extends TabValueType>(props: PropsWithChildren<TabsProps<T>>) {
  const {
    disabled = false,
    label,
    options,
    value,
    className,
    children,
    large = false,
    borderColor = themeBorder('default'),
  } = props;

  return (
    <TabsContainer borderColor={borderColor} className={className} large={large}>
      <TabList aria-label={label} role="tablist">
        {options.map((option) => (
          <TabButton
            aria-controls={getTabPanelId(String(option.value))}
            aria-current={option.value === value}
            aria-selected={option.value === value}
            borderColor={borderColor}
            data-value={option.value}
            disabled={disabled || option.disabled}
            id={getTabId(String(option.value))}
            key={option.value.toString()}
            large={large}
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
              <Badge className="sw-ml-2 sw-font-semibold" variant="counterFailed">
                <FormattedMessage id="overview.failed.badge" values={{ counter: option.counter }} />
              </Badge>
            ) : null}
          </TabButton>
        ))}
      </TabList>
      <RightSection>{children}</RightSection>
    </TabsContainer>
  );
}

const TabsContainer = styled.div<{ borderColor: ReturnType<typeof themeBorder>; large: boolean }>`
  ${tw`sw-w-full`};
  ${(props) => !props.large && tw`sw-pl-4`}
  ${tw`sw-flex sw-justify-between`};
  border-bottom: ${(props) => props.borderColor};
`;

const TabList = styled.div`
  ${tw`sw-inline-flex`};
`;

const TabButton = styled(BareButton)<{
  borderColor: ReturnType<typeof themeBorder>;
  large: boolean;
  selected: boolean;
}>`
  ${tw`sw-relative`};
  ${tw` sw-mb-[-1px]`};
  ${tw`sw-flex sw-items-center`};
  ${(props) => (props.large ? tw`sw-body-md sw-px-6 sw-py-4` : tw`sw-body-sm sw-px-3 sw-py-1`)}
  ${tw`sw-font-semibold`};
  ${tw`sw-rounded-t-1`};

  height: 34px;
  background: ${(props) => (props.selected ? themeColor('backgroundSecondary') : 'none')};
  color: ${(props) => (props.selected ? themeColor('tabSelected') : themeColor('tab'))};
  border: ${(props) =>
    props.selected ? props.borderColor : themeBorder('default', 'transparent')};
  border-bottom: ${(props) =>
    props.selected ? themeBorder('default', 'backgroundSecondary') : props.borderColor};

  &:hover {
    background: ${(props) =>
      props.selected ? themeColor('backgroundSecondary') : themeColor('tabHover')};
  }

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
