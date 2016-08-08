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
import React from 'react';
import { getProjectLinks } from '../../../api/projectLinks';
import { isProvided } from '../../project-admin/links/utils';

export default class MetaLinks extends React.Component {
  static propTypes = {
    component: React.PropTypes.object.isRequired
  };

  state = {};

  componentDidMount () {
    this.mounted = true;
    this.loadLinks();
  }

  componentDidUpdate (prevProps) {
    if (prevProps.component !== this.props.component) {
      this.loadLinks();
    }
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  loadLinks () {
    getProjectLinks(this.props.component.key).then(links => {
      if (this.mounted) {
        this.setState({ links });
      }
    });
  }

  renderLinkIcon (link) {
    return isProvided(link) ?
        <i className={`icon-color-link icon-${link.type}`}/> :
        <i className="icon-color-link icon-detach"/>;
  }

  render () {
    const { links } = this.state;

    if (links == null || links.length === 0) {
      return null;
    }

    return (
        <ul className="overview-meta-list big-spacer-bottom">
          {links.map(link => (
              <li key={link.id}>
                <a className="link-with-icon" href={link.url} target="_blank">
                  {this.renderLinkIcon(link)}
                  &nbsp;
                  {link.name}
                </a>
              </li>
          ))}
        </ul>
    );
  }
}
