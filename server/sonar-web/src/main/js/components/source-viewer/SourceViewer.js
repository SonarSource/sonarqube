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
import React from 'react';
import BaseSourceViewer from './main';
import { getPeriodDate, getPeriodLabel } from '../../helpers/periods';

export default class SourceViewer extends React.Component {
  static propTypes = {
    component: React.PropTypes.shape({
      id: React.PropTypes.string.isRequired
    }).isRequired,
    period: React.PropTypes.object,
    line: React.PropTypes.oneOfType([React.PropTypes.number, React.PropTypes.string])
  };

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

  componentWillUnmount () {
    this.destroySourceViewer();
  }

  renderSourceViewer () {
    this.sourceViewer = new BaseSourceViewer();
    this.sourceViewer.render().$el.appendTo(this.refs.container);
    this.sourceViewer.open(this.props.component.id);
    this.sourceViewer.on('loaded', this.handleLoad.bind(this));
  }

  destroySourceViewer () {
    this.sourceViewer.destroy();
  }

  handleLoad () {
    const { period, line } = this.props;

    if (period) {
      const periodDate = getPeriodDate(period);
      const periodLabel = getPeriodLabel(period);
      this.sourceViewer.filterLinesByDate(periodDate, periodLabel);
    }

    if (line) {
      this.sourceViewer.highlightLine(line);
      this.sourceViewer.scrollToLine(line);
    }
  }

  render () {
    return <div ref="container"/>;
  }
}
