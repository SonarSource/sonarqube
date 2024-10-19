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
import { themeColor } from '../../helpers/theme';

export const NumberedListItem = styled.li`
  counter-increment: li;
  ${tw`sw-mb-4`}
  ${tw`sw-ml-1`}

  &::before {
    width: 19px;
    height: 19px;
    color: ${themeColor('numberedListText')};
    background-color: ${themeColor('numberedList')};
    border-radius: 20px;
    content: counter(li);

    ${tw`sw-typo-semibold`}
    ${tw`sw-p-1`}
    ${tw`sw-mr-3`}
    ${tw`sw-inline-block`}
    ${tw`sw-text-center`}
  }
`;

NumberedListItem.displayName = 'NumberedListItem';
