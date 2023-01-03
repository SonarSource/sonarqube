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
import * as React from 'react';
import { Link } from 'react-router';
import ChevronsIcon from 'sonar-ui-common/components/icons/ChevronsIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';

export interface ProjectCreationMenuItemProps {
  alm: string;
}

export default function ProjectCreationMenuItem(props: ProjectCreationMenuItemProps) {
  const { alm } = props;
  return (
    <Link
      className="display-flex-center"
      to={{ pathname: '/projects/create', query: { mode: alm } }}>
      {alm === 'manual' ? (
        <ChevronsIcon className="spacer-right" />
      ) : (
        <img
          alt={alm}
          className="spacer-right"
          width={16}
          src={`${getBaseUrl()}/images/alm/${alm}.svg`}
        />
      )}
      {translate('my_account.add_project', alm)}
    </Link>
  );
}
