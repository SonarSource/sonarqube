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

import { FlagMessage } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { Plugin } from '../../../types/plugins';
import PluginActions from './PluginActions';

interface Props {
  plugin: Plugin;
  refreshPending: () => void;
  status?: string;
}

export default function PluginStatus({ plugin, refreshPending, status }: Readonly<Props>) {
  switch (status) {
    case 'installing':
      return <FlagMessage variant="info">{translate('marketplace.install_pending')}</FlagMessage>;

    case 'updating':
      return <FlagMessage variant="info">{translate('marketplace.update_pending')}</FlagMessage>;

    case 'removing':
      return <FlagMessage variant="info">{translate('marketplace.uninstall_pending')}</FlagMessage>;

    default:
      return <PluginActions plugin={plugin} refreshPending={refreshPending} />;
  }
}
