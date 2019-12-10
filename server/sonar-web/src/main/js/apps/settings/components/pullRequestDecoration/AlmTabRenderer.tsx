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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { AlmSettingsBinding, ALM_KEYS } from '../../../../types/alm-settings';
import AlmPRDecorationForm, { AlmPRDecorationFormChildrenProps } from './AlmPRDecorationForm';
import AlmPRDecorationTable from './AlmPRDecorationTable';

export interface AlmTabRendererProps<B> {
  additionalColumnsHeaders: string[];
  additionalColumnsKeys: Array<keyof B>;
  alm: ALM_KEYS;
  editedDefinition?: B;
  defaultBinding: B;
  definitions: B[];
  form: (props: AlmPRDecorationFormChildrenProps<B>) => React.ReactNode;
  loading: boolean;
  multipleAlmEnabled: boolean;
  onCancel: () => void;
  onCreate: () => void;
  onDelete: (definitionKey: string) => void;
  onEdit: (definitionKey: string) => void;
  onSubmit: (config: B, originalKey: string) => void;
  success: boolean;
}

export default function AlmTabRenderer<B extends AlmSettingsBinding>(
  props: AlmTabRendererProps<B>
) {
  const {
    additionalColumnsHeaders,
    additionalColumnsKeys,
    alm,
    defaultBinding,
    definitions,
    editedDefinition,
    form,
    loading,
    multipleAlmEnabled,
    success
  } = props;

  let definition: B | undefined;
  let mappedDefinitions: Array<{ key: string; additionalColumns: string[] }> = [];
  let showEdit: boolean | undefined;

  if (!multipleAlmEnabled) {
    definition = editedDefinition;
    if (definition === undefined && definitions.length > 0) {
      definition = definitions[0];
    }
    showEdit = definition && editedDefinition === undefined;
  } else {
    mappedDefinitions = definitions.map(({ key, ...properties }) => {
      const additionalColumns = additionalColumnsKeys.map(k => (properties as any)[k]);
      return {
        key,
        additionalColumns
      };
    });
  }

  const help = (
    <FormattedMessage
      defaultMessage={translate(`settings.pr_decoration.${alm}.info`)}
      id={`settings.pr_decoration.${alm}.info`}
      values={{
        link: (
          <Link target="_blank" to="/documentation/analysis/pr-decoration/">
            {translate('learn_more')}
          </Link>
        )
      }}
    />
  );

  return multipleAlmEnabled ? (
    <DeferredSpinner loading={loading}>
      <AlmPRDecorationTable
        additionalColumnsHeaders={additionalColumnsHeaders}
        alm={alm}
        definitions={mappedDefinitions}
        onCreate={props.onCreate}
        onDelete={props.onDelete}
        onEdit={props.onEdit}
      />

      {editedDefinition && (
        <AlmPRDecorationForm
          alm={alm}
          bindingDefinition={editedDefinition}
          help={help}
          onCancel={props.onCancel}
          onSubmit={props.onSubmit}
          showInModal={true}>
          {form}
        </AlmPRDecorationForm>
      )}
    </DeferredSpinner>
  ) : (
    <AlmPRDecorationForm
      alm={alm}
      bindingDefinition={definition || defaultBinding}
      help={help}
      hideKeyField={true}
      loading={loading}
      onCancel={props.onCancel}
      onDelete={definition ? props.onDelete : undefined}
      onEdit={showEdit ? props.onEdit : undefined}
      onSubmit={props.onSubmit}
      readOnly={showEdit}
      success={success}>
      {form}
    </AlmPRDecorationForm>
  );
}
