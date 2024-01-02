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
import React from 'react';
import { FormattedMessage } from 'react-intl';
import DocLink from '../../../../components/common/DocLink';
import Link from '../../../../components/common/Link';
import ConfirmModal from '../../../../components/controls/ConfirmModal';
import RadioCard from '../../../../components/controls/RadioCard';
import { Button, ResetButtonLink, SubmitButton } from '../../../../components/controls/buttons';
import CheckIcon from '../../../../components/icons/CheckIcon';
import DeleteIcon from '../../../../components/icons/DeleteIcon';
import EditIcon from '../../../../components/icons/EditIcon';
import { Alert } from '../../../../components/ui/Alert';
import Spinner from '../../../../components/ui/Spinner';
import { translate } from '../../../../helpers/l10n';
import { useIdentityProviderQuery } from '../../../../queries/identity-provider/common';
import { useToggleScimMutation } from '../../../../queries/identity-provider/scim';
import { useSaveValueMutation } from '../../../../queries/settings';
import { ExtendedSettingDefinition } from '../../../../types/settings';
import { Provider } from '../../../../types/types';
import ConfigurationForm from './ConfigurationForm';
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
    deleteMutation: { isLoading: isDeleting, mutate: deleteConfiguration },
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
      <div className="authentication-configuration">
        <div className="spacer-bottom display-flex-space-between display-flex-center">
          <h4>{translate('settings.authentication.saml.configuration')}</h4>

          {!hasConfiguration && (
            <div>
              <Button onClick={handleCreateConfiguration}>
                {translate('settings.authentication.form.create')}
              </Button>
            </div>
          )}
        </div>
        {!hasConfiguration && (
          <div className="big-padded text-center huge-spacer-bottom authentication-no-config">
            {translate('settings.authentication.saml.form.not_configured')}
          </div>
        )}

        {hasConfiguration && (
          <>
            <div className="spacer-bottom big-padded bordered display-flex-space-between">
              <div>
                <h5>{name}</h5>
                <p>{url}</p>
                <p className="big-spacer-top big-spacer-bottom">
                  {samlEnabled ? (
                    <span className="authentication-enabled spacer-left">
                      <CheckIcon className="spacer-right" />
                      {translate('settings.authentication.form.enabled')}
                    </span>
                  ) : (
                    translate('settings.authentication.form.not_enabled')
                  )}
                </p>
                <Button className="spacer-top" disabled={scimStatus} onClick={handleToggleEnable}>
                  {samlEnabled
                    ? translate('settings.authentication.form.disable')
                    : translate('settings.authentication.form.enable')}
                </Button>
              </div>
              <div>
                <Link className="button spacer-right" target="_blank" to={CONFIG_TEST_PATH}>
                  {translate('settings.authentication.saml.form.test')}
                </Link>
                <Button className="spacer-right" onClick={handleCreateConfiguration}>
                  <EditIcon />
                  {translate('settings.authentication.form.edit')}
                </Button>
                <Button
                  className="button-red"
                  disabled={samlEnabled || isDeleting}
                  onClick={deleteConfiguration}
                >
                  <DeleteIcon />
                  {translate('settings.authentication.form.delete')}
                </Button>
              </div>
            </div>
            <div className="spacer-bottom big-padded bordered display-flex-space-between">
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  if (hasScimTypeChange) {
                    setShowConfirmProvisioningModal(true);
                  } else {
                    handleSaveGroup();
                  }
                }}
              >
                <fieldset className="display-flex-column big-spacer-bottom">
                  <label className="h5">
                    {translate('settings.authentication.form.provisioning')}
                  </label>
                  {samlEnabled ? (
                    <div className="display-flex-column spacer-top">
                      <RadioCard
                        className="sw-min-h-0"
                        label={translate('settings.authentication.saml.form.provisioning_at_login')}
                        title={translate('settings.authentication.saml.form.provisioning_at_login')}
                        selected={!(newScimStatus ?? scimStatus)}
                        onClick={() => setNewScimStatus(false)}
                      >
                        <p>
                          {translate('settings.authentication.saml.form.provisioning_at_login.sub')}
                        </p>
                      </RadioCard>
                      <RadioCard
                        className="spacer-top sw-min-h-0"
                        label={translate(
                          'settings.authentication.saml.form.provisioning_with_scim',
                        )}
                        title={translate(
                          'settings.authentication.saml.form.provisioning_with_scim',
                        )}
                        selected={newScimStatus ?? scimStatus}
                        onClick={() => setNewScimStatus(true)}
                        disabled={!hasScim || hasDifferentProvider}
                      >
                        {!hasScim ? (
                          <p>
                            <FormattedMessage
                              id="settings.authentication.saml.form.provisioning.disabled"
                              values={{
                                documentation: (
                                  <DocLink to="/instance-administration/authentication/saml/scim/overview">
                                    {translate('documentation')}
                                  </DocLink>
                                ),
                              }}
                            />
                          </p>
                        ) : (
                          <>
                            {hasDifferentProvider && (
                              <p className="spacer-bottom text-bold">
                                {translate(
                                  'settings.authentication.form.other_provisioning_enabled',
                                )}
                              </p>
                            )}
                            <p className="spacer-bottom ">
                              {translate(
                                'settings.authentication.saml.form.provisioning_with_scim.sub',
                              )}
                            </p>
                            <p className="spacer-bottom ">
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
                                    <DocLink to="/instance-administration/authentication/saml/scim/overview">
                                      {translate('documentation')}
                                    </DocLink>
                                  ),
                                }}
                              />
                            </p>
                          </>
                        )}
                      </RadioCard>
                    </div>
                  ) : (
                    <Alert className="big-spacer-top" variant="info">
                      {translate('settings.authentication.saml.enable_first')}
                    </Alert>
                  )}
                </fieldset>
                {samlEnabled && (
                  <>
                    <SubmitButton disabled={!hasScimConfigChange}>{translate('save')}</SubmitButton>
                    <ResetButtonLink
                      className="spacer-left"
                      onClick={() => {
                        setNewScimStatus(undefined);
                        setNewGroupSetting();
                      }}
                      disabled={!hasScimConfigChange}
                    >
                      {translate('cancel')}
                    </ResetButtonLink>
                  </>
                )}
                {showConfirmProvisioningModal && (
                  <ConfirmModal
                    onConfirm={() => handleConfirmChangeProvisioning()}
                    header={translate(
                      'settings.authentication.saml.confirm',
                      newScimStatus ? 'scim' : 'jit',
                    )}
                    onClose={() => setShowConfirmProvisioningModal(false)}
                    isDestructive={!newScimStatus}
                    confirmButtonText={translate('yes')}
                  >
                    {translate(
                      'settings.authentication.saml.confirm',
                      newScimStatus ? 'scim' : 'jit',
                      'description',
                    )}
                  </ConfirmModal>
                )}
              </form>
            </div>
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
