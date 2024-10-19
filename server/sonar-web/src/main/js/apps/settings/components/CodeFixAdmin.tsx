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
import { Button, ButtonVariety, Checkbox, LinkStandalone } from '@sonarsource/echoes-react';
import { BasicSeparator, Title } from 'design-system';
import React, { useEffect } from 'react';
import { FormattedMessage } from 'react-intl';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import { translate } from '../../../helpers/l10n';
import { getAiCodeFixTermsOfServiceUrl } from '../../../helpers/urls';
import { useRemoveCodeSuggestionsCache } from '../../../queries/fix-suggestions';
import { useGetValueQuery, useSaveSimpleValueMutation } from '../../../queries/settings';
import { Feature } from '../../../types/features';
import { SettingsKey } from '../../../types/settings';
import PromotedSection from '../../overview/branches/PromotedSection';

interface Props extends WithAvailableFeaturesProps {}

const CODE_FIX_SETTING_KEY = SettingsKey.CodeSuggestion;

function CodeFixAdmin({ hasFeature }: Readonly<Props>) {
  const { data: codeFixSetting } = useGetValueQuery({
    key: CODE_FIX_SETTING_KEY,
  });

  const removeCodeSuggestionsCache = useRemoveCodeSuggestionsCache();

  const { mutate: saveSetting } = useSaveSimpleValueMutation();

  const isCodeFixEnabled = codeFixSetting?.value === 'true';

  const [enableCodeFix, setEnableCodeFix] = React.useState(isCodeFixEnabled);
  const [acceptedTerms, setAcceptedTerms] = React.useState(false);
  const isValueChanged = enableCodeFix !== isCodeFixEnabled;

  useEffect(() => {
    setEnableCodeFix(isCodeFixEnabled);
  }, [isCodeFixEnabled]);

  const handleSave = () => {
    saveSetting(
      { key: CODE_FIX_SETTING_KEY, value: enableCodeFix ? 'true' : 'false' },
      {
        onSuccess: removeCodeSuggestionsCache,
      },
    );
  };

  const handleCancel = () => {
    setEnableCodeFix(isCodeFixEnabled);
    setAcceptedTerms(false);
  };

  if (!hasFeature(Feature.FixSuggestions)) {
    return null;
  }

  return (
    <div className="sw-flex">
      <div className="sw-flex-1 sw-p-6">
        <Title className="sw-heading-md sw-mb-6">{translate('property.codefix.admin.title')}</Title>
        <PromotedSection
          content={
            <>
              <p>{translate('property.codefix.admin.promoted_section.content1')}</p>
              <p className="sw-mt-2">
                {translate('property.codefix.admin.promoted_section.content2')}
              </p>
            </>
          }
          title={translate('property.codefix.admin.promoted_section.title')}
        />
        <p>{translate('property.codefix.admin.description')}</p>
        <Checkbox
          className="sw-mt-6"
          label={translate('property.codefix.admin.checkbox.label')}
          checked={Boolean(enableCodeFix)}
          onCheck={() => setEnableCodeFix(!enableCodeFix)}
        />
        {isValueChanged && (
          <div>
            <BasicSeparator className="sw-mt-6" />
            {enableCodeFix && (
              <Checkbox
                className="sw-mt-6"
                label={
                  <FormattedMessage
                    id="property.codefix.admin.terms"
                    defaultMessage={translate('property.codefix.admin.acceptTerm.label')}
                    values={{
                      terms: (
                        <LinkStandalone to={getAiCodeFixTermsOfServiceUrl()}>
                          {translate('property.codefix.admin.acceptTerm.terms')}
                        </LinkStandalone>
                      ),
                    }}
                  />
                }
                checked={acceptedTerms}
                onCheck={() => setAcceptedTerms(!acceptedTerms)}
              />
            )}
            <div className="sw-mt-6">
              <Button
                variety={ButtonVariety.Primary}
                isDisabled={!acceptedTerms && enableCodeFix}
                onClick={() => {
                  handleSave();
                }}
              >
                {translate('save')}
              </Button>
              <Button className="sw-ml-3" variety={ButtonVariety.Default} onClick={handleCancel}>
                {translate('cancel')}
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default withAvailableFeatures(CodeFixAdmin);
