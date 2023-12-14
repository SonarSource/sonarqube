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
import * as React from 'react';
import Checkbox from '../../../../components/controls/Checkbox';
import Modal from '../../../../components/controls/Modal';
import { DeleteButton, SubmitButton } from '../../../../components/controls/buttons';
import PermissionHeader from '../../../../components/permissions/PermissionHeader';
import { Alert } from '../../../../components/ui/Alert';
import Spinner from '../../../../components/ui/Spinner';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import {
  PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE,
  convertToPermissionDefinitions,
  isPermissionDefinitionGroup,
} from '../../../../helpers/permissions';
import { useGithubRolesMappingQuery } from '../../../../queries/identity-provider/github';
import { GitHubMapping } from '../../../../types/provisioning';

interface Props {
  mapping: GitHubMapping[] | null;
  setMapping: React.Dispatch<React.SetStateAction<GitHubMapping[] | null>>;
  onClose: () => void;
}

interface PermissionCellProps {
  mapping: GitHubMapping;
  setMapping: React.Dispatch<React.SetStateAction<GitHubMapping[] | null>>;
  list?: GitHubMapping[];
}

const DEFAULT_CUSTOM_ROLE_PERMISSIONS: GitHubMapping['permissions'] = {
  user: true,
  codeViewer: false,
  issueAdmin: false,
  securityHotspotAdmin: false,
  admin: false,
  scan: false,
};

function PermissionRow(props: Readonly<PermissionCellProps>) {
  const { mapping, list } = props;

  return (
    <tr>
      <th scope="row" className="nowrap text-middle sw-pl-[10px]">
        <div className="sw-flex sw-max-w-[150px] sw-items-center">
          <b
            className={mapping.isBaseRole ? 'sw-capitalize' : 'sw-truncate'}
            title={mapping.githubRole}
          >
            {mapping.githubRole}
          </b>
          {!mapping.isBaseRole && (
            <DeleteButton
              className="sw-ml-1"
              aria-label={translateWithParameters(
                'settings.authentication.github.configuration.roles_mapping.dialog.delete_custom_role',
                mapping.githubRole,
              )}
              onClick={() => {
                props.setMapping(list?.filter((r) => r.githubRole !== mapping.githubRole) ?? null);
              }}
            />
          )}
        </div>
      </th>
      {Object.entries(mapping.permissions).map(([key, value]) => (
        <td key={key} className="permission-column text-center text-middle">
          <Checkbox
            checked={value}
            onCheck={(newValue) =>
              props.setMapping(
                list?.map((item) =>
                  item.id === mapping.id
                    ? { ...item, permissions: { ...item.permissions, [key]: newValue } }
                    : item,
                ) ?? null,
              )
            }
          />
        </td>
      ))}
    </tr>
  );
}

export default function GitHubMappingModal({ mapping, setMapping, onClose }: Readonly<Props>) {
  const { data: roles, isLoading } = useGithubRolesMappingQuery();
  const permissions = convertToPermissionDefinitions(
    PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE,
    'projects_role',
  );
  const [customRoleInput, setCustomRoleInput] = React.useState('');
  const [customRoleError, setCustomRoleError] = React.useState(false);

  const header = translate(
    'settings.authentication.github.configuration.roles_mapping.dialog.title',
  );

  const list = mapping ?? roles;

  const validateAndAddCustomRole = (e: React.FormEvent) => {
    e.preventDefault();
    const value = customRoleInput.trim();
    if (
      !list?.some((el) =>
        el.isBaseRole
          ? el.githubRole.toLowerCase() === value.toLowerCase()
          : el.githubRole === value,
      )
    ) {
      setMapping([
        {
          id: customRoleInput,
          githubRole: customRoleInput,
          permissions: { ...DEFAULT_CUSTOM_ROLE_PERMISSIONS },
        },
        ...(list ?? []),
      ]);
      setCustomRoleInput('');
    } else {
      setCustomRoleError(true);
    }
  };

  const haveEmptyCustomRoles = !!mapping?.some(
    (el) => !el.isBaseRole && !Object.values(el.permissions).some(Boolean),
  );

  return (
    <Modal
      contentLabel={header}
      onRequestClose={onClose}
      shouldCloseOnOverlayClick={false}
      shouldCloseOnEsc={false}
      size="medium"
    >
      <div className="modal-head">
        <h2>{header}</h2>
      </div>
      <div className="modal-body modal-container sw-p-0">
        <table className="data zebra permissions-table">
          <thead>
            <tr className="sw-sticky sw-top-0 sw-bg-white sw-z-filterbar">
              <th
                scope="col"
                className="nowrap bordered-bottom sw-pl-[10px] sw-align-middle sw-w-[150px]"
              >
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
            {list
              ?.filter((r) => r.isBaseRole)
              .map((mapping) => (
                <PermissionRow
                  key={mapping.id}
                  mapping={mapping}
                  setMapping={setMapping}
                  list={list}
                />
              ))}
            <tr>
              <td colSpan={7} className="sw-pt-2 sw-border-t">
                <form
                  className="sw-flex sw-h-9 sw-items-center"
                  onSubmit={validateAndAddCustomRole}
                >
                  <label htmlFor="custom-role-input">
                    {translate(
                      'settings.authentication.github.configuration.roles_mapping.dialog.add_custom_role',
                    )}
                  </label>
                  <input
                    className="sw-w-[300px] sw-mx-2"
                    id="custom-role-input"
                    maxLength={4000}
                    value={customRoleInput}
                    onChange={(event) => {
                      setCustomRoleError(false);
                      setCustomRoleInput(event.currentTarget.value);
                    }}
                    type="text"
                  />
                  <SubmitButton disabled={!customRoleInput.trim() || customRoleError}>
                    {translate('add_verb')}
                  </SubmitButton>
                  <Alert variant="error" className="sw-inline-block sw-ml-2 sw-mb-0">
                    {customRoleError &&
                      translate(
                        'settings.authentication.github.configuration.roles_mapping.role_exists',
                      )}
                  </Alert>
                </form>
              </td>
            </tr>
            {list
              ?.filter((r) => !r.isBaseRole)
              .map((mapping) => (
                <PermissionRow
                  key={mapping.id}
                  mapping={mapping}
                  setMapping={setMapping}
                  list={list}
                />
              ))}
          </tbody>
        </table>
        <Spinner loading={isLoading} />
        <div className="sw-bg-white sw-bottom-0 sw-sticky">
          <Alert variant="info" className="sw-m-2">
            {translate(
              'settings.authentication.github.configuration.roles_mapping.dialog.custom_roles_description',
            )}
          </Alert>
        </div>
      </div>
      <div className="modal-foot">
        <div className="sw-flex sw-items-center sw-justify-end sw-h-8">
          <Alert variant="error" className="sw-inline-block sw-mb-0 sw-mr-2">
            {haveEmptyCustomRoles &&
              translate(
                'settings.authentication.github.configuration.roles_mapping.empty_custom_role',
              )}
          </Alert>
          <SubmitButton disabled={haveEmptyCustomRoles} onClick={onClose}>
            {translate('close')}
          </SubmitButton>
        </div>
      </div>
    </Modal>
  );
}
