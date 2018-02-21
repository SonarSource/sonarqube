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
import * as React from 'react';
import { isProvided, getLinkName } from './utils';
import { ProjectLink } from '../../../app/types';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import BugTrackerIcon from '../../../components/ui/BugTrackerIcon';
import { Button } from '../../../components/ui/buttons';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  link: ProjectLink;
  onDelete: (linkId: string) => Promise<void>;
}

export default class LinkRow extends React.PureComponent<Props> {
  renderIcon = (iconClassName: string) => {
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
  };

  renderNameForProvided = (link: ProjectLink) => {
    return (
      <div>
        {this.renderIcon(`icon-${link.type}`)}
        <div className="display-inline-block text-top">
          <div>
            <span className="js-name">{getLinkName(link)}</span>
          </div>
          <div className="note little-spacer-top">
            <span className="js-type">{`sonar.links.${link.type}`}</span>
          </div>
        </div>
      </div>
    );
  };

  renderName = (link: ProjectLink) => {
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
  };

  renderDeleteButton = (link: ProjectLink) => {
    if (isProvided(link)) {
      return null;
    }

    return (
      <ConfirmButton
        confirmButtonText={translate('delete')}
        confirmData={link.id}
        isDestructive={true}
        modalBody={translateWithParameters(
          'project_links.are_you_sure_to_delete_x_link',
          link.name
        )}
        modalHeader={translate('project_links.delete_project_link')}
        onConfirm={this.props.onDelete}>
        {({ onClick }) => (
          <Button className="button-red js-delete-button" onClick={onClick}>
            {translate('delete')}
          </Button>
        )}
      </ConfirmButton>
    );
  };

  render() {
    const { link } = this.props;

    return (
      <tr data-name={link.name}>
        <td className="nowrap">{this.renderName(link)}</td>
        <td className="nowrap js-url">
          <a href={link.url} rel="nofollow" target="_blank">
            {link.url}
          </a>
        </td>
        <td className="thin nowrap">{this.renderDeleteButton(link)}</td>
      </tr>
    );
  }
}
