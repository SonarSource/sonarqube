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
import { Title } from '~design-system';
import { Visibility } from '~sonar-aligned/types/component';
import { translate } from '../../helpers/l10n';
import ChangeDefaultVisibilityForm from './ChangeDefaultVisibilityForm';
import CreateProjectForm from "./CreateProjectForm";

export interface Props {
  defaultProjectVisibility?: Visibility;
  hasProvisionPermission?: boolean;
  onChangeDefaultProjectVisibility: (visibility: Visibility) => void;
  onCreateProjectFormClosed: () => void;
}

export default function Header(props: Readonly<Props>) {
  const [visibilityForm, setVisibilityForm] = useState(false);
  const [createProjectForm, setCreateProjectForm] = useState(false);

  const { defaultProjectVisibility, hasProvisionPermission } = props;

  return (
    <header className="sw-mb-5">
      <div className="sw-flex sw-items-center sw-justify-between">
        <Title className="sw-m-0">{translate('projects_management')}</Title>
        <div className="sw-flex sw-items-center it__page-actions">
          {hasProvisionPermission && (
            <Button
              id="create-project"
              onClick={() => setCreateProjectForm(true)}
              variety={ButtonVariety.Primary}
            >
              {translate('qualifiers.create.TRK')}
            </Button>
          )}
        </div>
      </div>

      <p className="sw-mt-4">{translate('projects_management.page.description')}</p>

      {visibilityForm && (
        <ChangeDefaultVisibilityForm
          defaultVisibility={defaultProjectVisibility ?? Visibility.Public}
          onClose={() => setVisibilityForm(false)}
          onConfirm={props.onChangeDefaultProjectVisibility}
        />
      )}

      <CreateProjectForm
        isOpen={createProjectForm}
        onOpenChange={setCreateProjectForm}
        onClose={() => {
          setCreateProjectForm(false);
          props.onCreateProjectFormClosed();
        }}
      />
    </header>
  );
}
