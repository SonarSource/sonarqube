#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
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
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#

#
# Class used to more easily handle the context of a review when it needs to be passed to the Java side
#
# - to create a context: (example)
#
#      review_context = Api::ReviewContext.new(:review => myReview, :user => current_user, :params => {"comment.text" => comment_values[:text]})
#
# - when it needs to be passed to the Java side, it needs to be transformed into a 'string-only' hash: 
#
#      review_context.to_string_map
#

class Api::ReviewContext
  
  def initialize(options={})
    @review = options[:workflow]
    @project = options[:project]
    @user = options[:user]
    @params = options[:params]
  end

  def to_string_map
    result = {}
    result['review'] = create_string_map_from_attributes @review if @review
    result['project'] = create_string_map_from_attributes @project if @project
    result['user'] = create_string_map_from_attributes @user if @user
    result['params'] = ensure_key_and_values_as_strings @params if @params
    result
  end

  def create_string_map_from_attributes(object)
    ensure_key_and_values_as_strings(object.attributes)
  end

  def ensure_key_and_values_as_strings(params)
    map = {}
    params.each do |key, value|
      map[key.to_s] = value.to_s
    end
    map
  end

end
