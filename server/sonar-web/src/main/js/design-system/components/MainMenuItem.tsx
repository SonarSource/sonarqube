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
import { LAYOUT_GLOBAL_NAV_HEIGHT } from '../helpers/constants';
import { themeBorder, themeContrast } from '../helpers/theme';

export const MainMenuItem = styled.li`
  & a {
    ${tw`sw-block sw-box-border`};
    ${tw`sw-text-sm sw-font-semibold`};
    ${tw`sw-whitespace-nowrap`};
    ${tw`sw-no-underline`};
    ${tw`sw-select-none`};
    ${tw`sw-font-sans`};

    color: ${themeContrast('mainBar')};
    letter-spacing: 0.03em;
    line-height: calc(${LAYOUT_GLOBAL_NAV_HEIGHT}px - 4px); // - 4px border bottom
    border-bottom: ${themeBorder('active', 'transparent', 1)};

    &:visited {
      border-bottom: ${themeBorder('active', 'transparent', 1)};
      color: ${themeContrast('mainBar')};
    }

    &:active,
    &.active,
    &:focus {
      border-bottom: ${themeBorder('active', 'menuBorder', 1)};
      color: ${themeContrast('mainBar')};
    }

    &:hover,
    &.hover {
      border-bottom: ${themeBorder('active', 'menuBorder', 1)};
      color: ${themeContrast('mainBarHover')};
    }
  }

  &[aria-expanded='true'] a {
    border-bottom: ${themeBorder('active', 'menuBorder', 1)};
    color: ${themeContrast('mainBarHover')};
  }
`;
