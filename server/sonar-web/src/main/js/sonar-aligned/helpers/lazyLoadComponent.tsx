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
import { FlagMessage } from 'design-system';
import React, { Component, lazy, Suspense } from 'react';
import { translate } from '../../helpers/l10n';
import { requestTryAndRepeatUntil } from '../../helpers/request';

export function lazyLoadComponent<T extends React.ComponentType<any>>(
  factory: () => Promise<{ default: T }>,
  displayName?: string,
) {
  const LazyComponent = lazy(() =>
    requestTryAndRepeatUntil(factory, { max: 2, slowThreshold: 2 }, () => true),
  );

  function LazyComponentWrapper(props: React.ComponentProps<T>) {
    return (
      <LazyErrorBoundary>
        <Suspense fallback={null}>
          <LazyComponent {...props} />
        </Suspense>
      </LazyErrorBoundary>
    );
  }

  LazyComponentWrapper.displayName = displayName;
  return LazyComponentWrapper;
}

interface ErrorBoundaryProps {
  children: React.ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
}

export class LazyErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError() {
    // Update state so the next render will show the fallback UI.
    return { hasError: true };
  }

  render() {
    if (this.state.hasError) {
      return (
        <FlagMessage variant="error">{translate('default_component_error_message')}</FlagMessage>
      );
    }
    return this.props.children;
  }
}
