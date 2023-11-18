/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
  ActionCell,
  ButtonPrimary,
  ContentCell,
  HelperHintIcon,
  InteractiveIcon,
  LargeCenteredLayout,
  Link,
  PageContentFontWrapper,
  PencilIcon,
  Spinner,
  Table,
  TableRow,
  TableRowInteractive,
  Title,
} from 'design-system';
import { groupBy, orderBy } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { Profile } from '../../api/quality-profiles';
import A11ySkipTarget from '../../components/a11y/A11ySkipTarget';
import HelpTooltip from '../../components/controls/HelpTooltip';
import Suggestions from '../../components/embed-docs-modal/Suggestions';
import { translate } from '../../helpers/l10n';
import { getRulesUrl } from '../../helpers/urls';
import { Component } from '../../types/types';
import BuiltInQualityProfileBadge from '../quality-profiles/components/BuiltInQualityProfileBadge';
import AddLanguageModal from './components/AddLanguageModal';
import SetQualityProfileModal from './components/SetQualityProfileModal';
import { ProjectProfile } from './types';

export interface ProjectQualityProfilesAppRendererProps {
  allProfiles?: Profile[];
  component: Component;
  loading: boolean;
  onAddLanguage: (key: string) => Promise<void>;
  onCloseModal: () => void;
  onOpenAddLanguageModal: () => void;
  onOpenSetProfileModal: (projectProfile: ProjectProfile) => void;
  onSetProfile: (newKey: string | undefined, oldKey: string) => Promise<void>;
  projectProfiles?: ProjectProfile[];
  showAddLanguageModal?: boolean;
  showProjectProfileInModal?: ProjectProfile;
}

export default function ProjectQualityProfilesAppRenderer(
  props: Readonly<ProjectQualityProfilesAppRendererProps>,
) {
  const {
    allProfiles,
    component,
    loading,
    showProjectProfileInModal,
    projectProfiles,
    showAddLanguageModal,
  } = props;

  const profilesByLanguage = groupBy(allProfiles, 'language');
  const orderedProfiles = orderBy(projectProfiles, (p) => p.profile.languageName);

  const COLUMN_WIDTHS_WITH_PURGE_SETTING = ['auto', 'auto', 'auto', '5%'];

  const header = (
    <TableRow>
      <ContentCell>{translate('language')}</ContentCell>
      <ContentCell>{translate('project_quality_profile.current')}</ContentCell>
      <ContentCell>{translate('coding_rules.filters.activation.active_rules')}</ContentCell>
      <ActionCell>{translate('actions')}</ActionCell>
    </TableRow>
  );

  return (
    <LargeCenteredLayout id="project-quality-profiles">
      <PageContentFontWrapper className="sw-my-8 sw-body-sm">
        <Suggestions suggestions="project_quality_profiles" />
        <Helmet defer={false} title={translate('project_quality_profiles.page')} />
        <A11ySkipTarget anchor="profiles_main" />

        <header className="sw-mb-2 sw-flex sw-items-center">
          <Title>{translate('project_quality_profiles.page')}</Title>
          <HelpTooltip
            className="sw-ml-2 sw-mb-4"
            overlay={translate('quality_profiles.list.projects.help')}
          >
            <HelperHintIcon aria-label="help-tooltip" />
          </HelpTooltip>
        </header>

        <div>
          <p>{translate('project_quality_profiles.page.description')}</p>
          <div className="sw-mt-16">
            <Spinner loading={loading}>
              {!loading && orderedProfiles.length > 0 && (
                <Table
                  noHeaderTopBorder
                  className="sw-w-[60%]"
                  columnCount={COLUMN_WIDTHS_WITH_PURGE_SETTING.length}
                  columnWidths={COLUMN_WIDTHS_WITH_PURGE_SETTING}
                  header={header}
                >
                  {orderedProfiles.map((projectProfile) => {
                    const { profile, selected } = projectProfile;

                    return (
                      <TableRowInteractive key={profile.language}>
                        <ContentCell>
                          <span>{profile.languageName}</span>
                        </ContentCell>
                        <ContentCell>
                          <span>
                            {!selected && profile.isDefault ? (
                              <em>{translate('project_quality_profile.instance_default')}</em>
                            ) : (
                              <>
                                {profile.name}
                                {profile.isBuiltIn && (
                                  <BuiltInQualityProfileBadge className="spacer-left" />
                                )}
                              </>
                            )}
                          </span>
                        </ContentCell>
                        <ContentCell>
                          <Link to={getRulesUrl({ activation: 'true', qprofile: profile.key })}>
                            {profile.activeRuleCount}
                          </Link>
                        </ContentCell>

                        <ActionCell>
                          <InteractiveIcon
                            Icon={PencilIcon}
                            aria-label={translate('project_quality_profile.change_profile')}
                            onClick={() => {
                              props.onOpenSetProfileModal(projectProfile);
                            }}
                            size="small"
                            stopPropagation={false}
                          />
                        </ActionCell>
                      </TableRowInteractive>
                    );
                  })}
                </Table>
              )}

              <div className="sw-mt-8">
                <div className="sw-mb-4">
                  {translate('project_quality_profile.add_language.description')}
                </div>

                <ButtonPrimary disabled={loading} onClick={props.onOpenAddLanguageModal}>
                  {translate('project_quality_profile.add_language.action')}
                </ButtonPrimary>
              </div>

              {showProjectProfileInModal && (
                <SetQualityProfileModal
                  availableProfiles={profilesByLanguage[showProjectProfileInModal.profile.language]}
                  component={component}
                  currentProfile={showProjectProfileInModal.profile}
                  onClose={props.onCloseModal}
                  onSubmit={props.onSetProfile}
                  usesDefault={!showProjectProfileInModal.selected}
                />
              )}

              {showAddLanguageModal && projectProfiles && (
                <AddLanguageModal
                  profilesByLanguage={profilesByLanguage}
                  onClose={props.onCloseModal}
                  onSubmit={props.onAddLanguage}
                  unavailableLanguages={projectProfiles.map((p) => p.profile.language)}
                />
              )}
            </Spinner>
          </div>
        </div>
      </PageContentFontWrapper>
    </LargeCenteredLayout>
  );
}
