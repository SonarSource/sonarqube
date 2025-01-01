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

import styled from '@emotion/styled';
import * as React from 'react';
import {
  CodeSyntaxHighlighter,
  FlagMessage,
  HtmlFormatter,
  SanitizeLevel,
  ToggleButton,
  themeBorder,
  themeColor,
} from '~design-system';
import { RuleDescriptionSection, RuleDescriptionSections } from '../../apps/coding-rules/rule';
import applyCodeDifferences from '../../helpers/code-difference';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { isDefined } from '../../helpers/types';
import { useCveQuery } from '../../queries/cves';
import { CveDetails } from './CveDetails';
import OtherContextOption from './OtherContextOption';

const OTHERS_KEY = 'others';

interface Props {
  className?: string;
  cveId?: string;
  defaultContextKey?: string;
  language?: string;
  sections: RuleDescriptionSection[];
}

interface RuleDescriptionContextDisplay {
  content: string;
  displayName: string;
  key: string;
}

export default function RuleDescription({
  className,
  cveId,
  defaultContextKey,
  language,
  sections,
}: Readonly<Props>) {
  const [contexts, setContexts] = React.useState<RuleDescriptionContextDisplay[]>([]);
  const [defaultContext, setDefaultContext] = React.useState<
    RuleDescriptionContextDisplay | undefined
  >();
  const [selectedContext, setSelectedContext] = React.useState<
    RuleDescriptionContextDisplay | undefined
  >();

  const { data: cveData } = useCveQuery({ cveId });

  React.useEffect(() => {
    const contexts = sections
      .filter(
        (
          section,
        ): section is RuleDescriptionSection & Required<Pick<RuleDescriptionSection, 'context'>> =>
          section.context != null,
      )
      .map((section) => ({
        displayName: section.context.displayName || section.context.key,
        content: section.content,
        key: section.context.key,
      }))
      .sort((a, b) => a.displayName.localeCompare(b.displayName));

    if (contexts.length > 0) {
      contexts.push({
        displayName: translate('coding_rules.description_context.other'),
        content: '',
        key: OTHERS_KEY,
      });
    }

    let defaultContext: RuleDescriptionContextDisplay | undefined;

    if (defaultContextKey) {
      defaultContext = contexts.find((context) => context.key === defaultContextKey);
    }

    setContexts(contexts);
    setDefaultContext(defaultContext);
    setSelectedContext(defaultContext ?? contexts[0]);
  }, [sections, defaultContextKey]);

  const handleToggleContext = (value: string) => {
    const selected = contexts.find((ctxt) => ctxt.displayName === value);
    if (selected) {
      setSelectedContext(selected);
    }
  };

  const introductionSection = sections?.find(
    (section) => section.key === RuleDescriptionSections.INTRODUCTION,
  )?.content;

  const options = contexts.map((ctxt) => ({
    label: ctxt.displayName,
    value: ctxt.displayName,
  }));

  if (contexts.length > 0 && selectedContext) {
    return (
      <StyledHtmlFormatter
        className={className}
        ref={(node: HTMLDivElement) => {
          applyCodeDifferences(node);
        }}
      >
        <h2 className="sw-typo-semibold sw-mb-4">
          {translate('coding_rules.description_context.title')}
        </h2>
        {isDefined(introductionSection) && (
          <CodeSyntaxHighlighter
            className="rule-desc"
            htmlAsString={introductionSection}
            language={language}
            sanitizeLevel={SanitizeLevel.FORBID_SVG_MATHML}
          />
        )}
        {defaultContext && (
          <FlagMessage variant="info" className="sw-mb-4">
            {translateWithParameters(
              'coding_rules.description_context.default_information',
              defaultContext.displayName,
            )}
          </FlagMessage>
        )}
        <div className="sw-mb-4">
          <ToggleButton
            label={translate('coding_rules.description_context.title')}
            onChange={handleToggleContext}
            options={options}
            value={selectedContext.displayName}
          />

          {selectedContext.key !== OTHERS_KEY && (
            <h2>
              {translateWithParameters(
                'coding_rules.description_context.subtitle',
                selectedContext.displayName,
              )}
            </h2>
          )}
        </div>
        {selectedContext.key === OTHERS_KEY ? (
          <OtherContextOption />
        ) : (
          <CodeSyntaxHighlighter
            htmlAsString={selectedContext.content}
            language={language}
            sanitizeLevel={SanitizeLevel.FORBID_SVG_MATHML}
          />
        )}

        {cveData && <CveDetails cve={cveData} />}
      </StyledHtmlFormatter>
    );
  }

  return (
    <StyledHtmlFormatter
      className={className}
      ref={(node: HTMLDivElement) => {
        applyCodeDifferences(node);
      }}
    >
      {isDefined(introductionSection) && (
        <CodeSyntaxHighlighter
          className="rule-desc"
          htmlAsString={introductionSection}
          language={language}
          sanitizeLevel={SanitizeLevel.FORBID_SVG_MATHML}
        />
      )}

      <CodeSyntaxHighlighter
        htmlAsString={sections[0].content}
        language={language}
        sanitizeLevel={SanitizeLevel.FORBID_SVG_MATHML}
      />

      {cveData && <CveDetails cve={cveData} />}
    </StyledHtmlFormatter>
  );
}

const StyledHtmlFormatter = styled(HtmlFormatter)`
  margin-top: 1.5rem;
  margin-bottom: 1.5rem;

  .code-difference-container {
    flex-direction: column;
    width: fit-content;
    min-width: 100%;
  }

  .code-difference-scrollable {
    background-color: ${themeColor('codeSnippetBackground')};
    border: ${themeBorder('default', 'codeSnippetBorder')};
    border-radius: 0.5rem;
    padding: 1.5rem;
    overflow-x: auto;
  }

  .code-difference-scrollable .code-added,
  .code-difference-scrollable .code-removed {
    padding-left: 1.5rem;
    margin-left: -1.5rem;
    padding-right: 1.5rem;
    margin-right: -1.5rem;
    border-radius: 0;
  }

  .code-difference-scrollable .code-added {
    background-color: ${themeColor('codeLineCoveredUnderline')};
  }

  .code-difference-scrollable .code-removed {
    background-color: ${themeColor('codeLineUncoveredUnderline')};
  }

  a:has(code) {
    padding-bottom: 0.125rem;
  }
`;
