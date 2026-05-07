import React, { useState } from 'react';
import { CalendarPlus, Heart, X } from 'lucide-react';
import { Recipe } from '../../types';
import ConsumptionModal from '../calendar/ConsumptionModal';

interface RecipeModalProps {
  recipe: Recipe;
  onClose: () => void;
  isFavorite: boolean;
  onToggleFavorite: () => void;
  onAddToCalendar?: (payload: { date: string; quantity: number; consumeFromFridge?: boolean }) => Promise<void> | void;
}

const formatNumber = (value?: number) =>
  new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 }).format(value ?? 0);

const MacroBadge: React.FC<{ label: string; value?: number; accentClass: string }> = ({
  label,
  value,
  accentClass,
}) => (
  <span className={`inline-flex items-center rounded-full px-3 py-1 text-sm font-medium ${accentClass}`}>
    {label}: {formatNumber(value)}
  </span>
);

const RecipeModal: React.FC<RecipeModalProps> = ({
  recipe,
  onClose,
  isFavorite,
  onToggleFavorite,
  onAddToCalendar,
}) => {
  const [showConsumptionModal, setShowConsumptionModal] = useState(false);

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 p-4">
        <div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-2xl bg-white">
          <div className="sticky top-0 flex items-center justify-between rounded-t-2xl border-b border-gray-200 bg-white p-6">
            <h2 className="text-2xl font-bold text-gray-900">{recipe.title}</h2>
            <div className="flex items-center space-x-2">
              <button
                onClick={onToggleFavorite}
                className={`rounded-full p-2 transition-colors ${
                  isFavorite
                    ? 'bg-red-50 text-red-500 hover:bg-red-100'
                    : 'bg-gray-50 text-gray-400 hover:bg-gray-100 hover:text-red-500'
                }`}
              >
                <Heart className={`h-6 w-6 ${isFavorite ? 'fill-current' : ''}`} />
              </button>
              <button
                onClick={onClose}
                className="rounded-full bg-gray-50 p-2 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
              >
                <X className="h-6 w-6" />
              </button>
            </div>
          </div>

          <div className="p-6">
            <div className="mb-6 flex flex-wrap gap-3">
              {recipe.category && (
                <span className="inline-block rounded-full bg-blue-100 px-3 py-1 text-sm font-medium text-blue-800">
                  {recipe.category}
                </span>
              )}
              <span className="inline-block rounded-full bg-orange-100 px-3 py-1 text-sm font-medium text-orange-700">
                {formatNumber(recipe.totalCalories)} ккал
              </span>
              <span className="inline-block rounded-full bg-emerald-100 px-3 py-1 text-sm font-medium text-emerald-700">
                {formatNumber(recipe.caloriesPerServing)} ккал / порция
              </span>
              <span className="inline-block rounded-full bg-gray-100 px-3 py-1 text-sm font-medium text-gray-700">
                {formatNumber(recipe.servings)} порц.
              </span>
            </div>

            <div className="mb-6 grid grid-cols-1 gap-4 md:grid-cols-2">
              <div className="rounded-xl border border-gray-200 p-4">
                <p className="mb-3 text-sm font-medium text-gray-700">Всего в рецепте</p>
                <div className="mb-3 flex flex-wrap gap-2">
                  <MacroBadge label="Б" value={recipe.totalProteins} accentClass="bg-blue-50 text-blue-700" />
                  <MacroBadge label="Ж" value={recipe.totalFats} accentClass="bg-amber-50 text-amber-700" />
                  <MacroBadge label="У" value={recipe.totalCarbs} accentClass="bg-emerald-50 text-emerald-700" />
                </div>
              </div>
              <div className="rounded-xl border border-gray-200 p-4">
                <p className="mb-3 text-sm font-medium text-gray-700">На одну порцию</p>
                <div className="mb-3 flex flex-wrap gap-2">
                  <MacroBadge label="Б" value={recipe.proteinsPerServing} accentClass="bg-blue-50 text-blue-700" />
                  <MacroBadge label="Ж" value={recipe.fatsPerServing} accentClass="bg-amber-50 text-amber-700" />
                  <MacroBadge label="У" value={recipe.carbsPerServing} accentClass="bg-emerald-50 text-emerald-700" />
                </div>
              </div>
            </div>

            {recipe.description && (
              <div className="mb-6">
                <p className="leading-relaxed text-gray-700">{recipe.description}</p>
              </div>
            )}

            <div className="mb-6">
              <h3 className="mb-4 text-lg font-semibold text-gray-900">
                Ингредиенты ({recipe.ingredients.length})
              </h3>
              <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                {recipe.ingredients.map((ingredient, index) => (
                  <div
                    key={index}
                    className="rounded-lg bg-gray-50 p-3"
                  >
                    <div className="mb-2 flex items-start justify-between gap-3">
                      <div>
                        <span className="font-medium text-gray-900">{ingredient.productName}</span>
                        <p className="mt-1 text-sm text-gray-500">
                          {formatNumber(ingredient.quantity)} {ingredient.unit || ''}
                        </p>
                      </div>
                      <span className="whitespace-nowrap text-sm text-orange-600">
                        {formatNumber(ingredient.calories)} ккал
                      </span>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <MacroBadge label="Б" value={ingredient.proteins} accentClass="bg-blue-50 text-blue-700" />
                      <MacroBadge label="Ж" value={ingredient.fats} accentClass="bg-amber-50 text-amber-700" />
                      <MacroBadge label="У" value={ingredient.carbs} accentClass="bg-emerald-50 text-emerald-700" />
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="flex flex-wrap justify-end gap-3">
              {onAddToCalendar && (
                <button
                  onClick={() => setShowConsumptionModal(true)}
                  className="flex items-center gap-2 rounded-lg border border-emerald-200 px-6 py-2 text-emerald-700 transition-colors hover:bg-emerald-50"
                >
                  <CalendarPlus className="h-4 w-4" />
                  <span>Приготовил и съел</span>
                </button>
              )}
              <button
                onClick={onClose}
                className="rounded-lg border border-gray-300 px-6 py-2 text-gray-700 transition-colors hover:bg-gray-50"
              >
                Закрыть
              </button>
              <button
                onClick={onToggleFavorite}
                className={`rounded-lg px-6 py-2 font-medium transition-colors ${
                  isFavorite
                    ? 'bg-red-500 text-white hover:bg-red-600'
                    : 'bg-gradient-to-r from-green-500 to-blue-500 text-white hover:from-green-600 hover:to-blue-600'
                }`}
              >
                {isFavorite ? 'Убрать из избранного' : 'Добавить в избранное'}
              </button>
            </div>
          </div>
        </div>
      </div>

      {showConsumptionModal && onAddToCalendar && (
        <ConsumptionModal
          title="Добавить рецепт в календарь"
          itemName={recipe.title}
          quantityLabel="Сколько порций съели"
          unitLabel="порц."
          consumeFromFridgeLabel="Списать ингредиенты из холодильника"
          defaultConsumeFromFridge={true}
          onClose={() => setShowConsumptionModal(false)}
          onSave={onAddToCalendar}
        />
      )}
    </>
  );
};

export default RecipeModal;
