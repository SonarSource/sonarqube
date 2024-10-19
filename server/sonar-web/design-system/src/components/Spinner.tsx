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
import { keyframes } from '@emotion/react';
import styled from '@emotion/styled';
import classNames from 'classnames';
import * as React from 'react';
import { useIntl } from 'react-intl';
import tw from 'twin.macro';
import { themeColor } from '../helpers/theme';

interface Props {
  ariaLabel?: string;
  className?: string;
  customSpinner?: JSX.Element;
  loading?: boolean;
  placeholder?: boolean;
}

/** @deprecated Use Spinner from Echoes instead.
 *
 * Some of the props have changed or been renamed:
 * - ~`customSpinner`~ has been removed
 * - `loading` is now `isLoading`
 * - `placeholder` is now `hasPlaceholder`
 */
export function Spinner(props: React.PropsWithChildren<Props>) {
  const intl = useIntl();
  const {
    customSpinner,
    className,
    children,
    placeholder,
    ariaLabel = intl.formatMessage({ id: 'loading' }),
    loading = true,
  } = props;

  if (customSpinner) {
    return <>{loading ? customSpinner : children}</>;
  }

  return (
    // Below: using <></> won't work in extenstions ('React' is not defined). This is because the
    // name 'React' would already have been minified to something else when <> is resolved to
    // React.Fragment
    // eslint-disable-next-line react/jsx-fragments
    <React.Fragment>
      <div className="sw-relative">
        <div
          className={classNames('sw-overflow-hidden', {
            'sw-sr-only': !loading,
            it__loading: loading,
          })}
        >
          <StyledSpinner aria-live="polite" className={className} role="status">
            {loading && <span className="sw-sr-only">{ariaLabel}</span>}
          </StyledSpinner>
        </div>
      </div>
      {!loading && (children ?? (placeholder && <Placeholder className={className} />) ?? null)}
    </React.Fragment>
  );
}

const spinAnimation = keyframes`
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(-360deg);
  }
`;

const StyledSpinner = styled.div`
  border: 2px solid transparent;
  background:
    linear-gradient(0deg, ${themeColor('primary')} 50%, transparent 50% 100%) border-box,
    linear-gradient(90deg, ${themeColor('primary')} 25%, transparent 75% 100%) border-box;
  mask:
    linear-gradient(#fff 0 0) padding-box,
    linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  animation: ${spinAnimation} 1s infinite linear;

  ${tw`sw-h-4 sw-w-4`};
  ${tw`sw-inline-block`};
  ${tw`sw-box-border`};
  ${tw`sw-rounded-pill`}
`;

const Placeholder = styled.div`
  position: relative;
  visibility: hidden;

  ${tw`sw-inline-flex sw-items-center sw-justify-center`};
  ${tw`sw-h-4 sw-w-4`};
`;
