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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import { Button, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import Select from 'sonar-ui-common/components/controls/Select';
import AlertSuccessIcon from 'sonar-ui-common/components/icons/AlertSuccessIcon';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import MandatoryFieldMarker from 'sonar-ui-common/components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from 'sonar-ui-common/components/ui/MandatoryFieldsExplanation';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { AlmSettingsInstance, ProjectAlmBindingResponse } from '../../../../types/alm-settings';
import AlmSpecificForm from './AlmSpecificForm';

export interface PRDecorationBindingRendererProps {
  formData: T.Omit<ProjectAlmBindingResponse, 'alm'>;
  instances: AlmSettingsInstance[];
  isChanged: boolean;
  isConfigured: boolean;
  isValid: boolean;
  loading: boolean;
  onFieldChange: (id: keyof ProjectAlmBindingResponse, value: string | boolean) => void;
  onReset: () => void;
  onSubmit: () => void;
  saving: boolean;
  success: boolean;
  monorepoEnabled: boolean;
}

function optionRenderer(instance: AlmSettingsInstance) {
  return instance.url ? (
    <>
      <span>{instance.key} — </span>
      <span className="text-muted">{instance.url}</span>
    </>
  ) : (
    <span>{instance.key}</span>
  );
}

export default function PRDecorationBindingRenderer(props: PRDecorationBindingRendererProps) {
  const {
    formData,
    instances,
    isChanged,
    isConfigured,
    isValid,
    loading,
    saving,
    success,
    monorepoEnabled
  } = props;

  if (loading) {
    return <DeferredSpinner />;
  }

  if (instances.length < 1) {
    return (
      <div>
        <Alert className="spacer-top huge-spacer-bottom" variant="info">
          <FormattedMessage
            defaultMessage={translate('settings.pr_decoration.binding.no_bindings')}
            id="settings.pr_decoration.binding.no_bindings"
            values={{
              link: (
                <Link to="/documentation/analysis/pull-request/#pr-decoration">
                  {translate('learn_more')}
                </Link>
              )
            }}
          />
        </Alert>
      </div>
    );
  }

  const selected = formData.key && instances.find(i => i.key === formData.key);
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
        }}>
        <MandatoryFieldsExplanation className="form-field" />

        <div className="form-field">
          <label htmlFor="name">
            {translate('settings.pr_decoration.binding.form.name')}
            <MandatoryFieldMarker className="spacer-right" />
          </label>
          <Select
            autosize={true}
            className="abs-width-400"
            clearable={false}
            id="name"
            menuContainerStyle={{
              maxWidth: '210%' /* Allow double the width of the select */,
              width: 'auto'
            }}
            onChange={(instance: AlmSettingsInstance) => props.onFieldChange('key', instance.key)}
            optionRenderer={optionRenderer}
            options={instances}
            searchable={false}
            value={formData.key}
            valueKey="key"
            valueRenderer={optionRenderer}
          />
        </div>

        {alm && (
          <AlmSpecificForm
            alm={alm}
            formData={formData}
            onFieldChange={props.onFieldChange}
            monorepoEnabled={monorepoEnabled}
          />
        )}

        <div className="display-flex-center">
          <DeferredSpinner className="spacer-right" loading={saving} />
          {isChanged && (
            <SubmitButton className="spacer-right button-success" disabled={saving || !isValid}>
              <span data-test="project-settings__alm-save">{translate('save')}</span>
            </SubmitButton>
          )}
          {isConfigured && (
            <Button className="spacer-right" onClick={props.onReset}>
              <span data-test="project-settings__alm-reset">{translate('reset_verb')}</span>
            </Button>
          )}
          {!saving && success && (
            <span className="text-success">
              <AlertSuccessIcon className="spacer-right" />
              {translate('settings.state.saved')}
            </span>
          )}
        </div>
      </form>
    </div>
  );
}
