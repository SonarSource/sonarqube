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
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import MandatoryFieldMarker from 'sonar-ui-common/components/ui/MandatoryFieldMarker';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { ALM_DOCUMENTATION_PATHS } from '../../../../helpers/constants';
import { AlmKeys, ProjectAlmBindingResponse } from '../../../../types/alm-settings';
import InputForBoolean from '../inputs/InputForBoolean';

export interface AlmSpecificFormProps {
  alm: AlmKeys;
  formData: T.Omit<ProjectAlmBindingResponse, 'alm'>;
  onFieldChange: (id: keyof ProjectAlmBindingResponse, value: string | boolean) => void;
  monorepoEnabled: boolean;
}

interface LabelProps {
  help?: boolean;
  helpParams?: T.Dict<string | JSX.Element>;
  id: string;
  optional?: boolean;
}

interface CommonFieldProps extends LabelProps {
  onFieldChange: (id: keyof ProjectAlmBindingResponse, value: string | boolean) => void;
  propKey: keyof ProjectAlmBindingResponse;
}

function renderLabel(props: LabelProps) {
  const { help, helpParams, optional, id } = props;
  return (
    <label className="display-flex-center" htmlFor={id}>
      {translate('settings.pr_decoration.binding.form', id)}
      {!optional && <MandatoryFieldMarker />}
      {help && (
        <HelpTooltip
          className="spacer-left"
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
  );
}

function renderBooleanField(
  props: Omit<CommonFieldProps, 'optional'> & {
    value: boolean;
    inputExtra?: React.ReactNode;
  }
) {
  const { id, value, onFieldChange, propKey, inputExtra } = props;
  return (
    <div className="form-field">
      {renderLabel({ ...props, optional: true })}
      <div className="display-flex-center">
        <InputForBoolean
          isDefault={true}
          name={id}
          onChange={v => onFieldChange(propKey, v)}
          value={value}
        />
        {inputExtra}
      </div>
    </div>
  );
}

function renderField(
  props: CommonFieldProps & {
    value: string;
  }
) {
  const { id, propKey, value, onFieldChange } = props;
  return (
    <div className="form-field">
      {renderLabel(props)}
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

export default function AlmSpecificForm(props: AlmSpecificFormProps) {
  const {
    alm,
    formData: { repository, slug, summaryCommentEnabled, monorepo },
    monorepoEnabled
  } = props;

  let formFields: JSX.Element;

  switch (alm) {
    case AlmKeys.Azure:
      formFields = (
        <>
          {renderField({
            help: true,
            id: 'azure.project',
            onFieldChange: props.onFieldChange,
            propKey: 'slug',
            value: slug || ''
          })}
          {renderField({
            help: true,
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
            helpParams: {
              example: (
                <>
                  {'.../projects/'}
                  <strong>{'{KEY}'}</strong>
                  {'/repos/{SLUG}/browse'}
                </>
              )
            },
            id: 'bitbucket.repository',
            onFieldChange: props.onFieldChange,
            propKey: 'repository',
            value: repository || ''
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
            helpParams: {
              example: (
                <>
                  {'https://bitbucket.org/{workspace}/'}
                  <strong>{'{repository}'}</strong>
                </>
              )
            },
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
            helpParams: { example: 'SonarSource/sonarqube' },
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
            id: 'gitlab.repository',
            onFieldChange: props.onFieldChange,
            propKey: 'repository',
            value: repository || ''
          })}
        </>
      );
      break;
  }

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
