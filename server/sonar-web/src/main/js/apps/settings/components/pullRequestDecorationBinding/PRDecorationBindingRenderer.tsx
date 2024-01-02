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
import { Button, SubmitButton } from '../../../../components/controls/buttons';
import AlmSettingsInstanceSelector from '../../../../components/devops-platform/AlmSettingsInstanceSelector';
import AlertSuccessIcon from '../../../../components/icons/AlertSuccessIcon';
import { Alert } from '../../../../components/ui/Alert';
import DeferredSpinner from '../../../../components/ui/DeferredSpinner';
import MandatoryFieldMarker from '../../../../components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from '../../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../../helpers/l10n';
import { getGlobalSettingsUrl } from '../../../../helpers/urls';
import {
  AlmSettingsInstance,
  ProjectAlmBindingConfigurationErrors,
  ProjectAlmBindingConfigurationErrorScope,
  ProjectAlmBindingResponse,
} from '../../../../types/alm-settings';
import { ALM_INTEGRATION_CATEGORY } from '../../constants';
import AlmSpecificForm from './AlmSpecificForm';

export interface PRDecorationBindingRendererProps {
  formData: Omit<ProjectAlmBindingResponse, 'alm'>;
  instances: AlmSettingsInstance[];
  isChanged: boolean;
  isConfigured: boolean;
  isValid: boolean;
  loading: boolean;
  onFieldChange: (id: keyof ProjectAlmBindingResponse, value: string | boolean) => void;
  onReset: () => void;
  onSubmit: () => void;
  updating: boolean;
  successfullyUpdated: boolean;
  onCheckConfiguration: () => void;
  checkingConfiguration: boolean;
  configurationErrors?: ProjectAlmBindingConfigurationErrors;
  isSysAdmin: boolean;
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
    return <DeferredSpinner />;
  }

  if (instances.length < 1) {
    return (
      <div>
        <Alert className="spacer-top huge-spacer-bottom" variant="info">
          {isSysAdmin ? (
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
          ) : (
            translate('settings.pr_decoration.binding.no_bindings')
          )}
        </Alert>
      </div>
    );
  }

  const selected = formData.key && instances.find((i) => i.key === formData.key);
  const alm = selected && selected.alm;

  return (
    <div>
      <header className="page-header">
        <h1 className="page-title">{translate('settings.pr_decoration.binding.title')}</h1>
      </header>

      <div className="markdown small spacer-top big-spacer-bottom">
        {translate('settings.pr_decoration.binding.description')}
      </div>

      <form
        onSubmit={(event: React.SyntheticEvent<HTMLFormElement>) => {
          event.preventDefault();
          props.onSubmit();
        }}
      >
        <MandatoryFieldsExplanation className="form-field" />

        <div className="settings-definition big-spacer-bottom">
          <div className="settings-definition-left">
            <label className="h3" htmlFor="name">
              {translate('settings.pr_decoration.binding.form.name')}
              <MandatoryFieldMarker className="spacer-right" />
            </label>
            <div className="markdown small spacer-top">
              {translate('settings.pr_decoration.binding.form.name.help')}
            </div>
          </div>
          <div className="settings-definition-right">
            <AlmSettingsInstanceSelector
              instances={instances}
              onChange={(instance: AlmSettingsInstance) => props.onFieldChange('key', instance.key)}
              initialValue={formData.key}
              classNames="abs-width-400 big-spacer-top it__configuration-name-select"
              inputId="name"
            />
          </div>
        </div>

        {alm && (
          <AlmSpecificForm
            alm={alm}
            instances={instances}
            formData={formData}
            onFieldChange={props.onFieldChange}
          />
        )}

        <div className="display-flex-center big-spacer-top action-section">
          {isChanged && (
            <SubmitButton className="spacer-right button-success" disabled={updating || !isValid}>
              <span data-test="project-settings__alm-save">{translate('save')}</span>
              <DeferredSpinner className="spacer-left" loading={updating} />
            </SubmitButton>
          )}
          {!updating && successfullyUpdated && (
            <span className="text-success spacer-right">
              <AlertSuccessIcon className="spacer-right" />
              {translate('settings.state.saved')}
            </span>
          )}
          {isConfigured && (
            <>
              <Button className="spacer-right" onClick={props.onReset}>
                <span data-test="project-settings__alm-reset">{translate('reset_verb')}</span>
              </Button>
              {!isChanged && (
                <Button onClick={props.onCheckConfiguration} disabled={checkingConfiguration}>
                  {translate('settings.pr_decoration.binding.check_configuration')}
                  <DeferredSpinner className="spacer-left" loading={checkingConfiguration} />
                </Button>
              )}
            </>
          )}
        </div>
        {!checkingConfiguration && configurationErrors?.errors && (
          <Alert variant="error" display="inline" className="big-spacer-top">
            <p className="spacer-bottom">
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
                      'settings.pr_decoration.binding.check_configuration.failure.check_global_settings'
                    )}
                    values={{
                      link: (
                        <Link to={getGlobalSettingsUrl(ALM_INTEGRATION_CATEGORY, { alm })}>
                          {translate(
                            'settings.pr_decoration.binding.check_configuration.failure.check_global_settings.link'
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
          </Alert>
        )}
        {isConfigured && !isChanged && !checkingConfiguration && !configurationErrors && (
          <Alert variant="success" display="inline" className="big-spacer-top">
            {translate('settings.pr_decoration.binding.check_configuration.success')}
          </Alert>
        )}
      </form>
    </div>
  );
}
