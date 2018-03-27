/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

/* eslint-disable import/export */

declare module '@atlaskit/button' {
  export type ButtonAppearances =
    | 'default'
    | 'danger'
    | 'link'
    | 'primary'
    | 'subtle'
    | 'subtle-link'
    | 'warning'
    | 'help';

  export interface ButtonProps {
    appearance?: ButtonAppearances;
    children?: React.ReactNode;
    className?: string;
    form?: string;
    href?: string;
    iconAfter?: JSX.Element;
    iconBefore?: JSX.Element;
    innerRef?: Function;
    isDisabled?: boolean;
    isSelected?: boolean;
    onBlur?: (e: React.SyntheticEvent<FocusEvent>) => void;
    onClick?: (e: React.SyntheticEvent<MouseEvent>) => void;
    onFocus?: (e: React.SyntheticEvent<FocusEvent>) => void;
    spacing?: 'compact' | 'default' | 'none';
    tabIndex?: number;
    target?: string;
    type?: 'button' | 'submit';
    shouldFitContainer?: boolean;
  }

  export default class Button extends React.Component<ButtonProps> {}
}

declare module '@atlaskit/checkbox' {
  export interface CheckboxProps {
    isChecked: boolean;
    isIndeterminate?: boolean;
    isDisabled?: boolean;
    isFullWidth?: boolean;
    label: string;
    name: string;
    isInvalid?: boolean;
    onChange?: (event: React.ChangeEvent<HTMLInputElement>) => void;
    value: number | string;
  }

  export class CheckboxStateless extends React.Component<CheckboxProps> {}
}

declare module '@atlaskit/logo' {
  export interface LogoProps {
    size?: 'xsmall' | 'small' | 'medium' | 'large' | 'xlarge';
    textColor?: string;
    iconColor?: string;
    iconGradientStart?: string;
    iconGradientStop?: string;
    label?: string;
  }

  export class BitbucketIcon extends React.Component<LogoProps> {}
}

declare module '@atlaskit/single-select' {
  export interface GroupType {
    heading?: string;
    items: ItemType[];
  }

  export interface ItemType {
    content?: React.ReactNode;
    description?: string;
    label?: string;
    tooltipDescription?: string;
    tooltipPosition?: 'top' | 'bottom' | 'left';
    value?: string | number;
    filterValues?: string[];
    isDisabled?: boolean;
    isSelected?: boolean;
    elemBefore?: React.ReactNode;
  }

  export default class SingleSelect extends React.Component<{
    defaultSelected?: ItemType;
    droplistShouldFitContainer?: boolean;
    hasAutocomplete?: boolean;
    invalidMessage?: React.ReactNode;
    isDisabled?: boolean;
    isRequired?: boolean;
    isInvalid?: boolean;
    items?: GroupType[];
    label?: string;
    noMatchesFound?: string;
    onFilterChange?: Function;
    onSelected?: Function;
    placeholder?: string;
    shouldFitContainer?: boolean;
    shouldFlip?: boolean;
    shouldFocus?: boolean;
    maxHeight?: number;
  }> {}
}

declare module '@atlaskit/spinner' {
  type SpinnerSizes = 'small' | 'medium' | 'large' | 'xlarge' | number;

  export interface SpinnerProps {
    delay?: number;
    invertColor?: boolean;
    isCompleting?: boolean;
    onComplete?: Function;
    size?: SpinnerSizes;
  }

  export default class Spinner extends React.Component<SpinnerProps> {}
}

declare module '@atlaskit/tooltip' {
  export interface TooltipProps {
    children: React.ReactNode;
    content?: React.ReactNode;
    hideTooltipOnClick?: boolean;
    mousePosition?: 'bottom' | 'left' | 'right' | 'top';
    position?: 'bottom' | 'left' | 'right' | 'top' | 'mouse';
    tag?: string;
    truncate?: boolean;
  }

  export default class Tooltip extends React.Component<TooltipProps> {}
}
