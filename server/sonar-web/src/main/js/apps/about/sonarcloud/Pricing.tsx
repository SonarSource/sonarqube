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
import * as React from 'react';
import Helmet from 'react-helmet';
import { addWhitePageClass, removeWhitePageClass } from 'sonar-ui-common/helpers/pages';
import { scrollToElement } from 'sonar-ui-common/helpers/scrolling';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import Footer from './components/Footer';
import { FixedNavBar, TopNavBar } from './components/NavBars';
import './new_style.css';

export default class Pricing extends React.PureComponent {
  container?: HTMLElement | null;

  componentDidMount() {
    addWhitePageClass();
  }

  componentWillUnmount() {
    removeWhitePageClass();
  }

  handleClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.stopPropagation();
    if (this.container) {
      scrollToElement(this.container, { bottomOffset: window.innerHeight - 200 });
    }
  };

  getReference = (node: HTMLElement | null) => {
    this.container = node;
  };

  render() {
    return (
      <div className="global-container">
        <div className="page-wrapper">
          <div className="page-container sc-page">
            <Helmet title="Plans and Pricing | SonarCloud">
              <meta
                content="Get all the SonarCloud features and functionality for free on your open-source projects. If you need privacy for your code, we have a pricing plan to fit your needs."
                name="description"
              />
            </Helmet>
            <FixedNavBar onPricingPage={true} />
            <PageBackgroundHeader />
            <TopNavBar onPricingPage={true} whiteLogo={true} />
            <ForEveryoneBlock onClick={this.handleClick} />
            <LoginCTA />
            <PricingFAQ getReference={this.getReference} />
          </div>
        </div>
        <Footer />
      </div>
    );
  }
}

function PageBackgroundHeader() {
  return (
    <div className="sc-header-background">
      <div className="sc-background-center">
        <img alt="" height="562px" src={`${getBaseUrl()}/images/sonarcloud/pricing-header.svg`} />
      </div>
    </div>
  );
}

interface ForEveryoneBlockProps {
  onClick: (event: React.MouseEvent<HTMLAnchorElement>) => void;
}

function ForEveryoneBlock({ onClick }: ForEveryoneBlockProps) {
  return (
    <div className="sc-section big-spacer-top">
      <h2 className="white text-center sc-big-spacer-top sc-big-spacer-bottom">
        SonarCloud is for everyone
      </h2>
      <div className="sc-pricing-free">
        <h4>
          Free for <span className="sc-title-orange">public projects</span>
        </h4>
        <ul className="big-spacer-top big-spacer-bottom">
          <li>
            <em>Unlimited lines of code</em>
          </li>
          <li>
            <em>Anyone can see your project</em> and browse your code
          </li>
          <li>
            You have access to the <em>full SonarCloud feature set</em>
          </li>
          <li>
            <em>Choose members of your team</em> who can work on your projects
          </li>
        </ul>
        <em>Loved by Open-Source Developers</em>
        <div className="sc-pricing-privacy">
          <h4 className="white">
            Need <span className="sc-title-orange">privacy?</span>
          </h4>
          <ul className="big-spacer-top big-spacer-bottom">
            <li>
              <em>Create private projects,</em> priced per lines of code.{' '}
              <a href="#" onClick={onClick}>
                See price list
              </a>
            </li>
            <li>
              <em>You have strict control</em> over who can view your private data
            </li>
          </ul>
          <div className="big-spacer-left">
            <div className="starts-at">starts at 10€/month</div>
            <em>Free 14-day trial</em>
          </div>
        </div>
      </div>
    </div>
  );
}

function LoginCTA() {
  return (
    <div className="sc-section text-center pricing-section sc-big-spacer-bottom">
      <h5 className="sc-big-spacer-top big-spacer-bottom">
        Log in to SonarCloud and choose your pricing plan
      </h5>
      <a className="sc-orange-button" href={`${getBaseUrl()}/sessions/new`}>
        Start Using SonarCloud
      </a>
    </div>
  );
}

