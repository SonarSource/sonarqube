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
import './DeferredSpinner.css';

interface Props {
  ariaLabel?: string;
  children?: React.ReactNode;
  className?: string;
  customSpinner?: JSX.Element;
  loading?: boolean;
  timeout?: number;
}

interface State {
  showSpinner: boolean;
}

const DEFAULT_TIMEOUT = 100;

/**
 * Recommendation: do not render this component conditionally based on any loading state:
 *   // Don't do this:
 *   {loading && <DeferredSpinner />}
 * Instead, pass the loading state as a prop:
 *   // Do this:
 *   <DeferredSpinner loading={loading} />
 */
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
    const { ariaLabel = translate('loading'), children, className, customSpinner } = this.props;
    const { showSpinner } = this.state;

    if (customSpinner) {
      return showSpinner ? customSpinner : children;
    }

    return (
      <>
        <i
          aria-live="polite"
          data-testid="deferred-spinner"
          className={classNames('deferred-spinner', className, {
            'a11y-hidden': !showSpinner,
            'is-loading': showSpinner,
          })}
        >
          {showSpinner && <span className="a11y-hidden">{ariaLabel}</span>}
        </i>
        {!showSpinner && children}
      </>
    );
  }
}
