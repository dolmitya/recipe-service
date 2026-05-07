import React, { useEffect, useMemo, useState } from 'react';
import { CalendarPlus, ChefHat, ChevronLeft, ChevronRight, Heart, Plus, Search, Users } from 'lucide-react';
import { Recipe, RecipeSuggestion } from '../../types';
import {
  addCalendarEntry,
  addToFavorites,
  createRecipe,
  getFavoriteRecipes,
  getRecipeSuggestions,
  getRecipes,
  removeFromFavorites,
  searchRecipesByProducts,
} from '../../services/api';
import RecipeModal from './RecipeModal';
import AddRecipeModal from './AddRecipeModal';

const RECIPES_PER_PAGE = 6;

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

const PaginationControls: React.FC<{
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}> = ({ currentPage, totalPages, onPageChange }) => {
  if (totalPages <= 1) {
    return null;
  }

  return (
    <div className="mt-8 flex flex-wrap items-center justify-center gap-2">
      <button
        type="button"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 1}
        className="inline-flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
      >
        <ChevronLeft className="h-4 w-4" />
        Назад
      </button>

      {Array.from({ length: totalPages }, (_, index) => index + 1).map((page) => (
        <button
          key={page}
          type="button"
          onClick={() => onPageChange(page)}
          className={`h-10 min-w-10 rounded-lg px-3 text-sm font-medium transition-colors ${
            page === currentPage
              ? 'bg-green-500 text-white'
              : 'border border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
          }`}
        >
          {page}
        </button>
      ))}

      <button
        type="button"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage === totalPages}
        className="inline-flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
      >
        Вперёд
        <ChevronRight className="h-4 w-4" />
      </button>
    </div>
  );
};

