#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2016 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#
class Api::ResourcesController < Api::ApiController

  # since version 3.3
  # Exemple : /api/resources/search?s=sonar
  #
  # -- Optional parameters
  # 'display_key' is used to return the resource key instead of the resource id. Default is false
  #
  def search
    search_text = params[:s]||''
    page=(params[:p] ? params[:p].to_i : 1)
    page_size=(params[:ps] ? params[:ps].to_i : 10)
    display_key=params[:display_key]||false
    display_uuid=params[:display_uuid]||false
    if params[:q]
      qualifiers=params[:q].split(',')
    elsif params[:qp]
      qualifiers=Java::OrgSonarServerUi::JRubyFacade.getInstance().getQualifiersWithProperty(params[:qp])
    else
      qualifiers=[]
    end

    bad_request("Page index must be greater than 0") if page<=0
    bad_request("Page size must be greater than 0") if page_size<=0

    key = escape_like(search_text).downcase
    conditions=['kee like ?']
    condition_values=[key + '%']

    unless qualifiers.empty?
      conditions<<'qualifier in (?)'
      condition_values<<qualifiers
    end
    indexes = ResourceIndex.all(:select => 'distinct(component_uuid),root_component_uuid,qualifier,name_size', # optimization to not load unused columns like 'kee'
                                 :conditions => [conditions.join(' and ')].concat(condition_values),
                                 :order => 'name_size')

    indexes = select_authorized(:user, indexes)
    total = indexes.size

    select2_format=(params[:f]=='s2')

    if select2_format && qualifiers.size>1
      # select2.js does not manage lazy loading of grouped options -> (almost) all the results are returned
      resource_uuids=indexes[0...100].map { |index| index.component_uuid }
    else
      # we don't group results when only one qualifier is requested, so we can enable lazy loading (pagination)
      offset=(page-1)*page_size
      resource_uuids=indexes[offset...offset+page_size].map { |index| index.component_uuid }
    end

    resources=[]
    unless resource_uuids.empty?
      resources=Project.all(:select => 'id,qualifier,name,long_name,kee,uuid', :conditions => ['uuid in (?) and enabled=?', resource_uuids, true])
    end

    if select2_format
      # customize format for select2.js (see ApplicationHelper::resource_select_tag)
      if qualifiers.size>1
        resources_by_qualifier = resources.group_by(&:qualifier)
        json = {
          :more => false,
          :results => resources_by_qualifier.map { |qualifier, grouped_resources| {:text => message("qualifiers.#{qualifier}"),
                                                                                   :children => grouped_resources.map { |r| {:id => display_key ? r.key : r.id, :text => r.name(true)} }} }
        }
      else
        json = {
          :more => (page * page_size)<total,
          :results => resources.map { |resource| {:id => display_uuid ? resource.uuid : (display_key ? resource.key : resource.id), :text => resource.name(true)} }
        }
      end
    else
      json = {:total => total, :page => page, :page_size => page_size, :data => resources.map { |r| {:id => r.id, :key => r.key, :nm => r.name(true), :q => r.qualifier} }}
    end

    respond_to do |format|
      format.json { render :json => jsonp(json) }
      format.xml { render :xml => xml_not_supported }
      format.text { render :text => text_not_supported }
    end
  end

end
