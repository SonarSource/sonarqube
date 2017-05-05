/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import classNames from 'classnames';

type Props = {
  children?: React.Element<*>,
  className?: string,
  loading?: boolean,
  timeout: number
};

type State = {
  showSpinner: boolean
};

export default class DeferredSpinner extends React.PureComponent {
  props: Props;
  state: State;
  timer: number;

  static defaultProps = {
    timeout: 100
  };

  constructor(props: Props) {
    super(props);
    this.state = { showSpinner: false };
  }

  componentDidMount() {
    if (this.props.loading == null || this.props.loading === true) {
      this.startTimer();
    }
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.loading === false && nextProps.loading === true) {
      this.stopTimer();
      this.startTimer();
    }
    if (this.props.loading === true && nextProps.loading === false) {
      this.stopTimer();
      this.setState({ showSpinner: false });
    }
  }

  componentWillUnmount() {
    this.stopTimer();
  }

  startTimer = () => {
    this.timer = setTimeout(() => this.setState({ showSpinner: true }), this.props.timeout);
  };

  stopTimer = () => {
    clearTimeout(this.timer);
  };

  render() {
    return this.state.showSpinner
      ? <i className={classNames('spinner', this.props.className)} />
      : this.props.children || null;
  }
}
