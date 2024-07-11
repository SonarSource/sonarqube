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

import { DropdownMenu } from '@sonarsource/echoes-react';
import * as React from 'react';
import { DocLink } from '../../helpers/doc-links';
import { translate } from '../../helpers/l10n';
import { SuggestionLink } from '../../types/types';
import { Image } from '../common/Image';
import { DocItemLink } from './DocItemLink';
import { SuggestionsContext } from './SuggestionsContext';

function IconLink({
  icon = 'embed-doc/sq-icon.svg',
  link,
  text,
}: {
  icon?: string;
  link: string;
  text: string;
}) {
  return (
    <DropdownMenu.ItemLink
      prefix={
        <Image
          alt={text}
          aria-hidden
          className="sw-mr-2"
          height="18"
          src={`/images/${icon}`}
          width="18"
        />
      }
      to={link}
    >
      {text}
    </DropdownMenu.ItemLink>
  );
}

function Suggestions({ suggestions }: Readonly<{ suggestions: SuggestionLink[] }>) {
  return (
    <>
      <DropdownMenu.GroupLabel>{translate('docs.suggestion')}</DropdownMenu.GroupLabel>

      {suggestions.map((suggestion) => (
        <DocItemLink key={suggestion.link} to={suggestion.link}>
          {suggestion.text}
        </DocItemLink>
      ))}

      <DropdownMenu.Separator />
    </>
  );
}

export function EmbedDocsPopup() {
  const firstItemRef = React.useRef<HTMLAnchorElement>(null);
  const { suggestions } = React.useContext(SuggestionsContext);

  React.useEffect(() => {
    firstItemRef.current?.focus();
  }, []);

  return (
    <>
      {suggestions.length !== 0 && <Suggestions suggestions={suggestions} />}

      <DocItemLink to={DocLink.Root}>{translate('docs.documentation')}</DocItemLink>

      <DropdownMenu.ItemLink to="/web_api">
        {translate('api_documentation.page')}
      </DropdownMenu.ItemLink>

      <DropdownMenu.ItemLink to="/web_api_v2">
        {translate('api_documentation.page.v2')}
      </DropdownMenu.ItemLink>

      <DropdownMenu.Separator />

      <DropdownMenu.ItemLink to="https://community.sonarsource.com/">
        {translate('docs.get_help')}
      </DropdownMenu.ItemLink>

      <DropdownMenu.Separator />

      <DropdownMenu.GroupLabel>{translate('docs.stay_connected')}</DropdownMenu.GroupLabel>

      <IconLink
        link="https://www.sonarsource.com/products/sonarqube/whats-new/?referrer=sonarqube"
        text={translate('docs.news')}
      />

      <IconLink
        link="https://www.sonarsource.com/products/sonarqube/roadmap/?referrer=sonarqube"
        text={translate('docs.roadmap')}
      />

      <IconLink
        icon="embed-doc/x-icon-black.svg"
        link="https://twitter.com/SonarQube"
        text="X @SonarQube"
      />
    </>
  );
}
