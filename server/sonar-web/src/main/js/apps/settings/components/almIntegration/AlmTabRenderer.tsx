/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  AlmBindingDefinition,
  AlmKeys,
  AlmSettingsBindingStatus,
  isBitbucketCloudBindingDefinition
} from '../../../../types/alm-settings';
import AlmBindingDefinitionBox from './AlmBindingDefinitionBox';
import AlmBindingDefinitionForm, {
  AlmBindingDefinitionFormChildrenProps
} from './AlmBindingDefinitionForm';
import CreationTooltip from './CreationTooltip';

export interface AlmTabRendererProps<B> {
  alm: AlmKeys;
  branchesEnabled: boolean;
  definitionStatus: T.Dict<AlmSettingsBindingStatus>;
  editedDefinition?: B;
  definitions: B[];
  form: (props: AlmBindingDefinitionFormChildrenProps<B>) => React.ReactNode;
  help: React.ReactNode;
  loadingAlmDefinitions: boolean;
  loadingProjectCount: boolean;
  multipleAlmEnabled: boolean;
  onCancel: () => void;
  onCheck: (definitionKey: string) => void;
  onCreate: () => void;
  onDelete: (definitionKey: string) => void;
  onEdit: (definitionKey: string) => void;
  onSubmit: (config: B, originalKey: string) => void;
  optionalFields?: Array<keyof B>;
  submitting: boolean;
  success: boolean;
}

export default function AlmTabRenderer<B extends AlmBindingDefinition>(
  props: AlmTabRendererProps<B>
) {
  const {
    alm,
    branchesEnabled,
    definitions,
    definitionStatus,
    editedDefinition,
    form,
    loadingAlmDefinitions,
    loadingProjectCount,
    multipleAlmEnabled,
    optionalFields,
    help
  } = props;

  const preventCreation = loadingProjectCount || (!multipleAlmEnabled && definitions.length > 0);

  return (
    <div className="big-padded">
      <DeferredSpinner loading={loadingAlmDefinitions}>
        {definitions.length === 0 && (
          <p className="spacer-top">{translate('settings.almintegration.empty', alm)}</p>
        )}

        <div className={definitions.length > 0 ? 'spacer-bottom text-right' : 'big-spacer-top'}>
          <CreationTooltip alm={alm} preventCreation={preventCreation}>
            <Button
              data-test="settings__alm-create"
              disabled={preventCreation}
              onClick={props.onCreate}>
              {translate('settings.almintegration.create')}
            </Button>
          </CreationTooltip>
        </div>
        {definitions.map(def => (
          <AlmBindingDefinitionBox
            alm={isBitbucketCloudBindingDefinition(def) ? AlmKeys.BitbucketCloud : alm}
            branchesEnabled={branchesEnabled}
            definition={def}
            key={def.key}
            multipleDefinitions={definitions.length > 1}
            onCheck={props.onCheck}
            onDelete={props.onDelete}
            onEdit={props.onEdit}
            status={definitionStatus[def.key]}
          />
        ))}

        {editedDefinition && (
          <AlmBindingDefinitionForm
            bindingDefinition={editedDefinition}
            help={help}
            isSecondInstance={definitions.length === 1}
            onCancel={props.onCancel}
            onSubmit={props.onSubmit}
            optionalFields={optionalFields}>
            {form}
          </AlmBindingDefinitionForm>
        )}
      </DeferredSpinner>
    </div>
  );
}
