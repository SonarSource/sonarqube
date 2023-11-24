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

import { ItemDivider, ItemHeader, ItemLink, OpenNewTabIcon } from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import { getBaseUrl } from '../../helpers/system';
import { SuggestionLink } from '../../types/types';
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
    <ItemLink to={link}>
      <img
        alt={text}
        aria-hidden
        className="spacer-right"
        height="18"
        src={`${getBaseUrl()}/images/${icon}`}
        width="18"
      />
      {text}
    </ItemLink>
  );
}

function Suggestions({
  firstItemRef,
  suggestions,
}: {
  firstItemRef: React.RefObject<HTMLAnchorElement>;
  suggestions: SuggestionLink[];
}) {
  return (
    <>
      <ItemHeader id="suggestion">{translate('docs.suggestion')}</ItemHeader>
      {suggestions.map((suggestion, i) => (
        <DocItemLink
          innerRef={i === 0 ? firstItemRef : undefined}
          key={suggestion.link}
          to={suggestion.link}
        >
          {suggestion.text}
        </DocItemLink>
      ))}
      <ItemDivider />
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
      {suggestions.length !== 0 && (
        <Suggestions firstItemRef={firstItemRef} suggestions={suggestions} />
      )}
      <DocItemLink innerRef={suggestions.length === 0 ? firstItemRef : undefined} to="/">
        {translate('docs.documentation')}
      </DocItemLink>
      <ItemLink to="/web_api">{translate('api_documentation.page')}</ItemLink>
      <ItemLink to="/web_api_v2">{translate('api_documentation.page.v2')}</ItemLink>
      <ItemDivider />
      <ItemLink to="https://community.sonarsource.com/">
        <OpenNewTabIcon />
        {translate('docs.get_help')}
      </ItemLink>
      <ItemDivider />
      <ItemHeader id="stay_connected">{translate('docs.stay_connected')}</ItemHeader>
      <IconLink
        link="https://www.sonarsource.com/products/sonarqube/whats-new/?referrer=sonarqube"
        text={translate('docs.news')}
      />
      <IconLink
        link="https://www.sonarsource.com/products/sonarqube/roadmap/?referrer=sonarqube"
        text={translate('docs.roadmap')}
      />
      <IconLink
        icon="embed-doc/twitter-icon.svg"
        link="https://twitter.com/SonarQube"
        text="Twitter"
      />
    </>
  );
}
