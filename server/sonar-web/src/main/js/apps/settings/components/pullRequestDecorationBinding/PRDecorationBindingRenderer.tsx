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
import {
  BasicSeparator,
  ButtonPrimary,
  ButtonSecondary,
  FlagMessage,
  Link,
  Note,
  RequiredIcon,
  Spinner,
  SubHeading,
  SubTitle,
} from '~design-system';
import AlmSettingsInstanceSelector from '../../../../components/devops-platform/AlmSettingsInstanceSelector';
import MandatoryFieldsExplanation from '../../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../../helpers/l10n';
import { getGlobalSettingsUrl } from '../../../../helpers/urls';
import {
  AlmSettingsInstance,
  ProjectAlmBindingConfigurationErrorScope,
  ProjectAlmBindingConfigurationErrors,
  ProjectAlmBindingResponse,
} from '../../../../types/alm-settings';
import { ALM_INTEGRATION_CATEGORY } from '../../constants';
import AlmSpecificForm from './AlmSpecificForm';

export interface PRDecorationBindingRendererProps {
  checkingConfiguration: boolean;
  configurationErrors?: ProjectAlmBindingConfigurationErrors;
  formData: Omit<ProjectAlmBindingResponse, 'alm'>;
  instances: AlmSettingsInstance[];
  isChanged: boolean;
  isConfigured: boolean;
  isSysAdmin: boolean;
  isValid: boolean;
  loading: boolean;
  onCheckConfiguration: () => void;
  onFieldChange: (id: keyof ProjectAlmBindingResponse, value: string | boolean) => void;
  onReset: () => void;
  onSubmit: () => void;
  successfullyUpdated: boolean;
  updating: boolean;
}

export default function PRDecorationBindingRenderer(props: PRDecorationBindingRendererProps) {
  const {
    formData,
    instances,
    isChanged,
    isConfigured,
    isValid,
    loading,
    updating,
    successfullyUpdated,
    checkingConfiguration,
    configurationErrors,
    isSysAdmin,
  } = props;

  if (loading) {
    return <Spinner />;
  }

  if (instances.length < 1) {
    return (
      <div>
        <FlagMessage variant="info">
          {isSysAdmin ? (
            <p>
              <FormattedMessage
                defaultMessage={translate('settings.pr_decoration.binding.no_bindings.admin')}
                id="settings.pr_decoration.binding.no_bindings.admin"
                values={{
                  link: (
                    <Link to={getGlobalSettingsUrl(ALM_INTEGRATION_CATEGORY)}>
                      {translate('settings.pr_decoration.binding.no_bindings.link')}
                    </Link>
                  ),
                }}
              />
            </p>
          ) : (
            translate('settings.pr_decoration.binding.no_bindings')
          )}
        </FlagMessage>
      </div>
    );
  }

  const selected = formData.key ? instances.find((i) => i.key === formData.key) : undefined;

  return (
    <div className="sw-p-6">
      <SubTitle as="h3">{translate('settings.pr_decoration.binding.title')}</SubTitle>

      <Note className="markdown">{translate('settings.pr_decoration.binding.description')}</Note>

      <BasicSeparator className="sw-my-6" />

      <form
        onSubmit={(event: React.SyntheticEvent<HTMLFormElement>) => {
          event.preventDefault();
          props.onSubmit();
        }}
      >
        <MandatoryFieldsExplanation />

        <div className="sw-p-6 sw-flex sw-gap-12">
          <div className="sw-w-abs-300">
            <SubHeading>
              <label htmlFor="name">
                {translate('settings.pr_decoration.binding.form.name')}
                <RequiredIcon className="sw-mr-2" />
              </label>
            </SubHeading>
            <div className="markdown">
              {translate('settings.pr_decoration.binding.form.name.help')}
            </div>
          </div>
          <div className="sw-flex-1">
            <AlmSettingsInstanceSelector
              instances={instances}
              onChange={(instance: AlmSettingsInstance) => props.onFieldChange('key', instance.key)}
              initialValue={formData.key}
              className="sw-w-abs-400 it__configuration-name-select"
              inputId="name"
            />
          </div>
        </div>

        {selected?.alm && (
          <AlmSpecificForm
            alm={selected.alm}
            instances={instances}
            formData={formData}
            onFieldChange={props.onFieldChange}
          />
        )}

        <div className="sw-flex sw-items-center sw-mt-8 sw-gap-2">
          {isChanged && (
            <>
              <ButtonPrimary disabled={updating || !isValid} type="submit">
                <span data-test="project-settings__alm-save">{translate('save')}</span>
              </ButtonPrimary>
              <Spinner loading={updating} />
            </>
          )}
          {!updating && successfullyUpdated && (
            <FlagMessage variant="success">{translate('settings.state.saved')}</FlagMessage>
          )}
          {isConfigured && (
            <>
              <ButtonSecondary onClick={props.onReset}>
                <span data-test="project-settings__alm-reset">{translate('reset_verb')}</span>
              </ButtonSecondary>
              {!isChanged && (
                <>
                  <ButtonSecondary
                    onClick={props.onCheckConfiguration}
                    disabled={checkingConfiguration}
                  >
                    {translate('settings.pr_decoration.binding.check_configuration')}
                  </ButtonSecondary>
                  <Spinner loading={checkingConfiguration} />
                </>
              )}
            </>
          )}
        </div>
        {!checkingConfiguration && configurationErrors?.errors && (
          <FlagMessage variant="error" className="sw-mt-6">
            <div>
              <p className="sw-mb-2">
                {translate('settings.pr_decoration.binding.check_configuration.failure')}
              </p>
              <ul className="list-styled">
                {configurationErrors.errors.map((error, i) => (
                  // eslint-disable-next-line react/no-array-index-key
                  <li key={i}>{error.msg}</li>
                ))}
              </ul>
              {configurationErrors.scope === ProjectAlmBindingConfigurationErrorScope.Global && (
                <p>
                  {isSysAdmin ? (
                    <FormattedMessage
                      id="settings.pr_decoration.binding.check_configuration.failure.check_global_settings"
                      defaultMessage={translate(
                        'settings.pr_decoration.binding.check_configuration.failure.check_global_settings',
                      )}
                      values={{
                        link: (
                          <Link
                            to={getGlobalSettingsUrl(ALM_INTEGRATION_CATEGORY, {
                              alm: selected?.alm,
                            })}
                          >
                            {translate(
                              'settings.pr_decoration.binding.check_configuration.failure.check_global_settings.link',
                            )}
                          </Link>
                        ),
                      }}
                    />
                  ) : (
                    translate('settings.pr_decoration.binding.check_configuration.contact_admin')
                  )}
                </p>
              )}
            </div>
          </FlagMessage>
        )}
        {isConfigured && !isChanged && !checkingConfiguration && !configurationErrors && (
          <FlagMessage variant="success" className="sw-mt-6">
            {translate('settings.pr_decoration.binding.check_configuration.success')}
          </FlagMessage>
        )}
      </form>
    </div>
  );
}
