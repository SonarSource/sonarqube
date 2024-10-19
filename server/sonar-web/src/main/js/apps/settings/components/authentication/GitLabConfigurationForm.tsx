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

import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { FlagMessage, Modal, Spinner } from 'design-system';
import { keyBy } from 'lodash';
import React, { SyntheticEvent, useEffect, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../../components/common/DocumentationLink';
import { DocLink } from '../../../../helpers/doc-links';
import { translate } from '../../../../helpers/l10n';
import {
  useCreateGitLabConfigurationMutation,
  useUpdateGitLabConfigurationMutation,
} from '../../../../queries/identity-provider/gitlab';
import { GitLabConfigurationCreateBody, GitlabConfiguration } from '../../../../types/provisioning';
import { DefinitionV2, SettingType } from '../../../../types/settings';
import AuthenticationFormField from './AuthenticationFormField';

interface Props {
  gitlabConfiguration: GitlabConfiguration | null;
  onClose: () => void;
}

interface ErrorValue {
  key: string;
  message: string;
}

interface FormData {
  definition: DefinitionV2;
  required: boolean;
  value: string | boolean | string[];
}

const DEFAULT_URL = 'https://gitlab.com';
const FORM_ID = 'gitlab-configuration-form';

export default function GitLabConfigurationForm(props: Readonly<Props>) {
  const { gitlabConfiguration } = props;
  const isCreate = gitlabConfiguration === null;
  const [errors, setErrors] = useState<Record<string, ErrorValue>>({});
  const { mutate: createConfig, isPending: createLoading } = useCreateGitLabConfigurationMutation();
  const { mutate: updateConfig, isPending: updateLoading } = useUpdateGitLabConfigurationMutation();

  const [formData, setFormData] = useState<Record<keyof GitLabConfigurationCreateBody, FormData>>({
    applicationId: {
      value: gitlabConfiguration?.applicationId ?? '',
      required: true,
      definition: {
        name: translate('settings.authentication.gitlab.form.applicationId.name'),
        key: 'applicationId',
        description: translate('settings.authentication.gitlab.form.applicationId.description'),
        secured: false,
      },
    },
    url: {
      value: gitlabConfiguration?.url ?? DEFAULT_URL,
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
      value: gitlabConfiguration?.synchronizeGroups ?? false,
      required: false,
      definition: {
        name: translate('settings.authentication.gitlab.form.synchronizeGroups.name'),
        secured: false,
        key: 'synchronizeGroups',
        description: translate('settings.authentication.gitlab.form.synchronizeGroups.description'),
        type: SettingType.BOOLEAN,
      },
    },
  });

  const header = translate('settings.authentication.gitlab.form', isCreate ? 'create' : 'edit');

  // In case of URL update, the user must provide the secret again
  // This relation is specific to URL & Secret so no need for a generic solution here
  const isSecretMissingToUpdateUrl =
    !isCreate && formData.url.value !== gitlabConfiguration.url && formData.secret.value === '';
  const canBeSaved =
    !isSecretMissingToUpdateUrl &&
    Object.values(formData).every(({ definition, required, value }) => {
      return (!isCreate && definition.secured) || !required || value !== '';
    });

  const handleSubmit = (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (canBeSaved) {
      const submitData = Object.entries(formData).reduce<GitLabConfigurationCreateBody>(
        (acc, [key, { value }]: [keyof GitLabConfigurationCreateBody, FormData]) => {
          if (
            value === '' ||
            (key !== 'secret' && !isCreate && value === gitlabConfiguration[key])
          ) {
            return acc;
          }
          return {
            ...acc,
            [key]: value,
          };
        },
        {} as GitLabConfigurationCreateBody,
      );
      if (!isCreate) {
        updateConfig(
          { id: gitlabConfiguration.id, data: submitData },
          { onSuccess: props.onClose },
        );
      } else {
        createConfig(submitData, { onSuccess: props.onClose });
      }
    } else {
      const errors = Object.entries(formData)
        .filter(([_, v]) => v.required && Boolean(v.value) !== false)
        .map(([key]) => ({ key, message: translate('field_required') }));
      setErrors(keyBy(errors, 'key'));
    }
  };

  const formBody = (
    <form id={FORM_ID} onSubmit={handleSubmit}>
      <FlagMessage variant="info" className="sw-w-full sw-mb-8">
        <span>
          <FormattedMessage
            id="settings.authentication.help"
            values={{
              link: (
                <DocumentationLink to={DocLink.AlmGitLabAuth}>
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

  useEffect(() => {
    if (isSecretMissingToUpdateUrl) {
      setErrors({
        ...errors,
        secret: {
          key: 'secret',
          message: translate('settings.authentication.gitlab.form.secret.required_for_url_change'),
        },
      });
    }

    return () => {
      const newErrors = {
        ...errors,
      };
      delete newErrors.secret;
      setErrors(newErrors);
    };
    // We only want to run this effect when isSecretMissingToUpdateUrl changes:
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isSecretMissingToUpdateUrl]);

  return (
    <Modal
      headerTitle={header}
      onClose={props.onClose}
      body={formBody}
      primaryButton={
        <>
          <Spinner loading={createLoading || updateLoading} />
          <Button
            form={FORM_ID}
            type="submit"
            isDisabled={!canBeSaved}
            variety={ButtonVariety.Primary}
          >
            {translate('settings.almintegration.form.save')}
          </Button>
        </>
      }
    />
  );
}
