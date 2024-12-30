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
import { LAYOUT_VIEWPORT_MIN_WIDTH } from '../helpers';
import { themeColor, themeContrast, themeShadow } from '../helpers/theme';

export const TopBar = styled.nav`
  ${tw`sw-px-6 sw-pt-4`}
  ${tw`sw-box-border`};
  ${tw`sw-w-full`};
  ${tw`sw-font-sans`}
  ${tw`sw-text-sm`}

  min-width: ${LAYOUT_VIEWPORT_MIN_WIDTH}px;

  background-color: ${themeColor('navbar')};
  color: ${themeContrast('navbar')};
  box-shadow: ${themeShadow('lg')};
`;
