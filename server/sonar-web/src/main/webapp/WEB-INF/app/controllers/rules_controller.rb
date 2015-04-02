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
require "erb"
include ERB::Util

class RulesController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION

  # GET /rules/show/<key>
  # This URL is used by the Eclipse plugin
  #
  # Example: GET /rules/show/squid:AvoidCycles
  #
  def show
    require_parameters :id

    # the redirect is needed for the backward compatibility with eclipse plugin
    url = url_for :controller => 'coding_rules', :action => 'index'
    url = url + '#rule_key=' + url_encode(params[:id])
    redirect_to url
  end
end
