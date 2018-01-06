/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import Backbone from 'backbone';
import React, { Component } from 'react';
import TokensView from '../tokens-view';

export default class Tokens extends Component {
  componentDidMount() {
    this.renderView();
  }

  componentWillUnmount() {
    this.destroyView();
  }

  destroyView() {
    if (this.destroyView) {
      this.tokensView.destroy();
    }
  }

  renderView() {
    const account = new Backbone.Model({
      id: this.props.user.login
    });

    this.tokensView = new TokensView({
      el: this.refs.container,
      model: account
    }).render();
  }

  render() {
    return <div ref="container" />;
  }
}
