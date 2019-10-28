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
import { ALM_KEYS, AzureBindingDefinition } from '../../../../types/alm-settings';
import AlmPRDecorationFormModal from './AlmPRDecorationFormModal';
import AlmPRDecorationTable from './AlmPRDecorationTable';
import AzureFormModal from './AzureFormModal';
import TabHeader from './TabHeader';

export interface AzureTabRendererProps {
  editedDefinition?: AzureBindingDefinition;
  definitions: AzureBindingDefinition[];
  loading: boolean;
  onCancel: () => void;
  onCreate: () => void;
  onDelete: (definitionKey: string) => void;
  onEdit: (definitionKey: string) => void;
  onSubmit: (config: AzureBindingDefinition, originalKey: string) => void;
}

export default function AzureTabRenderer(props: AzureTabRendererProps) {
  const { definitions, editedDefinition, loading } = props;
  return (
    <>
      <TabHeader alm={ALM_KEYS.AZURE} onCreate={props.onCreate} />

      <DeferredSpinner loading={loading}>
        <AlmPRDecorationTable
          additionalColumnsHeaders={[]}
          alm={ALM_KEYS.AZURE}
          definitions={definitions.map(({ key }) => ({
            key,
            additionalColumns: []
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
          {childProps => <AzureFormModal {...childProps} />}
        </AlmPRDecorationFormModal>
      )}
    </>
  );
}
