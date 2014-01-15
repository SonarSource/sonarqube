#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
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

require "json"

class Api::GroupsController < Api::ApiController

  # GETs should be safe (see http://www.w3.org/2001/tag/doc/whenToUseGet.html)

  # GET /api/groups/search?<parameters>
  def search
	# TODO search using parameters
    groups = Group.all
	
    hash = {:groups => groups.map { |group| group.to_hash }}
	
    respond_to do |format|
      format.json { render :json => jsonp(hash) }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'groups') }
    end
  end
  
  def create
    verify_post_request
    access_denied unless has_role?(:admin)
    require_parameters :name

    group = Group.find_by_name(params[:name])

    if group
      render_bad_request('A group with this name already exists')
    else
      group = prepare_group
      group.save!
      hash = group.to_hash
      respond_to do |format|
        format.json { render :json => jsonp(group) }
        format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'groups') }
	  end
    end
  end

  def update
    verify_post_request
    access_denied unless has_role?(:admin)
    require_parameters :name

    group = Group.find_by_name(params[:name])

    if group.nil?
      render_bad_request("Could not find group with name #{params[:name]}")
    elsif group.update_attributes!(params)
      hash = group.to_hash
      respond_to do |format|
        format.json { render :json => jsonp(hash) }
        format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'groups') }
      end
    end
  end

  def destroy
    bad_request("Missing group key") unless params[:name].present?

    group = Group.find_by_name(params[:name])
    bad_request("Not valid group") unless group
    access_denied unless has_role?(:admin)

	Group.delete( group )

    render_success("Group deleted")
  end
  
  protected

  def groups_to_json(groups)
    json = []
    groups.each do |group|
      json << group.to_hash_json
    end
    json
  end

  def groups_to_xml(groups)
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct! 
    xml.metrics do
      groups.each do |group|
        xml << group.to_xml(params)
      end
    end
  end

  private

  def prepare_group
    group = Group.new(params)
	group
  end

end
