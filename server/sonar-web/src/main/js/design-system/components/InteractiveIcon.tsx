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

import { css } from '@emotion/react';
import styled from '@emotion/styled';
import classNames from 'classnames';
import React, { ForwardedRef, MouseEvent, forwardRef, useCallback } from 'react';
import tw from 'twin.macro';
import { OPACITY_20_PERCENT } from '../helpers/constants';
import { themeBorder, themeColor, themeContrast } from '../helpers/theme';
import { isDefined } from '../helpers/types';
import { ThemedProps } from '../types/theme';
import { IconProps } from './icons/Icon';

export type InteractiveIconSize = 'small' | 'medium';

export interface InteractiveIconProps {
  Icon: React.ComponentType<React.PropsWithChildren<IconProps>>;
  'aria-label': string;
  children?: React.ReactNode;
  className?: string;
  currentColor?: boolean;
  disabled?: boolean;
  iconProps?: IconProps;
  id?: string;
  innerRef?: React.Ref<HTMLButtonElement>;
  onClick?: (event: MouseEvent<HTMLButtonElement>) => void;
  size?: InteractiveIconSize;
  stopPropagation?: boolean;
}

export const InteractiveIconBase = forwardRef(
  (props: InteractiveIconProps, ref: ForwardedRef<HTMLButtonElement>) => {
    const {
      Icon,
      children,
      disabled,
      onClick,
      size = 'medium',
      iconProps = {},
      stopPropagation = true,
      ...htmlProps
    } = props;

    const handleClick = useCallback(
      (event: React.MouseEvent<HTMLButtonElement>) => {
        if (stopPropagation) {
          event.stopPropagation();
        }

        if (onClick && !disabled) {
          onClick(event);
        }
      },
      [disabled, onClick, stopPropagation],
    );

    const propsForInteractiveWrapper = {
      ...htmlProps,
      'aria-disabled': disabled,
      disabled,
      size,
    };

    return (
      <IconButton {...propsForInteractiveWrapper} onClick={handleClick} ref={ref} type="button">
        <Icon className={classNames({ 'sw-mr-1': isDefined(children) })} {...iconProps} />
        {children}
      </IconButton>
    );
  },
);

InteractiveIconBase.displayName = 'InteractiveIconBase';

const buttonIconStyle = (props: ThemedProps & { size: InteractiveIconSize }) => css`
  box-sizing: border-box;
  border: none;
  outline: none;
  text-decoration: none;
  color: var(--color);
  background-color: var(--background);
  transition:
    background-color 0.2s ease,
    outline 0.2s ease,
    color 0.2s ease;

  ${tw`sw-inline-flex sw-items-center sw-justify-center`}
  ${tw`sw-cursor-pointer`}

  ${{
    small: tw`sw-h-6 sw-px-1 sw-rounded-1/2`,
    medium: tw`sw-h-control sw-px-[0.625rem] sw-rounded-2`,
  }[props.size]}


  &:hover,
  &:focus,
  &:active {
    color: var(--colorHover);
    background-color: var(--backgroundHover);
  }

  &:focus,
  &:active {
    outline: ${themeBorder('focus', 'var(--focus)')(props)};
  }

  &:disabled,
  &:disabled:hover {
    color: var(--echoes-color-icon-disabled);
    background-color: var(--background);

    ${tw`sw-cursor-not-allowed`}
  }
`;

const IconButton = styled.button`
  ${buttonIconStyle}
`;

/**
 * @deprecated Use ButtonIcon from Echoes instead.
 *
 * Use the `variety` prop with the ButtonVariety enum to change the
 * button's look and feel.
 *
 * Some of the props have changed or been renamed:
 * - `disabled` is now `isDisabled`, note that an Echoes Tooltip won't work
 * on a disabled button, use a text notice or ToggleTip next to the disabled button instead.
 * - `Icon` is restricted to Echoes' Icons
 * - `aria-label` is now `ariaLabel`
 * - `size` now requires a value from the ButtonSize enum
 *
 * New props:
 * - `tooltipContent` overrides the content of the tooltip (which defaults to the value of ariaLabel!)
 * - `tooltipProps` allows you to customize the tooltip positioning (`align` and `side`)
 *
 * See the {@link https://xtranet-sonarsource.atlassian.net/wiki/spaces/Platform/pages/3382706231/Button | Migration Guide} for more information.
 */
