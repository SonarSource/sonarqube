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
import { Modal } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { SafeHTMLInjection, SanitizeLevel, CenteredLayout, HtmlFormatter} from '~design-system';
import { noop } from 'lodash';
import styled from '@emotion/styled';
import { themeColor } from '~design-system';


export interface DataAccessConsentProps {
  message: string;
  disableSessionStorage?: boolean;
  storageKey?: string;
  primaryButtonText?: string;
  requireCheckbox?: boolean;
  checkboxText?: string;
}

function hasUserAlreadyAcknowledged(storageKey: string) {
  const v = sessionStorage.getItem(storageKey);
  return v !== undefined && v !== null && v.length > 0;
}

export default function DataAccessConsent(props: Readonly<DataAccessConsentProps>) {
  const {
    message,
    disableSessionStorage = false,
    storageKey = 'us-data-protection-acknowledged',
    primaryButtonText,
    requireCheckbox = false,
    checkboxText,
  } = props;

  const [checked, setChecked] = React.useState(false);

  const [isOpen, setIsOpen] = React.useState(() => {
    if (disableSessionStorage) return true;
    return !hasUserAlreadyAcknowledged(storageKey);
  });

  const acceptConsent = () => {
    if (!disableSessionStorage) {
      sessionStorage.setItem(storageKey, 'true');
    }
    setIsOpen(false);
  };

  if (!isOpen) return null;

  const isPrimaryDisabled = requireCheckbox ? !checked : false;

  return (
    <Modal onClose={noop} closeOnOverlayClick={false}>
      <Modal.Body>
        <MsaContentStyle>
        <HtmlFormatter>
          <SafeHTMLInjection htmlAsString={message} />
        </HtmlFormatter>
        </MsaContentStyle>

        {requireCheckbox && (
          <label className="sw-flex sw-gap-2 sw-items-start sw-mt-4">
            <input
              type="checkbox"
              checked={checked}
              onChange={(e) => setChecked(e.currentTarget.checked)}
              style={{ marginTop: 3 }}
            />
            <span className="sw-text-sm">
              <strong>{checkboxText}</strong>
            </span>
          </label>
        )}
      </Modal.Body>

      <Modal.Footer
        primaryButton={
          <Button onClick={acceptConsent} variety={ButtonVariety.Primary} isDisabled={isPrimaryDisabled}>
            {primaryButtonText ? primaryButtonText : (
              <FormattedMessage id="login.us_data_protection_consent.button_primary" />
            )}
          </Button>
        }
        secondaryButton={null}
      />
    </Modal>
  );
}

const MsaContentStyle = styled.div`
  font-family: Overpass, sans-serif;

  h1{
    font-weight: 300;
    font-size: 1.5rem;
    line-height: 2rem;
    margin: 0 0 0.75rem 0;
  }

  p {
    margin: 0;
    color: ${themeColor('pageContent')};
    line-height: 1.25rem;
  }

  a {
    color: #2563eb;


  }
`;
