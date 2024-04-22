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
import { LanguagesContext } from '../../app/components/languages/LanguagesContext';

export function withCLanguageFeature<P>(
  WrappedComponent: React.ComponentType<
    React.PropsWithChildren<P & { hasCLanguageFeature: boolean }>
  >,
) {
  class Wrapper extends React.Component<Omit<P, 'hasCLanguageFeature'>> {
    static displayName = getWrappedDisplayName(WrappedComponent, 'withCLanguageFeature');

    render() {
      return (
        <LanguagesContext.Consumer>
          {(languages) => {
            const hasCLanguageFeature = languages['c'] !== undefined;

            return (
              <WrappedComponent {...(this.props as P)} hasCLanguageFeature={hasCLanguageFeature} />
            );
          }}
        </LanguagesContext.Consumer>
      );
    }
  }

  return Wrapper;
}
