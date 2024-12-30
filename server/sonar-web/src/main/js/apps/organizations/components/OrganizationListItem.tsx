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
import { Badge } from '~design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { Organization } from '../../../types/types';
import OrganizationAvatar from './OrganizationAvatar';
import OrganizationLink from './OrganizationLink';

interface Props {
  organization: Organization;
  onClick?: () => void;
}

export default function OrganizationListItem({ organization, onClick }: Props) {
  const { actions = {} } = organization;
  return (
    <OrganizationLink
      className="sw-flex sw-gap-3 sw-items-center sw-mb-4"
      organization={organization}
      onClick={onClick}
    >
      <OrganizationAvatar organization={organization} small={true} />
      <span className="spacer-left">{organization.name}</span>
      {actions.admin && <Badge>{translate('admin')}</Badge>}
    </OrganizationLink>
  );
}
