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
import * as React from 'react';
import isValidUri from '../../app/utils/isValidUri';
import Link from '../../components/common/Link';
import { Button } from '../../components/controls/buttons';
import ConfirmButton from '../../components/controls/ConfirmButton';
import ProjectLinkIcon from '../../components/icons/ProjectLinkIcon';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { getLinkName, isProvided } from '../../helpers/projectLinks';
import { ProjectLink } from '../../types/types';

interface Props {
  link: ProjectLink;
  onDelete: (linkId: string) => Promise<void>;
}

export default class LinkRow extends React.PureComponent<Props> {
  renderNameForProvided = (link: ProjectLink) => {
    return (
      <div className="display-inline-block text-top">
        <div>
          <span className="js-name">{getLinkName(link)}</span>
        </div>
        <div className="note little-spacer-top">
          <span className="js-type">{`sonar.links.${link.type}`}</span>
        </div>
      </div>
    );
  };

  renderName = (link: ProjectLink) => {
    return (
      <div>
        <ProjectLinkIcon className="little-spacer-right" type={link.type} />
        {isProvided(link) ? (
          this.renderNameForProvided(link)
        ) : (
          <div className="display-inline-block text-top">
            <span className="js-name">{link.name}</span>
          </div>
        )}
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
          link.name!
        )}
        modalHeader={translate('project_links.delete_project_link')}
        onConfirm={this.props.onDelete}
      >
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
          {isValidUri(link.url) ? (
            <Link to={link.url} target="_blank">
              {link.url}
            </Link>
          ) : (
            link.url
          )}
        </td>
        <td className="thin nowrap">{this.renderDeleteButton(link)}</td>
      </tr>
    );
  }
}
