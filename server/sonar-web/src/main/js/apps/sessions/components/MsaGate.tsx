import * as React from 'react';
import DataAccessConsent from '../../apps/sessions/components/DataAccessConsent';

export default function MsaGate({
  children,
  enabled,
}: {
  children: React.ReactNode;
  enabled: boolean;
}) {
  const [show, setShow] = React.useState(false);

  React.useEffect(() => {
    setShow(enabled);
  }, [enabled]);

  if (show) {
    return (
      <>
        {children}
        <DataAccessConsent
          disableSessionStorage={true}
          message={`<div>
      <h1>
      Welcome to AutoRABIT
    </h1>
      </div>
      <div> By accessing or using AutoRABIT's systems, you acknowledge and agree that your organization is bound by the terms of AutoRABIT’s
        <a href="https://www.autorabit.com/agreement/"> Master Software Agreement("MSA").</a>
        If your organization has a separately executed agreement with AutoRABIT governing its use of the services, that agreement will control to the extent of any conflict with the MSA.
        <br/><br/>
        By continuing, you confirm that you are authorized to accept the Master Software Agreement on behalf of your organization.
        </br>
        Please review the MSA carefully before proceeding.</div>`}
          requireCheckbox={true}
          checkboxText="I have read and agree to the MSA and confirm that I am authorized to accept on behalf of my organization."
          primaryButtonText="Accept and Continue"
        />
      </>
    );
  }

  return <>{children}</>;
}

