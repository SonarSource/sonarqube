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
import { ReactNode } from 'react';
import tw from 'twin.macro';
import { themeColor } from '../../helpers/theme';

interface Props {
  children: ReactNode;
  isOverflowVisible?: boolean;
  isScrollable?: boolean;
}

export function ModalBody({ children, isScrollable = true, isOverflowVisible = false }: Props) {
  return (
    <StyledMain
      className={classNames({ scrollable: isScrollable, overflowVisible: isOverflowVisible })}
    >
      {children}
    </StyledMain>
  );
}

const StyledMain = styled.div`
  ${tw`sw-typo-default`}
  ${tw`sw-px-3`} // to accomodate a possible scrollbar
  ${tw`-sw-mx-3`}
  ${tw`sw-my-12`}
  ${tw`sw-overflow-x-hidden`}

  color: ${themeColor('pageContent')};

  &.scrollable {
    overflow-y: auto;
  }

  &.overflowVisible {
    overflow: visible;
  }
`;
