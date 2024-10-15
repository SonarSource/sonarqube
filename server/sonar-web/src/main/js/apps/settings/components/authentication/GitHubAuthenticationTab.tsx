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

import { Spinner } from '@sonarsource/echoes-react';
import { ButtonSecondary, FlagMessage, Highlight, Note } from 'design-system/lib';
import { isEmpty, omitBy } from 'lodash';
import React, { FormEvent, useContext, useState } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import GitHubSynchronisationWarning from '../../../../app/components/GitHubSynchronisationWarning';
import { AvailableFeaturesContext } from '../../../../app/components/available-features/AvailableFeaturesContext';
import DocumentationLink from '../../../../components/common/DocumentationLink';
import { DocLink } from '../../../../helpers/doc-links';
import { translate } from '../../../../helpers/l10n';
import {
  useDeleteGitHubConfigurationMutation,
  useSearchGitHubConfigurationsQuery,
  useUpdateGitHubConfigurationMutation,
} from '../../../../queries/dop-translation';
import { useIdentityProviderQuery } from '../../../../queries/identity-provider/common';
import {
  useGithubRolesMappingMutation,
  useSyncWithGitHubNow,
} from '../../../../queries/identity-provider/github';
import { GitHubConfigurationPayload } from '../../../../types/dop-translation';
import { Feature } from '../../../../types/features';
import { DevopsRolesMapping, ProvisioningType } from '../../../../types/provisioning';
import { Provider } from '../../../../types/types';
import AuthenticationFormField from './AuthenticationFormField';
import AutoProvisioningConsent from './AutoProvisionningConsent';
import ConfigurationDetails from './ConfigurationDetails';
import ConfirmProvisioningModal from './ConfirmProvisioningModal';
import GitHubConfigurationForm from './GitHubConfigurationForm';
import GitHubConfigurationValidity from './GitHubConfigurationValidity';
import GitHubMappingModal from './GitHubMappingModal';
import ProvisioningSection from './ProvisioningSection';
import TabHeader from './TabHeader';
import { GITHUB_PROVISIONING_FIELDS_DEFINITIONS } from './utils';

type ChangesForm = Partial<
  Pick<GitHubConfigurationPayload, 'allowUsersToSignUp' | 'projectVisibility' | 'provisioningType'>
>;

