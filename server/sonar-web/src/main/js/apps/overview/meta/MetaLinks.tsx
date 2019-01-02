/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import MetaLink from './MetaLink';
import { orderLinks } from '../../projectLinks/utils';
import { getProjectLinks } from '../../../api/projectLinks';
import { translate } from '../../../helpers/l10n';

interface Props {
  component: T.LightComponent;
}

interface State {
  links?: T.ProjectLink[];
}

export default class MetaLinks extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
    this.loadLinks();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.component.key !== this.props.component.key) {
      this.loadLinks();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadLinks = () =>
    getProjectLinks(this.props.component.key).then(
      links => {
        if (this.mounted) {
          this.setState({ links });
        }
      },
      () => {}
    );

  render() {
    const { links } = this.state;

    if (!links || links.length === 0) {
      return null;
    }

    const orderedLinks = orderLinks(links);

    return (
      <div className="overview-meta-card">
        <h4 className="overview-meta-header">{translate('overview.external_links')}</h4>
        <ul className="overview-meta-list">
          {orderedLinks.map(link => (
            <MetaLink key={link.id} link={link} />
          ))}
        </ul>
      </div>
    );
  }
}
