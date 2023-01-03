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
import { FormattedMessage } from 'react-intl';
import { Button } from 'sonar-ui-common/components/controls/buttons';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import AlertErrorIcon from 'sonar-ui-common/components/icons/AlertErrorIcon';
import AlertSuccessIcon from 'sonar-ui-common/components/icons/AlertSuccessIcon';
import DeleteIcon from 'sonar-ui-common/components/icons/DeleteIcon';
import EditIcon from 'sonar-ui-common/components/icons/EditIcon';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getEdition, getEditionUrl } from '../../../../helpers/editions';
import { IMPORT_COMPATIBLE_ALMS } from '../../../../helpers/constants';
import {
  AlmBindingDefinition,
  AlmKeys,
  AlmSettingsBindingStatus,
  AlmSettingsBindingStatusType
} from '../../../../types/alm-settings';
import { EditionKey } from '../../../../types/editions';

export interface AlmBindingDefinitionBoxProps {
  alm: AlmKeys;
  branchesEnabled: boolean;
  definition: AlmBindingDefinition;
  multipleDefinitions: boolean;
  onCheck: (definitionKey: string) => void;
  onDelete: (definitionKey: string) => void;
  onEdit: (definitionKey: string) => void;
  status?: AlmSettingsBindingStatus;
}

const DEFAULT_STATUS: AlmSettingsBindingStatus = {
  alertSuccess: false,
  failureMessage: '',
  type: AlmSettingsBindingStatusType.Validating
};

const STATUS_ICON = {
  [AlmSettingsBindingStatusType.Failure]: <AlertErrorIcon className="spacer-left" />,
  [AlmSettingsBindingStatusType.Success]: <AlertSuccessIcon className="spacer-left" />
};

function getPRDecorationFeatureStatus(
  branchesEnabled: boolean,
  type: AlmSettingsBindingStatusType.Success | AlmSettingsBindingStatusType.Failure
) {
  if (branchesEnabled) {
    return STATUS_ICON[type];
  }

  return (
    <div className="display-inline-flex-center">
      <strong className="spacer-left">
        {translate('settings.almintegration.feature.pr_decoration.disabled')}
      </strong>
      <HelpTooltip
        className="little-spacer-left"
        overlay={
          <FormattedMessage
            id="settings.almintegration.feature.pr_decoration.disabled.no_branches"
            defaultMessage={translate(
              'settings.almintegration.feature.pr_decoration.disabled.no_branches'
            )}
            values={{
              link: (
                <a
                  href={getEditionUrl(getEdition(EditionKey.developer), {
                    sourceEdition: EditionKey.community
                  })}
                  rel="noopener noreferrer"
                  target="_blank">
                  {translate(
                    'settings.almintegration.feature.pr_decoration.disabled.no_branches.link'
                  )}
                </a>
              )
            }}
          />
        }
      />
    </div>
  );
}

function getImportFeatureStatus(
  definition: AlmBindingDefinition,
  multipleDefinitions: boolean,
  type: AlmSettingsBindingStatusType.Success | AlmSettingsBindingStatusType.Failure
) {
  if (multipleDefinitions) {
    return (
      <div className="display-inline-flex-center">
        <strong className="spacer-left">
          {translate('settings.almintegration.feature.alm_repo_import.disabled')}
        </strong>
        <HelpTooltip
          className="little-spacer-left"
          overlay={translate('settings.almintegration.feature.alm_repo_import.disabled.multiple')}
        />
      </div>
    );
  }

  if (!definition.url) {
    return (
      <div className="display-inline-flex-center">
        <strong className="spacer-left">
          {translate('settings.almintegration.feature.alm_repo_import.disabled')}
        </strong>
        <HelpTooltip
          className="little-spacer-left"
          overlay={translate('settings.almintegration.feature.alm_repo_import.disabled.no_url')}
        />
      </div>
    );
  }

  return STATUS_ICON[type];
}

export default function AlmBindingDefinitionBox(props: AlmBindingDefinitionBoxProps) {
  const { alm, branchesEnabled, definition, multipleDefinitions, status = DEFAULT_STATUS } = props;

  const prDecoFeatureTitle =
    alm === AlmKeys.GitLab
      ? translate('settings.almintegration.feature.mr_decoration.title')
      : translate('settings.almintegration.feature.pr_decoration.title');

  const prDecoFeatureDescription =
    alm === AlmKeys.GitLab
      ? translate('settings.almintegration.feature.mr_decoration.description')
      : translate('settings.almintegration.feature.pr_decoration.description');

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

      {status.type === AlmSettingsBindingStatusType.Validating ? (
        <>
          <i className="spinner spacer-right" />
          {translate('settings.almintegration.checking_configuration')}
        </>
      ) : (
        <>
          {status.type !== AlmSettingsBindingStatusType.Warning && (
            <div className="display-flex-row spacer-bottom">
              <div className="huge-spacer-right">
                <Tooltip overlay={prDecoFeatureDescription}>
                  <span>{prDecoFeatureTitle}</span>
                </Tooltip>
                {getPRDecorationFeatureStatus(branchesEnabled, status.type)}
              </div>
              {IMPORT_COMPATIBLE_ALMS.includes(alm) && (
                <div>
                  <Tooltip
                    overlay={translate(
                      'settings.almintegration.feature.alm_repo_import.description'
                    )}>
                    <span>
                      {translate('settings.almintegration.feature.alm_repo_import.title')}
                    </span>
                  </Tooltip>
                  {getImportFeatureStatus(definition, multipleDefinitions, status.type)}
                </div>
              )}
            </div>
          )}

          <div className="width-50">
            {status.type === AlmSettingsBindingStatusType.Warning && (
              <Alert variant="warning">
                {translate('settings.almintegration.could_not_validate')}
              </Alert>
            )}

            {status.type === AlmSettingsBindingStatusType.Failure && (
              <Alert variant="error">{status.failureMessage}</Alert>
            )}

            {status.type === AlmSettingsBindingStatusType.Success && status.alertSuccess && (
              <Alert variant="success">
                {translate('settings.almintegration.configuration_valid')}
              </Alert>
            )}
          </div>

          <Button className="big-spacer-top" onClick={() => props.onCheck(definition.key)}>
            {translate('settings.almintegration.check_configuration')}
          </Button>
        </>
      )}
    </div>
  );
}
