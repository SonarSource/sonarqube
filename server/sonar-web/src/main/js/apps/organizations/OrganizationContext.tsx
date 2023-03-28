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
import React from 'react';
import { useOutletContext } from 'react-router-dom';
import { getWrappedDisplayName } from '../../components/hoc/utils';
import { Organization } from "../../types/types";

export interface OrganizationContextProps {
  organization: Organization;
}

export function withOrganizationContext<P extends Partial<OrganizationContextProps>>(
    WrappedComponent: React.ComponentType<P>
): React.ComponentType<Omit<P, keyof OrganizationContextProps>> {
  function ComponentWithOrganizationProps(props: P) {
    const context = useOutletContext<OrganizationContextProps>();
    return <WrappedComponent {...props} {...context} />;
  }

  (ComponentWithOrganizationProps as React.FC<P>).displayName = getWrappedDisplayName(
      WrappedComponent,
      'withOrganizationContext'
  );

  return ComponentWithOrganizationProps;
}
