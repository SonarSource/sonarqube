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

import { Note } from '~design-system';
import TagsList from '../../../components/tags/TagsList';
import { translate } from '../../../helpers/l10n';
import { Dict, RuleDetails } from '../../../types/types';
import RuleDetailsTagsPopup from './RuleDetailsTagsPopup';

interface Props {
  canWrite: boolean | undefined;
  onTagsChange: (tags: string[]) => void;
  referencedRepositories: Dict<{ key: string; language: string; name: string }>;
  ruleDetails: RuleDetails;
}

export default function RuleDetailsHeaderActions(props: Readonly<Props>) {
  const { canWrite, ruleDetails, onTagsChange } = props;
  const { sysTags = [], tags = [] } = ruleDetails;
  const allTags = [...sysTags, ...tags];
  const TAGS_TO_DISPLAY = 1;

  return (
    <Note className="sw-flex sw-flex-wrap sw-items-center sw-gap-2 sw-typo-sm">
      {/* Tags */}
      <div className="it__coding-rules-detail-property" data-meta="tags">
        <TagsList
          className="sw-typo-sm"
          tagsClassName="sw-typo-sm"
          allowUpdate={canWrite}
          tagsToDisplay={TAGS_TO_DISPLAY}
          tags={allTags.length > 0 ? allTags : [translate('coding_rules.no_tags')]}
          overlay={
            canWrite ? (
              <RuleDetailsTagsPopup setTags={onTagsChange} sysTags={sysTags} tags={tags} />
            ) : undefined
          }
        />
      </div>
    </Note>
  );
}
