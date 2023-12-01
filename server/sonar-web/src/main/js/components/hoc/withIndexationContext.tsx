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
import { IndexationContext } from '../../app/components/indexation/IndexationContext';
import { IndexationContextInterface } from '../../types/indexation';
import { getWrappedDisplayName } from './utils';

export interface WithIndexationContextProps {
  indexationContext: IndexationContextInterface;
}

export default function withIndexationContext<P>(
  WrappedComponent: React.ComponentType<React.PropsWithChildren<P & WithIndexationContextProps>>,
) {
  return class WithIndexationContext extends React.PureComponent<
    Omit<P, keyof WithIndexationContextProps>
  > {
    static displayName = getWrappedDisplayName(WrappedComponent, 'withIndexationContext');

    render() {
      return (
        <IndexationContext.Consumer>
          {(indexationContext) => {
            if (indexationContext) {
              return (
                <WrappedComponent indexationContext={indexationContext} {...(this.props as P)} />
              );
            }

            return null;
          }}
        </IndexationContext.Consumer>
      );
    }
  };
}
