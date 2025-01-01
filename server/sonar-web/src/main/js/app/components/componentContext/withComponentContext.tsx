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

import * as React from 'react';
import { getWrappedDisplayName } from '~sonar-aligned/components/hoc/utils';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { ComponentContextShape } from '../../../types/component';
import { ComponentContext } from './ComponentContext';

export default function withComponentContext<P extends Partial<ComponentContextShape>>(
  WrappedComponent: React.ComponentType<React.PropsWithChildren<P>>,
) {
  return class WithComponentContext extends React.PureComponent<
    Omit<P, keyof ComponentContextShape>
  > {
    static displayName = getWrappedDisplayName(WrappedComponent, 'withComponentContext');

    render() {
      return (
        <ComponentContext.Consumer>
          {(componentContext) => <WrappedComponent {...componentContext} {...(this.props as P)} />}
        </ComponentContext.Consumer>
      );
    }
  };
}

export function useComponent() {
  return React.useContext(ComponentContext);
}

export function useTopLevelComponentKey() {
  const { component } = useComponent();

  const componentKey = React.useMemo(() => {
    if (!component) {
      return undefined;
    }

    let current = component.breadcrumbs.length - 1;

    while (
      current > 0 &&
      !(
        [
          ComponentQualifier.Project,
          ComponentQualifier.Portfolio,
          ComponentQualifier.Application,
        ] as string[]
      ).includes(component.breadcrumbs[current].qualifier)
    ) {
      current--;
    }

    return component.breadcrumbs[current].key;
  }, [component]);

  return componentKey;
}
