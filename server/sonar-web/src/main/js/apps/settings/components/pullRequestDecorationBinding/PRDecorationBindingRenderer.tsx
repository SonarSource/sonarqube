/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import Select from 'sonar-ui-common/components/controls/Select';
import AlertSuccessIcon from 'sonar-ui-common/components/icons/AlertSuccessIcon';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { ALM_KEYS } from '../../utils';

export interface PRDecorationBindingRendererProps {
  formData: T.ProjectAlmBinding;
  hasBinding: boolean;
  instances: T.AlmSettingsInstance[];
  isValid: boolean;
  loading: boolean;
  onFieldChange: (id: keyof T.ProjectAlmBinding, value: string) => void;
  onReset: () => void;
  onSubmit: () => void;
  saving: boolean;
  success: boolean;
}

function renderLabel(v: T.AlmSettingsInstance) {
  return v.url ? `${v.key} — ${v.url}` : v.key;
}

function renderField(props: {
  help?: boolean;
  helpParams?: { [key: string]: string | number | boolean | Date | JSX.Element | null | undefined };
  id: string;
  onFieldChange: (id: keyof T.ProjectAlmBinding, value: string) => void;
  propKey: keyof T.ProjectAlmBinding;
  value: string;
}) {
  const { help, helpParams, id, propKey, value, onFieldChange } = props;
  return (
    <div className="form-field">
      <label className="display-flex-center" htmlFor={id}>
        {translate('settings.pr_decoration.binding.form', id)}
        <em className="mandatory spacer-right">*</em>
        {help && (
          <HelpTooltip
            overlay={
              <FormattedMessage
                defaultMessage={translate('settings.pr_decoration.binding.form', id, 'help')}
                id={`settings.pr_decoration.binding.form.${id}.help`}
                values={helpParams}
              />
            }
            placement="right"
          />
        )}
      </label>
      <input
        className="input-super-large"
        id={id}
        maxLength={256}
        name={id}
        onChange={e => onFieldChange(propKey, e.currentTarget.value)}
        type="text"
        value={value}
      />
    </div>
  );
}

export default function PRDecorationBindingRenderer(props: PRDecorationBindingRendererProps) {
  const {
    formData: { key, repository, repositoryKey, repositorySlug },
    hasBinding,
    instances,
    isValid,
    loading,
    saving,
    success
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

  const selected = key && instances.find(i => i.key === key);
  const alm = selected && (selected.alm as ALM_KEYS);

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
        <div className="form-field">
          <label htmlFor="name">
            {translate('settings.pr_decoration.binding.form.name')}
            <em className="mandatory spacer-right">*</em>
          </label>
          <Select
            className="abs-width-400"
            clearable={false}
            id="name"
            onChange={({ value }: { value: string }) => props.onFieldChange('key', value)}
            options={instances.map(v => ({ value: v.key, label: renderLabel(v) }))}
            searchable={false}
            value={key}
          />
        </div>

        {alm === ALM_KEYS.BITBUCKET && (
          <>
            {renderField({
              help: true,
              helpParams: {
                example: (
                  <>
                    {'.../projects/'}
                    <strong>{'{KEY}'}</strong>
                    {'/repos/{SLUG}/browse'}
                  </>
                )
              },
              id: 'repository_key',
              onFieldChange: props.onFieldChange,
              propKey: 'repositoryKey',
              value: repositoryKey || ''
            })}
            {renderField({
              help: true,
              helpParams: {
                example: (
                  <>
                    {'.../projects/{KEY}/repos/'}
                    <strong>{'{SLUG}'}</strong>
                    {'/browse'}
                  </>
                )
              },
              id: 'repository_slug',
              onFieldChange: props.onFieldChange,
              propKey: 'repositorySlug',
              value: repositorySlug || ''
            })}
          </>
        )}

        {alm === ALM_KEYS.GITHUB &&
          renderField({
            help: true,
            helpParams: { example: 'SonarSource/sonarqube' },
            id: 'repository',
            onFieldChange: props.onFieldChange,
            propKey: 'repository',
            value: repository || ''
          })}

        <div className="display-flex-center">
          <DeferredSpinner className="spacer-right" loading={saving} />
          <SubmitButton className="spacer-right" disabled={saving || !isValid}>
            {translate('save')}
          </SubmitButton>
          {hasBinding && (
            <Button className="spacer-right" onClick={props.onReset}>
              {translate('reset_verb')}
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
