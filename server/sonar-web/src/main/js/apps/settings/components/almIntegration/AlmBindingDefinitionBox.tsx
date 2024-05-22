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
import {
  BasicSeparator,
  ButtonSecondary,
  DangerButtonSecondary,
  FlagErrorIcon,
  FlagMessage,
  FlagSuccessIcon,
  HelperHintIcon,
  Spinner,
} from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import DocumentationLink from '../../../../components/common/DocumentationLink';
import Tooltip from '../../../../components/controls/Tooltip';
import { ALM_DOCUMENTATION_PATHS, IMPORT_COMPATIBLE_ALMS } from '../../../../helpers/constants';
import { getEdition, getEditionUrl } from '../../../../helpers/editions';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import {
  AlmBindingDefinitionBase,
  AlmKeys,
  AlmSettingsBindingStatus,
  AlmSettingsBindingStatusType,
} from '../../../../types/alm-settings';
import { EditionKey } from '../../../../types/editions';

export interface AlmBindingDefinitionBoxProps {
  alm: AlmKeys;
  branchesEnabled: boolean;
  definition: AlmBindingDefinitionBase;
  onCheck: (definitionKey: string) => void;
  onDelete: (definitionKey: string) => void;
  onEdit: (definitionKey: string) => void;
  status?: AlmSettingsBindingStatus;
}

const DEFAULT_STATUS: AlmSettingsBindingStatus = {
  alertSuccess: false,
  failureMessage: '',
  type: AlmSettingsBindingStatusType.Validating,
};

const STATUS_ICON = {
  [AlmSettingsBindingStatusType.Failure]: <FlagErrorIcon className="sw-ml-1" />,
  [AlmSettingsBindingStatusType.Success]: <FlagSuccessIcon className="sw-ml-1" />,
  [AlmSettingsBindingStatusType.Validating]: <div className="sw-ml-1 sw-inline-block sw-w-4" />,
};

function getPRDecorationFeatureStatus(branchesEnabled: boolean, type: keyof typeof STATUS_ICON) {
  if (branchesEnabled) {
    return STATUS_ICON[type];
  }

  return (
    <div className="sw-inline-flex sw-items-center">
      <strong className="sw-ml-2">
        {translate('settings.almintegration.feature.pr_decoration.disabled')}
      </strong>
      <HelpTooltip
        className="sw-ml-1"
        overlay={
          <FormattedMessage
            id="settings.almintegration.feature.pr_decoration.disabled.no_branches"
            defaultMessage={translate(
              'settings.almintegration.feature.pr_decoration.disabled.no_branches',
            )}
            values={{
              link: (
                <a
                  href={getEditionUrl(getEdition(EditionKey.developer), {
                    sourceEdition: EditionKey.community,
                  })}
                  rel="noopener noreferrer"
                  target="_blank"
                >
                  {translate(
                    'settings.almintegration.feature.pr_decoration.disabled.no_branches.link',
                  )}
                </a>
              ),
            }}
          />
        }
      >
        <HelperHintIcon />
      </HelpTooltip>
    </div>
  );
}

function getImportFeatureStatus(
  alm: AlmKeys,
  definition: AlmBindingDefinitionBase,
  type: keyof typeof STATUS_ICON,
) {
  if (definition.url !== undefined || alm === AlmKeys.BitbucketCloud) {
    return STATUS_ICON[type];
  }

  return (
    <div className="sw-inline-flex sw-items-center">
      <strong className="sw-ml-2">
        {translate('settings.almintegration.feature.alm_repo_import.disabled')}
      </strong>
      <HelpTooltip
        className="sw-ml-1"
        overlay={translate('settings.almintegration.feature.alm_repo_import.disabled.no_url')}
      />
    </div>
  );
}

function getPrDecoFeatureDescription(alm: AlmKeys) {
  switch (alm) {
    case AlmKeys.GitLab:
      return translate('settings.almintegration.feature.status_reporting.description_mr');
    case AlmKeys.GitHub:
      return translate(
        'settings.almintegration.feature.status_reporting.description_pr_and_commits',
      );
    default:
      return translate('settings.almintegration.feature.status_reporting.description_pr');
  }
}

