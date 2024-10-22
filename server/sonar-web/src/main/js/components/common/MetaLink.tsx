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

import { LinkStandalone } from '@sonarsource/echoes-react';
import React, { useState } from 'react';
import { CloseIcon, InputField, InteractiveIcon } from '~design-system';
import isValidUri from '../../app/utils/isValidUri';
import { translate } from '../../helpers/l10n';
import { getLinkName } from '../../helpers/projectLinks';
import { ProjectLink } from '../../types/types';

interface Props {
  link: ProjectLink;
}

export default function MetaLink({ link }: Readonly<Props>) {
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
  return (
    <li>
      <LinkStandalone
        onClick={isValid ? undefined : handleClick}
        shouldPreventDefault={!isValid}
        to={link.url}
      >
        {linkTitle}
      </LinkStandalone>

      {expanded && (
        <div className="sw-mt-1 sw-flex sw-items-center">
          <InputField onClick={handleSelect} readOnly type="text" value={link.url} size="large" />
          <InteractiveIcon
            Icon={CloseIcon}
            aria-label={translate('hide')}
            className="sw-ml-1"
            onClick={handleCollapse}
          />
        </div>
      )}
    </li>
  );
}
