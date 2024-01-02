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
import {
  LAYOUT_VIEWPORT_MAX_WIDTH,
  LAYOUT_VIEWPORT_MAX_WIDTH_LARGE,
  LAYOUT_VIEWPORT_MIN_WIDTH,
} from '../helpers';

const BaseLayout = styled.div`
  box-sizing: border-box;
  min-width: ${LAYOUT_VIEWPORT_MIN_WIDTH}px;
  margin: 0 auto;

  ${tw`sw-px-14`}
`;

export const CenteredLayout = styled(BaseLayout)`
  max-width: ${LAYOUT_VIEWPORT_MAX_WIDTH}px;
`;

export const LargeCenteredLayout = styled(BaseLayout)`
  max-width: ${LAYOUT_VIEWPORT_MAX_WIDTH_LARGE}px;
`;
