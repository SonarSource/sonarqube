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
import { keyframes } from '@emotion/react';
import styled from '@emotion/styled';
import React, { DetailedHTMLProps, HTMLAttributes } from 'react';
import { useIntl } from 'react-intl';
import tw from 'twin.macro';
import { themeColor } from '../helpers/theme';

interface Props {
  ariaLabel?: string;
  children?: React.ReactNode;
  className?: string;
  customSpinner?: JSX.Element;
  loading?: boolean;
  placeholder?: boolean;
  timeout?: number;
}

interface State {
  showSpinner: boolean;
}

const DEFAULT_TIMEOUT = 100;

export class DeferredSpinner extends React.PureComponent<Props, State> {
  timer?: number;
  static displayName = 'DeferredSpinner';
  state: State = { showSpinner: false };

  componentDidMount() {
    if (this.props.loading == null || this.props.loading) {
      this.startTimer();
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.loading === false && this.props.loading === true) {
      this.stopTimer();
      this.startTimer();
    }
    if (prevProps.loading === true && this.props.loading === false) {
      this.stopTimer();
      this.setState({ showSpinner: false });
    }
  }

  componentWillUnmount() {
    this.stopTimer();
  }

  startTimer = () => {
    this.timer = window.setTimeout(() => {
      this.setState({ showSpinner: true });
    }, this.props.timeout ?? DEFAULT_TIMEOUT);
  };

  stopTimer = () => {
    window.clearTimeout(this.timer);
  };

  render() {
    const { showSpinner } = this.state;
    const { customSpinner, className, children, placeholder, ariaLabel } = this.props;
    if (showSpinner) {
      if (customSpinner) {
        return customSpinner;
      }
      // Overwrite aria-label only if defined
      return <Spinner {...(ariaLabel ? { 'aria-label': ariaLabel } : {})} className={className} />;
    }
    if (children) {
      return children;
    }
    if (placeholder) {
      return <Placeholder className={className} />;
    }
    return null;
  }
}

const spinAnimation = keyframes`
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(-360deg);
  }
`;

/* Exported to allow styles to be overridden */
export const StyledSpinner = styled.div`
  border: 2px solid transparent;
  background: linear-gradient(0deg, ${themeColor('primary')} 50%, transparent 50% 100%) border-box,
    linear-gradient(90deg, ${themeColor('primary')} 25%, transparent 75% 100%) border-box;
  mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  animation: ${spinAnimation} 1s infinite linear;

  ${tw`sw-h-4 sw-w-4`};
  ${tw`sw-inline-block`};
  ${tw`sw-box-border`};
  ${tw`sw-rounded-pill`}
`;

export function Spinner(props: DetailedHTMLProps<HTMLAttributes<HTMLDivElement>, HTMLDivElement>) {
  const intl = useIntl();

  return (
    <StyledSpinner aria-label={intl.formatMessage({ id: 'loading' })} role="status" {...props} />
  );
}

const Placeholder = styled.div`
  position: relative;
  visibility: hidden;

  ${tw`sw-inline-flex sw-items-center sw-justify-center`};
  ${tw`sw-h-4 sw-w-4`};
`;