export default function AlmBindingDefinitionBox(props: AlmBindingDefinitionBoxProps) {
  const { alm, branchesEnabled, definition, status = DEFAULT_STATUS } = props;

  return (
    <div className="it__alm-binding-definition sw-pb-10">
      <BasicSeparator className="sw-mb-6" />
      <div className="sw-float-right">
        <ButtonSecondary
          aria-label={translateWithParameters(
            'settings.almintegration.edit_configuration',
            definition.key,
          )}
          onClick={() => {
            props.onEdit(definition.key);
          }}
        >
          {translate('edit')}
        </ButtonSecondary>
        <DangerButtonSecondary
          aria-label={translateWithParameters(
            'settings.almintegration.delete_configuration',
            definition.key,
          )}
          className="sw-ml-2"
          onClick={() => {
            props.onDelete(definition.key);
          }}
        >
          {translate('delete')}
        </DangerButtonSecondary>
      </div>

      <div className="sw-mb-4">
        <h3>{definition.key}</h3>
        {definition.url && <span>{definition.url}</span>}
      </div>

      {status.type !== AlmSettingsBindingStatusType.Warning && (
        <div className="sw-flex sw-mb-3">
          <div className="sw-mr-10">
            <Tooltip content={getPrDecoFeatureDescription(alm)}>
              <span>{translate('settings.almintegration.feature.status_reporting.title')}</span>
            </Tooltip>
            {getPRDecorationFeatureStatus(branchesEnabled, status.type)}
          </div>
          {IMPORT_COMPATIBLE_ALMS.includes(alm) && (
            <div>
              <Tooltip
                content={translate('settings.almintegration.feature.alm_repo_import.description')}
              >
                <span>{translate('settings.almintegration.feature.alm_repo_import.title')}</span>
              </Tooltip>
              {getImportFeatureStatus(alm, definition, status.type)}
            </div>
          )}
        </div>
      )}

      {status.type === AlmSettingsBindingStatusType.Warning && (
        <div className="sw-mb-3">
          <FlagMessage variant="warning">
            {translate('settings.almintegration.could_not_validate')}
          </FlagMessage>
        </div>
      )}

      {status.type === AlmSettingsBindingStatusType.Failure && (
        <div className="sw-mb-3">
          <FlagMessage variant="error">{status.failureMessage}</FlagMessage>
        </div>
      )}

      {status.type === AlmSettingsBindingStatusType.Success && status.alertSuccess && (
        <>
          <div className="sw-mb-3">
            <FlagMessage variant="success">
              {translate('settings.almintegration.configuration_valid')}
            </FlagMessage>
          </div>
          {alm === AlmKeys.GitHub && (
            <div className="sw-mb-3">
              <FlagMessage variant="warning">
                <p>
                  <FormattedMessage
                    id="settings.almintegration.github.additional_permission"
                    defaultMessage={translate(
                      'settings.almintegration.github.additional_permission',
                    )}
                    values={{
                      link: (
                        <DocumentationLink to={ALM_DOCUMENTATION_PATHS[AlmKeys.GitHub]}>
                          {translate('learn_more')}
                        </DocumentationLink>
                      ),
                    }}
                  />
                </p>
              </FlagMessage>
            </div>
          )}
        </>
      )}

      <div className="sw-flex sw-items-center">
        <ButtonSecondary
          aria-label={translateWithParameters(
            'settings.almintegration.check_configuration_x',
            definition.key,
          )}
          onClick={() => props.onCheck(definition.key)}
        >
          {translate('settings.almintegration.check_configuration')}
        </ButtonSecondary>
        <Spinner
          ariaLabel={translate('settings.almintegration.checking_configuration')}
          className="sw-ml-3"
          loading={status.type === AlmSettingsBindingStatusType.Validating}
        />
        {status.type === AlmSettingsBindingStatusType.Validating && (
          <span className="sw-ml-2">
            {translate('settings.almintegration.checking_configuration')}
          </span>
        )}
      </div>
    </div>
  );
}
