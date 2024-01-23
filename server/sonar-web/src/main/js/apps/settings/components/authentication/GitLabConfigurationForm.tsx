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

import { ButtonPrimary, FlagMessage, Modal, Spinner } from 'design-system';
import { isArray, keyBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../../components/common/DocumentationLink';
import { translate } from '../../../../helpers/l10n';
import {
  useCreateGitLabConfigurationMutation,
  useUpdateGitLabConfigurationMutation,
} from '../../../../queries/identity-provider/gitlab';
import { GitLabConfigurationCreateBody, GitlabConfiguration } from '../../../../types/provisioning';
import { DefinitionV2, SettingType } from '../../../../types/settings';
import { DOCUMENTATION_LINK_SUFFIXES } from './Authentication';
import AuthenticationFormField from './AuthenticationFormField';

interface Props {
  data: GitlabConfiguration | null;
  onClose: () => void;
}

interface ErrorValue {
  key: string;
  message: string;
}

interface FormData {
  value: string | boolean | string[];
  required: boolean;
  definition: DefinitionV2;
}

const DEFAULT_URL = 'https://gitlab.com';

export default function GitLabConfigurationForm(props: Readonly<Props>) {
  const { data } = props;
  const isCreate = data === null;
  const [errors, setErrors] = React.useState<Record<string, ErrorValue>>({});
  const { mutate: createConfig, isLoading: createLoading } = useCreateGitLabConfigurationMutation();
  const { mutate: updateConfig, isLoading: updateLoading } = useUpdateGitLabConfigurationMutation();

  const [formData, setFormData] = React.useState<
    Record<keyof GitLabConfigurationCreateBody, FormData>
  >({
    applicationId: {
      value: data?.applicationId ?? '',
      required: true,
      definition: {
        name: translate('settings.authentication.gitlab.form.applicationId.name'),
        key: 'applicationId',
        description: translate('settings.authentication.gitlab.form.applicationId.description'),
        secured: false,
      },
    },
    url: {
      value: data?.url ?? DEFAULT_URL,
      required: true,
      definition: {
        name: translate('settings.authentication.gitlab.form.url.name'),
        secured: false,
        key: 'url',
        description: translate('settings.authentication.gitlab.form.url.description'),
      },
    },
    secret: {
      value: '',
      required: true,
      definition: {
        name: translate('settings.authentication.gitlab.form.secret.name'),
        secured: true,
        key: 'secret',
        description: translate('settings.authentication.gitlab.form.secret.description'),
      },
    },
    synchronizeGroups: {
      value: data?.synchronizeGroups ?? false,
      required: false,
      definition: {
        name: translate('settings.authentication.gitlab.form.synchronizeGroups.name'),
        secured: false,
        key: 'synchronizeGroups',
        description: translate('settings.authentication.gitlab.form.synchronizeGroups.description'),
        type: SettingType.BOOLEAN,
      },
    },
    allowedGroups: {
      value: data?.allowedGroups ?? [],
      required: true,
      definition: {
        name: translate('settings.authentication.gitlab.form.allowedGroups.name'),
        secured: false,
        key: 'allowedGroups',
        description: translate('settings.authentication.gitlab.form.allowedGroups.description'),
        multiValues: true,
      },
    },
  });

  const header = translate('settings.authentication.gitlab.form', isCreate ? 'create' : 'edit');

  const canBeSaved = Object.values(formData).every(({ definition, required, value }) => {
    return (
      (!isCreate && definition.secured) ||
      !required ||
      (isArray(value) ? value.some((val) => val !== '') : value !== '')
    );
  });

  const handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (canBeSaved) {
      const submitData = Object.entries(formData).reduce<GitLabConfigurationCreateBody>(
        (acc, [key, { value }]: [keyof GitLabConfigurationCreateBody, FormData]) => {
          if (value === '') {
            return acc;
          }
          return {
            ...acc,
            [key]: value,
          };
        },
        {} as GitLabConfigurationCreateBody,
      );
      if (data) {
        updateConfig({ id: data.id, data: submitData }, { onSuccess: props.onClose });
      } else {
        createConfig(submitData, { onSuccess: props.onClose });
      }
    } else {
      const errors = Object.entries(formData)
        .filter(([_, v]) => v.required && !v.value)
        .map(([key]) => ({ key, message: translate('field_required') }));
      setErrors(keyBy(errors, 'key'));
    }
  };

  const FORM_ID = 'gitlab-configuration-form';

  const formBody = (
    <form id={FORM_ID} onSubmit={handleSubmit}>
      <FlagMessage variant="info" className="sw-w-full sw-mb-8">
        <span>
          <FormattedMessage
            id="settings.authentication.help"
            values={{
              link: (
                <DocumentationLink
                  to={`/instance-administration/authentication/${DOCUMENTATION_LINK_SUFFIXES.gitlab}/`}
                >
                  {translate('settings.authentication.help.link')}
                </DocumentationLink>
              ),
            }}
          />
        </span>
      </FlagMessage>
      {Object.entries(formData).map(
        ([key, { value, required, definition }]: [
          key: keyof GitLabConfigurationCreateBody,
          FormData,
        ]) => (
          <div key={key} className="sw-mb-8">
            <AuthenticationFormField
              settingValue={value}
              definition={definition}
              mandatory={required}
              onFieldChange={(_, value) => {
                setFormData((prev) => ({ ...prev, [key]: { ...prev[key], value } }));
              }}
              isNotSet={isCreate}
              error={errors[key]?.message}
            />
          </div>
        ),
      )}
    </form>
  );

  return (
    <Modal
      headerTitle={header}
      onClose={props.onClose}
      body={formBody}
      primaryButton={
        <ButtonPrimary form={FORM_ID} type="submit" disabled={!canBeSaved}>
          {translate('settings.almintegration.form.save')}
          <Spinner className="sw-ml-2" loading={createLoading || updateLoading} />
        </ButtonPrimary>
      }
    />
  );
}
