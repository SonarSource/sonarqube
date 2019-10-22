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
import { AlmDefinitionFormField } from './AlmDefinitionFormField';

export interface BitbucketFormModalProps {
  formData: T.BitbucketBindingDefinition;
  onFieldChange: (fieldId: keyof T.BitbucketBindingDefinition, value: string) => void;
}

export default function BitbucketFormModal(props: BitbucketFormModalProps) {
  const { formData, onFieldChange } = props;

  return (
    <>
      <AlmDefinitionFormField
        autoFocus={true}
        formData={formData}
        help={true}
        id="name"
        isTextArea={false}
        maxLength={40}
        onFieldChange={onFieldChange}
        propKey="key"
      />
      <AlmDefinitionFormField
        formData={formData}
        help={false}
        id="url.bitbucket"
        isTextArea={false}
        maxLength={2000}
        onFieldChange={onFieldChange}
        propKey="url"
      />
      <AlmDefinitionFormField
        formData={formData}
        help={false}
        id="personal_access_token"
        isTextArea={true}
        maxLength={2000}
        onFieldChange={onFieldChange}
        propKey="personalAccessToken"
      />
    </>
  );
}
