/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { Button, ButtonIcon, DeleteButton } from 'sonar-ui-common/components/controls/buttons';
import EditIcon from 'sonar-ui-common/components/icons/EditIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { ALM_KEYS } from '../../../../types/alm-settings';

export interface AlmPRDecorationTableProps {
  additionalColumnsHeaders: Array<string>;
  alm: ALM_KEYS;
  definitions: Array<{
    key: string;
    additionalColumns: Array<string>;
  }>;
  onCreate: () => void;
  onDelete: (definitionKey: string) => void;
  onEdit: (definitionKey: string) => void;
}

export default function AlmPRDecorationTable(props: AlmPRDecorationTableProps) {
  const { additionalColumnsHeaders, alm, definitions } = props;

  return (
    <>
      <div className="spacer-top big-spacer-bottom display-flex-space-between">
        <h4 className="display-inline">
          {translate(
            'settings',
            alm === ALM_KEYS.GITLAB ? 'mr_decoration' : 'pr_decoration',
            'table.title'
          )}
        </h4>
        <Button data-test="settings__alm-create" onClick={props.onCreate}>
          {translate('settings.pr_decoration.table.create')}
        </Button>
      </div>

      <table className="data zebra fixed spacer-bottom">
        <thead>
          <tr>
            <th>{translate('settings.pr_decoration.table.column.name')}</th>
            {additionalColumnsHeaders.map(h => (
              <th key={h}>{h}</th>
            ))}
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
            <tr data-test="settings__alm-empty-table">
              <td colSpan={3 + additionalColumnsHeaders.length}>
                {translate('settings.pr_decoration.table.empty', alm)}
              </td>
            </tr>
          ) : (
            definitions.map(({ key, additionalColumns }) => (
              <tr data-test="settings__alm-table-row" key={key}>
                <td className="nowrap hide-overflow" title={key}>
                  {key}
                </td>
                {additionalColumns.map(value => (
                  <td className="nowrap hide-overflow" key={value} title={value}>
                    {value}
                  </td>
                ))}
                <td className="text-center" data-test="settings__alm-table-row-edit">
                  <ButtonIcon onClick={() => props.onEdit(key)}>
                    <EditIcon />
                  </ButtonIcon>
                </td>
                <td className="text-center" data-test="settings__alm-table-row-delete">
                  <DeleteButton onClick={() => props.onDelete(key)} />
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </>
  );
}
