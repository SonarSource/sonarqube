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
import { useCurrentUser } from '../../app/components/current-user/CurrentUserContext';
import { CustomEvents } from '../../helpers/constants';
import { DocLink } from '../../helpers/doc-links';
import { translate } from '../../helpers/l10n';
import { GlobalSettingKeys } from "../../types/settings";
import { Permissions } from '../../types/permissions';
import { SuggestionLink } from '../../types/types';
import { DocItemLink } from './DocItemLink';
import { SuggestionsContext } from './SuggestionsContext';

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
  const { currentUser } = useCurrentUser();
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

  const runModeTour = () => {
    document.dispatchEvent(new CustomEvent(CustomEvents.RunTourMode));
  };

  const isAdminOrQGAdmin =
    currentUser.permissions?.global.includes(Permissions.Admin) ||
    currentUser.permissions?.global.includes(Permissions.QualityGateAdmin);

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

      <DropdownMenu.ItemLink to="https://www.codescan.io/blog">
        CodeScan Blog
      </DropdownMenu.ItemLink>

      <DropdownMenu.ItemLink to="https://twitter.com/CodeScanforSFDC">
        @CodeScanforSFDC
      </DropdownMenu.ItemLink>
    </>
  );
}
