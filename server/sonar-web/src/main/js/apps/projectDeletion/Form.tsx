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
import { deleteApplication } from '../../api/application';
import { deletePortfolio } from '../../api/components';
import { Button } from '../../components/controls/buttons';
import ConfirmButton from '../../components/controls/ConfirmButton';
import { Router, withRouter } from '../../components/hoc/withRouter';
import { addGlobalSuccessMessage } from '../../helpers/globalMessages';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { isApplication, isPortfolioLike } from '../../types/component';
import { Component } from '../../types/types';
import { deleteProject } from '../../api/codescan';

interface Props {
  component: Pick<Component, 'id' | 'key' | 'name' | 'qualifier' | 'organization'>;
  router: Router;
}

export class Form extends React.PureComponent<Props> {
  handleDelete = async () => {
    const { component } = this.props;
    let redirectTo = '/';
    if (isPortfolioLike(component.qualifier)) {
      await deletePortfolio(component.key);
      redirectTo = '/portfolios';
    } else if (isApplication(component.qualifier)) {
      await deleteApplication(component.key);
    } else {
      await deleteProject(component.id, true)
    }

    addGlobalSuccessMessage(
      translateWithParameters('project_deletion.resource_deleted', component.name)
    );
    this.props.router.replace(redirectTo);
  };

  render() {
    const { component } = this.props;
    return (
      <ConfirmButton
        confirmButtonText={translate('delete')}
        isDestructive={true}
        modalBody={translateWithParameters(
          'project_deletion.delete_resource_confirmation',
          component.name
        )}
        modalHeader={translate('qualifier.delete', component.qualifier)}
        onConfirm={this.handleDelete}
      >
        {({ onClick }) => (
          <Button className="button-red" id="delete-project" onClick={onClick}>
            {translate('delete')}
          </Button>
        )}
      </ConfirmButton>
    );
  }
}

export default withRouter(Form);
