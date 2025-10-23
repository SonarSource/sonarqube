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
import { Modal } from '~design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import { getBaseUrl } from '../../helpers/system';
import { EmbedDocsPopup } from './EmbedDocsPopup';
import "./EmbedDocsPopupHelper.css";

const ClearButton = ({ onClick }: { onClick: () => void }) => (
  <button onClick={onClick} aria-label={translate('close')}>
    &#10005; {/* Unicode for "X" close icon */}
  </button>
);

export default function EmbedDocsPopupHelper() {
  const [aboutCodescanOpen, setAboutCodescanOpen] = React.useState<boolean>(false);

  const renderAboutCodescan = (link: string, icon: string, text: string) => {
    return (
      <Modal
        noBackground={true}
        onRequestClose={() => setAboutCodescanOpen(false)}
        contentLabel={translate('embed_docs.codescan_version')}
        isOpen={aboutCodescanOpen}
      >
      <div>
        <span className="cross-button cross-btn-icon-with-img">
            <ClearButton onClick={() => setAboutCodescanOpen(false)} />
        </span>
        <a href={link} rel="noopener noreferrer" target="_blank">
          <img alt={text} className="display-img-dialog" src={`${getBaseUrl()}/images/${icon}`} />
        </a>
       </div>

      </Modal>
    );
  };

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

      {aboutCodescanOpen &&
        renderAboutCodescan(
          'https://knowledgebase.autorabit.com/codescan/docs/codescan-release-notes',
          'embed-doc/codescan-version-25_1_12.png',
          translate('embed_docs.codescan_version'),
        )}
    </div>
  );
}
