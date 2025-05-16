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

import {Helmet} from "react-helmet-async";
import {translate} from "../../helpers/l10n";
import {ButtonPrimary, FormField, InputField, InputTextArea, LargeCenteredLayout} from "~design-system";
import './styles.css'
import {Select} from "@sonarsource/echoes-react";
import {
  caseRecordTypeOptions,
  categoryOptions,
  classificationOptions, hostingTypeOptions, pluginTypeOptions,
  productOptions,
  severityOptions, versionOptions
} from "./constants";
import * as React from "react";
import {getValue} from "../../api/settings";
import {GlobalSettingKeys} from "../../types/settings";

export default function WebToCase() {

  const [pageReturnUrl, setPageReturnUrl] = React.useState<any>();
  const [supportSalesforceOrgId, setSupportSalesforceOrgId] = React.useState<any>();
  const [webToCaseURL, setWebToCaseURL] = React.useState<any>();
  React.useEffect(() => {
    setPageReturnUrl(window.location.href);

    getValue({key: GlobalSettingKeys.CodescanSupportSalesforceOrdId}).then((salesforceOrgId)=>{
      setSupportSalesforceOrgId(salesforceOrgId?.value);
      setWebToCaseURL("https://webto.salesforce.com/servlet/servlet.WebToCase?encoding=UTF-8&orgId="+salesforceOrgId?.value);
    });

  }, []);
  const header = translate('docs.web_to_case');
  return (
      <LargeCenteredLayout>
        <Helmet title={header} titleTemplate="%s"/>
        <div className="page page-limited huge-spacer-top huge-spacer-bottom">
          <header className="page-header huge-spacer-bottom page-header-ctnr">
            <h1 className="page-title huge center">
              <strong>{header}</strong>
            </h1>
          </header>
        </div>

        <form action={webToCaseURL} method="POST">

          <input type="hidden" name="orgid" value={supportSalesforceOrgId}/>
          <InputField name="retURL" type="hidden" value={pageReturnUrl}/>
          <div className="center">
            <div className="row">
              <Select className="sw-mb-4"
                  data={severityOptions}
                  id="00Ncs000003PUXR"
                  isRequired
                  isSearchable
                  label="Severity"
                  name="00Ncs000003PUXR"/>

              <Select className="sw-mb-4"
                  data={productOptions}
                  id="00Ncs000002a1qr"
                  isRequired
                  isSearchable
                  label="Product/Module"
                  name="00Ncs000002a1qr"/>

              <Select className="sw-mb-4"
                  data={classificationOptions}
                  id="00Ncs000002a31R"
                  isRequired
                  isSearchable
                  label="Classification"
                  name="00Ncs000002a31R"/>

              <Select className="sw-mb-4"
                  data={hostingTypeOptions}
                  id="00Ncs000002a5Xt"
                  isRequired
                  isSearchable
                  label="Hosting Type"
                  name="00Ncs000002a5Xt"/>

              <Select className="sw-mb-4"
                  data={versionOptions}
                  id="00Ncs000002a5mP"
                  isRequired
                  isSearchable
                  label="Version"
                  name="00Ncs000002a5mP"/>

              <Select className="sw-mb-4"
                  data={categoryOptions}
                  id="00Ncs000003PXwj"
                  isRequired
                  isSearchable
                  label="Category"
                  name="00Ncs000003PXwj"/>

              <Select className="sw-mb-4"
                  data={pluginTypeOptions}
                  id="00Ncs000003PWxR"
                  isRequired
                  isSearchable
                  label="Plugin Type"
                  name="00Ncs000003PWxR"/>
              <Select className="sw-mb-4"
                      data={caseRecordTypeOptions}
                      id="recordType"
                      isRequired
                      isSearchable
                      label="Case Record Type"
                      name="recordType"/>
            </div>
            <div className="row">
              <FormField htmlFor="name" label="Contact Name" required>
                <InputField autoFocus
                    id="name"
                    maxLength={80}
                    name="name"
                    required
                    size="full"
                    type="text"/>
              </FormField>

              <FormField htmlFor="email" label="Email" required>
                <InputField autoFocus
                    id="email"
                    maxLength={80}
                    name="email"
                    required
                    size="full"
                    type="text"/>
              </FormField>

              <FormField htmlFor="phone" label="Phone" required>
                <InputField autoFocus
                    id="phone"
                    maxLength={40}
                    name="phone"
                    required
                    size="full"
                    type="text"/>
              </FormField>
              <FormField htmlFor="company" label="Company" required>
                <InputField autoFocus
                            id="company"
                            maxLength={80}
                            name="company"
                            required
                            size="full"
                            type="text"/>
              </FormField>
              <FormField htmlFor="subject" label="Subject" required>
                <InputField autoFocus
                    id="subject"
                    maxLength={80}
                    name="subject"
                    required
                    size="full"
                    type="text"/>
              </FormField>

              <FormField htmlFor="description" label="Description" required>
                <InputTextArea autoFocus
                    id="description"
                    name="description"
                    required
                    rows={8}
                    size="full"/>
              </FormField>

            </div>
          </div>
          <span className="submit">
            <ButtonPrimary type="submit" className="submit-button" >Submit</ButtonPrimary>
          </span>

        </form>

      </LargeCenteredLayout>

  );
}