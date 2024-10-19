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
import { LightLabel, SubnavigationHeading, themeBorder } from 'design-system';
import * as React from 'react';
import { collapsePath } from '../../../helpers/path';

const COLLAPSE_PATH_LIMIT = 8;

interface Props {
  path: string;
}

export default function SubnavigationIssueComponentName({ path }: Props) {
  return (
    <StyledHeading className="sw-pb-1 sw-pt-6 sw-flex sw-truncate" title={path}>
      <LightLabel className="sw-truncate">{collapsePath(path, COLLAPSE_PATH_LIMIT)}</LightLabel>
    </StyledHeading>
  );
}

const StyledHeading = styled(SubnavigationHeading)`
  &:not(:last-child) {
    border-bottom: ${themeBorder('default')};
  }
`;
