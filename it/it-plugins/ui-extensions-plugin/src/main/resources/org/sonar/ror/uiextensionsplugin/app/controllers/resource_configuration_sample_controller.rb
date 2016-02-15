class ResourceConfigurationSampleController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE

  def index
    init_resource_for_role(:user, :resource) if params[:resource]
  end

end
