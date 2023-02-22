/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { themeColor, themeContrast } from '../helpers/theme';

interface MainTextProps {
  match?: string;
  name: string;
}

export function SearchText({ match, name }: MainTextProps) {
  return match ? (
    <StyledText
      // Safe: comes from the search engine, that injects bold tags into component names
      // eslint-disable-next-line react/no-danger
      dangerouslySetInnerHTML={{ __html: match }}
    />
  ) : (
    <StyledText title={name}>{name}</StyledText>
  );
}

export function TextMuted({ text }: { text: string }) {
  return <StyledMutedText title={text}>{text}</StyledMutedText>;
}

export const StyledText = styled.span`
  ${tw`sw-inline-block`};
  ${tw`sw-truncate`};
  ${tw`sw-font-semibold`};
  ${tw`sw-max-w-abs-600`}

  mark {
    ${tw`sw-inline-block`};

    background: ${themeColor('searchHighlight')};
    color: ${themeContrast('searchHighlight')};
  }
`;

const StyledMutedText = styled(StyledText)`
  ${tw`sw-font-regular`};
  color: ${themeColor('dropdownMenuSubTitle')};
`;
