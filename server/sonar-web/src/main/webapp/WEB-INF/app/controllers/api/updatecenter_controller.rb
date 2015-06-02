#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
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

require 'json'

class Api::UpdatecenterController < Api::ApiController

  #
  # GET /api/updatecenter/installed_plugins
  # curl http://localhost:9000/api/updatecenter/installed_plugins -v
  #
  def installed_plugins
    respond_to do |format|
      format.json { render :json => jsonp(plugins_to_json(user_plugins())) }
      format.xml  { render :xml => plugins_to_xml(user_plugins()) }
      format.text { render :text => text_not_supported }
    end
  end

  private

  def plugins_to_json(plugins=[])
    json=[]
    plugins.each do |p|
      json<<plugin_to_json(p)
    end
    json
  end

  def plugin_to_json(plugin)
    hash={}
    hash['key']=plugin.getKey()
    hash['name']=plugin.getName()
    hash['version']=plugin.getVersion().getName()
    hash
  end

  def plugins_to_xml(plugins, xml=Builder::XmlMarkup.new(:indent => 0))
    xml.plugins do
      plugins.each do |plugin|
        xml.plugin do
          xml.key(plugin.getKey())
          xml.name(plugin.getName())
          xml.version(plugin.getVersion().getName())
        end
      end
    end
  end

  def user_plugins
    java_facade.getPluginInfos().to_a.sort
  end
end
