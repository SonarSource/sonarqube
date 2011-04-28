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
module ReviewsHelper
  
  def projects_for_select
    Project.find(:all, :select => 'id,name,long_name', :conditions => ['enabled=? AND scope=? AND qualifier IN (?)', true, 'PRJ', ['TRK', 'VW','SVW']], :order => 'name ASC')
  end
  
  def to_xml(reviews, convert_markdown)
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct!
    
    xml.reviews do
      reviews.each do |review|
        review_to_xml(xml, review, convert_markdown)
      end
    end
  end
  
  def review_to_xml(xml, review, html=false)
    xml.review do
      xml.id(review.id.to_i)
      xml.createdAt(format_datetime(review.created_at))
      xml.updatedAt(format_datetime(review.updated_at))
      xml.user(review.user.login)
      xml.assignee(review.assignee.login) if review.assignee
      xml.title(review.title)
      xml.type(review.review_type)
      xml.status(review.status)
      xml.severity(review.severity)
      xml.resource(review.resource.kee)  if review.resource
      xml.line(review.resource_line) if review.resource_line > 0
      xml.comments do
        review.review_comments.each do |comment|
          xml.comment do
            xml.author(comment.user.login)
            xml.updatedAt(format_datetime(comment.updated_at))
            xml.text(html ? markdown_to_html(comment.review_text): comment.review_text)
          end
        end
      end
    end
  end
  
  def to_json(reviews, convert_markdown=false)
    JSON(reviews.collect{|review| review_to_json(review, convert_markdown)})
  end

  def review_to_json(review, html=false)
    json = {}
    json['id'] = review.id.to_i
    json['createdAt'] = format_datetime(review.created_at)
    json['updatedAt'] = format_datetime(review.updated_at)
    json['author'] = review.user.login
    json['assignee'] = review.assignee.login if review.assignee
    json['title'] = review.title if review.title
    json['type'] = review.review_type
    json['status'] = review.status
    json['severity'] = review.severity
    json['resource'] = review.resource.kee if review.resource
    json['line'] = review.resource_line if review.resource_line > 0
    comments = []
    review.review_comments.each do |comment|
      comments << {
        'author' => comment.user.login,
        'updatedAt' => format_datetime(comment.updated_at),
        'text' => (html ? markdown_to_html(comment.review_text): comment.review_text)
      }
    end
    json['comments'] = comments
    json
  end
  
end
