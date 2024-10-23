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
import { Image } from '~sonar-aligned/components/common/Image';
import { getRedirectUrlForZoho } from "../../api/codescan";
import { getValue } from "../../api/settings";
import { DocLink } from '../../helpers/doc-links';
import { translate } from '../../helpers/l10n';
import { GlobalSettingKeys } from "../../types/settings";
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

export function EmbedDocsPopup({ setAboutCodescanOpen }) {
  const firstItemRef = React.useRef<HTMLAnchorElement>(null);
  const { suggestions } = React.useContext(SuggestionsContext);
  const [zohoUrl, setZohoUrl] = React.useState<any>();

  React.useEffect(() => {
    firstItemRef.current?.focus();

    getValue({ key: GlobalSettingKeys.CodescanSupportLink }).then((enabledSupportLink) => {
      // Get zoho re-direct url.
      if (!enabledSupportLink || enabledSupportLink.value === "true") {
        getRedirectUrlForZoho().then(response => {
          setZohoUrl(response.redirectUrl);
        });
      }
    });
  }, []);

  return (
    <>
      {suggestions.length !== 0 && <Suggestions suggestions={suggestions} />}

      <DropdownMenu.GroupLabel>{translate('docs.suggestion')}</DropdownMenu.GroupLabel>
      <DropdownMenu.ItemLink to="https://knowledgebase.autorabit.com/product-guides/codescan/getting-started">
        {translate('docs.suggestion_help')}
      </DropdownMenu.ItemLink>

      <DropdownMenu.Separator />
      
      <DocItemLink to={DocLink.Documentation}>{translate('docs.documentation')}</DocItemLink>

      <DropdownMenu.ItemLink to="/web_api">
        {translate('api_documentation.page')}
      </DropdownMenu.ItemLink>

      <DropdownMenu.ItemLink to="/web_api_v2">
        {translate('api_documentation.page.v2')}
      </DropdownMenu.ItemLink>

      <DropdownMenu.Separator />

      {zohoUrl && (
        <DropdownMenu.ItemLink to={zohoUrl}>
          {translate('docs.get_help')}
        </DropdownMenu.ItemLink>
      )}

      <DropdownMenu.ItemButton onClick={() => setAboutCodescanOpen(true)}>
        {translate('embed_docs.about_codescan')}
      </DropdownMenu.ItemButton>

      <DropdownMenu.Separator />

      <DropdownMenu.GroupLabel>{translate('docs.stay_connected')}</DropdownMenu.GroupLabel>

      <IconLink
        icon="sonarcloud-square-logo.svg"
        link="https://www.codescan.io/blog"
        text="CodeScan Blog "
      />

      <IconLink
        icon="embed-doc/x-icon-black.svg"
        link="https://twitter.com/CodeScanforSFDC"
        text="@CodeScanforSFDC"
      />
    </>
  );
}
