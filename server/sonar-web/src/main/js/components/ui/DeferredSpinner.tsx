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
import classNames from 'classnames';
import * as React from 'react';
import './DeferredSpinner.css';

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

export default class DeferredSpinner extends React.PureComponent<Props, State> {
  timer?: number;

  state: State = { showSpinner: false };

  componentDidMount() {
    if (this.props.loading == null || this.props.loading === true) {
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
    this.timer = window.setTimeout(
      () => this.setState({ showSpinner: true }),
      this.props.timeout || DEFAULT_TIMEOUT
    );
  };

  stopTimer = () => {
    window.clearTimeout(this.timer);
  };

  render() {
    const { ariaLabel, children, className, customSpinner, placeholder } = this.props;
    if (this.state.showSpinner) {
      return (
        customSpinner || (
          <i
            className={classNames('deferred-spinner', className)}
            aria-live={ariaLabel ? 'polite' : undefined}
            aria-label={ariaLabel}
          />
        )
      );
    }
    return (
      children ||
      (placeholder ? (
        <i
          data-testid="deferred-spinner-placeholder"
          className={classNames('deferred-spinner-placeholder', className)}
          aria-live={ariaLabel ? 'polite' : undefined}
          aria-label={ariaLabel}
        />
      ) : null)
    );
  }
}
