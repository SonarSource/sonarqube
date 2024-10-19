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
import { ContentCell, NumericalCell, TableRow } from 'design-system';
import { sortBy } from 'lodash';
import * as React from 'react';
import { StickyTable } from '../../../app/components/admin/StickyTable';
import { translate } from '../../../helpers/l10n';
import { Group, Provider } from '../../../types/types';
import ListItem from './ListItem';

interface Props {
  organization: string | undefined;
  groups: Group[];
  manageProvider: Provider | undefined;
}

function Header() {
  return (
    <TableRow>
      <ContentCell>{translate('user_groups.page.group_header')}</ContentCell>
      <NumericalCell>{translate('members')}</NumericalCell>
      <ContentCell>{translate('description')}</ContentCell>
      <NumericalCell>{translate('actions')}</NumericalCell>
    </TableRow>
  );
}

export default function List(props: Readonly<Props>) {
  const { organization, groups, manageProvider } = props;

  return (
    <StickyTable columnCount={4} header={<Header />} id="groups-list">
      {sortBy(groups, (group) => group.name.toLowerCase()).map((group) => (
        <ListItem organization={organization} group={group} key={group.name} manageProvider={manageProvider} />
      ))}
    </StickyTable>
  );
}
