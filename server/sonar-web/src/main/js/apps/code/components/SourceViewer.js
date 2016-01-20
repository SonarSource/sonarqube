/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React, { Component } from 'react';

import BaseSourceViewer from '../../../components/source-viewer/main';


export default class SourceViewer extends Component {
  componentDidMount () {
    this.renderSourceViewer();
  }

  shouldComponentUpdate (nextProps) {
    return nextProps.component.id !== this.props.component.id;
  }

  componentWillUpdate () {
    this.destroySourceViewer();
  }

  componentDidUpdate () {
    this.renderSourceViewer();
  }

  componentWillUnmount() {
    this.destroySourceViewer();
  }

  renderSourceViewer () {
    this.sourceViewer = new BaseSourceViewer();
    this.sourceViewer.render().$el.appendTo(this.refs.container);
    this.sourceViewer.open(this.props.component.id);
  }

  destroySourceViewer () {
    this.sourceViewer.destroy();
  }

  render () {
    return <div ref="container" className="code-source-viewer"></div>;
  }
}
