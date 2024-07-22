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
import { Button, ButtonVariety, Spinner } from '@sonarsource/echoes-react';
import { FlagMessage, Modal } from 'design-system';
import { isEmpty, keyBy } from 'lodash';
import React, { useEffect, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../../components/common/DocumentationLink';
import { DocLink } from '../../../../helpers/doc-links';
import { translate } from '../../../../helpers/l10n';
import {
  useCreateGitHubConfigurationMutation,
  useUpdateGitHubConfigurationMutation,
} from '../../../../queries/dop-translation';
import { AlmKeys } from '../../../../types/alm-settings';
import {
  GitHubConfigurationPayload,
  GitHubConfigurationResponse,
} from '../../../../types/dop-translation';
import { ProvisioningType } from '../../../../types/provisioning';
import { DefinitionV2 } from '../../../../types/settings';
import { Provider } from '../../../../types/types';
import AuthenticationFormField from './AuthenticationFormField';
import ConfirmProvisioningModal from './ConfirmProvisioningModal';
import {
  FORM_ID,
  GITHUB_AUTH_FORM_FIELDS_ORDER,
  GitHubAuthFormFields,
  getInitialGitHubFormData,
} from './utils';

interface Props {
  gitHubConfiguration?: GitHubConfigurationResponse;
  isLegacyConfiguration: boolean;
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

export default function GitHubConfigurationForm(props: Readonly<Props>) {
  const { gitHubConfiguration, isLegacyConfiguration, onClose } = props;
  const isCreate = gitHubConfiguration === undefined;

  const [errors, setErrors] = useState<Partial<Record<GitHubAuthFormFields, ErrorValue>>>({});
  const [isConfirmModalOpen, setIsConfirmModalOpen] = React.useState(false);

  const { mutate: createConfig, isPending: isCreating } = useCreateGitHubConfigurationMutation();
  const { mutate: updateConfig, isPending: isUpdating } = useUpdateGitHubConfigurationMutation();

  const [formData, setFormData] = useState<Record<GitHubAuthFormFields, FormData>>(
    getInitialGitHubFormData(gitHubConfiguration),
  );

  const header = translate('settings.authentication.github.form', isCreate ? 'create' : 'edit');

  // In case of API/Web URL update, the user must provide the private key again
  // This relation is specific to API/Web URL & Private Key so no need for a generic solution here
  const isSecretMissingToUpdateApiUrl =
    !isCreate &&
    formData.apiUrl.value !== gitHubConfiguration.apiUrl &&
    formData.privateKey.value === '';
  const isSecretMissingToUpdateWebUrl =
    !isCreate &&
    formData.webUrl.value !== gitHubConfiguration.webUrl &&
    formData.privateKey.value === '';
  const isFormValid =
    !isSecretMissingToUpdateApiUrl &&
    !isSecretMissingToUpdateWebUrl &&
    Object.values(formData).every(({ definition, required, value }) => {
      return (!isCreate && definition.secured) || !required || value !== '';
    });

  const onSave = () => {
    const newGitHubConfiguration = Object.entries(formData).reduce(
      (config, [key, { required, value }]: [GitHubAuthFormFields, FormData]) => {
        if (
          value === undefined ||
          (value === '' && required) ||
          (!isCreate &&
            key in gitHubConfiguration &&
            value === gitHubConfiguration[key as keyof GitHubConfigurationResponse])
        ) {
          return config;
        }

        // https://www.totaltypescript.com/as-never
        config[key] = value as never;

        return config;
      },
      {} as Pick<GitHubConfigurationPayload, GitHubAuthFormFields>,
    );

    if (isCreate) {
      createConfig(
        {
          allowUsersToSignUp: true,
          enabled: false,
          projectVisibility: true,
          provisioningType: ProvisioningType.jit,
          userConsentRequiredAfterUpgrade: false,
          ...newGitHubConfiguration,
        },
        { onSuccess: onClose },
      );
    } else {
      updateConfig(
        { id: gitHubConfiguration.id, gitHubConfiguration: newGitHubConfiguration },
        { onSuccess: onClose },
      );
    }
  };

  const handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (isFormValid) {
      if (
        isEmpty(formData.allowedOrganizations.value) &&
        (gitHubConfiguration?.provisioningType === ProvisioningType.auto ||
          gitHubConfiguration?.allowUsersToSignUp)
      ) {
        setIsConfirmModalOpen(true);
      } else {
        onSave();
      }
    } else {
      const errors = Object.entries(formData)
        .filter(([_, v]) => v.required && Boolean(v.value) !== false)
        .map(([key]) => ({ key, message: translate('field_required') }));
      setErrors(keyBy(errors, 'key'));
    }
  };

  const helpMessage = isLegacyConfiguration ? `legacy_help.${AlmKeys.GitHub}` : 'help';

  const formBody = (
    <form id={FORM_ID} onSubmit={handleSubmit}>
      <FlagMessage
        className="sw-w-full sw-mb-8"
        variant={isLegacyConfiguration ? 'warning' : 'info'}
      >
        <span>
          <FormattedMessage
            id={`settings.authentication.${helpMessage}`}
            values={{
              link: (
                <DocumentationLink to={DocLink.AlmGitHubAuth}>
                  <FormattedMessage id="settings.authentication.help.link" />
                </DocumentationLink>
              ),
            }}
          />
        </span>
      </FlagMessage>

      {GITHUB_AUTH_FORM_FIELDS_ORDER.map((key) => {
        const { value, required, definition } = formData[key];

        return (
          <div key={key} className="sw-mb-8">
            <AuthenticationFormField
              settingValue={value}
              definition={definition}
              mandatory={required}
              onFieldChange={(_, value) => {
                setFormData((prev) => ({ ...prev, [key]: { ...prev[key], value } }));
              }}
              isNotSet={isCreate || isLegacyConfiguration}
              error={errors[key]?.message}
            />
          </div>
        );
      })}
    </form>
  );

  useEffect(() => {
    if (isSecretMissingToUpdateApiUrl || isSecretMissingToUpdateWebUrl) {
      setErrors({
        ...errors,
        privateKey: {
          key: 'privateKey',
          message: translate(
            'settings.authentication.github.form.private_key.required_for_url_change',
          ),
        },
      });
    }

    return () => {
      const newErrors = {
        ...errors,
      };
      delete newErrors.privateKey;
      setErrors(newErrors);
    };
    // We only want to run this effect when isSecretMissingToUpdateUrl changes:
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isSecretMissingToUpdateApiUrl, isSecretMissingToUpdateWebUrl]);

  return (
    <>
      <Modal
        body={formBody}
        headerTitle={header}
        isScrollable
        onClose={onClose}
        primaryButton={
          <>
            <Spinner className="sw-ml-2" isLoading={isCreating || isUpdating} />
            <Button
              form={FORM_ID}
              type="submit"
              hasAutoFocus
              isDisabled={!isFormValid}
              variety={ButtonVariety.Primary}
            >
              <FormattedMessage id="settings.almintegration.form.save" />
            </Button>
          </>
        }
      />
      {isConfirmModalOpen && (
        <ConfirmProvisioningModal
          allowUsersToSignUp={gitHubConfiguration?.allowUsersToSignUp}
          isAllowListEmpty={isEmpty(gitHubConfiguration?.allowedOrganizations)}
          onClose={() => setIsConfirmModalOpen(false)}
          onConfirm={onSave}
          provider={Provider.Github}
          provisioningStatus={gitHubConfiguration?.provisioningType ?? ProvisioningType.jit}
        />
      )}
    </>
  );
}
