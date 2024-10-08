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
import tw from 'twin.macro';
import { themeBorder, themeColor, themeContrast } from '../helpers/theme';
import { ThemeColors } from '../types/theme';

type BadgeVariant = 'default' | 'new' | 'deleted' | 'counter' | 'counterFailed';

const variantList: Record<BadgeVariant, ThemeColors> = {
  default: 'badgeDefault',
  new: 'badgeNew',
  deleted: 'badgeDeleted',
  counter: 'badgeCounter',
  counterFailed: 'badgeCounterFailed',
};

interface BadgeProps extends React.PropsWithChildren {
  className?: string;
  title?: string;
  variant?: BadgeVariant;
}

function getColor(variantInfo: ThemeColors) {
  if (variantInfo === 'badgeCounterFailed') {
    return 'var(--echoes-color-text-danger-bold)';
  }

  return themeContrast(variantInfo);
}

export function Badge({ className, children, title, variant = 'default' }: BadgeProps) {
  const commonProps = {
    'aria-label': title,
    className,
    role: title ? 'img' : 'presentation',
    title,
  };

  const Component = ['counter', 'counterFailed'].includes(variant) ? StyledCounter : StyledBadge;

  return (
    <Component variantInfo={variantList[variant]} {...commonProps}>
      {children}
    </Component>
  );
}

const StyledBadge = styled.span<{
  variantInfo: ThemeColors;
}>`
  ${tw`sw-text-[0.75rem]`};
  ${tw`sw-leading-[0.938rem]`};
  ${tw`sw-font-semibold`};
  ${tw`sw-inline-block`};
  ${tw`sw-whitespace-nowrap`};
  ${tw`sw-px-[0.125rem] sw-py-[0.03125rem]`};
  ${tw`sw-rounded-1/2`};

  color: ${({ variantInfo }) => themeContrast(variantInfo)};
  background-color: ${({ variantInfo }) => themeColor(variantInfo)};
  text-transform: uppercase;

  &:empty {
    ${tw`sw-hidden`}
  }
`;

const StyledCounter = styled.span<{
  variantInfo: ThemeColors;
}>`
  ${tw`sw-min-w-5 sw-min-h-5`};
  ${tw`sw-text-[0.75rem]`};
  ${tw`sw-font-regular`};
  ${tw`sw-box-border sw-px-[5px]`};
  ${tw`sw-inline-flex`};
  ${tw`sw-leading-[1.125rem]`};
  ${tw`sw-items-center sw-justify-center`};
  ${tw`sw-rounded-pill`};

  color: ${({ variantInfo }) => getColor(variantInfo)};
  background-color: ${({ variantInfo }) => themeColor(variantInfo)};
  border: ${({ variantInfo }) =>
    themeBorder(
      'default',
      variantInfo === 'badgeCounterFailed' ? 'badgeCounterFailedBorder' : 'transparent',
    )};

  &:empty {
    ${tw`sw-hidden`}
  }
`;
