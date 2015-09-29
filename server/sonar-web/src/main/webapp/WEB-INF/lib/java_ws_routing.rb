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

module ActionController
  module Routing
    class RouteSet
      def add_java_ws_routes
        # Deprecated Ruby on Rails web services
        deprecated_web_services = Java::OrgSonarServerUi::JRubyFacade.new.getRubyRailsWebservices()
        deprecated_web_services.each do |ws|
          eval(ws.getTemplate())
          prepend_route("api/plugins/#{ws.getId()}/:action/:id", {:controller => "api/#{ws.getId()}", :requirements => {:id => /.*/}})
        end

        # Full Java web services
        ws_engine = Java::OrgSonarServerPlatform::Platform.component(Java::OrgSonarServerWs::WebServiceEngine.java_class)
        ws_engine.controllers().each do |controller|
          controller.actions.each do |action|
            if (!action.handler().java_kind_of?(Java::OrgSonarApiServerWs::RailsHandler))
              prepend_route("#{controller.path()}/#{action.key()}.:responseFormat/:id", {:controller => 'api/java_ws', :action => 'index', :wsaction => action.key(), :wspath => controller.path()})
              prepend_route("#{controller.path()}/#{action.key()}/:id", {:controller => 'api/java_ws', :action => 'index', :wsaction => action.key(), :wspath => controller.path()})
              if action.key()=='index'
                prepend_route("#{controller.path()}", {:controller => 'api/java_ws', :action => 'index', :wsaction => action.key(), :wspath => controller.path()})
              end
            end
          end
        end
      end
    end
  end
end

# support for routes reloading in dev mode
if ENV['RAILS_ENV'] == 'development'
  module ActionController
    module Routing
      class RouteSet
        alias rails_reload reload

        def reload
          rails_reload
          add_java_ws_routes
        end
      end
    end
  end
end
