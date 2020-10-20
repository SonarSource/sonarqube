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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import AlertErrorIcon from 'sonar-ui-common/components/icons/AlertErrorIcon';
import AlertSuccessIcon from 'sonar-ui-common/components/icons/AlertSuccessIcon';
import DeleteIcon from 'sonar-ui-common/components/icons/DeleteIcon';
import EditIcon from 'sonar-ui-common/components/icons/EditIcon';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { AlmBindingDefinition, AlmSettingsBindingStatus } from '../../../../types/alm-settings';

export interface AlmBindingDefinitionBoxProps {
  definition: AlmBindingDefinition;
  multipleDefinitions: boolean;
  onCheck: (definitionKey: string) => void;
  onDelete: (definitionKey: string) => void;
  onEdit: (definitionKey: string) => void;
  status?: AlmSettingsBindingStatus;
}

const DEFAULT_STATUS = { alert: false, errorMessage: '', validating: true };

function getImportFeatureStatus(multipleDefinitions: boolean, error: boolean) {
  if (multipleDefinitions) {
    return (
      <div className="display-inline-flex-center">
        <strong className="spacer-left">
          {translate('settings.almintegration.feature.alm_repo_import.disabled')}
        </strong>
        <HelpTooltip
          className="little-spacer-left"
          overlay={translate('settings.almintegration.feature.alm_repo_import.help')}
        />
      </div>
    );
  } else {
    return error ? (
      <AlertErrorIcon className="spacer-left" />
    ) : (
      <AlertSuccessIcon className="spacer-left" />
    );
  }
}

export default function AlmBindingDefinitionBox(props: AlmBindingDefinitionBoxProps) {
  const { definition, multipleDefinitions, status = DEFAULT_STATUS } = props;

  return (
    <div className="boxed-group-inner bordered spacer-top spacer-bottom it__alm-binding-definition">
      <div className="actions pull-right">
        <Button onClick={() => props.onEdit(definition.key)}>
          <EditIcon className="spacer-right" />
          {translate('edit')}
        </Button>
        <Button className="button-red spacer-left" onClick={() => props.onDelete(definition.key)}>
          <DeleteIcon className="spacer-right" />
          {translate('delete')}
        </Button>
      </div>

      <div className="big-spacer-bottom">
        <h3>{definition.key}</h3>
        {definition.url && <span>{definition.url}</span>}
      </div>

      <DeferredSpinner
        customSpinner={
          <div>
            <i className="deferred-spinner spacer-right" />
            {translate('settings.almintegration.checking_configuration')}
          </div>
        }
        loading={status.validating}>
        <div className="display-flex-row spacer-bottom">
          <div className="huge-spacer-right">
            {translate('settings.almintegration.feature.pr_decoration.title')}
            {status.errorMessage ? (
              <AlertErrorIcon className="spacer-left" />
            ) : (
              <AlertSuccessIcon className="spacer-left" />
            )}
          </div>
          <div>
            {translate('settings.almintegration.feature.alm_repo_import.title')}
            {getImportFeatureStatus(multipleDefinitions, Boolean(status.errorMessage))}
          </div>
        </div>

        {status.alert && (
          <div className="width-50">
            <Alert variant={status.errorMessage ? 'error' : 'success'}>
              {status.errorMessage || translate('settings.almintegration.configuration_valid')}
            </Alert>
          </div>
        )}

        <Button className="big-spacer-top" onClick={() => props.onCheck(definition.key)}>
          {translate('settings.almintegration.check_configuration')}
        </Button>
      </DeferredSpinner>
    </div>
  );
}
