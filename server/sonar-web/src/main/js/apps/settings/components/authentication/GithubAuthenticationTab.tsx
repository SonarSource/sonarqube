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
import { ButtonSecondary, FlagMessage, Highlight, Note, Spinner } from 'design-system';
import React, { FormEvent, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import GitHubSynchronisationWarning from '../../../../app/components/GitHubSynchronisationWarning';
import DocumentationLink from '../../../../components/common/DocumentationLink';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { useIdentityProviderQuery } from '../../../../queries/identity-provider/common';
import {
  useCheckGitHubConfigQuery,
  useSyncWithGitHubNow,
} from '../../../../queries/identity-provider/github';
import { AlmKeys } from '../../../../types/alm-settings';
import { ProvisioningType } from '../../../../types/provisioning';
import { ExtendedSettingDefinition } from '../../../../types/settings';
import { Provider } from '../../../../types/types';
import { AuthenticationTabs, DOCUMENTATION_LINK_SUFFIXES } from './Authentication';
import AuthenticationFormField from './AuthenticationFormField';
import AutoProvisioningConsent from './AutoProvisionningConsent';
import ConfigurationDetails from './ConfigurationDetails';
import ConfigurationForm from './ConfigurationForm';
import GitHubConfigurationValidity from './GitHubConfigurationValidity';
import GitHubConfirmModal from './GitHubConfirmModal';
import GitHubMappingModal from './GitHubMappingModal';
import ProvisioningSection from './ProvisioningSection';
import TabHeader from './TabHeader';
import useGithubConfiguration, {
  GITHUB_ADDITIONAL_FIELDS,
  GITHUB_JIT_FIELDS,
  GITHUB_PROVISIONING_FIELDS,
  isAllowToSignUpEnabled,
  isOrganizationListEmpty,
} from './hook/useGithubConfiguration';

interface GithubAuthenticationProps {
  definitions: ExtendedSettingDefinition[];
  currentTab: AuthenticationTabs;
}

const GITHUB_EXCLUDED_FIELD = ['sonar.auth.github.enabled', ...GITHUB_ADDITIONAL_FIELDS];

export default function GithubAuthenticationTab(props: GithubAuthenticationProps) {
  const { definitions, currentTab } = props;
  const { data } = useIdentityProviderQuery();
  const [showEditModal, setShowEditModal] = useState(false);
  const [showMappingModal, setShowMappingModal] = useState(false);
  const [showConfirmProvisioningModal, setShowConfirmProvisioningModal] = useState(false);

  const {
    hasConfiguration,
    hasGithubProvisioning,
    githubProvisioningStatus,
    isLoading,
    values,
    setNewValue,
    canBeSave,
    url,
    appId,
    enabled,
    newGithubProvisioningStatus,
    setProvisioningType,
    hasGithubProvisioningTypeChange,
    hasGithubProvisioningConfigChange,
    resetJitSetting,
    changeProvisioning,
    toggleEnable,
    rolesMapping,
    setRolesMapping,
    applyAdditionalOptions,
    hasLegacyConfiguration,
    deleteMutation: { isPending: isDeleting, mutate: deleteConfiguration },
  } = useGithubConfiguration(definitions);

  const provisioningStatus =
    newGithubProvisioningStatus ?? githubProvisioningStatus
      ? ProvisioningType.auto
      : ProvisioningType.jit;

  const hasDifferentProvider = data?.provider !== undefined && data.provider !== Provider.Github;
  const { canSyncNow, synchronizeNow } = useSyncWithGitHubNow();
  const { refetch } = useCheckGitHubConfigQuery(enabled);

  const handleCreateConfiguration = () => {
    setShowEditModal(true);
  };

  const handleCloseConfiguration = () => {
    refetch();
    setShowEditModal(false);
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (
      hasGithubProvisioningTypeChange ||
      (isOrganizationListEmpty(values) &&
        (isAllowToSignUpEnabled(values) || provisioningStatus === ProvisioningType.auto))
    ) {
      setShowConfirmProvisioningModal(true);
    } else {
      applyAdditionalOptions();
    }
  };

  const handleProvisioningTypeChange = (type: ProvisioningType) => {
    setProvisioningType(type === ProvisioningType.auto);
  };

  return (
    <Spinner loading={isLoading}>
      <div>
        <TabHeader
          title={translate('settings.authentication.github.configuration')}
          showCreate={!hasConfiguration}
          onCreate={handleCreateConfiguration}
          configurationValidity={
            <>
              {enabled && !hasLegacyConfiguration && (
                <GitHubConfigurationValidity
                  selectedOrganizations={
                    (values['sonar.auth.github.organizations']?.value as string[]) ?? []
                  }
                  isAutoProvisioning={!!(newGithubProvisioningStatus ?? githubProvisioningStatus)}
                />
              )}
            </>
          }
        />
        {!hasConfiguration && !hasLegacyConfiguration && (
          <div>{translate('settings.authentication.github.form.not_configured')}</div>
        )}
        {!hasConfiguration && hasLegacyConfiguration && (
          <FlagMessage variant="warning">
            <div>
              <FormattedMessage
                id="settings.authentication.github.form.legacy_configured"
                defaultMessage={translate('settings.authentication.github.form.legacy_configured')}
                values={{
                  documentation: (
                    <DocumentationLink to="/instance-administration/authentication/github">
                      {translate('settings.authentication.github.form.legacy_configured.link')}
                    </DocumentationLink>
                  ),
                }}
              />
            </div>
          </FlagMessage>
        )}
        {hasConfiguration && (
          <>
            <ConfigurationDetails
              title={translateWithParameters('settings.authentication.github.appid_x', appId)}
              url={url}
              canDisable={!githubProvisioningStatus}
              enabled={enabled}
              isDeleting={isDeleting}
              onEdit={handleCreateConfiguration}
              onDelete={deleteConfiguration}
              onToggle={toggleEnable}
            />

            <ProvisioningSection
              provisioningType={
                newGithubProvisioningStatus ?? githubProvisioningStatus
                  ? ProvisioningType.auto
                  : ProvisioningType.jit
              }
              onChangeProvisioningType={handleProvisioningTypeChange}
              disabledConfigText={translate('settings.authentication.github.enable_first')}
              enabled={enabled}
              hasUnsavedChanges={!!hasGithubProvisioningConfigChange}
              onSave={handleSubmit}
              onCancel={() => {
                setProvisioningType(undefined);
                resetJitSetting();
              }}
              jitTitle={translate('settings.authentication.form.provisioning_at_login')}
              jitDescription={
                <FormattedMessage
                  id="settings.authentication.github.form.provisioning_at_login.description"
                  values={{
                    documentation: (
                      <DocumentationLink
                        to={`/instance-administration/authentication/${
                          DOCUMENTATION_LINK_SUFFIXES[AlmKeys.GitHub]
                        }/`}
                      >
                        {translate('learn_more')}
                      </DocumentationLink>
                    ),
                  }}
                />
              }
              jitSettings={
                <>
                  {Object.values(values).map((val) => {
                    if (!GITHUB_JIT_FIELDS.includes(val.key)) {
                      return null;
                    }
                    return (
                      <AuthenticationFormField
                        key={val.key}
                        settingValue={values[val.key]?.newValue ?? values[val.key]?.value}
                        definition={val.definition}
                        mandatory={val.mandatory}
                        onFieldChange={setNewValue}
                        isNotSet={val.isNotSet}
                      />
                    );
                  })}
                </>
              }
              autoTitle={translate('settings.authentication.github.form.provisioning_with_github')}
              hasDifferentProvider={hasDifferentProvider}
              hasFeatureEnabled={hasGithubProvisioning}
              autoFeatureDisabledText={
                <FormattedMessage
                  id="settings.authentication.github.form.provisioning.disabled"
                  defaultMessage={translate(
                    'settings.authentication.github.form.provisioning.disabled',
                  )}
                  values={{
                    documentation: (
                      <DocumentationLink to="/instance-administration/authentication/github">
                        {translate('documentation')}
                      </DocumentationLink>
                    ),
                  }}
                />
              }
              autoDescription={
                <FormattedMessage
                  id="settings.authentication.github.form.provisioning_with_github.description"
                  values={{
                    documentation: (
                      <DocumentationLink
                        to={`/instance-administration/authentication/${
                          DOCUMENTATION_LINK_SUFFIXES[AlmKeys.GitHub]
                        }/`}
                      >
                        {translate('learn_more')}
                      </DocumentationLink>
                    ),
                  }}
                />
              }
              synchronizationDetails={<GitHubSynchronisationWarning />}
              onSyncNow={synchronizeNow}
              canSync={canSyncNow}
              autoSettings={
                <>
                  {Object.values(values).map((val) => {
                    if (!GITHUB_PROVISIONING_FIELDS.includes(val.key)) {
                      return null;
                    }
                    return (
                      <div key={val.key}>
                        <AuthenticationFormField
                          settingValue={values[val.key]?.newValue ?? values[val.key]?.value}
                          definition={val.definition}
                          mandatory={val.mandatory}
                          onFieldChange={setNewValue}
                          isNotSet={val.isNotSet}
                        />
                      </div>
                    );
                  })}
                  <div className="sw-mt-6">
                    <div className="sw-flex">
                      <Highlight className="sw-mb-4 sw-mr-4 sw-flex sw-items-center sw-gap-2">
                        {translate(
                          'settings.authentication.github.configuration.roles_mapping.title',
                        )}
                      </Highlight>
                      <ButtonSecondary
                        className="sw--mt-2"
                        onClick={() => setShowMappingModal(true)}
                      >
                        {translate(
                          'settings.authentication.github.configuration.roles_mapping.button_label',
                        )}
                      </ButtonSecondary>
                    </div>
                    <Note className="sw-mt-2">
                      {translate(
                        'settings.authentication.github.configuration.roles_mapping.description',
                      )}
                    </Note>
                  </div>
                </>
              }
            />
            {showConfirmProvisioningModal && (
              <GitHubConfirmModal
                onConfirm={() => changeProvisioning()}
                onClose={() => setShowConfirmProvisioningModal(false)}
                values={values}
                hasGithubProvisioningTypeChange={hasGithubProvisioningTypeChange}
                provisioningStatus={provisioningStatus}
              />
            )}
            {showMappingModal && (
              <GitHubMappingModal
                mapping={rolesMapping}
                setMapping={setRolesMapping}
                onClose={() => setShowMappingModal(false)}
              />
            )}
          </>
        )}

        {showEditModal && (
          <ConfigurationForm
            tab={AlmKeys.GitHub}
            excludedField={GITHUB_EXCLUDED_FIELD}
            loading={isLoading}
            values={values}
            setNewValue={setNewValue}
            canBeSave={canBeSave}
            onClose={handleCloseConfiguration}
            create={!hasConfiguration}
            hasLegacyConfiguration={hasLegacyConfiguration}
            provisioningStatus={provisioningStatus}
          />
        )}

        {currentTab === AlmKeys.GitHub && <AutoProvisioningConsent />}
      </div>
    </Spinner>
  );
}
