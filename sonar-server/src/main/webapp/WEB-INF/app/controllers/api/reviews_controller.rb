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

require 'json'

class Api::ReviewsController < Api::ApiController

  def index
    convert_markdown=(params[:output]=='HTML')
    reviews=select_authorized(:user, Review.search(params), :project)
    
    respond_to do |format|
      format.json { render :json => jsonp(Review.reviews_to_json(reviews, convert_markdown)) }
      format.xml {render :xml => Review.reviews_to_xml(reviews, convert_markdown)}
      format.text { render :text => text_not_supported }
    end
  end

end