export default function GitHubAuthenticationTab() {
  const [isFormOpen, setIsFormOpen] = React.useState(false);
  const [changes, setChanges] = React.useState<ChangesForm | undefined>(undefined);
  const [tokenKey, setTokenKey] = React.useState<number>(0);
  const [isConfirmProvisioningModalOpen, setIsConfirmProvisioningModalOpen] = React.useState(false);
  const [isMappingModalOpen, setIsMappingModalOpen] = useState(false);
  const [rolesMapping, setRolesMapping] = useState<DevopsRolesMapping[] | null>(null);

  const hasGithubProvisioning = useContext(AvailableFeaturesContext).includes(
    Feature.GithubProvisioning,
  );

  const { data: identityProvider } = useIdentityProviderQuery();
  const { data: list, isLoading: isLoadingList } = useSearchGitHubConfigurationsQuery();
  const gitHubConfiguration = list?.githubConfigurations[0];

  const { canSyncNow, synchronizeNow } = useSyncWithGitHubNow();

  const { mutate: updateConfig, isPending: isUpdating } = useUpdateGitHubConfigurationMutation();
  const { mutate: deleteConfig, isPending: isDeleting } = useDeleteGitHubConfigurationMutation();
  const { mutateAsync: updateMapping } = useGithubRolesMappingMutation();

  const { formatMessage } = useIntl();

  const hasConfiguration = gitHubConfiguration !== undefined;
  const hasLegacyConfiguration = gitHubConfiguration?.applicationId === '';
  const isConfigurationEnabled = gitHubConfiguration?.enabled === true;

  const hasDifferentProvider =
    identityProvider?.provider !== undefined && identityProvider.provider !== Provider.Github;
  const allowUsersToSignUpDefinition = GITHUB_PROVISIONING_FIELDS_DEFINITIONS.allowUsersToSignUp;
  const projectVisibilityDefinition = GITHUB_PROVISIONING_FIELDS_DEFINITIONS.projectVisibility;

  const provisioningType = changes?.provisioningType ?? gitHubConfiguration?.provisioningType;
  const allowUsersToSignUp = changes?.allowUsersToSignUp ?? gitHubConfiguration?.allowUsersToSignUp;
  const projectVisibility = changes?.projectVisibility ?? gitHubConfiguration?.projectVisibility;

  const onChangeProvisioningType = (val?: ProvisioningType) => {
    setRolesMapping(null);

    if (val === ProvisioningType.auto) {
      setAutoProvisioning();
    } else if (val === ProvisioningType.jit) {
      setJITProvisioning();
    } else {
      setChanges({
        allowUsersToSignUp: gitHubConfiguration?.allowUsersToSignUp,
        projectVisibility: gitHubConfiguration?.projectVisibility,
        provisioningType: gitHubConfiguration?.provisioningType,
      });
    }
  };

  const onDeleteConfiguration = () => {
    if (!gitHubConfiguration) {
      return;
    }
    deleteConfig(gitHubConfiguration.id);
  };

  const onSaveProvisioning = (e: FormEvent) => {
    e.preventDefault();
    if (
      changes?.provisioningType !== undefined ||
      (isEmpty(gitHubConfiguration?.allowedOrganizations) &&
        (allowUsersToSignUp || provisioningType === ProvisioningType.auto))
    ) {
      setIsConfirmProvisioningModalOpen(true);
    } else {
      onUpdateProvisioning();
    }
  };

  const onToggleConfiguration = () => {
    if (!gitHubConfiguration) {
      return;
    }
    updateConfig({
      id: gitHubConfiguration.id,
      gitHubConfiguration: { enabled: !isConfigurationEnabled },
    });
  };

  const onUpdateProvisioning = () => {
    if ((!changes && !rolesMapping) || !gitHubConfiguration) {
      return;
    }

    updateConfig(
      {
        id: gitHubConfiguration.id,
        gitHubConfiguration: omitBy(changes, (value) => value === undefined),
      },
      {
        onSuccess: () => {
          setChanges(undefined);
          setTokenKey(tokenKey + 1);
        },
      },
    );
    if (provisioningType === ProvisioningType.auto && rolesMapping) {
      updateMapping(rolesMapping)
        .then(() => {
          setRolesMapping(null);
        })
        .catch(() => {});
    }
  };

  const setAutoProvisioning = () => {
    setChanges((prevChanges) => ({
      ...prevChanges,
      provisioningType: ProvisioningType.auto,
    }));
  };

  const setJITProvisioning = () => {
    setChanges((prevChanges) => ({
      ...prevChanges,
      provisioningType: ProvisioningType.jit,
    }));
  };

  return (
    <Spinner isLoading={isLoadingList}>
      <div>
        <TabHeader
          title={translate('settings.authentication.github.configuration')}
          showCreate={!hasConfiguration || hasLegacyConfiguration}
          onCreate={() => {
            setIsFormOpen(true);
          }}
          configurationValidity={
            <>
              {!isLoadingList && isConfigurationEnabled && !hasLegacyConfiguration && (
                <GitHubConfigurationValidity
                  isAutoProvisioning={provisioningType === ProvisioningType.auto}
                  selectedOrganizations={gitHubConfiguration.allowedOrganizations}
                />
              )}
            </>
          }
        />
        {!hasConfiguration && (
          <div>{translate('settings.authentication.github.form.not_configured')}</div>
        )}
        {hasLegacyConfiguration && (
          <FlagMessage variant="warning">
            <div>
              <FormattedMessage
                id="settings.authentication.github.form.legacy_configured"
                defaultMessage={translate('settings.authentication.github.form.legacy_configured')}
                values={{
                  documentation: (
                    <DocumentationLink to={DocLink.AlmGitHubAuth}>
                      {translate('settings.authentication.github.form.legacy_configured.link')}
                    </DocumentationLink>
                  ),
                }}
              />
            </div>
          </FlagMessage>
        )}
        {!hasLegacyConfiguration && hasConfiguration && (
          <>
            <ConfigurationDetails
              title={formatMessage(
                { id: 'settings.authentication.github.appid_x' },
                { applicationId: gitHubConfiguration.applicationId },
              )}
              url={gitHubConfiguration.apiUrl}
              canDisable={
                !isUpdating && gitHubConfiguration.provisioningType !== ProvisioningType.auto
              }
              enabled={isConfigurationEnabled}
              isDeleting={isDeleting}
              onEdit={() => {
                setIsFormOpen(true);
              }}
              onDelete={onDeleteConfiguration}
              onToggle={onToggleConfiguration}
            />

            <ProvisioningSection
              autoDescription={
                <FormattedMessage
                  id="settings.authentication.github.form.provisioning_with_github.description"
                  values={{
                    documentation: (
                      <DocumentationLink to={DocLink.AlmGitHubAuth}>
                        {translate('learn_more')}
                      </DocumentationLink>
                    ),
                  }}
                />
              }
              autoFeatureDisabledText={
                <FormattedMessage
                  id="settings.authentication.github.form.provisioning.disabled"
                  defaultMessage={translate(
                    'settings.authentication.github.form.provisioning.disabled',
                  )}
                  values={{
                    documentation: (
                      <DocumentationLink to={DocLink.AlmGitHubAuth}>
                        {translate('documentation')}
                      </DocumentationLink>
                    ),
                  }}
                />
              }
              autoSettings={
                <>
                  <AuthenticationFormField
                    definition={projectVisibilityDefinition}
                    isNotSet={gitHubConfiguration.provisioningType !== ProvisioningType.auto}
                    mandatory
                    onFieldChange={(_, value) =>
                      setChanges((prevChanges) => ({
                        ...prevChanges,
                        projectVisibility: value as boolean,
                      }))
                    }
                    settingValue={projectVisibility}
                  />
                  <div className="sw-mt-6">
                    <div className="sw-flex">
                      <Highlight className="sw-mb-4 sw-mr-4 sw-flex sw-items-center sw-gap-2">
                        <FormattedMessage id="settings.authentication.configuration.roles_mapping.title" />
                      </Highlight>
                      <ButtonSecondary
                        className="sw--mt-2"
                        onClick={() => setIsMappingModalOpen(true)}
                      >
                        <FormattedMessage id="settings.authentication.configuration.roles_mapping.button_label" />
                      </ButtonSecondary>
                    </div>
                    <Note className="sw-mt-2">
                      <FormattedMessage id="settings.authentication.github.configuration.roles_mapping.description" />
                    </Note>
                  </div>
                </>
              }
              autoTitle={translate('settings.authentication.github.form.provisioning_with_github')}
              canSync={canSyncNow}
              disabledConfigText={translate('settings.authentication.github.enable_first')}
              enabled={isConfigurationEnabled}
              hasDifferentProvider={hasDifferentProvider}
              hasFeatureEnabled={hasGithubProvisioning}
              hasUnsavedChanges={changes !== undefined || rolesMapping !== null}
              isLoading={isUpdating}
              jitDescription={
                <FormattedMessage
                  id="settings.authentication.github.form.provisioning_at_login.description"
                  values={{
                    documentation: (
                      <DocumentationLink to={DocLink.AlmGitHubAuth}>
                        {translate('learn_more')}
                      </DocumentationLink>
                    ),
                  }}
                />
              }
              jitSettings={
                <AuthenticationFormField
                  definition={allowUsersToSignUpDefinition}
                  isNotSet={gitHubConfiguration.provisioningType !== ProvisioningType.auto}
                  mandatory
                  onFieldChange={(_, value) =>
                    setChanges((prevChanges) => ({
                      ...prevChanges,
                      allowUsersToSignUp: value as boolean,
                    }))
                  }
                  settingValue={allowUsersToSignUp}
                />
              }
              jitTitle={translate('settings.authentication.form.provisioning_at_login')}
              onCancel={() => {
                setChanges(undefined);
                onChangeProvisioningType();
              }}
              onChangeProvisioningType={onChangeProvisioningType}
              onSave={onSaveProvisioning}
              onSyncNow={synchronizeNow}
              provisioningType={provisioningType ?? ProvisioningType.jit}
              synchronizationDetails={<GitHubSynchronisationWarning />}
            />

            {provisioningType && (
              <ConfirmProvisioningModal
                allowUsersToSignUp={allowUsersToSignUp}
                hasProvisioningTypeChange={changes?.provisioningType !== undefined}
                isAllowListEmpty={isEmpty(gitHubConfiguration.allowedOrganizations)}
                isOpen={isConfirmProvisioningModalOpen}
                onClose={() => setIsConfirmProvisioningModalOpen(false)}
                onConfirm={onUpdateProvisioning}
                provider={Provider.Github}
                provisioningStatus={provisioningType}
              />
            )}

            {isMappingModalOpen && (
              <GitHubMappingModal
                mapping={rolesMapping}
                setMapping={setRolesMapping}
                onClose={() => setIsMappingModalOpen(false)}
              />
            )}
          </>
        )}

        {isFormOpen && (
          <GitHubConfigurationForm
            gitHubConfiguration={gitHubConfiguration}
            isStandardModeConfiguration={hasLegacyConfiguration}
            onClose={() => {
              setIsFormOpen(false);
            }}
          />
        )}

        <AutoProvisioningConsent githubConfiguration={gitHubConfiguration} />
      </div>
    </Spinner>
  );
}
