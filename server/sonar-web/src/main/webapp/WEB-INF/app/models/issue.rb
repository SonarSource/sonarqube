#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2016 SonarSource
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

class Issue

  def self.comment_to_hash(comment)
    {
        :key => comment.key(),
        :login => comment.userLogin(),
        :htmlText => Internal.text.markdownToHtml(comment.markdownText()),
        :createdAt => Api::Utils.format_datetime(comment.createdAt())
    }
  end

  def self.changelog_to_hash(changelog)
    hash = []
    changelog.changes.each do |change|
      user = changelog.user(change)

      hash_change = {}
      if user
        hash_change[:user] = user.login()
        hash_change[:userName] = user.name()
        hash_change[:email] = user.email()
      end
      hash_change[:creationDate] = Api::Utils.format_datetime(change.creationDate()) if change.creationDate()
      hash_change[:diffs] = []

      change.diffs.entrySet().each do |entry|
        key = entry.getKey()
        diff = entry.getValue()
        hash_diff = {}
        hash_diff[:key] = key
        if key == 'effort'
          # effort is store as a number of minutes in the changelog, so we first create a Duration from the value then we decode it
          hash_diff[:newValue] = Internal.durations.encode(Internal.durations.create(diff.newValue().to_i)) if diff.newValue.present?
          hash_diff[:oldValue] = Internal.durations.encode(Internal.durations.create(diff.oldValue().to_i)) if diff.oldValue.present?
        else
          hash_diff[:newValue] = diff.newValue() if diff.newValue.present?
          hash_diff[:oldValue] = diff.oldValue() if diff.oldValue.present?
        end
        hash_change[:diffs] << hash_diff
      end

      hash << hash_change
    end
    hash
  end

end
