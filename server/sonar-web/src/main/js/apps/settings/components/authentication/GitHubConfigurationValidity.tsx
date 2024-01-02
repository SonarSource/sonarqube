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
import { TextMuted } from 'design-system';
import React, { useEffect, useState } from 'react';
import theme, { colors } from '../../../../app/theme';
import Modal from '../../../../components/controls/Modal';
import { Button } from '../../../../components/controls/buttons';
import CheckIcon from '../../../../components/icons/CheckIcon';
import ClearIcon from '../../../../components/icons/ClearIcon';
import HelpIcon from '../../../../components/icons/HelpIcon';
import { Alert, AlertVariant } from '../../../../components/ui/Alert';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { useCheckGitHubConfigQuery } from '../../../../queries/identity-provider/github';
import { GitHubProvisioningStatus } from '../../../../types/provisioning';

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
  selectedOrganizations: string[];
}

export default function GitHubConfigurationValidity({
  isAutoProvisioning,
  selectedOrganizations,
}: Props) {
  const [openDetails, setOpenDetails] = useState(false);
  const [messages, setMessages] = useState<string[]>([]);
  const [alertVariant, setAlertVariant] = useState<AlertVariant>('loading');
  const { data, isFetching, refetch } = useCheckGitHubConfigQuery(true);
  const modalHeader = translate(`${intlPrefix}.details.title`);

  const applicationField = isAutoProvisioning ? 'autoProvisioning' : 'jit';

  const isValidApp =
    data?.application[applicationField].status === GitHubProvisioningStatus.Success;

  const failedOrgs = selectedOrganizations.filter((o) => {
    return !data?.installations.find((i) => i.organization === o);
  });

  useEffect(() => {
    if (isFetching) {
      setMessages([translate(`${intlPrefix}.loading`)]);
      setAlertVariant('loading');
      return;
    }

    const invalidOrgs =
      isValidApp && data
        ? data.installations.filter(
            (org) => org[applicationField].status === GitHubProvisioningStatus.Failed,
          )
        : [];

    const invalidOrgsMessages = invalidOrgs.map((org) =>
      translateWithParameters(
        `${intlPrefix}.invalid_org`,
        org.organization,
        org[applicationField].errorMessage ?? '',
      ),
    );

    if (isValidApp && invalidOrgs.length === 0) {
      setMessages([
        translateWithParameters(
          `${intlPrefix}.valid${data.installations.length === 1 ? '_one' : ''}`,
          translate(
            `settings.authentication.github.form.provisioning_with_github_short.${applicationField}`,
          ),
          data.installations.length === 1
            ? data.installations[0].organization
            : data.installations.length,
        ),
      ]);
      setAlertVariant('success');
    } else if (isValidApp && !isAutoProvisioning) {
      setMessages([translate(`${intlPrefix}.valid.short`), ...invalidOrgsMessages]);
      setAlertVariant('warning');
    } else {
      setMessages([
        translateWithParameters(
          `${intlPrefix}.invalid`,
          data?.application[applicationField].errorMessage ?? '',
        ),
        ...invalidOrgsMessages,
      ]);
      setAlertVariant('error');
    }
  }, [isFetching, isValidApp, isAutoProvisioning, applicationField, data]);

  return (
    <>
      <Alert
        title={messages[0]}
        variant={alertVariant}
        aria-live="polite"
        role="status"
        aria-atomic
        aria-busy={isFetching}
      >
        <div className="sw-flex sw-justify-between sw-items-center">
          <div>
            {messages.map((msg) => (
              <div key={msg}>{msg}</div>
            ))}
          </div>
          <div className="sw-flex">
            <Button
              onClick={() => setOpenDetails(true)}
              disabled={isFetching}
              className="sw-mr-2 sw-whitespace-nowrap sw-text-center"
            >
              {translate(`${intlPrefix}.details`)}
            </Button>
            <Button
              onClick={() => refetch()}
              disabled={isFetching}
              className="sw-whitespace-nowrap sw-text-center"
            >
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
                    valid={inst[applicationField].status === GitHubProvisioningStatus.Success}
                  />
                  <span className="sw-ml-2">{inst.organization}</span>
                  {inst[applicationField].status === GitHubProvisioningStatus.Failed && (
                    <span> - {inst[applicationField].errorMessage}</span>
                  )}
                </li>
              ))}
              {failedOrgs.map((fo) => (
                <li key={fo}>
                  <HelpIcon fillInner={colors.gray60} fill={colors.white} role="img" />
                  <TextMuted
                    className="sw-ml-2"
                    text={translateWithParameters(`${intlPrefix}.details.org_not_found`, fo)}
                  />
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
