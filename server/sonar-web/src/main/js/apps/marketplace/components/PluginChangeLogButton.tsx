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
import { Button } from '@sonarsource/echoes-react';
import { DropdownToggler } from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Release, Update } from '../../../types/plugins';
import PluginChangeLog from './PluginChangeLog';

interface Props {
  pluginName: string;
  release: Release;
  update: Update;
}

export default function PluginChangeLogButton({ pluginName, release, update }: Readonly<Props>) {
  const [open, setOpen] = React.useState(false);

  return (
    <DropdownToggler
      allowResizing
      onRequestClose={() => setOpen(false)}
      open={open}
      id={`plugin-changelog-${pluginName}`}
      overlay={<PluginChangeLog release={release} update={update} />}
    >
      <Button
        aria-label={translateWithParameters(
          'marketplace.show_plugin_changelog',
          pluginName,
          release.version,
        )}
        onClick={() => setOpen((open) => !open)}
      >
        {translate('see_changelog')}
      </Button>
    </DropdownToggler>
  );
}
