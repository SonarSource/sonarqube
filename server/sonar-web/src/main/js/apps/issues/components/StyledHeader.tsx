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
import { Theme, themeColor, themeShadow } from '~design-system';

interface StyledHeaderProps {
  headerHeight: number;
  theme?: Theme;
}

export const PSEUDO_SHADOW_HEIGHT = 16;

// Box-shadow on scroll source: https://codepen.io/StijnDeWitt/pen/LryNxa
export const StyledHeader = styled.div<StyledHeaderProps>`
  position: sticky;
  top: -${PSEUDO_SHADOW_HEIGHT}px;
  z-index: 1;
  -webkit-backface-visibility: hidden;
  & > div {
    position: sticky;
    top: 0;
    margin-top: -${PSEUDO_SHADOW_HEIGHT}px;
    box-sizing: border-box;
    background-color: ${themeColor('backgroundSecondary')};
    z-index: 3;
  }
  &:before {
    content: '';
    display: block;
    height: ${PSEUDO_SHADOW_HEIGHT}px;
    position: sticky;
    top: ${({ headerHeight }) => `calc(${headerHeight}px - ${PSEUDO_SHADOW_HEIGHT}px)`};
    box-shadow: ${themeShadow('sm')};
  }
  &:after {
    content: '';
    display: block;
    height: ${PSEUDO_SHADOW_HEIGHT}px;
    position: sticky;
    background: linear-gradient(
      ${themeColor('backgroundSecondary')} 10%,
      rgba(255, 255, 255, 0.8) 50%,
      rgba(255, 255, 255, 0.4) 70%,
      transparent
    );
    top: 0;
    z-index: 2;
  }
`;

export default StyledHeader;
