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
import { ALM_KEYS, BitbucketBindingDefinition } from '../../../../types/alm-settings';
import AlmPRDecorationFormModal from './AlmPRDecorationFormModal';
import AlmPRDecorationTable from './AlmPRDecorationTable';
import BitbucketFormModal from './BitbucketFormModal';
import TabHeader from './TabHeader';

export interface BitbucketTabRendererProps {
  editedDefinition?: BitbucketBindingDefinition;
  definitions: BitbucketBindingDefinition[];
  loading: boolean;
  onCancel: () => void;
  onCreate: () => void;
  onDelete: (definitionKey: string) => void;
  onEdit: (definitionKey: string) => void;
  onSubmit: (config: BitbucketBindingDefinition, originalKey: string) => void;
}

export default function BitbucketTabRenderer(props: BitbucketTabRendererProps) {
  const { definitions, editedDefinition, loading } = props;
  return (
    <>
      <TabHeader alm={ALM_KEYS.BITBUCKET} onCreate={props.onCreate} />

      <DeferredSpinner loading={loading}>
        <AlmPRDecorationTable
          additionalColumnsHeaders={[
            translate(`settings.pr_decoration.table.column.bitbucket.url`)
          ]}
          alm={ALM_KEYS.BITBUCKET}
          definitions={definitions.map(({ key, url }) => ({
            key,
            additionalColumns: [url]
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
          {childProps => <BitbucketFormModal {...childProps} />}
        </AlmPRDecorationFormModal>
      )}
    </>
  );
}
