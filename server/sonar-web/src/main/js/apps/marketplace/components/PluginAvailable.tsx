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
import styled from '@emotion/styled';
import { Badge, ContentCell, UnorderedList } from 'design-system';
import * as React from 'react';
import { translateWithParameters } from '../../../helpers/l10n';
import { AvailablePlugin, InstalledPlugin } from '../../../types/plugins';
import PluginChangeLogButton from './PluginChangeLogButton';
import PluginDescription from './PluginDescription';
import PluginLicense from './PluginLicense';
import PluginOrganization from './PluginOrganization';
import PluginStatus from './PluginStatus';
import PluginUrls from './PluginUrls';

export interface PluginAvailableProps {
  installedPlugins: InstalledPlugin[];
  plugin: AvailablePlugin;
  readOnly: boolean;
  refreshPending: () => void;
  status?: string;
}

export default function PluginAvailable(props: Readonly<PluginAvailableProps>) {
  const { installedPlugins, plugin, readOnly, status } = props;
  const installedPluginKeys = installedPlugins.map(({ key }) => key);
  return (
    <>
      <PluginDescription plugin={plugin} />
      <ContentCell>
        <div className="sw-mr-2">
          <Badge variant="new">{plugin.release.version}</Badge>
        </div>
        <div className="sw-mr-2">{plugin.release.description}</div>
        <PluginChangeLogButton
          pluginName={plugin.name}
          release={plugin.release}
          update={plugin.update}
        />
        {plugin.update.requires.length > 0 && (
          <p className="sw-mt-2">
            <strong className="sw-body-sm-highlight">
              {translateWithParameters(
                'marketplace.installing_this_plugin_will_also_install_x',
                plugin.update.requires
                  .filter(({ key }) => !installedPluginKeys.includes(key))
                  .map((requiredPlugin) => requiredPlugin.name)
                  .join(', '),
              )}
            </strong>
          </p>
        )}
      </ContentCell>

      <ContentCell>
        <StyledUnorderedList>
          <PluginUrls plugin={plugin} />
          <PluginLicense license={plugin.license} />
          <PluginOrganization plugin={plugin} />
        </StyledUnorderedList>
      </ContentCell>

      {!readOnly && (
        <ContentCell>
          <PluginStatus plugin={plugin} refreshPending={props.refreshPending} status={status} />
        </ContentCell>
      )}
    </>
  );
}

const StyledUnorderedList = styled(UnorderedList)`
  margin-top: 0;

  & li:first {
    margin-top: 0;
  }
`;
