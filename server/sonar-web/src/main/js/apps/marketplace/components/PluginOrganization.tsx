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
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import { Plugin } from '../../../api/plugins';

interface Props {
  plugin: Plugin;
}

export default function PluginOrganization({ plugin }: Props) {
  if (!plugin.organizationName) {
    return null;
  }
  return (
    <li className="little-spacer-bottom">
      <FormattedMessage
        defaultMessage={translate('marketplace.developed_by_x')}
        id="marketplace.developed_by_x"
        values={{
          organization: plugin.organizationUrl ? (
            <a className="js-plugin-organization" href={plugin.organizationUrl} target="_blank">
              {plugin.organizationName}
            </a>
          ) : (
            <span className="js-plugin-organization">{plugin.organizationName}</span>
          )
        }}
      />
    </li>
  );
}
