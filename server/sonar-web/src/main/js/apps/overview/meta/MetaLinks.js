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
import React from 'react';
import PropTypes from 'prop-types';
import MetaLink from './MetaLink';
import { getProjectLinks } from '../../../api/projectLinks';
import { orderLinks } from '../../project-admin/links/utils';

export default class MetaLinks extends React.PureComponent {
  static propTypes = {
    component: PropTypes.object.isRequired
  };

  state = {};

  componentDidMount() {
    this.mounted = true;
    this.loadLinks();
  }

  componentDidUpdate(prevProps) {
    if (prevProps.component.key !== this.props.component.key) {
      this.loadLinks();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadLinks() {
    getProjectLinks(this.props.component.key).then(links => {
      if (this.mounted) {
        this.setState({ links });
      }
    });
  }

  render() {
    const { links } = this.state;

    if (links == null || links.length === 0) {
      return null;
    }

    const orderedLinks = orderLinks(links);

    return (
      <div className="overview-meta-card">
        <ul className="overview-meta-list">
          {orderedLinks.map(link => <MetaLink key={link.id} link={link} />)}
        </ul>
      </div>
    );
  }
}
