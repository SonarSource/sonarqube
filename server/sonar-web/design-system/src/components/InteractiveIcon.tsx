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
import React from 'react';
import tw from 'twin.macro';
import { OPACITY_20_PERCENT } from '../helpers/constants';
import { themeBorder, themeColor, themeContrast } from '../helpers/theme';
import { isDefined } from '../helpers/types';
import { ThemedProps } from '../types/theme';
import { BaseLink, LinkProps } from './Link';
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
  onClick?: VoidFunction;
  size?: InteractiveIconSize;
  stopPropagation?: boolean;
  to?: LinkProps['to'];
}

export class InteractiveIconBase extends React.PureComponent<InteractiveIconProps> {
  handleClick = (event: React.MouseEvent<HTMLButtonElement | HTMLAnchorElement>) => {
    const { disabled, onClick, stopPropagation = true } = this.props;

    if (stopPropagation) {
      event.stopPropagation();
    }

    if (onClick && !disabled) {
      onClick();
    }
  };

  render() {
    const {
      Icon,
      children,
      disabled,
      innerRef,
      onClick,
      size = 'medium',
      to,
      iconProps = {},
      ...htmlProps
    } = this.props;

    const props = {
      ...htmlProps,
      'aria-disabled': disabled,
      disabled,
      size,
      type: 'button' as const,
    };

    if (to) {
      return (
        <IconLink {...props} onClick={onClick} showExternalIcon={false} stopPropagation to={to}>
          <Icon className={classNames({ 'sw-mr-1': isDefined(children) })} {...iconProps} />
          {children}
        </IconLink>
      );
    }

    return (
      <IconButton {...props} onClick={this.handleClick} ref={innerRef}>
        <Icon className={classNames({ 'sw-mr-1': isDefined(children) })} {...iconProps} />
        {children}
      </IconButton>
    );
  }
}

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
    color: ${themeContrast('buttonDisabled')(props)};
    background-color: var(--background);

    ${tw`sw-cursor-not-allowed`}
  }
`;

const IconLink = styled(BaseLink)`
  ${buttonIconStyle}
`;

const IconButton = styled.button`
  ${buttonIconStyle}
`;

export const InteractiveIcon: React.FC<React.PropsWithChildren<InteractiveIconProps>> = styled(
  InteractiveIconBase,
)`
  --background: ${themeColor('interactiveIcon')};
  --backgroundHover: ${themeColor('interactiveIconHover')};
  --color: ${({ currentColor, theme }) =>
    currentColor ? 'currentColor' : themeContrast('interactiveIcon')({ theme })};
  --colorHover: ${themeContrast('interactiveIconHover')};
  --focus: ${themeColor('interactiveIconFocus', OPACITY_20_PERCENT)};
`;

export const DiscreetInteractiveIcon: React.FC<React.PropsWithChildren<InteractiveIconProps>> =
  styled(InteractiveIcon)`
    --color: ${themeColor('discreetInteractiveIcon')};
  `;

export const DestructiveIcon: React.FC<React.PropsWithChildren<InteractiveIconProps>> = styled(
  InteractiveIconBase,
)`
  --background: ${themeColor('destructiveIcon')};
  --backgroundHover: ${themeColor('destructiveIconHover')};
  --color: ${themeContrast('destructiveIcon')};
  --colorHover: ${themeContrast('destructiveIconHover')};
  --focus: ${themeColor('destructiveIconFocus', OPACITY_20_PERCENT)};
`;

export const DismissProductNewsIcon: React.FC<React.PropsWithChildren<InteractiveIconProps>> =
  styled(InteractiveIcon)`
    --background: ${themeColor('productNews')};
    --backgroundHover: ${themeColor('productNewsHover')};
    --color: ${themeContrast('productNews')};
    --colorHover: ${themeContrast('productNewsHover')};
    --focus: ${themeColor('interactiveIconFocus', OPACITY_20_PERCENT)};

    height: 28px;
  `;
