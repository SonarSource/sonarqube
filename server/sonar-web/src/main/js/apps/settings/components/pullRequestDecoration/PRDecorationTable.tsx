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
import { ButtonIcon } from 'sonar-ui-common/components/controls/buttons';
import DeleteIcon from 'sonar-ui-common/components/icons/DeleteIcon';
import EditIcon from 'sonar-ui-common/components/icons/EditIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { ALM_KEYS } from '../../utils';

export interface PRDecorationTableProps {
  definitions: T.GithubDefinition[];
  alm: ALM_KEYS;
  onDelete: (config: T.GithubDefinition) => void;
  onEdit: (config: T.GithubDefinition) => void;
}

export default function PRDecorationTable(props: PRDecorationTableProps) {
  const { definitions, alm } = props;

  return (
    <>
      <table className="data zebra spacer-bottom">
        <thead>
          <tr>
            <th>{translate('settings.pr_decoration.table.column.name')}</th>
            <th>{translate(`settings.pr_decoration.table.column.${alm}.url`)}</th>
            <th>{translate('settings.pr_decoration.table.column.app_id')}</th>
            <th className="thin">{translate('settings.pr_decoration.table.column.edit')}</th>
            <th className="thin">{translate('settings.pr_decoration.table.column.delete')}</th>
          </tr>
        </thead>
        <tbody>
          {definitions.map(definition => (
            <tr key={definition.key}>
              <td>{definition.key}</td>
              <td>{definition.url}</td>
              <td>{definition.appId}</td>
              <td>
                <ButtonIcon onClick={() => props.onEdit(definition)}>
                  <EditIcon />
                </ButtonIcon>
              </td>
              <td>
                <ButtonIcon onClick={() => props.onDelete(definition)}>
                  <DeleteIcon />
                </ButtonIcon>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}
