import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import LoginForm from './components/auth/LoginForm';
import Navigation from './components/layout/Navigation';
import FridgeSection from './components/fridge/FridgeSection';
import RecipesSection from './components/recipes/RecipesSection';
import FavoritesSection from './components/favorites/FavoritesSection';

const MainApp: React.FC = () => {
  const { isAuthenticated } = useAuth();
  const [activeTab, setActiveTab] = useState('fridge');

  if (!isAuthenticated) {
    return <LoginForm />;
  }

  const renderContent = () => {
    switch (activeTab) {
      case 'fridge':
        return <FridgeSection />;
      case 'recipes':
        return <RecipesSection />;
      case 'favorites':
        return <FavoritesSection />;
      default:
        return <FridgeSection />;
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 to-blue-50">
      <Navigation activeTab={activeTab} onTabChange={setActiveTab} />
      <main className="py-8">
        {renderContent()}
      </main>
    </div>
  );
};

function App() {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
}

export default App;