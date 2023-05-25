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
import { DeferredSpinner, SubnavigationHeading, themeBorder } from 'design-system';
import * as React from 'react';
import { Paging } from '../../../types/types';
import IssuesCounter from '../components/IssuesCounter';

interface Props {
  loading: boolean;
  paging: Paging | undefined;
  selectedIndex: number | undefined;
}

export default function SubnavigationIssuesListHeader(props: Props) {
  const { loading, paging, selectedIndex } = props;

  return (
    <StyledHeader>
      <DeferredSpinner loading={loading}>
        {paging && <IssuesCounter current={selectedIndex} total={paging.total} />}
      </DeferredSpinner>
    </StyledHeader>
  );
}

const StyledHeader = styled(SubnavigationHeading)`
  position: sticky;
  top: 0;
  border-bottom: ${themeBorder('default', 'filterbarBorder')};
`;
