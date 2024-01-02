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
import { Spinner, SubnavigationHeading, themeBorder } from 'design-system';
import * as React from 'react';
import { Paging } from '../../../types/types';
import IssuesCounter from '../components/IssuesCounter';

interface Props {
  loading: boolean;
  paging: Paging | undefined;
}

export default function SubnavigationIssuesListHeader(props: Props) {
  const { loading, paging } = props;

  return (
    <StyledHeader>
      <Spinner loading={loading}>{paging && <IssuesCounter total={paging.total} />}</Spinner>
    </StyledHeader>
  );
}

const StyledHeader = styled(SubnavigationHeading)`
  position: sticky;
  top: 0;
  border-bottom: ${themeBorder('default', 'filterbarBorder')};
`;
