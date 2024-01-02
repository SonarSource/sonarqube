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
import { CloseIcon, InputField, InteractiveIcon, Link } from 'design-system';
import React, { useState } from 'react';
import isValidUri from '../../app/utils/isValidUri';
import { translate } from '../../helpers/l10n';
import { getLinkName } from '../../helpers/projectLinks';
import { ProjectLink } from '../../types/types';
import { ClearButton } from '../controls/buttons';
import ProjectLinkIcon from '../icons/ProjectLinkIcon';

interface Props {
  iconOnly?: boolean;
  link: ProjectLink;
  // TODO Remove this prop when all links are migrated to the new design
  miui?: boolean;
}

export default function MetaLink({ iconOnly, link, miui }: Props) {
  const [expanded, setExpanded] = useState(false);

  const handleClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    setExpanded((expanded) => !expanded);
  };

  const handleCollapse = () => {
    setExpanded(false);
  };

  const handleSelect = (event: React.MouseEvent<HTMLInputElement>) => {
    event.currentTarget.select();
  };

  const linkTitle = getLinkName(link);
  const isValid = isValidUri(link.url);
  return miui ? (
    <li>
      <Link
        isExternal
        to={link.url}
        preventDefault={!isValid}
        onClick={isValid ? undefined : handleClick}
        rel="nofollow noreferrer noopener"
        target="_blank"
        icon={<ProjectLinkIcon miui className="little-spacer-right" type={link.type} />}
      >
        {!iconOnly && linkTitle}
      </Link>

      {expanded && (
        <div className="little-spacer-top display-flex-center">
          <InputField
            className="overview-key width-80"
            onClick={handleSelect}
            readOnly
            type="text"
            value={link.url}
          />
          <InteractiveIcon
            Icon={CloseIcon}
            aria-label={translate('hide')}
            className="sw-ml-1"
            onClick={handleCollapse}
          />
        </div>
      )}
    </li>
  ) : (
    <li>
      <a
        className="link-no-underline"
        href={isValid ? link.url : undefined}
        onClick={isValid ? undefined : handleClick}
        rel="nofollow noreferrer noopener"
        target="_blank"
        title={linkTitle}
      >
        <ProjectLinkIcon className="little-spacer-right" type={link.type} />
        {!iconOnly && linkTitle}
      </a>
      {expanded && (
        <div className="little-spacer-top display-flex-center">
          <input
            className="overview-key width-80"
            onClick={handleSelect}
            readOnly
            type="text"
            value={link.url}
          />
          <ClearButton className="little-spacer-left" onClick={handleCollapse} />
        </div>
      )}
    </li>
  );
}
