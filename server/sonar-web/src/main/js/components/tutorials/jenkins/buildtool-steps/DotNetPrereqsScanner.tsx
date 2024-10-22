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
  ClipboardIconButton,
  FlagMessage,
  ListItem,
  NumberedListItem,
  OrderedList,
} from '~design-system';
import { translate } from '../../../../helpers/l10n';
import { InlineSnippet } from '../../components/InlineSnippet';
import SentenceWithHighlights from '../../components/SentenceWithHighlights';

export default function DotNetPrereqsScanner() {
  return (
    <NumberedListItem>
      <SentenceWithHighlights
        highlightKeys={['default_scanner']}
        translationKey="onboarding.tutorial.with.jenkins.dotnet.scanner.prereqs.title"
      />
      <br />
      <FlagMessage className="sw-mt-2" variant="info">
        {translate('onboarding.tutorial.with.jenkins.dotnet.scanner.prereqs.info')}
      </FlagMessage>
      <OrderedList tickStyle="ALPHA" className="sw-ml-12">
        <ListItem>
          <SentenceWithHighlights
            highlightKeys={['path']}
            translationKey="onboarding.tutorial.with.jenkins.dotnet.scanner.prereqs.step1"
          />
        </ListItem>
        <ListItem>
          <SentenceWithHighlights
            highlightKeys={['default_scanner', 'add_scanner_for_msbuild']}
            translationKey="onboarding.tutorial.with.jenkins.dotnet.scanner.prereqs.step2"
          />
        </ListItem>
        <ListItem>
          <SentenceWithHighlights
            highlightKeys={['name']}
            translationKey="onboarding.tutorial.with.jenkins.dotnet.scanner.prereqs.step3"
          />
          <InlineSnippet className="sw-ml-1" snippet="SonarScanner for .NET" />
          <ClipboardIconButton className="sw-ml-2 sw-align-sub" copyValue="SonarScanner for .NET" />
        </ListItem>
        <ListItem>
          <SentenceWithHighlights
            highlightKeys={['install_from']}
            translationKey="onboarding.tutorial.with.jenkins.dotnet.scanner.prereqs.step5"
          />
        </ListItem>
      </OrderedList>
    </NumberedListItem>
  );
}
