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
import { getProp, themeColor, themeContrast } from '../helpers/theme';
import { MetricsLabel } from '../types/measures';

type sizeType = 'xs' | 'sm' | 'md' | 'xl';
interface Props extends React.AriaAttributes {
  className?: string;
  label: string;
  rating?: MetricsLabel;
  size?: sizeType;
}

const SIZE_MAPPING = {
  xs: '1rem',
  sm: '1.5rem',
  md: '2rem',
  xl: '4rem',
};

export function MetricsRatingBadge({
  className,
  size = 'sm',
  label,
  rating,
  ...ariaAttrs
}: Readonly<Props>) {
  if (!rating) {
    return (
      <StyledNoRatingBadge
        aria-label={label}
        className={className}
        size={SIZE_MAPPING[size]}
        {...ariaAttrs}
      >
        —
      </StyledNoRatingBadge>
    );
  }
  return (
    <MetricsRatingBadgeStyled
      aria-label={label}
      className={className}
      rating={rating}
      size={SIZE_MAPPING[size]}
      {...ariaAttrs}
    >
      {rating}
    </MetricsRatingBadgeStyled>
  );
}

const StyledNoRatingBadge = styled.div<{ size: string }>`
  display: inline-flex;
  align-items: center;
  justify-content: center;

  width: ${getProp('size')};
  height: ${getProp('size')};
`;

const getFontSize = (size: string) => {
  switch (size) {
    case '2rem':
      return '0.875rem';
    case '4rem':
      return '1.5rem';
    default:
      return '0.75rem';
  }
};

const MetricsRatingBadgeStyled = styled.div<{ rating: MetricsLabel; size: string }>`
  width: ${getProp('size')};
  height: ${getProp('size')};
  color: ${({ rating }) => themeContrast(`rating.${rating}`)};
  font-size: ${({ size }) => getFontSize(size)};
  background-color: ${({ rating }) => themeColor(`rating.${rating}`)};

  display: inline-flex;
  align-items: center;
  justify-content: center;

  ${tw`sw-rounded-pill`};
  ${tw`sw-font-semibold`};
`;
