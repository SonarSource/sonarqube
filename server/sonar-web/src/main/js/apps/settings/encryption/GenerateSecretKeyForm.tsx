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
  ButtonPrimary,
  ClipboardIconButton,
  CodeSnippet,
  ListItem,
  SubHeading,
  UnorderedList,
} from 'design-system';
import * as React from 'react';
import { useCallback, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../components/common/DocumentationLink';
import Spinner from '../../../components/ui/Spinner';
import { translate } from '../../../helpers/l10n';

interface Props {
  generateSecretKey: () => Promise<void>;
  secretKey?: string;
}

export default function GenerateSecretKeyForm({ secretKey, generateSecretKey }: Readonly<Props>) {
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = useCallback(
    (event: React.FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      setSubmitting(true);
      generateSecretKey().then(
        () => setSubmitting(false),
        () => setSubmitting(false),
      );
    },
    [generateSecretKey],
  );

  return (
    <div id="generate-secret-key-form-container">
      {secretKey ? (
        <>
          <div className="sw-mb-4">
            <SubHeading id="secret-key-title">{translate('encryption.secret_key')}</SubHeading>
            <div className="sw-flex">
              <CodeSnippet className="it__secret-key sw-p-1" isOneLine noCopy snippet={secretKey} />
              <ClipboardIconButton
                aria-label={translate('copy_to_clipboard')}
                className="sw-ml-2"
                copyValue={secretKey}
              />
            </div>
          </div>
          <SubHeading className="sw-mb-2">{translate('encryption.how_to_use')}</SubHeading>
          <div>
            <UnorderedList ticks>
              <ListItem>
                <FormattedMessage
                  defaultMessage={translate('encryption.how_to_use.content1')}
                  id="encryption.how_to_use.content1"
                  values={{
                    secret_file: (
                      <CodeSnippet isOneLine noCopy snippet="~/.sonar/sonar-secret.txt" />
                    ),
                    property: <CodeSnippet isOneLine noCopy snippet="sonar.secretKeyPath" />,
                    propreties_file: (
                      <CodeSnippet isOneLine noCopy snippet="conf/sonar.properties" />
                    ),
                  }}
                />
              </ListItem>
              <ListItem>{translate('encryption.how_to_use.content2')}</ListItem>
              <ListItem>
                <FormattedMessage
                  defaultMessage={translate('encryption.how_to_use.content3')}
                  id="encryption.how_to_use.content3"
                  values={{
                    property: <CodeSnippet isOneLine noCopy snippet="sonar.secretKeyPath" />,
                  }}
                />
              </ListItem>
              <ListItem>{translate('encryption.how_to_use.content4')}</ListItem>
            </UnorderedList>
          </div>
        </>
      ) : (
        <form id="generate-secret-key-form" onSubmit={handleSubmit}>
          <p>
            <FormattedMessage
              defaultMessage={translate('encryption.secret_key_description')}
              id="encryption.secret_key_description"
              values={{
                moreInformationLink: (
                  <DocumentationLink to="/instance-administration/security/">
                    {translate('more_information')}
                  </DocumentationLink>
                ),
              }}
            />
          </p>
          <ButtonPrimary className="sw-mt-4" type="submit" disabled={submitting}>
            {translate('encryption.generate_secret_key')}
          </ButtonPrimary>
          <Spinner loading={submitting} />
        </form>
      )}
    </div>
  );
}
