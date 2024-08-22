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
import { addGlobalSuccessMessage } from 'design-system';
import * as React from 'react';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { isPortfolioLike } from '~sonar-aligned/helpers/component';
import { Router } from '~sonar-aligned/types/router';
import ConfirmButton from '../../components/controls/ConfirmButton';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { useDeleteApplicationMutation } from '../../queries/applications';
import { useDeletePortfolioMutation } from '../../queries/portfolios';
import { useDeleteProjectMutation } from '../../queries/projects';
import { isApplication } from '../../types/component';
import { Component } from '../../types/types';

interface Props {
  component: Pick<Component, 'key' | 'name' | 'qualifier'>;
  router: Router;
}

export function Form({ component, router }: Readonly<Props>) {
  const { mutate: deleteProject } = useDeleteProjectMutation();
  const { mutate: deleteApplication } = useDeleteApplicationMutation();
  const { mutate: deletePortfolio } = useDeletePortfolioMutation();

  const handleDelete = () => {
    let deleteMethod = deleteProject;
    let redirectTo = '/';

    if (isPortfolioLike(component.qualifier)) {
      deleteMethod = deletePortfolio;
      redirectTo = '/portfolios';
    } else if (isApplication(component.qualifier)) {
      deleteMethod = deleteApplication;
    }

    deleteMethod(component.key, {
      onSuccess: () => {
        addGlobalSuccessMessage(
          translateWithParameters('project_deletion.resource_deleted', component.name),
        );

        router.replace(redirectTo);
      },
    });
  };

  return (
    <ConfirmButton
      confirmButtonText={translate('delete')}
      isDestructive
      modalBody={translateWithParameters(
        'project_deletion.delete_resource_confirmation',
        component.name,
      )}
      modalHeader={translate('qualifier.delete', component.qualifier)}
      onConfirm={handleDelete}
    >
      {({ onClick }) => (
        <Button id="delete-project" onClick={onClick} variety={ButtonVariety.Danger}>
          {translate('delete')}
        </Button>
      )}
    </ConfirmButton>
  );
}

export default withRouter(Form);
