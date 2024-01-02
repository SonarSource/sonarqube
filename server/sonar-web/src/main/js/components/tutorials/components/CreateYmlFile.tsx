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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { ClipboardIconButton } from '../../../components/controls/clipboard';
import { translate } from '../../../helpers/l10n';
import CodeSnippet from '../../common/CodeSnippet';

export interface CreateYmlFileProps {
  yamlFileName: string;
  yamlTemplate: string;
}

export default function CreateYmlFile(props: CreateYmlFileProps) {
  const { yamlTemplate, yamlFileName } = props;
  return (
    <li className="abs-width-800">
      <FormattedMessage
        defaultMessage={translate('onboarding.tutorial.with.github_action.yaml.create_yml')}
        id="onboarding.tutorial.with.github_action.yaml.create_yml"
        values={{
          file: (
            <>
              <code className="rule">{yamlFileName}</code>
              <ClipboardIconButton copyValue={yamlFileName} />
            </>
          ),
        }}
      />
      <CodeSnippet snippet={yamlTemplate} />
    </li>
  );
}
