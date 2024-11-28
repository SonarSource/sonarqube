import React, { useEffect, useState } from 'react';
import { searchMembers } from '../../api/organizations';
import './SearchUser.css';

interface CustomSearchInputProps {
  excludedUsers: string[];
  handleValueChange: (selectedUser: any | null) => void;
  selectedUser: any | null;
  organization: { kee: string };
}

const CustomSearchInput: React.FC<CustomSearchInputProps> = ({
  excludedUsers,
  handleValueChange,
  selectedUser,
  organization,
}) => {
  const [inputValue, setInputValue] = useState<string>('');
  const [options, setOptions] = useState<any[]>([]);
  const [internalSelectedUser, setInternalSelectedUser] = useState<any | null>(selectedUser);

  useEffect(() => {
    setInternalSelectedUser(selectedUser); // Sync selectedUser prop with internal state
    if (selectedUser) {
      setInputValue(selectedUser.login); // Set input value to the selected user's login
    }
  }, [selectedUser]);

  const handleSearch = (query: string) => {
    if (query.length < 3) {
      setOptions([]);
      return;
    }

    const data = { organization: organization.kee, selected: 'deselected' };

    searchMembers({ ...data, q: query })
      .then(({ users }) => {
        const filteredUsers = users
          .filter((user) => !excludedUsers.includes(user.login))
          .map((user) => ({
            label: user.login, // Use only the login as the label
            value: user.login, // Ensure that the value is the login
            login: user.login,
            name: user.name,
          }));
        setOptions(filteredUsers);
      })
      .catch(() => setOptions([]));
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const query = e.target.value;
    setInputValue(query);

    // Clear the selected user if the input changes
    if (internalSelectedUser) {
      setInternalSelectedUser(null);
      handleValueChange(null);
    }

    handleSearch(query); // Trigger the search function on input change
  };

  const handleSelect = (user: any) => {
    setInputValue(user.login); // Set the input to the selected user's login
    setInternalSelectedUser(user); // Update the internal selected user
    handleValueChange(user); // Pass the selected user to the parent
    setOptions([]); // Clear the options after selection
  };

  const clearSelection = () => {
    setInputValue(''); // Clear the input value
    setInternalSelectedUser(null); // Clear the selected user
    handleValueChange(null); // Notify the parent
    setOptions([]); // Clear the dropdown options
  };

  return (
    <div className="custom-search-input">
      <div className="input-wrapper">
        <input
          type="text"
          value={inputValue}
          onChange={handleInputChange}
          placeholder="Search for a user"
          autoFocus
        />
        {inputValue && (
          <button className="clear-button" onClick={clearSelection}>
            ✖
          </button>
        )}
      </div>
      <div className={`dropdown-panel ${options.length > 0 ? 'open' : ''}`}>
        {options.map((user) => (
          <div key={user.value} className="dropdown-option" onClick={() => handleSelect(user)}>
            {user.login}
          </div>
        ))}
      </div>
    </div>
  );
};

export default CustomSearchInput;
