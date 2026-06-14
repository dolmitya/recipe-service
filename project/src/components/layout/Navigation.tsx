import React from 'react';
import { BookOpen, CalendarDays, Heart, LogOut, Refrigerator, User } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

interface NavigationProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
}

const Navigation: React.FC<NavigationProps> = ({ activeTab, onTabChange }) => {
  const { logout, userLabel } = useAuth();

  const tabs = [
    { id: 'fridge', label: 'Холодильник', icon: Refrigerator },
    { id: 'recipes', label: 'Рецепты', icon: BookOpen },
    { id: 'favorites', label: 'Избранное', icon: Heart },
    { id: 'calendar', label: 'Календарь', icon: CalendarDays },
  ];

  return (
    <nav className="border-b border-gray-200 bg-white shadow-lg">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between gap-4">
          <div className="flex min-w-0 items-center">
            <div className="mr-4 flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-gradient-to-r from-green-500 to-blue-500">
              <Refrigerator className="h-6 w-6 text-white" />
            </div>
            <h1 className="truncate text-xl font-bold text-gray-900">Умный холодильник</h1>
          </div>

          <div className="flex items-center gap-1 overflow-x-auto">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => onTabChange(tab.id)}
                  className={`flex shrink-0 items-center space-x-2 rounded-lg px-4 py-2 font-medium transition-all duration-200 ${
                    activeTab === tab.id
                      ? 'bg-gradient-to-r from-green-500 to-blue-500 text-white shadow-md'
                      : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                  }`}
                >
                  <Icon className="h-4 w-4" />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </div>

          <div className="flex shrink-0 items-center gap-3">
            {userLabel && (
              <div className="flex max-w-56 items-center gap-2 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-700">
                <User className="h-4 w-4 shrink-0 text-gray-500" />
                <span className="truncate">{userLabel}</span>
              </div>
            )}

            <button
              onClick={logout}
              className="flex items-center space-x-2 rounded-lg px-4 py-2 text-red-600 transition-all duration-200 hover:bg-red-50 hover:text-red-700"
            >
              <LogOut className="h-4 w-4" />
              <span>Выйти</span>
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navigation;
