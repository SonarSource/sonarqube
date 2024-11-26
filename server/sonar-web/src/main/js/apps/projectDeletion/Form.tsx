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
import { Router } from '~sonar-aligned/types/router';
import ConfirmButton from '../../components/controls/ConfirmButton';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { Component } from '../../types/types';
import { deleteProject } from '../../api/codescan';

interface Props {
  component: Pick<Component, 'id' | 'key' | 'name' | 'qualifier' | 'organization'>;
  router: Router;
}

export function Form({ component, router }: Readonly<Props>) {

  const handleDelete = async () => {
    deleteProject(component.id, true).then(() => {
      addGlobalSuccessMessage(translateWithParameters('project_deletion.resource_deleted', component.name));
      router.replace('/');
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
