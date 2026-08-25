import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { Search, Bell, ChevronDown, LogOut, User as UserIcon } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const Navbar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const displayName = user?.name || user?.email?.split('@')[0] || 'Alex Developer';

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/projects`);
    }
  };

  return (
    <header className="cs-navbar">
      {/* Search Input Bar */}
      <form className="cs-navbar-search" onSubmit={handleSearchSubmit}>
        <Search className="cs-search-icon" />
        <input
          type="text"
          className="cs-search-input"
          placeholder="Search projects, repositories, files..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
        <div className="cs-search-shortcut">
          <span>Ctrl</span>
          <span>+</span>
          <span>/</span>
        </div>
      </form>

      {/* Right Controls & User Badge */}
      <div className="cs-navbar-right">
        {/* Notifications Icon */}
        <button className="cs-icon-btn" title="Notifications">
          <Bell className="cs-nav-utility-icon" />
          <span className="cs-notification-dot" />
        </button>

        {/* User Profile Dropdown */}
        <div className="cs-user-dropdown-wrapper">
          <div className="cs-user-profile" onClick={() => setShowProfileMenu(!showProfileMenu)}>
            <div className="cs-user-avatar">
              {user?.avatarUrl ? (
                <img src={user.avatarUrl} alt={displayName} />
              ) : (
                <span className="cs-avatar-initial">{displayName.charAt(0).toUpperCase()}</span>
              )}
            </div>
            <span className="cs-user-name">{displayName}</span>
            <ChevronDown className="cs-dropdown-icon" />
          </div>

          {showProfileMenu && (
            <div className="cs-profile-menu">
              <div className="cs-menu-header">
                <div className="cs-menu-name">{displayName}</div>
                <div className="cs-menu-email">{user?.email || 'alex.developer@codesense.ai'}</div>
              </div>
              <div className="cs-menu-divider" />
              <button className="cs-menu-item" onClick={() => navigate('/settings')}>
                <UserIcon style={{ width: '14px', height: '14px' }} />
                Account Settings
              </button>
              <button className="cs-menu-item text-danger" onClick={handleLogout}>
                <LogOut style={{ width: '14px', height: '14px' }} />
                Logout
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

export default Navbar;
