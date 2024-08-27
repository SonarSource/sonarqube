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
import {
  ButtonSecondary,
  Checkbox,
  ContentCell,
  DestructiveIcon,
  FlagMessage,
  FormField,
  InputField,
  Modal,
  Table,
  TableRow,
  TableRowInteractive,
  TrashIcon,
} from 'design-system';
import * as React from 'react';
import PermissionHeader from '../../../../components/permissions/PermissionHeader';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import {
  PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE,
  convertToPermissionDefinitions,
  isPermissionDefinitionGroup,
} from '../../../../helpers/permissions';
import { AlmKeys } from '../../../../types/alm-settings';
import { GitHubMapping, GitLabMapping } from '../../../../types/provisioning';

type RolesMapping = GitHubMapping[] | GitLabMapping[] | null;

interface Props {
  isLoading: boolean;
  mapping: RolesMapping;
  mappingFor: AlmKeys.GitHub | AlmKeys.GitLab;
  onClose: () => void;
  roles?: RolesMapping;
  setMapping: React.Dispatch<React.SetStateAction<RolesMapping>>;
}

interface PermissionCellProps extends Pick<Props, 'setMapping'> {
  list?: GitHubMapping[] | GitLabMapping[];
  mapping: GitHubMapping | GitLabMapping;
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
  const { list, mapping } = props;
  const isGitHubMapping = 'githubRole' in mapping;
  const role = isGitHubMapping ? mapping.githubRole : mapping.gitlabRole;
  const isBaseRole = mapping.baseRole;

  const setMapping = () => {
    if (isGitHubMapping) {
      return props.setMapping(
        (list as GitHubMapping[])?.filter((r) => r.githubRole !== role) ?? null,
      );
    }
    return props.setMapping(
      (list as GitLabMapping[])?.filter((r) => r.gitlabRole !== role) ?? null,
    );
  };

  return (
    <TableRowInteractive>
      <ContentCell scope="row" className="sw-whitespace-nowrap">
        <div className="sw-flex sw-max-w-[330px] sw-items-center">
          <b className={isBaseRole ? 'sw-capitalize' : 'sw-truncate'} title={role}>
            {role}
          </b>

          {!isBaseRole && (
            <DestructiveIcon
              className="sw-ml-1"
              aria-label={translateWithParameters(
                'settings.authentication.configuration.roles_mapping.dialog.delete_custom_role',
                role,
              )}
              onClick={setMapping}
              Icon={TrashIcon}
              size="small"
            />
          )}
        </div>
      </ContentCell>
      {Object.entries(mapping.permissions).map(([key, value]) => (
        <ContentCell key={key} className="sw-justify-center">
          <Checkbox
            checked={value}
            onCheck={(newValue) =>
              props.setMapping(
                (list?.map((item) =>
                  item.id === mapping.id
                    ? { ...item, permissions: { ...item.permissions, [key]: newValue } }
                    : item,
                ) ?? null) as RolesMapping,
              )
            }
          />
        </ContentCell>
      ))}
    </TableRowInteractive>
  );
}

