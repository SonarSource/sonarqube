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

import { ClipboardIconButton, CodeSnippet, HelperHintIcon, SubHeading } from '~design-system';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { translate } from '../../../../helpers/l10n';

interface MetaKeyProps {
  componentKey: string;
  qualifier: string;
}

export default function MetaKey({ componentKey, qualifier }: MetaKeyProps) {
  return (
    <>
      <div className="sw-flex sw-items-baseline">
        <SubHeading>{translate('overview.project_key', qualifier)}</SubHeading>
        <HelpTooltip
          className="sw-ml-1"
          overlay={
            <p className="sw-max-w-abs-250">
              {translate('overview.project_key.tooltip', qualifier)}
            </p>
          }
        >
          <HelperHintIcon />
        </HelpTooltip>
      </div>
      <div className="sw-w-full">
        <div className="sw-flex sw-gap-2 sw-items-center sw-min-w-0">
          <CodeSnippet className="sw-min-w-0 sw-px-1" isOneLine noCopy snippet={componentKey} />
          <ClipboardIconButton copyValue={componentKey} />
        </div>
      </div>
    </>
  );
}
