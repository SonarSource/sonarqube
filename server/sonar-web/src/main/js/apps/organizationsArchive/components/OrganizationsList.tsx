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

import { ActionCell, ContentCell, TableRow, NumericalCell } from '~design-system';
import DateFormatter from '../../../components/intl/DateFormatter';
import { StickyTable } from '../../../app/components/admin/StickyTable';
import { translate } from '../../../helpers/l10n';
import Actions from './Actions';

interface Props {
  organizations: ArchivedOrganization[];
}

export default function OrganizationsList({ organizations }: Props) {
  const header = (
    <TableRow>
      <ContentCell>{translate('organization.name')}</ContentCell>
      <ContentCell>{translate('organization.archived.date')}</ContentCell>
      <NumericalCell>{translate('organization.archived.total.users')}</NumericalCell>
      <NumericalCell>{translate('organization.archived.total.projects')}</NumericalCell>
      <NumericalCell>{translate('organization.archived.auto_delete')}</NumericalCell>
      <ActionCell>{translate('actions')}</ActionCell>
    </TableRow>
  );

  return (
    <StickyTable columnCount={6} header={header} id="organizations-list">
      {organizations !== undefined && organizations.map((organization) => (
        <TableRow key={organization.kee}>
          <ContentCell>{organization.name}</ContentCell>
          <ContentCell>
            <DateFormatter date={organization.archivedAt} long />
          </ContentCell>
          <NumericalCell>{organization.totalUsers}</NumericalCell>
          <NumericalCell>{organization.totalProjects}</NumericalCell>
          <NumericalCell>{organization.expiresIn}</NumericalCell>
          <ActionCell>
            <Actions organization={organization} />
          </ActionCell>
        </TableRow>
      ))}
    </StickyTable>
  );
}

