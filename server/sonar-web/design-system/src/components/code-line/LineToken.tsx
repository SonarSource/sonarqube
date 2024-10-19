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
import classNames from 'classnames';
import { ReactNode, RefObject } from 'react';
import tw from 'twin.macro';
import { themeColor } from '../../helpers/theme';
import { LineIssuePointer } from './LineIssuePointer';

export interface TokenModifiers {
  isHighlighted?: boolean;
  isLocation?: boolean;
  isSelected?: boolean;
  isUnderlined?: boolean;
}

interface Props extends TokenModifiers {
  children: ReactNode;
  className?: string;
  hasMarker?: boolean;
  issueFindingRef?: RefObject<HTMLDivElement>;
}

export function LineToken(props: Props) {
  const { children, className, hasMarker, issueFindingRef, ...modifiers } = props;

  return (
    <TokenStyled
      className={classNames(className, {
        'issue-underline': modifiers.isUnderlined,
        'issue-location': modifiers.isLocation,
        highlighted: modifiers.isHighlighted,
        selected: modifiers.isSelected,
        'has-marker': hasMarker,
      })}
    >
      <>{children}</>
      {modifiers.isUnderlined && <LineIssuePointer issueFindingRef={issueFindingRef} />}
    </TokenStyled>
  );
}

const TokenStyled = styled.span`
  display: inline-block;

  &.sym {
    ${tw`sw-cursor-pointer`}
  }

  &.sym.highlighted {
    background-color: ${themeColor('codeLineLocationHighlighted')};
    transition: background-color 0.3s;
  }

  &.issue-underline {
    position: relative;
    z-index: 1;
    text-decoration: underline ${themeColor('codeLineIssueSquiggle')};
    text-decoration: underline ${themeColor('codeLineIssueSquiggle')} wavy;
    text-decoration-thickness: 2px;
    text-decoration-skip-ink: none;
  }

  &.issue-location {
    line-height: 1.125rem;
    background-color: ${themeColor('codeLineIssueLocation')};
    transition: background-color 0.3s ease;
  }

  &.issue-location.selected {
    background-color: ${themeColor('codeLineIssueLocationSelected')};
  }

  &.issue-location.has-marker {
    ${tw`sw-pl-1`}
  }
`;
