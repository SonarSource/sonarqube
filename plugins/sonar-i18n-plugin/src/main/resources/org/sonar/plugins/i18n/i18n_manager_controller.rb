#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#

require "json"

class Api::I18nManagerController < Api::ApiController

  def index
      render :text => "Use one of the following:<br><ul>" + 
                      "<li>/api/plugins/i18n_manager/unknown_keys?format=text|json</li>" +
                      "</ul>"
  end
  
  # GET /api/plugins/i18n_manager/unknown_keys
  # Examples :
  #   curl -v http://localhost:9000/api/plugins/i18n_manager -u admin:admin
  #
  
  def unknown_keys
    begin
      output = ""
      properties = i18n_manager.unknown_keys
      
      properties.keys.sort.each {|key| output += "#{key}=#{properties[key]}\n" }
      
      output = "# No unknown keys" if output.empty?

      respond_to do |format|
        format.json { render :json => JSON(properties) }
        format.xml  { render :xml => xml_not_supported }
        format.text { render :text => output }
      end

    rescue ApiException => e
      render_error(e.msg, e.code)
      
    rescue Exception => e
      logger.error("Fails to execute #{request.url} : #{e.message}")
      render_error(e.message)
    end
  end
  
  private

  def i18n_manager
    java_facade.getComponentByClassname('i18n', 'org.sonar.plugins.i18n.I18nManager')
  end
  
end