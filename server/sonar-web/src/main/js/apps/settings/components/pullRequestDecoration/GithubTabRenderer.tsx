/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { ALM_KEYS } from '../../utils';
import AlmPRDecorationFormModal from './AlmPRDecorationFormModal';
import GithubFormModal from './GithubFormModal';
import GithubTable from './GithubTable';
import TabHeader from './TabHeader';

export interface GithubTabRendererProps {
  editedDefinition?: T.GithubBindingDefinition;
  definitions: T.GithubBindingDefinition[];
  loading: boolean;
  onCancel: () => void;
  onCreate: () => void;
  onDelete: (definitionKey: string) => void;
  onEdit: (config: T.GithubBindingDefinition) => void;
  onSubmit: (config: T.GithubBindingDefinition, originalKey: string) => void;
}

export default function GithubTabRenderer(props: GithubTabRendererProps) {
  const { definitions, editedDefinition, loading } = props;
  return (
    <>
      <TabHeader alm={ALM_KEYS.GITHUB} onCreate={props.onCreate} />

      <GithubTable
        definitions={definitions}
        loading={loading}
        onDelete={props.onDelete}
        onEdit={props.onEdit}
      />

      {editedDefinition && (
        <AlmPRDecorationFormModal
          bindingDefinition={editedDefinition}
          onCancel={props.onCancel}
          onSubmit={props.onSubmit}>
          {childProps => <GithubFormModal {...childProps} />}
        </AlmPRDecorationFormModal>
      )}
    </>
  );
}
