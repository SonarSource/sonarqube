/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import {
  ActionCell,
  ContentCell,
  DestructiveIcon,
  Link,
  Note,
  TableRow,
  TrashIcon,
} from 'design-system';
import * as React from 'react';
import isValidUri from '../../app/utils/isValidUri';
import ConfirmButton from '../../components/controls/ConfirmButton';
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
      <div>
        <div>
          <span>{getLinkName(link)}</span>
        </div>
        <Note className="sw-mt-1">
          <span>{`sonar.links.${link.type}`}</span>
        </Note>
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
        isDestructive
        modalBody={translateWithParameters(
          'project_links.are_you_sure_to_delete_x_link',
          link.name!,
        )}
        modalHeader={translate('project_links.delete_project_link')}
        onConfirm={this.props.onDelete}
      >
        {({ onClick }) => (
          <DestructiveIcon
            Icon={TrashIcon}
            aria-label={translateWithParameters('project_links.delete_x_link', link.name ?? '')}
            onClick={onClick}
            size="small"
          />
        )}
      </ConfirmButton>
    );
  };

  render() {
    const { link } = this.props;

    return (
      <TableRow data-name={link.name}>
        <ContentCell>
          {isProvided(link) ? (
            this.renderNameForProvided(link)
          ) : (
            <div>
              <span>{link.name}</span>
            </div>
          )}
        </ContentCell>
        <ContentCell>
          {isValidUri(link.url) ? (
            <Link to={link.url} target="_blank">
              {link.url}
            </Link>
          ) : (
            link.url
          )}
        </ContentCell>
        <ActionCell>{this.renderDeleteButton(link)}</ActionCell>
      </TableRow>
    );
  }
}
