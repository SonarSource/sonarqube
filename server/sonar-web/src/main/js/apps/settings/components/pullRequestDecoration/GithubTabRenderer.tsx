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
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { ALM_KEYS, GithubBindingDefinition } from '../../../../types/alm-settings';
import AlmPRDecorationFormModal from './AlmPRDecorationFormModal';
import AlmPRDecorationTable from './AlmPRDecorationTable';
import GithubFormModal from './GithubFormModal';
import TabHeader from './TabHeader';

export interface GithubTabRendererProps {
  editedDefinition?: GithubBindingDefinition;
  definitions: GithubBindingDefinition[];
  loading: boolean;
  onCancel: () => void;
  onCreate: () => void;
  onDelete: (definitionKey: string) => void;
  onEdit: (definitionKey: string) => void;
  onSubmit: (config: GithubBindingDefinition, originalKey: string) => void;
}

export default function GithubTabRenderer(props: GithubTabRendererProps) {
  const { definitions, editedDefinition, loading } = props;
  return (
    <>
      <TabHeader alm={ALM_KEYS.GITHUB} onCreate={props.onCreate} />

      <DeferredSpinner loading={loading}>
        <AlmPRDecorationTable
          additionalColumnsHeaders={[
            translate(`settings.pr_decoration.table.column.github.url`),
            translate('settings.pr_decoration.table.column.app_id')
          ]}
          alm={ALM_KEYS.GITHUB}
          definitions={definitions.map(({ key, appId, url }) => ({
            key,
            additionalColumns: [url, appId]
          }))}
          onDelete={props.onDelete}
          onEdit={props.onEdit}
        />
      </DeferredSpinner>

      {editedDefinition && (
        <AlmPRDecorationFormModal
          bindingDefinition={editedDefinition}
          onCancel={props.onCancel}
          onSubmit={props.onSubmit}>
          {childProps => <GithubFormModal {...childProps} />}
        </AlmPRDecorationFormModal>
      )}
    </>
  );
}
