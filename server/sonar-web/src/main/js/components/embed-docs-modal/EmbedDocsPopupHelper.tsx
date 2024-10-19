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

import {
  ButtonIcon,
  ButtonVariety,
  DropdownMenu,
  DropdownMenuAlign,
  IconQuestionMark,
} from '@sonarsource/echoes-react';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import { EmbedDocsPopup } from './EmbedDocsPopup';
import { getBaseUrl } from "../../helpers/system";
import { Modal } from "design-system";

export default function EmbedDocsPopupHelper() {

  const [aboutCodescanOpen, setAboutCodescanOpen] = React.useState<boolean>();

  const renderAboutCodescan = (link: string, icon: string, text: string) => {
    return (
      <Modal
        className="abs-width-auto"
        onRequestClose={() => setAboutCodescanOpen(false)}
        contentLabel=''
      >
        <a href={link} rel="noopener noreferrer" target="_blank">
          <img alt={text} src={`${getBaseUrl()}/images/${icon}`}/>
        </a>
        <span className="cross-button">
            <ClearButton onClick={() => setAboutCodescanOpen(false)}/>
          </span>
      </Modal>
    );
  }

  return (
    <div className="dropdown">
      <DropdownMenu.Root
        align={DropdownMenuAlign.End}
        id="help-menu-dropdown"
        items={<EmbedDocsPopup setAboutCodescanOpen={setAboutCodescanOpen} />}
      >
        <ButtonIcon
          Icon={IconQuestionMark}
          data-guiding-id="issue-5"
          ariaLabel={translate('help')}
          isIconFilled
          variety={ButtonVariety.DefaultGhost}
        />
      </DropdownMenu.Root>

      {aboutCodescanOpen && renderAboutCodescan(
        'https://knowledgebase.autorabit.com/codescan/docs/codescan-release-notes',
        'embed-doc/codescan-version-24_0_11.png',
        translate('embed_docs.codescan_version')
      )}
    </div>
  );
}
