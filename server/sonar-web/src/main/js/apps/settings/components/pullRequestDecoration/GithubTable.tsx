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
import { ButtonIcon, DeleteButton } from 'sonar-ui-common/components/controls/buttons';
import EditIcon from 'sonar-ui-common/components/icons/EditIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { GithubBindingDefinition } from '../../../../types/alm-settings';

export interface GithubTableProps {
  definitions: GithubBindingDefinition[];
  onDelete: (definitionKey: string) => void;
  onEdit: (config: GithubBindingDefinition) => void;
}

export default function GithubTable(props: GithubTableProps) {
  const { definitions } = props;

  return (
    <table className="data zebra fixed spacer-bottom">
      <thead>
        <tr>
          <th>{translate('settings.pr_decoration.table.column.name')}</th>
          <th>{translate(`settings.pr_decoration.table.column.github.url`)}</th>
          <th>{translate('settings.pr_decoration.table.column.app_id')}</th>
          <th className="action-small text-center">
            {translate('settings.pr_decoration.table.column.edit')}
          </th>
          <th className="action text-center">
            {translate('settings.pr_decoration.table.column.delete')}
          </th>
        </tr>
      </thead>
      <tbody>
        {definitions.length === 0 ? (
          <tr>
            <td colSpan={5}>{translate('settings.pr_decoration.table.empty.github')}</td>
          </tr>
        ) : (
          definitions.map(definition => (
            <tr key={definition.key}>
              <td className="nowrap hide-overflow" title={definition.key}>
                {definition.key}
              </td>
              <td className="nowrap hide-overflow" title={definition.url}>
                {definition.url}
              </td>
              <td className="nowrap hide-overflow" title={definition.appId}>
                {definition.appId}
              </td>
              <td className="text-center">
                <ButtonIcon onClick={() => props.onEdit(definition)}>
                  <EditIcon />
                </ButtonIcon>
              </td>
              <td className="text-center">
                <DeleteButton onClick={() => props.onDelete(definition.key)} />
              </td>
            </tr>
          ))
        )}
      </tbody>
    </table>
  );
}
