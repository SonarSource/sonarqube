import { Button, ButtonVariety, Modal } from "@sonarsource/echoes-react";
import { translate } from "../../helpers/l10n";
import * as React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { addGlobalErrorMessage, addGlobalSuccessMessage, FlagMessage, FormField, InputField } from "design-system";
import MandatoryFieldsExplanation from "../../components/ui/MandatoryFieldsExplanation";
import { OrganizationContextProps } from "../organizations/OrganizationContext";
import { useOutletContext } from "react-router-dom";
import { AxiosError, AxiosResponse } from "axios";
import { useCreateProjectMutation } from "../../queries/projects";
import { parseErrorResponse } from "../../helpers/request";

type Props = {
  isOpen: boolean;
  onOpenChange: (isOpen: boolean) => void;
  onClose: () => void;
}

const FORM_ID = 'create-project-form';
const BAD_REQUEST = 400;
const INTERNAL_SERVER_ERROR = 500;

export default function CreateProjectForm(props: Readonly<Props>) {

  const { isOpen, onOpenChange, onClose } = props;
  const intl = useIntl();
  const { organization } = useOutletContext<OrganizationContextProps>();

  const { mutate: createProject, isPending: submitting } = useCreateProjectMutation();

  const [error, setError] = React.useState<string | undefined>(undefined);
  const [changedParams, setChangedParams] = React.useState<Record<string, string>>({});

  React.useEffect(() => {
    if (!isOpen) {
      setChangedParams({ name: '', project: '', mainBranch: '' });
    }
  }, [isOpen]);

  const handleParameterChange = (
    event: React.SyntheticEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    const { name, value } = event.currentTarget;
    setChangedParams({ ...changedParams, [name]: value });
  };

  const handleError = (error: AxiosError<AxiosResponse>) => {
    const { response } = error;
    const message = parseErrorResponse(response);

    if (!response || ![BAD_REQUEST, INTERNAL_SERVER_ERROR].includes(response.status)) {
      addGlobalErrorMessage(message);
    } else {
      setError(message);
    }
  };

  const handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    createProject(
      {
        name: changedParams.name,
        project: changedParams.project,
        mainBranch: changedParams.mainBranch,
        organization: organization!.kee,
      },
      {
        onSuccess: () => {
          addGlobalSuccessMessage(
            <FormattedMessage
              defaultMessage={translate(
                'projects_management.project_has_been_successfully_created'
              )}
              id="projects_management.project_has_been_successfully_created"
              values={{
                project: changedParams.name,
              }}
            />
          );
          props.onClose();
        },
        onError: handleError
      },
    );
  };

  const formBody = () => {
    return (
      <form id={FORM_ID} onSubmit={handleFormSubmit}>
        {error && (
          <FlagMessage id="it__error-message" className="sw-mb-4" variant="error">
            {error}
          </FlagMessage>
        )}

        <FormField label={translate('onboarding.create_project.display_name')} htmlFor="name" required>
          <InputField
            id="name"
            name="name"
            size="full"
            type="text"
            onChange={handleParameterChange}
            value={changedParams['name'] ?? ''}
            maxLength={2000}
            required
            autoFocus
          />
        </FormField>

        <FormField label={translate('onboarding.create_project.project_key')} htmlFor="project" required>
          <InputField
            id="project"
            name="project"
            size="full"
            type="text"
            onChange={handleParameterChange}
            value={changedParams['project'] ?? ''}
            maxLength={400}
            required
          />
        </FormField>

        <FormField label={translate('onboarding.create_project.main_branch_name')} htmlFor="branch" required>
          <InputField
            id="branch"
            name="mainBranch"
            size="full"
            type="text"
            onChange={handleParameterChange}
            value={changedParams['mainBranch'] ?? ''}
            maxLength={400}
            required
          />
        </FormField>
      </form>
    )
  }

  return (
    <Modal
      title={translate('projects.add')}
      description={<MandatoryFieldsExplanation/>}
      isOpen={isOpen}
      onOpenChange={onOpenChange}
      primaryButton={
        <Button
          variety={ButtonVariety.Primary}
          isDisabled={submitting}
          isLoading={submitting}
          form={FORM_ID}
          type="submit"
        >
          {intl.formatMessage({ id: 'create' })}
        </Button>
      }
      secondaryButton={
        <Button
          variety={ButtonVariety.Default}
          isDisabled={submitting}
          onClick={onClose}
        >
          {intl.formatMessage({ id: 'cancel' })}
        </Button>
      }
      content={formBody()}
    />
  )
}