const RecipesSection: React.FC = () => {
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedRecipe, setSelectedRecipe] = useState<Recipe | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [favorites, setFavorites] = useState<Set<number>>(new Set());
  const [searchLoading, setSearchLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [titleQuery, setTitleQuery] = useState('');
  const [suggestions, setSuggestions] = useState<RecipeSuggestion[]>([]);
  const [currentPage, setCurrentPage] = useState(1);

  const normalizedTitleQuery = useMemo(() => titleQuery.trim().toLowerCase(), [titleQuery]);

  useEffect(() => {
    loadRecipes();
    loadFavorites();
  }, []);

  useEffect(() => {
    if (normalizedTitleQuery.length === 0) {
      setSuggestions([]);
      return;
    }

    const timeoutId = window.setTimeout(async () => {
      try {
        const data = await getRecipeSuggestions(normalizedTitleQuery);
        setSuggestions(Array.isArray(data) ? data : []);
      } catch (loadError) {
        console.error('Ошибка загрузки подсказок рецептов:', loadError);
        setSuggestions([]);
      }
    }, 250);

    return () => window.clearTimeout(timeoutId);
  }, [normalizedTitleQuery]);

  const loadRecipes = async (query?: string) => {
    try {
      setError(null);
      const data = await getRecipes(undefined, query);
      const normalizedRecipes = Array.isArray(data) ? data : [];
      setRecipes(normalizedRecipes);
      setCurrentPage(1);
      return normalizedRecipes;
    } catch (loadError) {
      console.error('Ошибка загрузки рецептов:', loadError);
      setError(loadError instanceof Error ? loadError.message : 'Ошибка загрузки рецептов.');
      setRecipes([]);
      setCurrentPage(1);
      return [];
    } finally {
      setLoading(false);
    }
  };

  const loadFavorites = async () => {
    try {
      const data: Recipe[] = await getFavoriteRecipes();
      setFavorites(new Set(data.map((recipe) => recipe.id)));
    } catch (loadError) {
      console.error('Ошибка загрузки избранного:', loadError);
    }
  };

  const handleTitleSearch = async () => {
    setSuggestions([]);
    setSearchLoading(true);
    try {
      const result = await loadRecipes(titleQuery.trim() || undefined);
      if (titleQuery.trim() && result.length === 0) {
        setError('По названию ничего не найдено.');
      }
    } finally {
      setSearchLoading(false);
    }
  };

  const handleSelectSuggestion = async (suggestion: RecipeSuggestion) => {
    setTitleQuery(suggestion.title);
    setSuggestions([]);
    setSearchLoading(true);

    try {
      const result = await loadRecipes(suggestion.title);
      if (result.length === 0) {
        setError('По названию ничего не найдено.');
      }
    } finally {
      setSearchLoading(false);
    }
  };

  const handleSearchByProducts = async () => {
    setSearchLoading(true);
    setError(null);

    try {
      const data = await searchRecipesByProducts();
      if (Array.isArray(data)) {
        setRecipes(data);
        setCurrentPage(1);
        if (data.length === 0) {
          setError('По вашим продуктам рецепты не найдены.');
        }
      } else {
        setRecipes([]);
        setCurrentPage(1);
        setError('Неверный формат данных поиска.');
      }
    } catch (searchError) {
      console.error('Ошибка поиска рецептов:', searchError);
      setError(searchError instanceof Error ? searchError.message : 'Ошибка поиска рецептов.');
      setRecipes([]);
      setCurrentPage(1);
    } finally {
      setSearchLoading(false);
    }
  };

  const handleCreateRecipe = async (recipeData: {
    title: string;
    description?: string;
    category?: string;
    servings?: number;
    ingredients: Array<{
      productName: string;
      quantity: number;
      unit?: string;
    }>;
  }) => {
    const newRecipe = await createRecipe(recipeData);
    setRecipes((current) => [newRecipe, ...current]);
    setShowAddModal(false);
    setCurrentPage(1);
  };

  const handleToggleFavorite = async (recipeId: number) => {
    try {
      if (favorites.has(recipeId)) {
        await removeFromFavorites(recipeId);
        setFavorites((previous) => {
          const next = new Set(previous);
          next.delete(recipeId);
          return next;
        });
      } else {
        await addToFavorites(recipeId);
        setFavorites((previous) => new Set(previous).add(recipeId));
      }
    } catch (favoriteError) {
      console.error('Ошибка обновления избранного:', favoriteError);
    }
  };

  const handleAddRecipeToCalendar = async (
    recipeId: number,
    payload: { date: string; quantity: number; consumeFromFridge?: boolean },
  ) => {
    await addCalendarEntry({
      date: payload.date,
      recipeId,
      quantity: payload.quantity,
      consumeFromFridge: payload.consumeFromFridge,
    });
  };

  const totalPages = Math.max(1, Math.ceil(recipes.length / RECIPES_PER_PAGE));
  const paginatedRecipes = useMemo(() => {
    const startIndex = (currentPage - 1) * RECIPES_PER_PAGE;
    return recipes.slice(startIndex, startIndex + RECIPES_PER_PAGE);
  }, [currentPage, recipes]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-12 w-12 animate-spin rounded-full border-b-2 border-green-500"></div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl p-6">
      <div className="mb-8 flex flex-col gap-6">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h2 className="text-3xl font-bold text-gray-900">Рецепты</h2>
            <p className="mt-2 text-gray-500">
              Поиск по названию работает отдельно от подбора по продуктам в холодильнике.
            </p>
            {recipes.length > 0 && (
              <p className="mt-2 text-sm text-gray-500">
                Показано {paginatedRecipes.length} из {recipes.length} рецептов.
              </p>
            )}
          </div>
          <button
            onClick={() => setShowAddModal(true)}
            className="shrink-0 rounded-lg bg-gradient-to-r from-green-500 to-blue-500 px-6 py-3 font-medium text-white shadow-md transition-all duration-200 hover:from-green-600 hover:to-blue-600"
          >
            <span className="flex items-center gap-2">
              <Plus className="h-5 w-5" />
              <span>Добавить рецепт</span>
            </span>
          </button>
        </div>

        <div className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
          <div className="flex flex-col gap-3 lg:flex-row">
            <div className="flex flex-1 gap-3">
              <div className="relative flex-1">
                <input
                  type="text"
                  value={titleQuery}
                  onChange={(event) => setTitleQuery(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      handleTitleSearch();
                    }
                  }}
                  placeholder="Поиск рецепта по названию"
                  autoComplete="off"
                  className="w-full rounded-lg border border-gray-300 px-4 py-3 focus:border-transparent focus:ring-2 focus:ring-green-500"
                />
                {suggestions.length > 0 && (
                  <div className="absolute z-10 mt-2 w-full overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg">
                    {suggestions.map((suggestion) => (
                      <button
                        key={suggestion.id}
                        type="button"
                        onClick={() => handleSelectSuggestion(suggestion)}
                        className="flex w-full items-center justify-between gap-3 px-4 py-3 text-left hover:bg-gray-50"
                      >
                        <span className="font-medium text-gray-900">{suggestion.title}</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
              <button
                onClick={handleTitleSearch}
                disabled={searchLoading}
                className="flex items-center gap-2 rounded-lg border border-gray-300 px-5 py-3 text-gray-700 transition-colors hover:bg-gray-50 disabled:opacity-50"
              >
                <Search className="h-4 w-4" />
                <span>По названию</span>
              </button>
            </div>

            <div className="flex gap-3">
              <button
                onClick={handleSearchByProducts}
                disabled={searchLoading}
                className="flex items-center gap-2 rounded-lg bg-gradient-to-r from-blue-500 to-purple-500 px-5 py-3 font-medium text-white shadow-md transition-all duration-200 hover:from-blue-600 hover:to-purple-600 disabled:opacity-50"
              >
                <Search className="h-4 w-4" />
                <span>{searchLoading ? 'Поиск...' : 'По продуктам'}</span>
              </button>
              <button
                onClick={() => {
                  setTitleQuery('');
                  setSuggestions([]);
                  setError(null);
                  setLoading(true);
                  loadRecipes();
                }}
                className="rounded-lg border border-gray-300 px-5 py-3 text-gray-700 transition-colors hover:bg-gray-50"
              >
                Сбросить
              </button>
            </div>
          </div>
        </div>
      </div>

      {error && (
        <div className="mb-6 rounded-lg border border-red-200 bg-red-50 p-4">
          <p className="text-red-600">{error}</p>
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
        {paginatedRecipes.map((recipe) => (
          <RecipeCard
            key={recipe.id}
            recipe={recipe}
            isFavorite={favorites.has(recipe.id)}
            onToggleFavorite={() => handleToggleFavorite(recipe.id)}
            onView={() => setSelectedRecipe(recipe)}
          />
        ))}
      </div>

      <PaginationControls currentPage={currentPage} totalPages={totalPages} onPageChange={setCurrentPage} />

      {recipes.length === 0 && !error && (
        <div className="py-12 text-center">
          <div className="mx-auto mb-4 flex h-24 w-24 items-center justify-center rounded-full bg-gray-100">
            <ChefHat className="h-12 w-12 text-gray-400" />
          </div>
          <h3 className="mb-2 text-xl font-semibold text-gray-600">Рецепты не найдены</h3>
          <p className="mb-4 text-gray-500">
            Попробуйте поиск по названию, по продуктам или добавьте свой рецепт.
          </p>
          <button
            onClick={() => setShowAddModal(true)}
            className="mx-auto rounded-lg bg-gradient-to-r from-green-500 to-blue-500 px-6 py-3 font-medium text-white transition-all duration-200 hover:from-green-600 hover:to-blue-600"
          >
            <span className="flex items-center gap-2">
              <Plus className="h-5 w-5" />
              <span>Добавить первый рецепт</span>
            </span>
          </button>
        </div>
      )}

      {selectedRecipe && (
        <RecipeModal
          recipe={selectedRecipe}
          onClose={() => setSelectedRecipe(null)}
          isFavorite={favorites.has(selectedRecipe.id)}
          onToggleFavorite={() => handleToggleFavorite(selectedRecipe.id)}
          onAddToCalendar={(payload) => handleAddRecipeToCalendar(selectedRecipe.id, payload)}
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
  return (
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-lg transition-shadow duration-200 hover:shadow-xl">
      <div className="p-6">
        <div className="mb-4 flex items-start justify-between gap-4">
          <h3 className="line-clamp-2 text-lg font-semibold text-gray-900">{recipe.title || 'Без названия'}</h3>
          <button
            onClick={onToggleFavorite}
            className={`shrink-0 rounded-full p-2 transition-colors ${
              isFavorite
                ? 'bg-red-50 text-red-500 hover:bg-red-100'
                : 'bg-gray-50 text-gray-400 hover:bg-gray-100 hover:text-red-500'
            }`}
          >
            <Heart className={`h-5 w-5 ${isFavorite ? 'fill-current' : ''}`} />
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
          <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2 py-1 text-xs text-emerald-700">
            {formatNumber(recipe.caloriesPerServing)} ккал / порц.
          </span>
        </div>

        <div className="mb-4 space-y-3">
          <div className="flex flex-wrap gap-2">
            <MacroBadge label="Б" value={recipe.totalProteins} accentClass="bg-blue-50 text-blue-700" />
            <MacroBadge label="Ж" value={recipe.totalFats} accentClass="bg-amber-50 text-amber-700" />
            <MacroBadge label="У" value={recipe.totalCarbs} accentClass="bg-emerald-50 text-emerald-700" />
          </div>
          <div className="text-sm text-gray-500">
            <Users className="mr-1 inline h-4 w-4" />
            Ингредиенты: {recipe.ingredients?.length || 0}
          </div>
          <div className="text-sm text-gray-500">
            <CalendarPlus className="mr-1 inline h-4 w-4" />
            Порций: {formatNumber(recipe.servings)}
          </div>
        </div>

        <button
          onClick={onView}
          className="w-full rounded-lg bg-gradient-to-r from-green-500 to-blue-500 px-4 py-2 font-medium text-white transition-all duration-200 hover:from-green-600 hover:to-blue-600"
        >
          Подробнее
        </button>
      </div>
    </div>
  );
};

export default RecipesSection;
