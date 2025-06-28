import React, { useState, useEffect } from 'react';
import { Heart, Search, ChefHat, Users, Plus } from 'lucide-react';
import { Recipe } from '../../types';
import { getRecipes, searchRecipesByProducts, addToFavorites, removeFromFavorites, createRecipe } from '../../services/api';
import RecipeModal from './RecipeModal';
import AddRecipeModal from './AddRecipeModal';
import { getFavoriteRecipes } from '../../services/api';

const RecipesSection: React.FC = () => {
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedRecipe, setSelectedRecipe] = useState<Recipe | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [favorites, setFavorites] = useState<Set<number>>(new Set());
  const [searchLoading, setSearchLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadRecipes();
    loadFavorites();
  }, []);

  const loadRecipes = async () => {
    try {
      setError(null);
      const data = await getRecipes();
      console.log('Loaded recipes:', data);
      
      // Проверяем, что data это массив
      if (Array.isArray(data)) {
        setRecipes(data);
      } else {
        console.error('Recipes data is not an array:', data);
        setRecipes([]);
        setError('Неверный формат данных рецептов');
      }
    } catch (error) {
      console.error('Ошибка загрузки рецептов:', error);
      setError('Ошибка загрузки рецептов: ' + (error instanceof Error ? error.message : 'Неизвестная ошибка'));
      setRecipes([]);
    } finally {
      setLoading(false);
    }
  };
  
const loadFavorites = async () => {
  try {
    const data: Recipe[] = await getFavoriteRecipes();
    const favoriteIds = new Set(data.map(recipe => recipe.id));
    setFavorites(favoriteIds);
  } catch (error) {
    console.error('Ошибка загрузки избранного:', error);
  }
};

  const handleSearchByProducts = async () => {
    setSearchLoading(true);
    setError(null);
    
    try {
      console.log('Searching recipes by products...');
      const data = await searchRecipesByProducts();
      console.log('Search results:', data);
      
      // Проверяем, что data это массив
      if (Array.isArray(data)) {
        setRecipes(data);
        if (data.length === 0) {
          setError('По вашим продуктам рецепты не найдены');
        }
      } else {
        console.error('Search results data is not an array:', data);
        setRecipes([]);
        setError('Неверный формат данных поиска');
      }
    } catch (error) {
      console.error('Ошибка поиска рецептов:', error);
      setError('Ошибка поиска рецептов: ' + (error instanceof Error ? error.message : 'Неизвестная ошибка'));
      setRecipes([]);
    } finally {
      setSearchLoading(false);
    }
  };

  const handleCreateRecipe = async (recipeData: {
    title: string;
    description?: string;
    category?: string;
    ingredients: Array<{
      productName: string;
      quantity: number;
      unit?: string;
    }>;
  }) => {
    try {
      const newRecipe = await createRecipe(recipeData);
      setRecipes([newRecipe, ...recipes]);
      setShowAddModal(false);
    } catch (error) {
      console.error('Ошибка создания рецепта:', error);
      throw error;
    }
  };

  const handleToggleFavorite = async (recipeId: number) => {
    try {
      if (favorites.has(recipeId)) {
        await removeFromFavorites(recipeId);
        setFavorites(prev => {
          const newSet = new Set(prev);
          newSet.delete(recipeId);
          return newSet;
        });
      } else {
        await addToFavorites(recipeId);
        setFavorites(prev => new Set(prev).add(recipeId));
      }
    } catch (error) {
      console.error('Ошибка обновления избранного:', error);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-500"></div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="flex justify-between items-center mb-8">
        <h2 className="text-3xl font-bold text-gray-900">Рецепты</h2>
        <div className="flex space-x-3">
          <button
            onClick={() => setShowAddModal(true)}
            className="bg-gradient-to-r from-green-500 to-blue-500 text-white px-6 py-3 rounded-lg font-medium hover:from-green-600 hover:to-blue-600 transition-all duration-200 flex items-center space-x-2 shadow-md"
          >
            <Plus className="w-5 h-5" />
            <span>Добавить рецепт</span>
          </button>
          <button
            onClick={handleSearchByProducts}
            disabled={searchLoading}
            className="bg-gradient-to-r from-blue-500 to-purple-500 text-white px-6 py-3 rounded-lg font-medium hover:from-blue-600 hover:to-purple-600 transition-all duration-200 flex items-center space-x-2 shadow-md disabled:opacity-50"
          >
            <Search className="w-5 h-5" />
            <span>{searchLoading ? 'Поиск...' : 'Найти рецепты по продуктам'}</span>
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <p className="text-red-600">{error}</p>
          <button
            onClick={() => {
              setError(null);
              loadRecipes();
            }}
            className="mt-2 text-red-600 hover:text-red-700 underline text-sm"
          >
            Попробовать снова
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {recipes.map((recipe) => (
          <RecipeCard
            key={recipe.id}
            recipe={recipe}
            isFavorite={favorites.has(recipe.id)}
            onToggleFavorite={() => handleToggleFavorite(recipe.id)}
            onView={() => setSelectedRecipe(recipe)}
          />
        ))}
      </div>

      {recipes.length === 0 && !error && (
        <div className="text-center py-12">
          <div className="w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <ChefHat className="w-12 h-12 text-gray-400" />
          </div>
          <h3 className="text-xl font-semibold text-gray-600 mb-2">Рецепты не найдены</h3>
          <p className="text-gray-500 mb-4">Попробуйте поискать рецепты по продуктам из холодильника или добавьте новый рецепт</p>
          <button
            onClick={() => setShowAddModal(true)}
            className="bg-gradient-to-r from-green-500 to-blue-500 text-white px-6 py-3 rounded-lg font-medium hover:from-green-600 hover:to-blue-600 transition-all duration-200 flex items-center space-x-2 mx-auto"
          >
            <Plus className="w-5 h-5" />
            <span>Добавить первый рецепт</span>
          </button>
        </div>
      )}

      {selectedRecipe && (
        <RecipeModal
          recipe={selectedRecipe}
          onClose={() => setSelectedRecipe(null)}
          isFavorite={favorites.has(selectedRecipe.id)}
          onToggleFavorite={() => handleToggleFavorite(selectedRecipe.id)}
        />
      )}

      {showAddModal && (
        <AddRecipeModal
          onClose={() => setShowAddModal(false)}
          onSave={handleCreateRecipe}
        />
      )}
    </div>
  );
};