export const InteractiveIcon = styled(InteractiveIconBase)`
  --background: ${themeColor('interactiveIcon')};
  --backgroundHover: ${themeColor('interactiveIconHover')};
  --color: ${({ currentColor, theme }) =>
    currentColor ? 'currentColor' : themeContrast('interactiveIcon')({ theme })};
  --colorHover: ${themeContrast('interactiveIconHover')};
  --focus: ${themeColor('interactiveIconFocus', OPACITY_20_PERCENT)};
`;

/**
 * @deprecated Use ButtonIcon from Echoes instead, with the ButtonVariety.DefaultGhost variety.
 *
 * Some of the props have changed or been renamed:
 * - `disabled` is now `isDisabled`, note that an Echoes Tooltip won't work
 * on a disabled button, use a text notice or ToggleTip next to the disabled button instead.
 * - `Icon` is restricted to Echoes' Icons
 * - `aria-label` is now `ariaLabel`
 * - `size` now requires a value from the ButtonSize enum
 *
 * New props:
 * - `tooltipContent` overrides the content of the tooltip (which defaults to the value of ariaLabel!)
 * - `tooltipProps` allows you to customize the tooltip positioning (`align` and `side`)
 *
 * See the {@link https://xtranet-sonarsource.atlassian.net/wiki/spaces/Platform/pages/3382706231/Button | Migration Guide} for more information.
 */
export const DiscreetInteractiveIcon = styled(InteractiveIcon)`
  --color: var(--echoes-color-icon-subdued);
`;

/**
 * @deprecated Use ButtonIcon from Echoes instead, with the ButtonVariety.DangerGhost variety.
 *
 * Some of the props have changed or been renamed:
 * - `disabled` is now `isDisabled`, note that an Echoes Tooltip won't work
 * on a disabled button, use a text notice or ToggleTip next to the disabled button instead.
 * - `Icon` is restricted to Echoes' Icons
 * - `aria-label` is now `ariaLabel`
 * - `size` now requires a value from the ButtonSize enum
 *
 * New props:
 * - `tooltipContent` overrides the content of the tooltip (which defaults to the value of ariaLabel!)
 * - `tooltipProps` allows you to customize the tooltip positioning (`align` and `side`)
 *
 * See the {@link https://xtranet-sonarsource.atlassian.net/wiki/spaces/Platform/pages/3382706231/Button | Migration Guide} for more information.
 */
export const DestructiveIcon = styled(InteractiveIconBase)`
  --background: ${themeColor('destructiveIcon')};
  --backgroundHover: ${themeColor('destructiveIconHover')};
  --color: ${themeContrast('destructiveIcon')};
  --colorHover: ${themeContrast('destructiveIconHover')};
  --focus: ${themeColor('destructiveIconFocus', OPACITY_20_PERCENT)};
`;

/**
 * @deprecated Use ButtonIcon from Echoes instead, with the ButtonVariety.DefaultGhost variety.
 *
 * Some of the props have changed or been renamed:
 * - `disabled` is now `isDisabled`, note that an Echoes Tooltip won't work
 * on a disabled button, use a text notice or ToggleTip next to the disabled button instead.
 * - `Icon` is restricted to Echoes' Icons
 * - `aria-label` is now `ariaLabel`
 * - `size` now requires a value from the ButtonSize enum
 *
 * New props:
 * - `tooltipContent` overrides the content of the tooltip (which defaults to the value of ariaLabel!)
 * - `tooltipProps` allows you to customize the tooltip positioning (`align` and `side`)
 *
 * See the {@link https://xtranet-sonarsource.atlassian.net/wiki/spaces/Platform/pages/3382706231/Button | Migration Guide} for more information.
 */
export const DismissProductNewsIcon = styled(InteractiveIcon)`
  --background: ${themeColor('productNews')};
  --backgroundHover: ${themeColor('productNewsHover')};
  --color: ${themeContrast('productNews')};
  --colorHover: ${themeContrast('productNewsHover')};
  --focus: ${themeColor('interactiveIconFocus', OPACITY_20_PERCENT)};

  height: 28px;
`;
