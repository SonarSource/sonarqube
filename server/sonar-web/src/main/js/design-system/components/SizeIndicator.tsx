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
import { inRange } from 'lodash';
import tw from 'twin.macro';
import { getProp, themeColor, themeContrast } from '../helpers/theme';
import { SizeLabel } from '../types/measures';

export interface Props {
  size?: 'xs' | 'sm' | 'md';
  value: number;
}

const SIZE_MAPPING = {
  xs: '1rem',
  sm: '1.5rem',
  md: '2rem',
};

const SIZE_IN_LOC = {
  xs: 1000,
  sm: 10_000,
  md: 100_000,
  l: 500_000,
};

export function SizeIndicator({ size = 'sm', value }: Props) {
  let letter: SizeLabel;
  if (inRange(value, 0, SIZE_IN_LOC.xs)) {
    letter = 'XS';
  } else if (inRange(value, SIZE_IN_LOC.xs, SIZE_IN_LOC.sm)) {
    letter = 'S';
  } else if (inRange(value, SIZE_IN_LOC.sm, SIZE_IN_LOC.md)) {
    letter = 'M';
  } else if (inRange(value, SIZE_IN_LOC.md, SIZE_IN_LOC.l)) {
    letter = 'L';
  } else {
    letter = 'XL';
  }

  return (
    <StyledContainer aria-hidden="true" size={SIZE_MAPPING[size]}>
      {letter}
    </StyledContainer>
  );
}

const StyledContainer = styled.div<{ size: string }>`
  width: ${getProp('size')};
  height: ${getProp('size')};
  font-size: ${({ size }) => (size === '2rem' ? '0.875rem' : `calc(${size}/2)`)};
  color: ${themeContrast('sizeIndicator')};
  background-color: ${themeColor('sizeIndicator')};

  ${tw`sw-inline-flex sw-items-center sw-justify-center`};
  ${tw`sw-leading-4`};
  ${tw`sw-rounded-pill`};
  ${tw`sw-font-semibold`};
`;
