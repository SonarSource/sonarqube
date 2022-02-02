/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { Button } from '../../../../components/controls/buttons';
import DeferredSpinner from '../../../../components/ui/DeferredSpinner';
import { translate } from '../../../../helpers/l10n';
import {
  AlmBindingDefinition,
  AlmBindingDefinitionBase,
  AlmKeys,
  AlmSettingsBindingStatus,
  isBitbucketCloudBindingDefinition
} from '../../../../types/alm-settings';
import { ExtendedSettingDefinition } from '../../../../types/settings';
import { Dict } from '../../../../types/types';
import { ALM_INTEGRATION } from '../AdditionalCategoryKeys';
import CategoryDefinitionsList from '../CategoryDefinitionsList';
import AlmBindingDefinitionBox from './AlmBindingDefinitionBox';
import AlmBindingDefinitionForm from './AlmBindingDefinitionForm';
import { AlmTabs } from './AlmIntegration';
import CreationTooltip from './CreationTooltip';

export interface AlmTabRendererProps {
  almTab: AlmTabs;
  branchesEnabled: boolean;
  definitionStatus: Dict<AlmSettingsBindingStatus>;
  editDefinition?: boolean;
  editedDefinition?: AlmBindingDefinition;
  definitions: AlmBindingDefinition[];
  loadingAlmDefinitions: boolean;
  loadingProjectCount: boolean;
  multipleAlmEnabled: boolean;
  onCancel: () => void;
  onCheck: (definitionKey: string) => void;
  onCreate: () => void;
  onDelete: (definitionKey: string) => void;
  onEdit: (definitionKey: string) => void;
  afterSubmit: (config: AlmBindingDefinitionBase) => void;
  settingsDefinitions: ExtendedSettingDefinition[];
}

export default function AlmTabRenderer(props: AlmTabRendererProps) {
  const {
    almTab,
    branchesEnabled,
    definitions,
    definitionStatus,
    editDefinition,
    editedDefinition,
    loadingAlmDefinitions,
    loadingProjectCount,
    multipleAlmEnabled,
    settingsDefinitions
  } = props;

  const preventCreation = loadingProjectCount || (!multipleAlmEnabled && definitions.length > 0);

  return (
    <div className="bordered">
      <div className="big-padded">
        <DeferredSpinner loading={loadingAlmDefinitions}>
          {definitions.length === 0 && (
            <p className="spacer-top">{translate('settings.almintegration.empty', almTab)}</p>
          )}

          <div className={definitions.length > 0 ? 'spacer-bottom text-right' : 'big-spacer-top'}>
            <CreationTooltip alm={almTab} preventCreation={preventCreation}>
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
              alm={isBitbucketCloudBindingDefinition(def) ? AlmKeys.BitbucketCloud : almTab}
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

          {editDefinition && (
            <AlmBindingDefinitionForm
              alm={almTab}
              bindingDefinition={editedDefinition}
              alreadyHaveInstanceConfigured={definitions.length > 0}
              onCancel={props.onCancel}
              afterSubmit={props.afterSubmit}
            />
          )}
        </DeferredSpinner>
      </div>

      <div className="huge-spacer-top huge-spacer-bottom bordered-top" />

      <div className="big-padded">
        <CategoryDefinitionsList
          category={ALM_INTEGRATION}
          definitions={settingsDefinitions}
          subCategory={almTab}
        />
      </div>
    </div>
  );
}
