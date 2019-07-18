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
import { translate } from 'sonar-ui-common/helpers/l10n';
import { Plugin } from '../../../api/plugins';
import PluginActions from './PluginActions';

interface Props {
  plugin: Plugin;
  refreshPending: () => void;
  status?: string;
}

export default function PluginStatus({ plugin, refreshPending, status }: Props) {
  return (
    <td className="text-top text-right width-20 little-spacer-left">
      {status === 'installing' && (
        <p className="text-success">{translate('marketplace.install_pending')}</p>
      )}

      {status === 'updating' && (
        <p className="text-success">{translate('marketplace.update_pending')}</p>
      )}

      {status === 'removing' && (
        <p className="text-danger">{translate('marketplace.uninstall_pending')}</p>
      )}

      {status == null && <PluginActions plugin={plugin} refreshPending={refreshPending} />}
    </td>
  );
}
