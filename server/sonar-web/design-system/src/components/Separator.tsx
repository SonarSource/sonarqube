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
import { themeColor } from '../helpers/theme';

export const BasicSeparator = styled.hr`
  height: 1px;
  background-color: ${themeColor('border')};

  ${tw`sw-my-1`}
  ${tw`sw-overflow-hidden`};
  ${tw`sw-clear-both`}
`;

export const BlueGreySeparator = styled(BasicSeparator)`
  background-color: ${themeColor('popupBorder')};
`;

export const GreySeparator = styled(BasicSeparator)`
  background-color: ${themeColor('subnavigationBorder')};
`;

export const SubnavigationFlowSeparator = styled(BasicSeparator)`
  background-color: ${themeColor('subnavigationExecutionFlowSeparator')};
`;