interface RecipeCardProps {
  recipe: Recipe;
  isFavorite: boolean;
  onToggleFavorite: () => void;
  onView: () => void;
}

const RecipeCard: React.FC<RecipeCardProps> = ({
  recipe,
  isFavorite,
  onToggleFavorite,
  onView,
}) => {
  // Проверяем, что recipe и его свойства существуют
  if (!recipe || !recipe.ingredients) {
    return (
      <div className="bg-white rounded-xl shadow-lg overflow-hidden border border-gray-200 p-6">
        <p className="text-red-500">Ошибка отображения рецепта</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl shadow-lg overflow-hidden border border-gray-200 hover:shadow-xl transition-shadow duration-200">
      <div className="p-6">
        <div className="flex justify-between items-start mb-4">
          <h3 className="text-lg font-semibold text-gray-900 line-clamp-2">{recipe.title || 'Без названия'}</h3>
          <button
            onClick={onToggleFavorite}
            className={`p-2 rounded-full transition-colors ${
              isFavorite
                ? 'bg-red-50 text-red-500 hover:bg-red-100'
                : 'bg-gray-50 text-gray-400 hover:bg-gray-100 hover:text-red-500'
            }`}
          >
            <Heart className={`w-5 h-5 ${isFavorite ? 'fill-current' : ''}`} />
          </button>
        </div>

        {recipe.description && (
          <p className="text-gray-600 text-sm mb-4 line-clamp-3">{recipe.description}</p>
        )}

        {recipe.category && (
          <div className="mb-4">
            <span className="inline-block bg-blue-100 text-blue-800 text-xs px-2 py-1 rounded-full">
              {recipe.category}
            </span>
          </div>
        )}

        <div className="mb-4">
          <p className="text-sm text-gray-500 mb-2">
            <Users className="w-4 h-4 inline mr-1" />
            Ингредиенты: {recipe.ingredients?.length || 0}
          </p>
          <div className="flex flex-wrap gap-1">
            {recipe.ingredients?.slice(0, 3).map((ingredient, index) => (
              <span
                key={index}
                className="inline-block bg-green-100 text-green-800 text-xs px-2 py-1 rounded"
              >
                {ingredient.productName || 'Неизвестный продукт'}
              </span>
            ))}
            {recipe.ingredients && recipe.ingredients.length > 3 && (
              <span className="inline-block bg-gray-100 text-gray-600 text-xs px-2 py-1 rounded">
                +{recipe.ingredients.length - 3} еще
              </span>
            )}
          </div>
        </div>

        <button
          onClick={onView}
          className="w-full bg-gradient-to-r from-green-500 to-blue-500 text-white py-2 px-4 rounded-lg font-medium hover:from-green-600 hover:to-blue-600 transition-all duration-200"
        >
          Подробнее
        </button>
      </div>
    </div>
  );
};

export default RecipesSection;