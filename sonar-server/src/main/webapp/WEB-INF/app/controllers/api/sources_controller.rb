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

class Api::SourcesController < Api::RestController

  def rest_call
    resource_id=params[:resource]
    if resource_id
      @resource=Project.by_key(resource_id)
      if @resource.nil?
        rest_status_ko('Resource not found', 404)
        return
      end
    end
    access_denied unless has_role?(:codeviewer, @resource)

    source = @resource.last_snapshot.source
    if !source
      rest_status_ko('Resource has no sources', 404)
    else
      #optimization
      #source.snapshot.project=@resource
      rest_render({:source => source, :params => params})
    end
  end

  def rest_to_json(objects)
    source = objects[:source]
    params = objects[:params]
    JSON([source.to_hash_json(params)])
  end

  def rest_to_xml(objects)
    source = objects[:source]
    params = objects[:params]
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct!
    source.to_xml(xml, params)
  end

  def rest_to_text(objects)
    source = objects[:source]
    params = objects[:params]
    source.to_txt(params)
  end

end