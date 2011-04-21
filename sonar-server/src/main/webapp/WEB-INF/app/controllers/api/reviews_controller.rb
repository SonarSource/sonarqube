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

class Api::ReviewsController < Api::ApiController

  def index
    @reviews = []
    @reviews << Review.find ( params[:id] )
    
    respond_to do |format|
      format.json { render :json => jsonp(to_json) }
      format.xml {render :xml => to_xml}
      format.text { render :text => text_not_supported }
    end
  end
  
  def to_xml
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct!
    
    xml.reviews do
      @reviews.each do |review|
        xml.review do
          xml.id(review.id)
          xml.updatedAt(review.updated_at)
          xml.user(review.user.login)
          xml.assignee(review.assignee.login)
          xml.title(review.title)
          xml.type(review.review_type)
          xml.status(review.status)
          xml.severity(review.severity)
          xml.resourceLine(review.resource_line)
          
          # Continue here with resource + violation + comments
        end
      end
    end
  end
  
  def to_json
    JSON(@reviews.collect{|review| review.to_hash_json(true)})
  end
    
end