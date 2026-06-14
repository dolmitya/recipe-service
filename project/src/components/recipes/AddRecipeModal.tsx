import React, { useEffect, useMemo, useState } from 'react';
import { Plus, Trash2, X } from 'lucide-react';
import { Ingredient, ProductSuggestion } from '../../types';
import { getProductSuggestions } from '../../services/api';

interface AddRecipeModalProps {
  onClose: () => void;
  onSave: (recipeData: {
    title: string;
    description?: string;
    category?: string;
    servings?: number;
    ingredients: Ingredient[];
  }) => void;
}

type IngredientSuggestionState = {
  suggestions: ProductSuggestion[];
  selectedSuggestion: ProductSuggestion | null;
};

const emptyIngredient = (): Ingredient => ({
  productName: '',
  quantity: 0,
  unit: '',
});

const emptySuggestionState = (): IngredientSuggestionState => ({
  suggestions: [],
  selectedSuggestion: null,
});

const AddRecipeModal: React.FC<AddRecipeModalProps> = ({ onClose, onSave }) => {
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    category: '',
    servings: '1',
  });
  const [ingredients, setIngredients] = useState<Ingredient[]>([emptyIngredient()]);
  const [ingredientStates, setIngredientStates] = useState<IngredientSuggestionState[]>([
    emptySuggestionState(),
  ]);
  const [loading, setLoading] = useState(false);

  const normalizedIngredientNames = useMemo(
    () => ingredients.map((ingredient) => ingredient.productName.trim().toLowerCase()),
    [ingredients],
  );
  const selectedIngredientNames = useMemo(
    () =>
      ingredientStates.map((state) => state.selectedSuggestion?.name.trim().toLowerCase() ?? ''),
    [ingredientStates],
  );

  useEffect(() => {
    const timeouts: number[] = [];

    ingredients.forEach((ingredient, index) => {
      const query = ingredient.productName.trim().toLowerCase();
      const selectedSuggestionName = selectedIngredientNames[index];

      if (!query) {
        setIngredientStates((current) => {
          if ((current[index]?.suggestions.length ?? 0) === 0) {
            return current;
          }

          return current.map((state, currentIndex) =>
            currentIndex === index ? { ...state, suggestions: [] } : state,
          );
        });
        return;
      }

      if (selectedSuggestionName && query === selectedSuggestionName) {
        return;
      }

      const timeoutId = window.setTimeout(async () => {
        try {
          const data = await getProductSuggestions(query);
          setIngredientStates((current) =>
            current.map((state, currentIndex) =>
              currentIndex === index
                ? { ...state, suggestions: Array.isArray(data) ? data : [] }
                : state,
            ),
          );
        } catch (error) {
          console.error('Ошибка загрузки подсказок ингредиентов:', error);
          setIngredientStates((current) =>
            current.map((state, currentIndex) =>
              currentIndex === index ? { ...state, suggestions: [] } : state,
            ),
          );
        }
      }, 250);

      timeouts.push(timeoutId);
    });

    return () => {
      timeouts.forEach((timeoutId) => window.clearTimeout(timeoutId));
    };
  }, [ingredients, normalizedIngredientNames, selectedIngredientNames]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setLoading(true);

    try {
      const validIngredients = ingredients.filter(
        (ingredient) => ingredient.productName.trim() && ingredient.quantity > 0,
      );

      if (validIngredients.length === 0) {
        alert('Добавьте хотя бы один ингредиент');
        return;
      }

      await onSave({
        title: formData.title,
        description: formData.description || undefined,
        category: formData.category || undefined,
        servings: Number(formData.servings) || 1,
        ingredients: validIngredients,
      });

      onClose();
    } catch (error) {
      console.error('Ошибка создания рецепта:', error);
    } finally {
      setLoading(false);
    }
  };

  const addIngredient = () => {
    setIngredients((current) => [...current, emptyIngredient()]);
    setIngredientStates((current) => [...current, emptySuggestionState()]);
  };

  const removeIngredient = (index: number) => {
    if (ingredients.length === 1) {
      return;
    }

    setIngredients((current) => current.filter((_, currentIndex) => currentIndex !== index));
    setIngredientStates((current) => current.filter((_, currentIndex) => currentIndex !== index));
  };

  const updateIngredient = (index: number, field: keyof Ingredient, value: string | number) => {
    setIngredients((current) =>
      current.map((ingredient, currentIndex) => {
        if (currentIndex !== index) {
          return ingredient;
        }

        if (field === 'productName') {
          const nextName = String(value);
          const selectedSuggestion = ingredientStates[index]?.selectedSuggestion;
          const isSameSuggestion =
            selectedSuggestion != null &&
            nextName.trim().toLowerCase() === selectedSuggestion.name.toLowerCase();

          if (!isSameSuggestion) {
            setIngredientStates((states) =>
              states.map((state, stateIndex) =>
                stateIndex === index
                  ? {
                      ...state,
                      selectedSuggestion: null,
                    }
                  : state,
              ),
            );

            return {
              ...ingredient,
              productName: nextName,
              unit: '',
            };
          }
        }

        return { ...ingredient, [field]: value };
      }),
    );
  };

  const selectSuggestion = (index: number, suggestion: ProductSuggestion) => {
    setIngredients((current) =>
      current.map((ingredient, currentIndex) =>
        currentIndex === index
          ? {
              ...ingredient,
              productName: suggestion.name,
              unit: suggestion.unit || '',
            }
          : ingredient,
      ),
    );

    setIngredientStates((current) =>
      current.map((state, currentIndex) =>
        currentIndex === index
          ? {
              suggestions: [],
              selectedSuggestion: suggestion,
            }
          : state,
      ),
    );
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 p-4">
      <div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white">
        <div className="sticky top-0 flex items-center justify-between rounded-t-2xl border-b border-gray-200 bg-white p-6">
          <h2 className="text-2xl font-bold text-gray-900">Добавить новый рецепт</h2>
          <button
            onClick={onClose}
            className="rounded-full bg-gray-50 p-2 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6 p-6">
          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700">Название рецепта *</label>
            <input
              type="text"
              value={formData.title}
              onChange={(event) => setFormData({ ...formData, title: event.target.value })}
              required
              className="w-full rounded-lg border border-gray-300 px-4 py-3 transition-all focus:border-transparent focus:ring-2 focus:ring-green-500"
              placeholder="Введите название рецепта"
            />
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div>
              <label className="mb-2 block text-sm font-medium text-gray-700">Категория</label>
              <input
                type="text"
                value={formData.category}
                onChange={(event) => setFormData({ ...formData, category: event.target.value })}
                className="w-full rounded-lg border border-gray-300 px-4 py-3 transition-all focus:border-transparent focus:ring-2 focus:ring-green-500"
                placeholder="Например: Завтрак"
              />
            </div>
            <div>
              <label className="mb-2 block text-sm font-medium text-gray-700">Порции</label>
              <input
                type="number"
                min="1"
                step="1"
                value={formData.servings}
                onChange={(event) => setFormData({ ...formData, servings: event.target.value })}
                className="w-full rounded-lg border border-gray-300 px-4 py-3 transition-all focus:border-transparent focus:ring-2 focus:ring-green-500"
                placeholder="1"
              />
            </div>
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700">Описание</label>
            <textarea
              value={formData.description}
              onChange={(event) => setFormData({ ...formData, description: event.target.value })}
              rows={3}
              className="w-full resize-none rounded-lg border border-gray-300 px-4 py-3 transition-all focus:border-transparent focus:ring-2 focus:ring-green-500"
              placeholder="Краткое описание рецепта"
            />
          </div>

          <div>
            <div className="mb-4 flex items-center justify-between">
              <label className="block text-sm font-medium text-gray-700">Ингредиенты *</label>
              <button
                type="button"
                onClick={addIngredient}
                className="flex items-center space-x-1 rounded-lg bg-green-500 px-3 py-1 text-sm text-white transition-colors hover:bg-green-600"
              >
                <Plus className="h-4 w-4" />
                <span>Добавить</span>
              </button>
            </div>

            <div className="space-y-3">
              {ingredients.map((ingredient, index) => {
                const state = ingredientStates[index] ?? emptySuggestionState();
                const hasSelectedSuggestion = state.selectedSuggestion !== null;

                return (
                  <div key={index} className="grid grid-cols-12 items-start gap-3">
                    <div className="relative col-span-5">
                      <input
                        type="text"
                        value={ingredient.productName}
                        onChange={(event) => updateIngredient(index, 'productName', event.target.value)}
                        placeholder="Название продукта"
                        autoComplete="off"
                        className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-transparent focus:ring-2 focus:ring-green-500"
                      />

                      {state.suggestions.length > 0 && (
                        <div className="absolute z-10 mt-2 w-full overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg">
                          {state.suggestions.map((suggestion) => {
                            const suggestionKey = `${index}-${suggestion.name}-${suggestion.unit || 'unitless'}`;
                            return (
                              <button
                                key={suggestionKey}
                                type="button"
                                onClick={() => selectSuggestion(index, suggestion)}
                                className="flex w-full items-center justify-between gap-3 px-4 py-3 text-left hover:bg-gray-50"
                              >
                                <div>
                                  <div className="font-medium text-gray-900">{suggestion.name}</div>
                                  <div className="text-xs text-gray-500">
                                    Единица: {suggestion.unit || 'ед.'}
                                  </div>
                                </div>
                              </button>
                            );
                          })}
                        </div>
                      )}
                    </div>

                    <div className="col-span-3">
                      <input
                        type="number"
                        step="0.1"
                        min="0"
                        value={ingredient.quantity || ''}
                        onChange={(event) =>
                          updateIngredient(index, 'quantity', parseFloat(event.target.value) || 0)
                        }
                        placeholder="Количество"
                        className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-transparent focus:ring-2 focus:ring-green-500"
                      />
                    </div>

                    <div className="col-span-3">
                      <input
                        type="text"
                        value={ingredient.unit || ''}
                        onChange={(event) => updateIngredient(index, 'unit', event.target.value)}
                        placeholder="Единица"
                        disabled={hasSelectedSuggestion}
                        className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-transparent focus:ring-2 focus:ring-green-500 disabled:bg-gray-100 disabled:text-gray-500"
                      />
                    </div>

                    <div className="col-span-1">
                      <button
                        type="button"
                        onClick={() => removeIngredient(index)}
                        disabled={ingredients.length === 1}
                        className="rounded-lg p-2 text-red-500 transition-colors hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>

                  </div>
                );
              })}
            </div>
          </div>

          <div className="flex justify-end space-x-3 border-t border-gray-200 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-gray-300 px-6 py-2 text-gray-700 transition-colors hover:bg-gray-50"
            >
              Отмена
            </button>
            <button
              type="submit"
              disabled={loading}
              className="rounded-lg bg-gradient-to-r from-green-500 to-blue-500 px-6 py-2 font-medium text-white transition-all duration-200 hover:from-green-600 hover:to-blue-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {loading ? 'Создание...' : 'Создать рецепт'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddRecipeModal;
