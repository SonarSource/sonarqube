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

import { Link, Spinner } from '@sonarsource/echoes-react';
import { FormattedMessage, useIntl } from 'react-intl';
import { DarkLabel, FlagMessage, InputSelect } from '~design-system';
import { useAppState } from '../../../../app/components/app-state/withAppStateContext';
import { LabelValueSelectOption } from '../../../../helpers/search';
import { AlmKeys } from '../../../../types/alm-settings';

interface Props {
  almKey: AlmKeys;
  error: boolean;
  loadingOrganizations?: boolean;
  onSelectOrganization?: (organizationKey: string) => void;
  organizationOptions: LabelValueSelectOption[];
  selectedOrganization?: LabelValueSelectOption;
}

export function MonorepoOrganisationSelector({
  almKey,
  error,
  loadingOrganizations,
  onSelectOrganization,
  organizationOptions,
  selectedOrganization,
}: Readonly<Props>) {
  const { formatMessage } = useIntl();
  const { canAdmin } = useAppState();

  return (
    !error && (
      <>
        <DarkLabel htmlFor={`${almKey}-monorepo-choose-organization`} className="sw-mb-2">
          <FormattedMessage id="onboarding.create_project.monorepo.choose_organization" />
        </DarkLabel>

        <Spinner isLoading={loadingOrganizations && !error}>
          {organizationOptions.length > 0 ? (
            <InputSelect
              size="full"
              isSearchable
              inputId={`${almKey}-monorepo-choose-organization`}
              options={organizationOptions}
              onChange={({ value }: LabelValueSelectOption) => {
                if (onSelectOrganization) {
                  onSelectOrganization(value);
                }
              }}
              placeholder={formatMessage({
                id: 'onboarding.create_project.monorepo.choose_organization.placeholder',
              })}
              value={selectedOrganization}
            />
          ) : (
            !loadingOrganizations && (
              <FlagMessage variant="error" className="sw-mb-2">
                <span>
                  {canAdmin ? (
                    <FormattedMessage
                      id="onboarding.create_project.monorepo.no_orgs_admin"
                      defaultMessage={formatMessage({
                        id: 'onboarding.create_project.monorepo.no_orgs_admin',
                      })}
                      values={{
                        almKey,
                        link: (
                          <Link to="/admin/settings?category=almintegration">
                            <FormattedMessage id="onboarding.create_project.monorepo.warning.message_admin.link" />
                          </Link>
                        ),
                      }}
                    />
                  ) : (
                    <FormattedMessage id="onboarding.create_project.monorepo.no_orgs" />
                  )}
                </span>
              </FlagMessage>
            )
          )}
        </Spinner>
      </>
    )
  );
}
