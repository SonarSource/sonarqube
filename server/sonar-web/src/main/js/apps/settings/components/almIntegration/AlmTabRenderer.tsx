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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { AlmBindingDefinition, AlmKeys } from '../../../../types/alm-settings';
import AlmBindingDefinitionForm, {
  AlmBindingDefinitionFormChildrenProps
} from './AlmBindingDefinitionForm';
import AlmBindingDefinitionsTable from './AlmBindingDefinitionsTable';
import AlmIntegrationFeatureBox, {
  AlmIntegrationFeatureBoxProps
} from './AlmIntegrationFeatureBox';

export interface AlmTabRendererProps<B> {
  additionalColumnsHeaders: string[];
  additionalColumnsKeys: Array<keyof B>;
  additionalTableInfo?: React.ReactNode;
  alm: AlmKeys;
  editedDefinition?: B;
  defaultBinding: B;
  definitions: B[];
  features?: AlmIntegrationFeatureBoxProps[];
  form: (props: AlmBindingDefinitionFormChildrenProps<B>) => React.ReactNode;
  help?: React.ReactNode;
  loading: boolean;
  multipleAlmEnabled: boolean;
  onCancel: () => void;
  onCreate: () => void;
  onDelete: (definitionKey: string) => void;
  onEdit: (definitionKey: string) => void;
  onSubmit: (config: B, originalKey: string) => void;
  optionalFields?: Array<keyof B>;
  success: boolean;
}

export default function AlmTabRenderer<B extends AlmBindingDefinition>(
  props: AlmTabRendererProps<B>
) {
  const {
    additionalColumnsHeaders,
    additionalColumnsKeys,
    additionalTableInfo,
    alm,
    defaultBinding,
    definitions,
    editedDefinition,
    features = [],
    form,
    loading,
    multipleAlmEnabled,
    optionalFields,
    success,
    help = (
      <FormattedMessage
        defaultMessage={translate(`settings.almintegration.${alm}.info`)}
        id={`settings.almintegration.${alm}.info`}
        values={{
          link: (
            <Link target="_blank" to="/documentation/analysis/pr-decoration/">
              {translate('learn_more')}
            </Link>
          )
        }}
      />
    )
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

  return (
    <div className="big-padded">
      {multipleAlmEnabled ? (
        <DeferredSpinner loading={loading}>
          <AlmBindingDefinitionsTable
            additionalColumnsHeaders={additionalColumnsHeaders}
            additionalTableInfo={additionalTableInfo}
            alm={alm}
            definitions={mappedDefinitions}
            onCreate={props.onCreate}
            onDelete={props.onDelete}
            onEdit={props.onEdit}
          />

          {editedDefinition && (
            <AlmBindingDefinitionForm
              bindingDefinition={editedDefinition}
              help={help}
              onCancel={props.onCancel}
              onSubmit={props.onSubmit}
              optionalFields={optionalFields}
              showInModal={true}>
              {form}
            </AlmBindingDefinitionForm>
          )}
        </DeferredSpinner>
      ) : (
        <AlmBindingDefinitionForm
          bindingDefinition={definition || defaultBinding}
          help={help}
          hideKeyField={true}
          loading={loading}
          onCancel={props.onCancel}
          onDelete={definition ? props.onDelete : undefined}
          onEdit={showEdit ? props.onEdit : undefined}
          onSubmit={props.onSubmit}
          optionalFields={optionalFields}
          readOnly={showEdit}
          success={success}>
          {form}
        </AlmBindingDefinitionForm>
      )}

      {features.length > 0 && (
        <div className="big-spacer-top big-padded-top bordered-top">
          <h3 className="big-spacer-bottom">{translate('settings.almintegration.features')}</h3>

          <div className="display-flex-wrap">
            {features.map((feature, i) => (
              <AlmIntegrationFeatureBox key={i} {...feature} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
