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
import { themeBorder, themeColor, themeContrast } from '../../helpers';

export const BareButton = styled.button`
  all: unset;
  cursor: pointer;

  &:focus-visible {
    background-color: ${themeColor('dropdownMenuHover')};
  }
`;

interface CodeViewerExpanderProps {
  direction: 'UP' | 'DOWN';
}

export const CodeViewerExpander = styled(BareButton)<CodeViewerExpanderProps>`
  ${tw`sw-flex sw-items-center sw-gap-2`}
  ${tw`sw-px-2 sw-py-1`}
  ${tw`sw-code`}
  ${tw`sw-w-full`}
  ${tw`sw-box-border`}

  color: ${themeContrast('codeLineEllipsis')};
  background-color: ${themeColor('codeLineEllipsis')};

  &:hover {
    color: ${themeContrast('codeLineEllipsisHover')};
    background-color: ${themeColor('codeLineEllipsisHover')};
  }

  border-top: ${(props) =>
    props.direction === 'DOWN' ? themeBorder('default', 'codeLineBorder') : 'none'};

  border-bottom: ${(props) =>
    props.direction === 'UP' ? themeBorder('default', 'codeLineBorder') : 'none'};
`;

export const IssueIndicatorButton = styled(BareButton)`
  color: ${themeColor('codeLineMeta')};
  text-decoration: none;

  ${tw`sw-whitespace-nowrap`}
`;

export const DuplicationBlock = styled(BareButton)`
  background-color: ${themeColor('codeLineDuplication')};
  outline: none;

  ${tw`sw-block`}
  ${tw`sw-w-1 sw-h-full`}
  ${tw`sw-ml-1/2`}
  ${tw`sw-cursor-pointer`}
`;

export const LineSCMStyled = styled(BareButton)`
  outline: none;

  ${tw`sw-pr-2`}
  ${tw`sw-truncate`}
  ${tw`sw-whitespace-nowrap`}
  ${tw`sw-cursor-pointer`}
  ${tw`sw-w-full sw-h-full`}

  &:hover {
    color: ${themeColor('codeLineMetaHover')};
  }
`;
