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
import { Link, ListItem } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { Plugin } from '../../../types/plugins';

export interface PluginOrganizationProps {
  plugin: Plugin;
}

export default function PluginOrganization({ plugin }: Readonly<PluginOrganizationProps>) {
  if (!plugin.organizationName) {
    return null;
  }
  return (
    <ListItem>
      <FormattedMessage
        id="marketplace.developed_by_x"
        values={{
          organization: plugin.organizationUrl ? (
            <Link to={plugin.organizationUrl}>{plugin.organizationName}</Link>
          ) : (
            <span>{plugin.organizationName}</span>
          ),
        }}
      />
    </ListItem>
  );
}
