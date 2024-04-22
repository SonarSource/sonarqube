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

import { DangerButtonPrimary, addGlobalSuccessMessage } from 'design-system';
import * as React from 'react';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Router } from '~sonar-aligned/types/router';
import { deleteApplication } from '../../api/application';
import { deletePortfolio, deleteProject } from '../../api/project-management';
import ConfirmButton from '../../components/controls/ConfirmButton';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { isApplication, isPortfolioLike } from '../../types/component';
import { Component } from '../../types/types';

interface Props {
  component: Pick<Component, 'key' | 'name' | 'qualifier'>;
  router: Router;
}

export class Form extends React.PureComponent<Props> {
  handleDelete = async () => {
    const { component } = this.props;
    let deleteMethod = deleteProject;
    let redirectTo = '/';

    if (isPortfolioLike(component.qualifier)) {
      deleteMethod = deletePortfolio;
      redirectTo = '/portfolios';
    } else if (isApplication(component.qualifier)) {
      deleteMethod = deleteApplication;
    }

    await deleteMethod(component.key);

    addGlobalSuccessMessage(
      translateWithParameters('project_deletion.resource_deleted', component.name),
    );

    this.props.router.replace(redirectTo);
  };

  render() {
    const { component } = this.props;
    return (
      <ConfirmButton
        confirmButtonText={translate('delete')}
        isDestructive
        modalBody={translateWithParameters(
          'project_deletion.delete_resource_confirmation',
          component.name,
        )}
        modalHeader={translate('qualifier.delete', component.qualifier)}
        onConfirm={this.handleDelete}
      >
        {({ onClick }) => (
          <DangerButtonPrimary id="delete-project" onClick={onClick}>
            {translate('delete')}
          </DangerButtonPrimary>
        )}
      </ConfirmButton>
    );
  }
}

export default withRouter(Form);
