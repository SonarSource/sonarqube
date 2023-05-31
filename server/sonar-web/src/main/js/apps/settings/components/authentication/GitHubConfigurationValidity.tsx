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
import React, { useEffect, useState } from 'react';
import theme from '../../../../app/theme';
import Modal from '../../../../components/controls/Modal';
import { Button } from '../../../../components/controls/buttons';
import CheckIcon from '../../../../components/icons/CheckIcon';
import ClearIcon from '../../../../components/icons/ClearIcon';
import { Alert, AlertVariant } from '../../../../components/ui/Alert';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { GitHubProvisioningStatus } from '../../../../types/provisioning';
import { useCheckGitHubConfigQuery } from './queries/identity-provider';

const intlPrefix = 'settings.authentication.github.configuration.validation';

function ValidityIcon({ valid }: { valid: boolean }) {
  const color = valid ? theme.colors.success500 : theme.colors.error500;

  return valid ? (
    <CheckIcon fill={color} label={translate(`${intlPrefix}.details.valid_label`)} />
  ) : (
    <ClearIcon fill={color} label={translate(`${intlPrefix}.details.invalid_label`)} />
  );
}

interface Props {
  isAutoProvisioning: boolean;
}

function GitHubConfigurationValidity({ isAutoProvisioning }: Props) {
  const [openDetails, setOpenDetails] = useState(false);
  const [messages, setMessages] = useState<string[]>([]);
  const [alertVariant, setAlertVariant] = useState<AlertVariant>('loading');
  const { data, isFetching, refetch } = useCheckGitHubConfigQuery();
  const modalHeader = translate(`${intlPrefix}.details.title`);

  const applicationField = isAutoProvisioning ? 'autoProvisioning' : 'jit';

  const isValidApp =
    data?.application[applicationField].status === GitHubProvisioningStatus.Success;

  useEffect(() => {
    if (isFetching) {
      setMessages([translate(`${intlPrefix}.loading`)]);
      setAlertVariant('loading');
      return;
    }

    const invalidOrgs =
      isValidApp && isAutoProvisioning && data
        ? data.installations.filter(
            (org) => org.autoProvisioning.status === GitHubProvisioningStatus.Failed
          )
        : [];

    if (isValidApp && invalidOrgs.length === 0) {
      setMessages([
        translateWithParameters(
          `${intlPrefix}.valid${data.installations.length === 1 ? '_one' : ''}`,
          isAutoProvisioning
            ? translate('settings.authentication.github.form.provisioning_with_github_short')
            : translate('settings.authentication.form.provisioning_at_login_short'),
          data.installations.length === 1
            ? data.installations[0].organization
            : data.installations.length
        ),
      ]);
      setAlertVariant('success');
    } else {
      setMessages([
        translateWithParameters(
          `${intlPrefix}.invalid`,
          data?.application[applicationField].errorMessage ?? ''
        ),
        ...invalidOrgs.map((org) =>
          translateWithParameters(
            `${intlPrefix}.invalid_org`,
            org.organization,
            org.autoProvisioning.errorMessage ?? ''
          )
        ),
      ]);
      setAlertVariant('error');
    }
  }, [isFetching, isValidApp, isAutoProvisioning, applicationField, data]);

  return (
    <>
      <Alert title={messages[0]} variant={alertVariant}>
        <div className="sw-flex sw-justify-between sw-items-center">
          <div>
            {messages.map((msg) => (
              <div key={msg}>{msg}</div>
            ))}
          </div>
          <div>
            <Button onClick={() => setOpenDetails(true)} disabled={isFetching} className="sw-mr-2">
              {translate(`${intlPrefix}.details`)}
            </Button>
            <Button onClick={() => refetch()} disabled={isFetching}>
              {translate(`${intlPrefix}.test`)}
            </Button>
          </div>
        </div>
      </Alert>
      {openDetails && (
        <Modal size="small" contentLabel={modalHeader} onRequestClose={() => setOpenDetails(false)}>
          <header className="modal-head">
            <h2>
              {modalHeader} <ValidityIcon valid={isValidApp} />
            </h2>
          </header>
          <div className="modal-body modal-container">
            {!isValidApp && (
              <Alert variant="error">{data?.application[applicationField].errorMessage}</Alert>
            )}
            <ul className="sw-pl-5">
              {data?.installations.map((inst) => (
                <li key={inst.organization}>
                  <ValidityIcon
                    valid={
                      !isAutoProvisioning ||
                      inst.autoProvisioning.status === GitHubProvisioningStatus.Success
                    }
                  />
                  <span className="sw-ml-2">{inst.organization}</span>
                  {isAutoProvisioning &&
                    inst.autoProvisioning.status === GitHubProvisioningStatus.Failed && (
                      <span> - {inst.autoProvisioning.errorMessage}</span>
                    )}
                </li>
              ))}
            </ul>
          </div>
          <footer className="modal-foot">
            <Button onClick={() => setOpenDetails(false)}>{translate('close')}</Button>
          </footer>
        </Modal>
      )}
    </>
  );
}

export default GitHubConfigurationValidity;
