/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { IS_SSR } from '../helpers/init';
import { translate } from '../helpers/l10n';
import { requestTryAndRepeatUntil } from '../helpers/request';
import { Alert } from './ui/Alert';

export function lazyLoadComponent<T extends React.ComponentType<any>>(
  factory: () => Promise<{ default: T }>,
  displayName?: string
) {
  const LazyComponent = React.lazy(() =>
    requestTryAndRepeatUntil(factory, { max: 2, slowThreshold: 2 }, () => true)
  );

  function LazyComponentWrapper(props: React.ComponentProps<T>) {
    if (IS_SSR) {
      return null;
    }
    return (
      <LazyErrorBoundary>
        <React.Suspense fallback={null}>
          <LazyComponent {...props} />
        </React.Suspense>
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

export class LazyErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError() {
    // Update state so the next render will show the fallback UI.
    return { hasError: true };
  }

  render() {
    if (this.state.hasError) {
      return <Alert variant="error">{translate('default_error_message')}</Alert>;
    }
    return this.props.children;
  }
}
