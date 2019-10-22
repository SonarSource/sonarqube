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
import { BitbucketBindingDefinition } from '../../../../types/alm-settings';

export interface BitbucketTableProps {
  definitions: BitbucketBindingDefinition[];
  onDelete: (definitionKey: string) => void;
  onEdit: (config: BitbucketBindingDefinition) => void;
}

export default function BitbucketTable(props: BitbucketTableProps) {
  const { definitions } = props;

  return (
    <table className="data zebra spacer-bottom">
      <thead>
        <tr>
          <th>{translate('settings.pr_decoration.table.column.name')}</th>
          <th>{translate(`settings.pr_decoration.table.column.bitbucket.url`)}</th>
          <th className="thin">{translate('settings.pr_decoration.table.column.edit')}</th>
          <th className="thin">{translate('settings.pr_decoration.table.column.delete')}</th>
        </tr>
      </thead>
      <tbody>
        {definitions.length < 1 ? (
          <tr>
            <td colSpan={4}>{translate('settings.pr_decoration.table.empty.bitbucket')}</td>
          </tr>
        ) : (
          definitions.map(definition => (
            <tr key={definition.key}>
              <td>{definition.key}</td>
              <td>{definition.url}</td>
              <td>
                <ButtonIcon onClick={() => props.onEdit(definition)}>
                  <EditIcon />
                </ButtonIcon>
              </td>
              <td>
                <DeleteButton onClick={() => props.onDelete(definition.key)} />
              </td>
            </tr>
          ))
        )}
      </tbody>
    </table>
  );
}
