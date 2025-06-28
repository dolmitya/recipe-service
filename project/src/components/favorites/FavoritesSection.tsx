import React, { useState, useEffect } from 'react';
import { Heart, ChefHat } from 'lucide-react';
import { Recipe } from '../../types';
import { getFavoriteRecipes, removeFromFavorites } from '../../services/api';
import RecipeModal from '../recipes/RecipeModal';

const FavoritesSection: React.FC = () => {
  const [favorites, setFavorites] = useState<Recipe[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedRecipe, setSelectedRecipe] = useState<Recipe | null>(null);

  useEffect(() => {
    loadFavorites();
  }, []);

  const loadFavorites = async () => {
    try {
      const data = await getFavoriteRecipes();
      setFavorites(data);
    } catch (error) {
      console.error('Ошибка загрузки избранных рецептов:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveFromFavorites = async (recipeId: number) => {
    try {
      await removeFromFavorites(recipeId);
      setFavorites(favorites.filter(recipe => recipe.id !== recipeId));
      if (selectedRecipe && selectedRecipe.id === recipeId) {
        setSelectedRecipe(null);
      }
    } catch (error) {
      console.error('Ошибка удаления из избранного:', error);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-500"></div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="flex items-center mb-8">
        <Heart className="w-8 h-8 text-red-500 mr-3 fill-current" />
        <h2 className="text-3xl font-bold text-gray-900">Любимые рецепты</h2>
        <span className="ml-4 bg-red-100 text-red-800 text-sm px-3 py-1 rounded-full">
          {favorites.length}
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {favorites.map((recipe) => (
          <FavoriteRecipeCard
            key={recipe.id}
            recipe={recipe}
            onRemove={() => handleRemoveFromFavorites(recipe.id)}
            onView={() => setSelectedRecipe(recipe)}
          />
        ))}
      </div>

      {favorites.length === 0 && (
        <div className="text-center py-12">
          <div className="w-24 h-24 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-4">
            <Heart className="w-12 h-12 text-red-300" />
          </div>
          <h3 className="text-xl font-semibold text-gray-600 mb-2">Нет избранных рецептов</h3>
          <p className="text-gray-500">Добавьте рецепты в избранное, чтобы они появились здесь</p>
        </div>
      )}

      {selectedRecipe && (
        <RecipeModal
          recipe={selectedRecipe}
          onClose={() => setSelectedRecipe(null)}
          isFavorite={true}
          onToggleFavorite={() => handleRemoveFromFavorites(selectedRecipe.id)}
        />
      )}
    </div>
  );
};

interface FavoriteRecipeCardProps {
  recipe: Recipe;
  onRemove: () => void;
  onView: () => void;
}

const FavoriteRecipeCard: React.FC<FavoriteRecipeCardProps> = ({
  recipe,
  onRemove,
  onView,
}) => {
  return (
    <div className="bg-white rounded-xl shadow-lg overflow-hidden border border-gray-200 hover:shadow-xl transition-shadow duration-200">
      <div className="p-6">
        <div className="flex justify-between items-start mb-4">
          <h3 className="text-lg font-semibold text-gray-900 line-clamp-2">{recipe.title}</h3>
          <button
            onClick={onRemove}
            className="p-2 bg-red-50 text-red-500 hover:bg-red-100 rounded-full transition-colors"
          >
            <Heart className="w-5 h-5 fill-current" />
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
            <ChefHat className="w-4 h-4 inline mr-1" />
            Ингредиенты: {recipe.ingredients.length}
          </p>
          <div className="flex flex-wrap gap-1">
            {recipe.ingredients.slice(0, 3).map((ingredient, index) => (
              <span
                key={index}
                className="inline-block bg-green-100 text-green-800 text-xs px-2 py-1 rounded"
              >
                {ingredient.productName}
              </span>
            ))}
            {recipe.ingredients.length > 3 && (
              <span className="inline-block bg-gray-100 text-gray-600 text-xs px-2 py-1 rounded">
                +{recipe.ingredients.length - 3} еще
              </span>
            )}
          </div>
        </div>

        <div className="flex space-x-2">
          <button
            onClick={onView}
            className="flex-1 bg-gradient-to-r from-green-500 to-blue-500 text-white py-2 px-4 rounded-lg font-medium hover:from-green-600 hover:to-blue-600 transition-all duration-200"
          >
            Подробнее
          </button>
          <button
            onClick={onRemove}
            className="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors"
          >
            Убрать
          </button>
        </div>
      </div>
    </div>
  );
};

export default FavoritesSection;