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
import { keyBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocLink from '../../../../components/common/DocLink';
import Modal from '../../../../components/controls/Modal';
import { ResetButtonLink, SubmitButton } from '../../../../components/controls/buttons';
import { Alert } from '../../../../components/ui/Alert';
import Spinner from '../../../../components/ui/Spinner';
import { translate } from '../../../../helpers/l10n';
import {
  useCreateGitLabConfigurationMutation,
  useUpdateGitLabConfigurationMutation,
} from '../../../../queries/identity-provider';
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
  value: string | boolean;
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
      value: '',
      required: true,
      definition: {
        name: translate('settings.authentication.gitlab.form.applicationId.name'),
        key: 'applicationId',
        description: translate('settings.authentication.gitlab.form.applicationId.description'),
        secured: true,
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
    clientSecret: {
      value: '',
      required: true,
      definition: {
        name: translate('settings.authentication.gitlab.form.clientSecret.name'),
        secured: true,
        key: 'clientSecret',
        description: translate('settings.authentication.gitlab.form.clientSecret.description'),
      },
    },
    synchronizeUserGroups: {
      value: data?.synchronizeUserGroups ?? false,
      required: false,
      definition: {
        name: translate('settings.authentication.gitlab.form.synchronizeUserGroups.name'),
        secured: false,
        key: 'synchronizeUserGroups',
        description: translate(
          'settings.authentication.gitlab.form.synchronizeUserGroups.description',
        ),
        type: SettingType.BOOLEAN,
      },
    },
  });

  const headerLabel = translate(
    'settings.authentication.gitlab.form',
    isCreate ? 'create' : 'edit',
  );

  const canBeSaved = Object.values(formData).every(
    (v) => (!isCreate && v.definition.secured) || !v.required || v.value !== '',
  );

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

  return (
    <Modal
      contentLabel={headerLabel}
      onRequestClose={props.onClose}
      shouldCloseOnOverlayClick={false}
      shouldCloseOnEsc
      size="medium"
    >
      <form onSubmit={handleSubmit}>
        <div className="modal-head">
          <h2>{headerLabel}</h2>
        </div>
        <div className="modal-body modal-container">
          <Alert variant="info">
            <FormattedMessage
              id="settings.authentication.help"
              values={{
                link: (
                  <DocLink
                    to={`/instance-administration/authentication/${DOCUMENTATION_LINK_SUFFIXES.gitlab}/`}
                  >
                    {translate('settings.authentication.help.link')}
                  </DocLink>
                ),
              }}
            />
          </Alert>
          {Object.entries(formData).map(
            ([key, { value, required, definition }]: [
              key: keyof GitLabConfigurationCreateBody,
              FormData,
            ]) => (
              <div key={key}>
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
        </div>

        <div className="modal-foot">
          <SubmitButton disabled={!canBeSaved}>
            {translate('settings.almintegration.form.save')}
            <Spinner className="spacer-left" loading={createLoading || updateLoading} />
          </SubmitButton>
          <ResetButtonLink onClick={props.onClose}>{translate('cancel')}</ResetButtonLink>
        </div>
      </form>
    </Modal>
  );
}
