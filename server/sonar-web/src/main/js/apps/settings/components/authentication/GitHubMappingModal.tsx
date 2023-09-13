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
import classNames from 'classnames';
import * as React from 'react';
import Checkbox from '../../../../components/controls/Checkbox';
import Modal from '../../../../components/controls/Modal';
import { SubmitButton } from '../../../../components/controls/buttons';
import PermissionHeader from '../../../../components/permissions/PermissionHeader';
import Spinner from '../../../../components/ui/Spinner';
import { translate } from '../../../../helpers/l10n';
import {
  PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE,
  convertToPermissionDefinitions,
  isPermissionDefinitionGroup,
} from '../../../../helpers/permissions';
import { useGithubRolesMappingQuery } from '../../../../queries/identity-provider';
import { GitHubMapping } from '../../../../types/provisioning';

interface Props {
  readonly mapping: GitHubMapping[] | null;
  readonly setMapping: React.Dispatch<React.SetStateAction<GitHubMapping[] | null>>;
  readonly onClose: () => void;
}

export default function GitHubMappingModal({ mapping, setMapping, onClose }: Props) {
  const { data: roles, isLoading } = useGithubRolesMappingQuery();
  const permissions = convertToPermissionDefinitions(
    PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE,
    'projects_role',
  );

  const header = translate(
    'settings.authentication.github.configuration.roles_mapping.dialog.title',
  );

  return (
    <Modal contentLabel={header} onRequestClose={onClose} shouldCloseOnEsc size="medium">
      <div className="modal-head">
        <h2>{header}</h2>
      </div>
      <div className="modal-body modal-container sw-p-0">
        <table className="data zebra permissions-table">
          <thead>
            <tr>
              <th scope="col" className="nowrap bordered-bottom sw-pl-[10px] sw-align-middle">
                <b>
                  {translate(
                    'settings.authentication.github.configuration.roles_mapping.dialog.roles_column',
                  )}
                </b>
              </th>
              {permissions.map((permission) => (
                <PermissionHeader
                  key={
                    isPermissionDefinitionGroup(permission) ? permission.category : permission.key
                  }
                  permission={permission}
                />
              ))}
            </tr>
          </thead>
          <tbody>
            {(mapping ?? roles)?.map(({ id, roleName, permissions }) => (
              <tr key={id}>
                <th scope="row" className="nowrap text-middle sw-pl-[10px]">
                  <b>{roleName}</b>
                </th>
                {Object.entries(permissions).map(([key, value]) => (
                  <td key={key} className={classNames('permission-column text-center text-middle')}>
                    <Checkbox
                      checked={value}
                      onCheck={(newValue) =>
                        setMapping(
                          (mapping ?? roles)?.map((item) =>
                            item.id === id
                              ? { ...item, permissions: { ...item.permissions, [key]: newValue } }
                              : item,
                          ) ?? null,
                        )
                      }
                    />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
        <Spinner loading={isLoading} />
      </div>
      <div className="modal-foot">
        <SubmitButton onClick={onClose}>{translate('close')}</SubmitButton>
      </div>
    </Modal>
  );
}