export function DevopsRolesMappingModal(props: Readonly<Props>) {
  const { isLoading, mapping, mappingFor, onClose, roles, setMapping } = props;
  const permissions = convertToPermissionDefinitions(
    PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE,
    'projects_role',
  );
  const [customRoleInput, setCustomRoleInput] = React.useState('');
  const [customRoleError, setCustomRoleError] = React.useState(false);

  const header = translateWithParameters(
    'settings.authentication.configuration.roles_mapping.dialog.title',
    translate('alm', mappingFor),
  );

  const list = mapping ?? roles;

  const validateAndAddCustomRole = (e: React.FormEvent) => {
    e.preventDefault();
    const value = customRoleInput.trim();
    if (
      mappingFor === AlmKeys.GitHub &&
      !(list as GitHubMapping[])?.some((el) =>
        el.baseRole ? el.githubRole.toLowerCase() === value.toLowerCase() : el.githubRole === value,
      )
    ) {
      setMapping([
        {
          id: customRoleInput,
          githubRole: customRoleInput,
          permissions: { ...DEFAULT_CUSTOM_ROLE_PERMISSIONS },
        },
        ...((list as GitHubMapping[]) ?? []),
      ]);
      setCustomRoleInput('');
    } else if (
      mappingFor === AlmKeys.GitLab &&
      !(list as GitLabMapping[])?.some((el) =>
        el.baseRole ? el.gitlabRole.toLowerCase() === value.toLowerCase() : el.gitlabRole === value,
      )
    ) {
      setMapping([
        {
          id: customRoleInput,
          gitlabRole: customRoleInput,
          permissions: { ...DEFAULT_CUSTOM_ROLE_PERMISSIONS },
        },
        ...((list as GitLabMapping[]) ?? []),
      ]);
      setCustomRoleInput('');
    } else {
      setCustomRoleError(true);
    }
  };

  const haveEmptyCustomRoles = !!mapping?.some(
    (el) => !el.baseRole && !Object.values(el.permissions).some(Boolean),
  );

  const formBody = (
    <div className="sw-p-0">
      <Table
        noHeaderTopBorder
        columnCount={permissions.length + 1}
        columnWidths={['auto', ...Array(permissions.length).fill('1%')]}
        header={
          <TableRow className="sw-sticky sw-top-0 sw-bg-white">
            <ContentCell className="sw-whitespace-nowrap">
              {translate('settings.authentication.configuration.roles_mapping.dialog.roles_column')}
            </ContentCell>
            {permissions.map((permission) => (
              <PermissionHeader
                key={isPermissionDefinitionGroup(permission) ? permission.category : permission.key}
                permission={permission}
              />
            ))}
          </TableRow>
        }
      >
        {list
          ?.filter((r) => r.baseRole)
          .map((mapping) => (
            <PermissionRow key={mapping.id} mapping={mapping} setMapping={setMapping} list={list} />
          ))}
        <TableRow>
          <ContentCell colSpan={7}>
            <div className="sw-flex sw-items-end">
              <form className="sw-flex sw-items-end" onSubmit={validateAndAddCustomRole}>
                <FormField
                  htmlFor="custom-role-input"
                  label={translate(
                    'settings.authentication.configuration.roles_mapping.dialog.add_custom_role',
                  )}
                >
                  <InputField
                    className="sw-w-[300px]"
                    id="custom-role-input"
                    maxLength={4000}
                    value={customRoleInput}
                    onChange={(event) => {
                      setCustomRoleError(false);
                      setCustomRoleInput(event.currentTarget.value);
                    }}
                    type="text"
                  />
                </FormField>
                <ButtonSecondary
                  type="submit"
                  className="sw-ml-2 sw-mr-4"
                  disabled={customRoleInput.trim() === '' || customRoleError}
                >
                  {translate('add_verb')}
                </ButtonSecondary>
              </form>
              {customRoleError && (
                <FlagMessage variant="error">
                  {translate('settings.authentication.configuration.roles_mapping.role_exists')}
                </FlagMessage>
              )}
            </div>
          </ContentCell>
        </TableRow>

        {list
          ?.filter((r) => !r.baseRole)
          .map((mapping) => (
            <PermissionRow key={mapping.id} mapping={mapping} setMapping={setMapping} list={list} />
          ))}
      </Table>
      <FlagMessage variant="info">
        {translateWithParameters(
          'settings.authentication.configuration.roles_mapping.dialog.custom_roles_description',
          translate('alm', mappingFor),
        )}
      </FlagMessage>

      <Spinner isLoading={isLoading} />
    </div>
  );

  return (
    <Modal closeOnOverlayClick={!haveEmptyCustomRoles} onClose={onClose} isLarge>
      <Modal.Header title={header} />
      <Modal.Body>{formBody}</Modal.Body>
      <Modal.Footer
        secondaryButton={
          <div className="sw-flex sw-items-center sw-justify-end sw-mt-2">
            {haveEmptyCustomRoles && (
              <FlagMessage variant="error" className="sw-inline-block sw-mb-0 sw-mr-2">
                {translate('settings.authentication.configuration.roles_mapping.empty_custom_role')}
              </FlagMessage>
            )}
            <ButtonSecondary disabled={haveEmptyCustomRoles} onClick={onClose}>
              {translate('close')}
            </ButtonSecondary>
          </div>
        }
      />
    </Modal>
  );
}
