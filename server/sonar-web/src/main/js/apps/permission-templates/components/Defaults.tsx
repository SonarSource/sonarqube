/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import { sortBy } from 'lodash';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  organization: T.Organization | undefined;
  template: T.PermissionTemplate;
}

export default function Defaults({ organization, template }: Props) {
  const qualifiersToDisplay =
    organization && !organization.isDefault ? ['TRK'] : template.defaultFor;

  const qualifiers = sortBy(qualifiersToDisplay)
    .map(qualifier => translate('qualifiers', qualifier))
    .join(', ');

  return (
    <div>
      <span className="badge spacer-right">
        {translateWithParameters('permission_template.default_for', qualifiers)}
      </span>
    </div>
  );
}
