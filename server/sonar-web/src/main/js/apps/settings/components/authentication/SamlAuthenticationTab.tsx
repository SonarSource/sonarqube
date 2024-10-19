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

import { ButtonSecondary, Spinner } from 'design-system';
import React, { FormEvent } from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../../components/common/DocumentationLink';
import ConfirmModal from '../../../../components/controls/ConfirmModal';
import { DocLink } from '../../../../helpers/doc-links';
import { translate } from '../../../../helpers/l10n';
import { useIdentityProviderQuery } from '../../../../queries/identity-provider/common';
import { useToggleScimMutation } from '../../../../queries/identity-provider/scim';
import { useSaveValueMutation } from '../../../../queries/settings';
import { ProvisioningType } from '../../../../types/provisioning';
import { ExtendedSettingDefinition } from '../../../../types/settings';
import { Provider } from '../../../../types/types';
import ConfigurationDetails from './ConfigurationDetails';
import ConfigurationForm from './ConfigurationForm';
import ProvisioningSection from './ProvisioningSection';
import TabHeader from './TabHeader';
import useSamlConfiguration, {
  SAML_ENABLED_FIELD,
  SAML_SCIM_DEPRECATED,
} from './hook/useSamlConfiguration';

interface SamlAuthenticationProps {
  definitions: ExtendedSettingDefinition[];
}

export const SAML = 'saml';

const CONFIG_TEST_PATH = '/saml/validation_init';
const SAML_EXCLUDED_FIELD = [SAML_ENABLED_FIELD, SAML_SCIM_DEPRECATED];

export default function SamlAuthenticationTab(props: SamlAuthenticationProps) {
  const { definitions } = props;
  const [showEditModal, setShowEditModal] = React.useState(false);
  const [showConfirmProvisioningModal, setShowConfirmProvisioningModal] = React.useState(false);
  const {
    hasScim,
    scimStatus,
    isLoading,
    samlEnabled,
    name,
    groupValue,
    url,
    hasConfiguration,
    values,
    setNewValue,
    canBeSave,
    hasScimTypeChange,
    hasScimConfigChange,
    newScimStatus,
    setNewScimStatus,
    setNewGroupSetting,
    deleteMutation: { isPending: isDeleting, mutate: deleteConfiguration },
  } = useSamlConfiguration(definitions);
  const toggleScim = useToggleScimMutation();

  const { data } = useIdentityProviderQuery();
  const { mutate: saveSetting } = useSaveValueMutation();

  const hasDifferentProvider = data?.provider !== undefined && data.provider !== Provider.Scim;

  const handleCreateConfiguration = () => {
    setShowEditModal(true);
  };

  const handleCancelConfiguration = () => {
    setShowEditModal(false);
  };

  const handleToggleEnable = () => {
    const value = values[SAML_ENABLED_FIELD];
    saveSetting({ newValue: !samlEnabled, definition: value.definition });
  };

  const handleSaveGroup = () => {
    if (groupValue.newValue !== undefined) {
      saveSetting({ newValue: groupValue.newValue, definition: groupValue.definition });
    }
  };

  const handleConfirmChangeProvisioning = async () => {
    await toggleScim.mutateAsync(!!newScimStatus);
    if (!newScimStatus) {
      handleSaveGroup();
    }
  };

  return (
    <Spinner loading={isLoading}>
      <div>
        <TabHeader
          title={translate('settings.authentication.saml.configuration')}
          showCreate={!hasConfiguration}
          onCreate={handleCreateConfiguration}
        />

        {!hasConfiguration && (
          <div>{translate('settings.authentication.saml.form.not_configured')}</div>
        )}

        {hasConfiguration && (
          <>
            <ConfigurationDetails
              title={name?.toString() ?? ''}
              url={url}
              canDisable={!scimStatus}
              enabled={samlEnabled}
              isDeleting={isDeleting}
              onEdit={handleCreateConfiguration}
              onDelete={deleteConfiguration}
              onToggle={handleToggleEnable}
              extraActions={
                <ButtonSecondary target="_blank" to={CONFIG_TEST_PATH}>
                  {translate('settings.authentication.saml.form.test')}
                </ButtonSecondary>
              }
            />
            <ProvisioningSection
              provisioningType={
                (newScimStatus ?? scimStatus) ? ProvisioningType.auto : ProvisioningType.jit
              }
              onChangeProvisioningType={(val: ProvisioningType) =>
                setNewScimStatus(val === ProvisioningType.auto)
              }
              disabledConfigText={translate('settings.authentication.saml.enable_first')}
              enabled={samlEnabled}
              hasUnsavedChanges={hasScimConfigChange}
              onSave={(e: FormEvent) => {
                e.preventDefault();
                if (hasScimTypeChange) {
                  setShowConfirmProvisioningModal(true);
                } else {
                  handleSaveGroup();
                }
              }}
              onCancel={() => {
                setNewScimStatus(undefined);
                setNewGroupSetting();
              }}
              jitTitle={translate('settings.authentication.saml.form.provisioning_at_login')}
              jitDescription={translate(
                'settings.authentication.saml.form.provisioning_at_login.sub',
              )}
              autoTitle={translate('settings.authentication.saml.form.provisioning_with_scim')}
              hasDifferentProvider={hasDifferentProvider}
              hasFeatureEnabled={hasScim}
              autoFeatureDisabledText={
                <FormattedMessage
                  id="settings.authentication.saml.form.provisioning.disabled"
                  values={{
                    documentation: (
                      <DocumentationLink to={DocLink.AlmSamlScimAuth}>
                        {translate('documentation')}
                      </DocumentationLink>
                    ),
                  }}
                />
              }
              autoDescription={
                <>
                  <p className="sw-mb-2">
                    {translate('settings.authentication.saml.form.provisioning_with_scim.sub')}
                  </p>
                  <p className="sw-mb-2">
                    {translate(
                      'settings.authentication.saml.form.provisioning_with_scim.description',
                    )}
                  </p>
                  <p>
                    <FormattedMessage
                      id="settings.authentication.saml.form.provisioning_with_scim.description.doc"
                      defaultMessage={translate(
                        'settings.authentication.saml.form.provisioning_with_scim.description.doc',
                      )}
                      values={{
                        documentation: (
                          <DocumentationLink to={DocLink.AlmSamlScimAuth}>
                            {translate('documentation')}
                          </DocumentationLink>
                        ),
                      }}
                    />
                  </p>
                </>
              }
            />
            <ConfirmModal
              onConfirm={() => handleConfirmChangeProvisioning()}
              header={translate(
                'settings.authentication.saml.confirm',
                newScimStatus ? 'scim' : 'jit',
              )}
              onClose={() => setShowConfirmProvisioningModal(false)}
              isDestructive={!newScimStatus}
              isOpen={showConfirmProvisioningModal}
              confirmButtonText={translate('yes')}
            >
              {translate(
                'settings.authentication.saml.confirm',
                newScimStatus ? 'scim' : 'jit',
                'description',
              )}
            </ConfirmModal>
          </>
        )}
        {showEditModal && (
          <ConfigurationForm
            tab={SAML}
            excludedField={SAML_EXCLUDED_FIELD}
            loading={isLoading}
            values={values}
            setNewValue={setNewValue}
            canBeSave={canBeSave}
            onClose={handleCancelConfiguration}
            create={!hasConfiguration}
          />
        )}
      </div>
    </Spinner>
  );
}