interface PricingFAQProps {
  getReference: (node: HTMLDivElement | null) => void;
}

function PricingFAQ({ getReference }: PricingFAQProps) {
  return (
    <div className="sc-section pricing-section big-spacer-top sc-big-spacer-bottom">
      <h5 className="text-center sc-big-spacer-top sc-big-spacer-bottom">Pricing FAQ</h5>
      <div className="sc-columns">
        <div className="sc-column sc-column-medium display-flex-center">
          <div>
            <div className="faq-title" ref={getReference}>
              How does pricing work for private projects?
            </div>
            <p className="big-spacer-bottom">
              Subscribing to a paid plan on SonarCloud allows you to create a private organization
              containing private projects. You pay up front for a maximum number of private lines of
              code to be analyzed in your organization.
              <br />
              <br />
              Find your max LOC below to see what it will cost you per month:
            </p>
            <table className="loc-price sc-big-spacer-bottom">
              <thead>
                <tr>
                  <th>Up to lines of code</th>
                  <th>Price per month in €</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>100k</td>
                  <td>10</td>
                </tr>
                <tr>
                  <td>250k</td>
                  <td>75</td>
                </tr>
                <tr>
                  <td>500k</td>
                  <td>150</td>
                </tr>
                <tr>
                  <td>1M</td>
                  <td>250</td>
                </tr>
                <tr>
                  <td>2M</td>
                  <td>500</td>
                </tr>
                <tr>
                  <td>5M</td>
                  <td>1500</td>
                </tr>
                <tr>
                  <td>10M</td>
                  <td>3000</td>
                </tr>
                <tr>
                  <td>20M</td>
                  <td>4000</td>
                </tr>
              </tbody>
            </table>
            <div className="faq-title">What payment options are available ?</div>
            <p>
              Payment is done online by credit card and will happen automatically every month, based
              on the plan you choose. We also accept to receive a purchase order and a wire transfer
              payment, if ordering a yearly subscription for more than 1M LOCs. In this case, you
              need to contact us through the Contact form.
            </p>
            <div className="faq-title">Can I try a private project on SonarCloud for free?</div>
            <p>
              Your first 14 days are on us. You just have to upgrade your organization to a paid
              plan, and fill your credit card information to get started. After your trial, if you
              love it you can continue using SonarCloud and you will be charged for the plan you
              selected when you first started your free trial. You can cancel anytime.
            </p>
          </div>
        </div>
        <div className="sc-column sc-column-medium display-flex-center">
          <div>
            <div className="faq-title">What is a Line of Code (LOC) on SonarCloud?</div>
            <p>
              LOCs are computed by summing up the LOCs of each project analyzed in SonarCloud. The
              LOCs used for a project are the LOCs found during the most recent analysis of this
              project.
            </p>
            <div className="faq-title">How are Lines of Code (LOCs) counted towards billing?</div>
            <p>
              Only LOC from your private projects are counted toward your maximum number of LOCs. If
              your project contains branches, the counted LOCs are the ones of the biggest branch.
              The count is not related to how frequently the source code is analyzed. If your
              private project has a 6K LOCs and you analyze it 100 times in the month, this will be
              counted as 6K for the billing. If you are getting close to the threshold you will be
              notified to either upgrade your plan or reduce the number of LOCs in your projects.
            </p>
            <div className="faq-title">When will I be invoiced?</div>
            <p>
              You will be invoiced once a month, the day of the month after your trial ends. For
              example if you start your free trial on January 1st, it will last till January 14th
              and you will be first billed on January 15th for your upcoming month, e.g. January
              15th to February 15th.
            </p>
            <div className="faq-title">Can I cancel my subscription?</div>
            <p>
              Of course! There&apos;s no commitment. You can delete your paid organization whenever
              you wish, or simply downgrade to the free tier if you wish to keep on analysing some
              public projects.
            </p>
            <div className="faq-title">Still have more questions?</div>
            <p>
              Contact us <a href={`${getBaseUrl()}/about/contact`}>here</a>.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
