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
import withAppStateContext from '../../../../app/components/app-state/withAppStateContext';
import Toggle from '../../../../components/controls/Toggle';
import { Alert } from '../../../../components/ui/Alert';
import MandatoryFieldMarker from '../../../../components/ui/MandatoryFieldMarker';
import { ALM_DOCUMENTATION_PATHS } from '../../../../helpers/constants';
import { translate } from '../../../../helpers/l10n';
import { convertGithubApiUrlToLink, stripTrailingSlash } from '../../../../helpers/urls';
import {
  AlmKeys,
  AlmSettingsInstance,
  ProjectAlmBindingResponse
} from '../../../../types/alm-settings';
import { EditionKey } from '../../../../types/editions';
import { AppState, Dict } from '../../../../types/types';

export interface AlmSpecificFormProps {
  alm: AlmKeys;
  instances: AlmSettingsInstance[];
  formData: Omit<ProjectAlmBindingResponse, 'alm'>;
  onFieldChange: (id: keyof ProjectAlmBindingResponse, value: string | boolean) => void;
  appState: AppState;
}

interface LabelProps {
  id: string;
  optional?: boolean;
}

interface CommonFieldProps extends LabelProps {
  help?: boolean;
  helpParams?: Dict<string | JSX.Element>;
  helpExample?: JSX.Element;
  onFieldChange: (id: keyof ProjectAlmBindingResponse, value: string | boolean) => void;
  propKey: keyof ProjectAlmBindingResponse;
}

function renderFieldWrapper(
  label: React.ReactNode,
  input: React.ReactNode,
  help?: React.ReactNode
) {
  return (
    <div className="settings-definition">
      <div className="settings-definition-left">
        {label}
        {help && <div className="markdown small spacer-top">{help}</div>}
      </div>
      <div className="settings-definition-right padded-top">{input}</div>
    </div>
  );
}

function renderHelp({ help, helpExample, helpParams, id }: CommonFieldProps) {
  return (
    help && (
      <>
        <FormattedMessage
          defaultMessage={translate('settings.pr_decoration.binding.form', id, 'help')}
          id={`settings.pr_decoration.binding.form.${id}.help`}
          values={helpParams}
        />
        {helpExample && (
          <div className="spacer-top nowrap">
            {translate('example')}: <em>{helpExample}</em>
          </div>
        )}
      </>
    )
  );
}

function renderLabel(props: LabelProps) {
  const { optional, id } = props;
  return (
    <label className="h3" htmlFor={id}>
      {translate('settings.pr_decoration.binding.form', id)}
      {!optional && <MandatoryFieldMarker />}
    </label>
  );
}

function renderBooleanField(
  props: Omit<CommonFieldProps, 'optional'> & {
    value: boolean;
    inputExtra?: React.ReactNode;
  }
) {
  const { id, value, onFieldChange, propKey, inputExtra } = props;
  return renderFieldWrapper(
    renderLabel({ ...props, optional: true }),
    <div className="display-flex-center big-spacer-top">
      <div className="display-inline-block text-top">
        <Toggle name={id} onChange={v => onFieldChange(propKey, v)} value={value} />
        {value == null && <span className="spacer-left note">{translate('settings.not_set')}</span>}
      </div>
      {inputExtra}
    </div>,
    renderHelp(props)
  );
}

function renderField(
  props: CommonFieldProps & {
    value: string;
  }
) {
  const { id, propKey, value, onFieldChange } = props;
  return renderFieldWrapper(
    renderLabel(props),
    <input
      className="input-super-large big-spacer-top"
      id={id}
      maxLength={256}
      name={id}
      onChange={e => onFieldChange(propKey, e.currentTarget.value)}
      type="text"
      value={value}
    />,
    renderHelp(props)
  );
}

