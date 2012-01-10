#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
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
class ReviewComment < ActiveRecord::Base
  belongs_to :user
  belongs_to :review
  validates_presence_of :user => "can't be empty"
  validate :comment_should_not_be_blank
  
  alias_attribute :text, :review_text

  def html_text
    Api::Utils.markdown_to_html(review_text)
  end

  def plain_text
    Api::Utils.convert_string_to_unix_newlines(review_text)
  end
  
  def excerpt
    text = plain_text.gsub("\n", " ")
    if text.size > 101
      text[0..100] + " ..."
    else
      text 
    end
  end

  private

  def comment_should_not_be_blank
    errors.add("Comment", " cannot be blank") if review_text.blank?
  end

end
