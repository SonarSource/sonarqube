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
import { FlagMessage, InputField, Note, RequiredIcon, SubHeading, Switch } from '~design-system';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../../app/components/available-features/withAvailableFeatures';
import DocumentationLink from '../../../../components/common/DocumentationLink';
import { DocLink } from '../../../../helpers/doc-links';
import { translate } from '../../../../helpers/l10n';
import { convertGithubApiUrlToLink, stripTrailingSlash } from '../../../../helpers/urls';
import {
  AlmKeys,
  AlmSettingsInstance,
  ProjectAlmBindingResponse,
} from '../../../../types/alm-settings';
import { Feature } from '../../../../types/features';
import { Dict } from '../../../../types/types';

export interface AlmSpecificFormProps extends WithAvailableFeaturesProps {
  alm: AlmKeys;
  formData: Omit<ProjectAlmBindingResponse, 'alm'>;
  instances: AlmSettingsInstance[];
  onFieldChange: (id: keyof ProjectAlmBindingResponse, value: string | boolean) => void;
}

interface LabelProps {
  id: string;
  optional?: boolean;
}

interface CommonFieldProps extends LabelProps {
  help?: boolean;
  helpExample?: JSX.Element;
  helpParams?: Dict<string | JSX.Element>;
  onFieldChange: (id: keyof ProjectAlmBindingResponse, value: string | boolean) => void;
  propKey: keyof ProjectAlmBindingResponse;
}

function renderFieldWrapper(
  label: React.ReactNode,
  input: React.ReactNode,
  help?: React.ReactNode,
) {
  return (
    <div className="sw-p-6 sw-flex sw-gap-12">
      <div className="sw-w-abs-300">
        <SubHeading>{label}</SubHeading>
        {help && <div className="markdown">{help}</div>}
      </div>
      <div className="sw-flex-1">{input}</div>
    </div>
  );
}

function renderHelp({ help, helpExample, helpParams = {}, id }: CommonFieldProps) {
  return (
    help && (
      <>
        <FormattedMessage
          defaultMessage={translate('settings.pr_decoration.binding.form', id, 'help')}
          id={`settings.pr_decoration.binding.form.${id}.help`}
          values={helpParams}
        />
        {helpExample && (
          <div className="sw-mt-2 sw-whitespace-nowrap">
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
    <label htmlFor={id}>
      {translate('settings.pr_decoration.binding.form', id)}
      {!optional && <RequiredIcon />}
    </label>
  );
}

function renderBooleanField(
  props: Omit<CommonFieldProps, 'optional'> & {
    inputExtra?: React.ReactNode;
    value: boolean;
  },
) {
  const { id, value, onFieldChange, propKey, inputExtra } = props;

  return renderFieldWrapper(
    renderLabel({ ...props, optional: true }),
    <div className="sw-flex sw-items-start">
      <Switch name={id} onChange={(v) => onFieldChange(propKey, v)} value={value} />
      {value == null && <Note className="sw-ml-2">{translate('settings.not_set')}</Note>}
      {inputExtra}
    </div>,
    renderHelp(props),
  );
}

function renderField(
  props: CommonFieldProps & {
    value: string;
  },
) {
  const { id, propKey, value, onFieldChange } = props;
  return renderFieldWrapper(
    renderLabel(props),
    <InputField
      id={id}
      maxLength={256}
      name={id}
      onChange={(e) => onFieldChange(propKey, e.currentTarget.value)}
      size="large"
      type="text"
      value={value}
    />,
    renderHelp(props),
  );
}

export function AlmSpecificForm(props: AlmSpecificFormProps) {
  const {
    alm,
    instances,
    formData: { repository, slug, summaryCommentEnabled, monorepo },
  } = props;

  let formFields: JSX.Element;
  const instance = instances.find((i) => i.alm === alm);

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
            value: slug || '',
          })}

          {renderField({
            help: true,
            helpExample: <strong>My Repository</strong>,
            id: 'azure.repository',
            onFieldChange: props.onFieldChange,
            propKey: 'repository',
            value: repository || '',
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
            value: repository || '',
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
            value: slug || '',
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
            value: repository || '',
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
            value: repository || '',
          })}

          {renderBooleanField({
            help: true,
            id: 'github.summary_comment_setting',
            onFieldChange: props.onFieldChange,
            propKey: 'summaryCommentEnabled',
            value: summaryCommentEnabled === undefined ? true : summaryCommentEnabled,
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
            value: repository || '',
          })}
        </>
      );
      break;
  }

  const monorepoEnabled = props.hasFeature(Feature.MonoRepositoryPullRequestDecoration);

  return (
    <>
      {formFields}
      {monorepoEnabled &&
        renderBooleanField({
          help: true,
          helpParams: {
            doc_link: (
              <DocumentationLink to={DocLink.Monorepos}>
                {translate('learn_more')}
              </DocumentationLink>
            ),
          },
          id: 'monorepo',
          onFieldChange: props.onFieldChange,
          propKey: 'monorepo',
          value: monorepo,
          inputExtra: monorepo && (
            <FlagMessage className="sw-ml-2" variant="warning">
              {translate('settings.pr_decoration.binding.form.monorepo.warning')}
            </FlagMessage>
          ),
        })}
    </>
  );
}

export default withAvailableFeatures(AlmSpecificForm);
