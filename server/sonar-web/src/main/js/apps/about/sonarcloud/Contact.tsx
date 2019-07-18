/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { Location } from 'history';
import * as React from 'react';
import { Helmet } from 'react-helmet';
import { Link } from 'react-router';
import Select from 'sonar-ui-common/components/controls/Select';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { isLoggedIn } from '../../../helpers/users';
import SQPageContainer from './components/SQPageContainer';
import './style.css';

const CATEGORIES = [
  { label: 'Commercial', value: 'commercial' },
  { label: 'Confidential Request', value: 'confidential_request' }
];

interface Props {
  location: Location;
}

interface State {
  category: string;
  organization: string;
  question: string;
  subject: string;
}

export default class Contact extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    const { query } = props.location;
    this.state = {
      category: query.category || '',
      organization: query.organization || '',
      question: query.question || '',
      subject: query.subject || ''
    };
  }

  getOrganizations = (organizations?: T.Organization[]) => {
    return (organizations || []).map(org => ({
      label: org.name,
      value: org.key
    }));
  };

  handleCategoryChange = ({ value }: { value: string }) => {
    this.setState({ category: value });
  };

  handleOrganizationChange = ({ value }: { value: string }) => {
    this.setState({ organization: value });
  };

  handleSubjectChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ subject: event.currentTarget.value });
  };

  handleQuestionChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({ question: event.currentTarget.value });
  };

  render() {
    return (
      <SQPageContainer>
        {({ currentUser, userOrganizations }) => (
          <div className="page page-limited sc-page sc-contact-page">
            <Helmet title="Contact Us | SonarCloud">
              <meta
                content="If you are looking for help with SonarCloud, our Support forum is the best place to get help."
                name="description"
              />
            </Helmet>
            <h1 className="sc-page-title">Contact us</h1>
            <Alert display="inline" variant="warning">
              If you are looking for help with SonarCloud, our{' '}
              <a
                href="https://community.sonarsource.com/c/help/sc"
                rel="noopener noreferrer"
                target="_blank">
                <strong>Support forum</strong>
              </a>{' '}
              is the best place to get help.
            </Alert>
            <br />
            <Alert display="inline" variant="warning">
              Use this contact form for commercial or confidential requests only.
            </Alert>
            {!isLoggedIn(currentUser) && (
              <p>
                You can{' '}
                <Link to={{ pathname: '/sessions/new', query: { return_to: '/about/contact' } }}>
                  log in to SonarCloud
                </Link>{' '}
                to automatically fill this form information and get better support.
              </p>
            )}
            <form action="https://formspree.io/contact@sonarcloud.io" method="POST">
              <div className="form-group">
                <label htmlFor="contact-name">Name</label>
                <input
                  autoFocus={true}
                  defaultValue={isLoggedIn(currentUser) ? currentUser.name : ''}
                  id="contact-name"
                  name="name"
                  required={true}
                  type="text"
                />
              </div>
              <div className="form-group">
                <label htmlFor="contact-email">Email</label>
                <input
                  defaultValue={isLoggedIn(currentUser) ? currentUser.email : ''}
                  id="contact-email"
                  name="_replyto"
                  required={true}
                  type="email"
                />
              </div>
              <div className="form-group category-select">
                <label htmlFor="contact-category">Category</label>
                <Select
                  id="contact-category"
                  name="category"
                  onChange={this.handleCategoryChange}
                  options={CATEGORIES}
                  placeholder="Choose a category"
                  required={true}
                  searchable={false}
                  value={this.state.category}
                />
                <input
                  className="category-select-helper"
                  required={true}
                  tabIndex={-1}
                  value={this.state.category}
                />
              </div>
              {isLoggedIn(currentUser) && (
                <div className="form-group category-select">
                  <label htmlFor="contact-organization">Organization concerned by the issue</label>
                  <Select
                    id="contact-organization"
                    name="organization"
                    onChange={this.handleOrganizationChange}
                    options={this.getOrganizations(userOrganizations)}
                    placeholder="Choose an organization"
                    searchable={false}
                    value={this.state.organization}
                  />
                </div>
              )}
              <div className="form-group">
                <label htmlFor="contact-subject">Subject</label>
                <input
                  id="contact-subject"
                  maxLength={70}
                  onChange={this.handleSubjectChange}
                  required={true}
                  type="text"
                  value={this.state.subject}
                />
                <input
                  name="_subject"
                  type="hidden"
                  value={`[${this.state.category}] ${this.state.subject}`}
                />
              </div>
              <div className="form-group">
                <label htmlFor="contact-question">How can we help?</label>
                <textarea
                  className="form-control"
                  id="contact-question"
                  name="question"
                  onChange={this.handleQuestionChange}
                  placeholder="Please describe precisely what is your issue..."
                  required={true}
                  rows={8}
                  value={this.state.question}
                />
              </div>
              <div className="form-group">
                {
                  // The following hidden input field must absolutely be kept
                  // This is a "honeypot" field to avoid spam by fooling scrapers
                }
                <input name="_gotcha" type="text" />
                <button type="submit">Send Request</button>
              </div>
              {isLoggedIn(currentUser) && (
                <input name="login" type="hidden" value={currentUser.login} />
              )}
            </form>
          </div>
        )}
      </SQPageContainer>
    );
  }
}
