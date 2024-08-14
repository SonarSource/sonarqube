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
import { forwardRef } from 'react';
import tw from 'twin.macro';
import { getProp, themeColor, themeContrast } from '../../helpers/theme';
import { RatingLabel } from '../types/measures';

type sizeType = keyof typeof SIZE_MAPPING;
interface Props extends React.AriaAttributes {
  className?: string;
  isLegacy?: boolean;
  label?: string;
  rating?: RatingLabel;
  size?: sizeType;
}

const SIZE_MAPPING = {
  xs: '1rem',
  sm: '1.5rem',
  md: '2rem',
  lg: '2.8rem',
  xl: '4rem',
};

export const MetricsRatingBadge = forwardRef<HTMLDivElement, Props>(
  (
    { className, size = 'sm', isLegacy = true, label, rating, ...ariaAttrs }: Readonly<Props>,
    ref,
  ) => {
    if (!rating) {
      return (
        <StyledNoRatingBadge
          aria-label={label}
          className={className}
          ref={ref}
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
        isLegacy={isLegacy}
        rating={rating}
        ref={ref}
        size={SIZE_MAPPING[size]}
        {...ariaAttrs}
      >
        {rating}
      </MetricsRatingBadgeStyled>
    );
  },
);

MetricsRatingBadge.displayName = 'MetricsRatingBadge';

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

const MetricsRatingBadgeStyled = styled.div<{
  isLegacy: boolean;
  rating: RatingLabel;
  size: string;
}>`
  width: ${getProp('size')};
  height: ${getProp('size')};
  color: ${({ rating }) => themeContrast(`rating.${rating}`)};
  font-size: ${({ size }) => getFontSize(size)};
  background-color: ${({ rating, isLegacy }) =>
    themeColor(`rating.${isLegacy ? 'legacy.' : ''}${rating}`)};
  user-select: none;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  ${tw`sw-rounded-pill`};
  ${tw`sw-font-semibold`};
`;
