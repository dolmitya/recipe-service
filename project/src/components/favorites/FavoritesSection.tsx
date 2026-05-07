import React, { useEffect, useState } from 'react';
import { Heart } from 'lucide-react';
import { Recipe } from '../../types';
import { addCalendarEntry, getFavoriteRecipes, removeFromFavorites } from '../../services/api';
import RecipeModal from '../recipes/RecipeModal';

const formatNumber = (value?: number) =>
  new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 }).format(value ?? 0);

const MacroBadge: React.FC<{ label: string; value?: number; accentClass: string }> = ({
  label,
  value,
  accentClass,
}) => (
  <span className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ${accentClass}`}>
    {label}: {formatNumber(value)}
  </span>
);

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
      setFavorites(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Ошибка загрузки избранных рецептов:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveFromFavorites = async (recipeId: number) => {
    try {
      await removeFromFavorites(recipeId);
      setFavorites((current) => current.filter((recipe) => recipe.id !== recipeId));
      if (selectedRecipe && selectedRecipe.id === recipeId) {
        setSelectedRecipe(null);
      }
    } catch (error) {
      console.error('Ошибка удаления из избранного:', error);
    }
  };

  const handleAddRecipeToCalendar = async (recipeId: number, payload: { date: string; quantity: number }) => {
    await addCalendarEntry({
      date: payload.date,
      recipeId,
      quantity: payload.quantity,
    });
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-12 w-12 animate-spin rounded-full border-b-2 border-red-500"></div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl p-6">
      <div className="mb-8 flex items-center">
        <Heart className="mr-3 h-8 w-8 fill-current text-red-500" />
        <h2 className="text-3xl font-bold text-gray-900">Избранные рецепты</h2>
        <span className="ml-4 rounded-full bg-red-100 px-3 py-1 text-sm text-red-800">
          {favorites.length}
        </span>
      </div>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
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
        <div className="py-12 text-center">
          <div className="mx-auto mb-4 flex h-24 w-24 items-center justify-center rounded-full bg-red-50">
            <Heart className="h-12 w-12 text-red-300" />
          </div>
          <h3 className="mb-2 text-xl font-semibold text-gray-600">Нет избранных рецептов</h3>
          <p className="text-gray-500">Добавьте рецепты в избранное, чтобы они появились здесь.</p>
        </div>
      )}

      {selectedRecipe && (
        <RecipeModal
          recipe={selectedRecipe}
          onClose={() => setSelectedRecipe(null)}
          isFavorite={true}
          onToggleFavorite={() => handleRemoveFromFavorites(selectedRecipe.id)}
          onAddToCalendar={(payload) => handleAddRecipeToCalendar(selectedRecipe.id, payload)}
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
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-lg transition-shadow duration-200 hover:shadow-xl">
      <div className="p-6">
        <div className="mb-4 flex items-start justify-between gap-4">
          <h3 className="line-clamp-2 text-lg font-semibold text-gray-900">{recipe.title}</h3>
          <button
            onClick={onRemove}
            className="shrink-0 rounded-full bg-red-50 p-2 text-red-500 transition-colors hover:bg-red-100"
          >
            <Heart className="h-5 w-5 fill-current" />
          </button>
        </div>

        {recipe.description && (
          <p className="mb-4 line-clamp-3 text-sm text-gray-600">{recipe.description}</p>
        )}

        <div className="mb-4 flex flex-wrap gap-2">
          {recipe.category && (
            <span className="inline-block rounded-full bg-blue-100 px-2 py-1 text-xs text-blue-800">
              {recipe.category}
            </span>
          )}
          <span className="inline-flex items-center gap-1 rounded-full bg-orange-100 px-2 py-1 text-xs text-orange-700">
            {formatNumber(recipe.totalCalories)} ккал
          </span>
        </div>

        <div className="mb-4 flex flex-wrap gap-2">
          <MacroBadge label="Б" value={recipe.totalProteins} accentClass="bg-blue-50 text-blue-700" />
          <MacroBadge label="Ж" value={recipe.totalFats} accentClass="bg-amber-50 text-amber-700" />
          <MacroBadge label="У" value={recipe.totalCarbs} accentClass="bg-emerald-50 text-emerald-700" />
        </div>

        <div className="flex space-x-2">
          <button
            onClick={onView}
            className="flex-1 rounded-lg bg-gradient-to-r from-green-500 to-blue-500 px-4 py-2 font-medium text-white transition-all duration-200 hover:from-green-600 hover:to-blue-600"
          >
            Подробнее
          </button>
          <button
            onClick={onRemove}
            className="rounded-lg bg-red-500 px-4 py-2 text-white transition-colors hover:bg-red-600"
          >
            Убрать
          </button>
        </div>
      </div>
    </div>
  );
};

export default FavoritesSection;
