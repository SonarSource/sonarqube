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
import classNames from 'classnames';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import './Spinner.css';

interface Props {
  ariaLabel?: string;
  children?: React.ReactNode;
  className?: string;
  customSpinner?: JSX.Element;
  loading?: boolean;
}

export default function Spinner(props: Props) {
  const {
    ariaLabel = translate('loading'),
    children,
    className,
    customSpinner,
    loading = true,
  } = props;

  if (customSpinner) {
    return <>{loading ? customSpinner : children}</>;
  }

  return (
    <>
      <div className="sw-overflow-hidden sw-relative">
        <i
          aria-live="polite"
          data-testid="spinner"
          className={classNames('spinner', className, {
            'sw-sr-only sw-left-[-10000px]': !loading,
            'is-loading': loading,
          })}
        >
          {loading && <span className="sw-sr-only">{ariaLabel}</span>}
        </i>
      </div>
      {!loading && children}
    </>
  );
}