export function AlmSpecificForm(props: AlmSpecificFormProps) {
  const {
    alm,
    instances,
    formData: { repository, slug, summaryCommentEnabled, monorepo },
    appState
  } = props;

  let formFields: JSX.Element;
  const instance = instances.find(i => i.alm === alm);

  switch (alm) {
    case AlmKeys.Azure:
      formFields = (
        <>
          {renderField({
            help: true,
            helpExample: <strong>My Project</strong>,
            id: 'azure.project',
            onFieldChange: props.onFieldChange,
            propKey: 'slug',
            value: slug || ''
          })}
          {renderField({
            help: true,
            helpExample: <strong>My Repository</strong>,
            id: 'azure.repository',
            onFieldChange: props.onFieldChange,
            propKey: 'repository',
            value: repository || ''
          })}
        </>
      );
      break;
    case AlmKeys.BitbucketServer:
      formFields = (
        <>
          {renderField({
            help: true,
            helpExample: (
              <>
                {instance?.url
                  ? `${stripTrailingSlash(instance.url)}/projects/`
                  : 'https://bb.company.com/projects/'}
                <strong>{'MY_PROJECT_KEY'}</strong>
                {'/repos/my-repository-slug/browse'}
              </>
            ),
            id: 'bitbucket.repository',
            onFieldChange: props.onFieldChange,
            propKey: 'repository',
            value: repository || ''
          })}
          {renderField({
            help: true,
            helpExample: (
              <>
                {instance?.url
                  ? `${stripTrailingSlash(instance.url)}/projects/MY_PROJECT_KEY/repos/`
                  : 'https://bb.company.com/projects/MY_PROJECT_KEY/repos/'}
                <strong>{'my-repository-slug'}</strong>
                {'/browse'}
              </>
            ),
            id: 'bitbucket.slug',
            onFieldChange: props.onFieldChange,
            propKey: 'slug',
            value: slug || ''
          })}
        </>
      );
      break;
    case AlmKeys.BitbucketCloud:
      formFields = (
        <>
          {renderField({
            help: true,
            helpExample: (
              <>
                {'https://bitbucket.org/my-workspace/'}
                <strong>{'my-repository-slug'}</strong>
              </>
            ),
            id: 'bitbucketcloud.repository',
            onFieldChange: props.onFieldChange,
            propKey: 'repository',
            value: repository || ''
          })}
        </>
      );
      break;
    case AlmKeys.GitHub:
      formFields = (
        <>
          {renderField({
            help: true,
            helpExample: (
              <>
                {instance?.url
                  ? `${stripTrailingSlash(convertGithubApiUrlToLink(instance.url))}/`
                  : 'https://github.com/'}
                <strong>{'sonarsource/sonarqube'}</strong>
              </>
            ),
            id: 'github.repository',
            onFieldChange: props.onFieldChange,
            propKey: 'repository',
            value: repository || ''
          })}
          {renderBooleanField({
            help: true,
            id: 'github.summary_comment_setting',
            onFieldChange: props.onFieldChange,
            propKey: 'summaryCommentEnabled',
            value: summaryCommentEnabled === undefined ? true : summaryCommentEnabled
          })}
        </>
      );
      break;
    case AlmKeys.GitLab:
      formFields = (
        <>
          {renderField({
            help: true,
            helpExample: <strong>123456</strong>,
            id: 'gitlab.repository',
            onFieldChange: props.onFieldChange,
            propKey: 'repository',
            value: repository || ''
          })}
        </>
      );
      break;
  }

  // This feature trigger will be replaced when SONAR-14349 is implemented
  const monorepoEnabled = [EditionKey.enterprise, EditionKey.datacenter].includes(
    appState.edition as EditionKey
  );

  return (
    <>
      {formFields}
      {monorepoEnabled &&
        renderBooleanField({
          help: true,
          helpParams: {
            doc_link: (
              <Link to={ALM_DOCUMENTATION_PATHS[alm]} target="_blank">
                {translate('learn_more')}
              </Link>
            )
          },
          id: 'monorepo',
          onFieldChange: props.onFieldChange,
          propKey: 'monorepo',
          value: monorepo,
          inputExtra: monorepo && (
            <Alert className="no-margin-bottom spacer-left" variant="warning" display="inline">
              {translate('settings.pr_decoration.binding.form.monorepo.warning')}
            </Alert>
          )
        })}
    </>
  );
}

export default withAppStateContext(AlmSpecificForm);
