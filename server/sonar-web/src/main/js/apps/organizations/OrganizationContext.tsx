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
import React from 'react';
import { Organization } from "../../types/types";
import { getWrappedDisplayName } from "~sonar-aligned/components/hoc/utils";

export const OrganizationContext = React.createContext<OrganizationContextProps>({});

export interface OrganizationContextProps {
  organization?: Organization;
}

export function withOrganizationContext<P extends Partial<OrganizationContextProps>>(
  WrappedComponent: React.ComponentType<React.PropsWithChildren<P & OrganizationContextProps>>,
): React.ComponentType<Omit<P, keyof OrganizationContextProps>> {
  return class WithOrganizationContext extends React.PureComponent<
    Omit<P, keyof OrganizationContextProps>
  > {
    static displayName = getWrappedDisplayName(WrappedComponent, 'withOrganizationContext');

    render() {
      return (
        <OrganizationContext.Consumer>
          {(organization) => <WrappedComponent organization={organization} {...(this.props as P)} />}
        </OrganizationContext.Consumer>
      );
    }
  };
}
