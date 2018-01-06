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
import { isProvided, isClickable } from './utils';
import { translate } from '../../../helpers/l10n';
import BugTrackerIcon from '../../../components/ui/BugTrackerIcon';

export default class LinkRow extends React.PureComponent {
  static propTypes = {
    link: PropTypes.object.isRequired,
    onDelete: PropTypes.func.isRequired
  };

  handleDeleteClick(e) {
    e.preventDefault();
    e.target.blur();
    this.props.onDelete();
  }

  renderIcon(iconClassName) {
    if (iconClassName === 'icon-issue') {
      return (
        <div className="display-inline-block text-top spacer-right">
          <BugTrackerIcon />
        </div>
      );
    }

    return (
      <div className="display-inline-block text-top spacer-right">
        <i className={iconClassName} />
      </div>
    );
  }

  renderNameForProvided(link) {
    return (
      <div>
        {this.renderIcon(`icon-${link.type}`)}
        <div className="display-inline-block text-top">
          <div>
            <span className="js-name">{link.name}</span>
          </div>
          <div className="note little-spacer-top">
            <span className="js-type">{`sonar.links.${link.type}`}</span>
          </div>
        </div>
      </div>
    );
  }

  renderName(link) {
    if (isProvided(link)) {
      return this.renderNameForProvided(link);
    }

    return (
      <div>
        {this.renderIcon('icon-detach')}
        <div className="display-inline-block text-top">
          <span className="js-name">{link.name}</span>
        </div>
      </div>
    );
  }

  renderUrl(link) {
    if (isClickable(link)) {
      return (
        <a href={link.url} target="_blank">
          {link.url}
        </a>
      );
    }

    return link.url;
  }

  renderDeleteButton(link) {
    if (isProvided(link)) {
      return null;
    }

    return (
      <button className="button-red js-delete-button" onClick={this.handleDeleteClick.bind(this)}>
        {translate('delete')}
      </button>
    );
  }

  render() {
    const { link } = this.props;

    return (
      <tr data-name={link.name}>
        <td className="nowrap">{this.renderName(link)}</td>
        <td className="nowrap js-url">{this.renderUrl(link)}</td>
        <td className="thin nowrap">{this.renderDeleteButton(link)}</td>
      </tr>
    );
  }
}
