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

import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { useState } from 'react';
import { Spinner, Title } from '~design-system';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { Router } from '~sonar-aligned/types/router';
import { createPermissionTemplate } from '../../../api/permissions';
import { translate } from '../../../helpers/l10n';
import { PERMISSION_TEMPLATES_PATH } from '../utils';
import Form from './Form';
import ProvisioningWarning from './ProvisioningWarning';
import { withOrganizationContext } from "../../organizations/OrganizationContext";
import { Organization } from "../../../types/types";

interface Props {
  organization: Organization;
  ready?: boolean;
  refresh: () => Promise<void>;
  router: Router;
}

function Header(props: Props) {
  const { organization, ready, router } = props;
  const [createModal, setCreateModal] = useState(false);

  const handleCreateModalSubmit = async (data: {
    description: string;
    name: string;
    projectKeyPattern: string;
  }) => {
    try {
      const response = await createPermissionTemplate({ ...data, organization: organization.kee });
      await props.refresh();
      router.push({
        pathname: `/organizations/${organization.kee}/${PERMISSION_TEMPLATES_PATH}`,
        query: { id: response.permissionTemplate.id },
      });
    } catch (e) {
      throwGlobalError(e);
    }
  };

  return (
    <header>
      <div id="project-permissions-header">
        <div className="sw-flex sw-justify-between">
          <div className="sw-flex sw-gap-3">
            <Title>{translate('permission_templates.page')}</Title>
            <Spinner className="sw-mt-2" loading={!ready} />
          </div>

          <Button onClick={() => setCreateModal(true)} variety={ButtonVariety.Primary}>
            {translate('create')}
          </Button>
        </div>
        <div className="sw-mb-4">{translate('permission_templates.page.description')}</div>
      </div>
      <ProvisioningWarning />

      {createModal && (
        <Form
          confirmButtonText={translate('create')}
          header={translate('permission_template.new_template')}
          onClose={() => setCreateModal(false)}
          onSubmit={handleCreateModalSubmit}
        />
      )}
    </header>
  );
}

export default withRouter(withOrganizationContext(Header));
