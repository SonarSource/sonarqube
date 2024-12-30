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
import { createRef, RefObject, useEffect, useState } from 'react';
import tw from 'twin.macro';
import { themeColor } from '../../helpers/theme';

const POINTER_HANDLE_HEIGHT_OFFSET = 2;
const POINTER_HANDLE_MAX_HEIGHT = 10;

interface Props {
  issueFindingRef?: RefObject<HTMLDivElement>;
}

export function LineIssuePointer({ issueFindingRef }: Props) {
  const [distance, setDistance] = useState(0);
  const pointerRef = createRef<HTMLDivElement>();

  useEffect(() => {
    if (pointerRef.current && issueFindingRef?.current) {
      setDistance(
        issueFindingRef.current.getBoundingClientRect().top -
          pointerRef.current.getBoundingClientRect().bottom,
      );
    }
  }, [pointerRef, issueFindingRef]);

  // Keep only the pointer head as reference for future calculations
  if (distance < POINTER_HANDLE_HEIGHT_OFFSET || distance > POINTER_HANDLE_MAX_HEIGHT) {
    return <EmptyIssuePointer data-testid="empty-issue-pointer" ref={pointerRef} />;
  }

  return <IssuePointer data-testid="issue-pointer" distance={distance} ref={pointerRef} />;
}

const EmptyIssuePointer = styled.div`
  bottom: -0.325rem;
  left: 50%;
  width: 0.3125rem;
  height: 0.3125rem;
  border: 0.125rem solid transparent;
  transform: translate(-50%, 0);

  ${tw`sw-block sw-absolute sw-rounded-2`};
`;

const IssuePointer = styled(EmptyIssuePointer)<{ distance: number }>`
  background-color: ${themeColor('codeLineIssueSquiggle')};
  border-color: ${themeColor('codeLineIssuePointerBorder')};

  &::after {
    position: absolute;
    top: 5px;
    left: 2px;
    display: block;
    width: 1px;
    height: ${({ distance }) => `${distance + POINTER_HANDLE_HEIGHT_OFFSET}px`};
    background-color: ${themeColor('codeLineIssueSquiggle')};
    content: '';
  }
`;
