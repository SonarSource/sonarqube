/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import Link from '../../../../components/common/Link';
import { getTabId, getTabPanelId } from '../../../../components/controls/BoxedTabs';
import { Button } from '../../../../components/controls/buttons';
import { Alert } from '../../../../components/ui/Alert';
import DeferredSpinner from '../../../../components/ui/DeferredSpinner';
import { translate } from '../../../../helpers/l10n';
import {
  AlmBindingDefinition,
  AlmBindingDefinitionBase,
  AlmKeys,
  AlmSettingsBindingStatus,
  isBitbucketCloudBindingDefinition,
} from '../../../../types/alm-settings';
import { Dict } from '../../../../types/types';
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
}

const AUTHENTICATION_AVAILABLE_PLATFORMS = [
  AlmKeys.GitHub,
  AlmKeys.GitLab,
  AlmKeys.BitbucketServer,
];

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
  } = props;

  const preventCreation = loadingProjectCount || (!multipleAlmEnabled && definitions.length > 0);

  return (
    <div
      className="bordered"
      role="tabpanel"
      id={getTabPanelId(almTab)}
      aria-labelledby={getTabId(almTab)}
    >
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
                onClick={props.onCreate}
              >
                {translate('settings.almintegration.create')}
              </Button>
            </CreationTooltip>
          </div>
          {definitions.map((def) => (
            <AlmBindingDefinitionBox
              alm={isBitbucketCloudBindingDefinition(def) ? AlmKeys.BitbucketCloud : almTab}
              branchesEnabled={branchesEnabled}
              definition={def}
              key={def.key}
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
              onCancel={props.onCancel}
              afterSubmit={props.afterSubmit}
            />
          )}
        </DeferredSpinner>
      </div>
      {AUTHENTICATION_AVAILABLE_PLATFORMS.includes(almTab) && (
        <Alert variant="info" className="spacer">
          <FormattedMessage
            id="settings.almintegration.tabs.authentication-moved"
            defaultMessage={translate('settings.almintegration.tabs.authentication_moved')}
            values={{
              link: (
                <Link
                  to={{
                    pathname: '/admin/settings',
                    search: `category=authentication&tab=${almTab}`,
                  }}
                >
                  {translate('property.category.authentication')}
                </Link>
              ),
            }}
          />
        </Alert>
      )}
    </div>
  );
}
