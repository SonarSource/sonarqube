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
import { themeBorder, themeColor } from 'design-system';
import React from 'react';

interface Props {
  content: React.ReactNode;
  title: string;
}

export default function PromotedSection({ content, title }: Readonly<Props>) {
  return (
    <StyledWrapper className="sw-p-4 sw-pl-6 sw-my-6 sw-rounded-2">
      <div className="sw-flex sw-justify-between sw-mb-2">
        <StyledTitle className="sw-typo-lg-semibold">{title}</StyledTitle>
      </div>
      <div className="sw-typo-default sw-mb-4">{content}</div>
    </StyledWrapper>
  );
}

const StyledWrapper = styled.div`
  background-color: ${themeColor('backgroundPromotedSection')};
  border: ${themeBorder('default')};
`;

const StyledTitle = styled.p`
  color: ${themeColor('primary')};
`;
