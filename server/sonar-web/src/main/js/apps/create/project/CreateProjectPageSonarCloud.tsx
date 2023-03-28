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
import { useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { translate } from "../../../helpers/l10n";
import { whenLoggedIn } from '../../../components/hoc/whenLoggedIn';
import './style.css';
import { Organization } from "../../../types/types";
import { CurrentUser } from "../../../types/users";
import withCurrentUserContext from "../../../app/components/current-user/withCurrentUserContext";
import { Router, withRouter } from "../../../components/hoc/withRouter";
import { SubmitButton } from "../../../components/controls/buttons";
import OrganizationInput from "../components/OrganizationInput";

interface Props {
  currentUser: CurrentUser;
  userOrganizations: Organization[];
  router: Router;
}

const CreateProjectPageSonarCloud: React.FC<Props> = ({ userOrganizations, router }) => {

  const [selectedOrganization, setSelectedOrganization] = useState<Organization>();

  const header = translate('onboarding.create_project.header');

  return (
      <>
        <Helmet title={header} titleTemplate="%s"/>
        <div className="page page-limited huge-spacer-top huge-spacer-bottom">
          <header className="page-header huge-spacer-bottom">
            <h1 className="page-title huge">
              <strong>{header}</strong>
            </h1>
          </header>
          <div className="create-project-manual">
            <div className="flex-1 huge-spacer-right">
              <form className="manual-project-create"
                    onSubmit={() => selectedOrganization && router.push(`/organizations/${selectedOrganization.kee}/extension/developer/projects`)}>
                {userOrganizations && (
                    <OrganizationInput
                        onChange={(org) => setSelectedOrganization(org)}
                        organization={selectedOrganization}
                        organizations={userOrganizations}
                    />
                )}
                <SubmitButton disabled={!selectedOrganization}>
                  {translate('set_up')}
                </SubmitButton>
              </form>
            </div>
          </div>
        </div>
      </>
  );
}

export default whenLoggedIn(withCurrentUserContext(withRouter(CreateProjectPageSonarCloud)));
