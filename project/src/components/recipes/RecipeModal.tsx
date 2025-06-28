import React from 'react';
import { X, Heart, Users } from 'lucide-react';
import { Recipe } from '../../types';

interface RecipeModalProps {
  recipe: Recipe;
  onClose: () => void;
  isFavorite: boolean;
  onToggleFavorite: () => void;
}

const RecipeModal: React.FC<RecipeModalProps> = ({
  recipe,
  onClose,
  isFavorite,
  onToggleFavorite,
}) => {
  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 p-6 flex justify-between items-center rounded-t-2xl">
          <h2 className="text-2xl font-bold text-gray-900">{recipe.title}</h2>
          <div className="flex items-center space-x-2">
            <button
              onClick={onToggleFavorite}
              className={`p-2 rounded-full transition-colors ${
                isFavorite
                  ? 'bg-red-50 text-red-500 hover:bg-red-100'
                  : 'bg-gray-50 text-gray-400 hover:bg-gray-100 hover:text-red-500'
              }`}
            >
              <Heart className={`w-6 h-6 ${isFavorite ? 'fill-current' : ''}`} />
            </button>
            <button
              onClick={onClose}
              className="p-2 rounded-full bg-gray-50 text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition-colors"
            >
              <X className="w-6 h-6" />
            </button>
          </div>
        </div>

        <div className="p-6">
          {recipe.category && (
            <div className="mb-4">
              <span className="inline-block bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm font-medium">
                {recipe.category}
              </span>
            </div>
          )}

          {recipe.description && (
            <div className="mb-6">
              <p className="text-gray-700 leading-relaxed">{recipe.description}</p>
            </div>
          )}

          <div className="mb-6">
            <div className="flex items-center mb-4">
              <Users className="w-5 h-5 text-gray-500 mr-2" />
              <h3 className="text-lg font-semibold text-gray-900">
                Ингредиенты ({recipe.ingredients.length})
              </h3>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {recipe.ingredients.map((ingredient, index) => (
                <div
                  key={index}
                  className="bg-gray-50 rounded-lg p-3 flex justify-between items-center"
                >
                  <span className="font-medium text-gray-900">
                    {ingredient.productName}
                  </span>
                  <span className="text-gray-600 text-sm">
                    {ingredient.quantity} {ingredient.unit || ''}
                  </span>
                </div>
              ))}
            </div>
          </div>

          <div className="flex justify-end space-x-3">
            <button
              onClick={onClose}
              className="px-6 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
            >
              Закрыть
            </button>
            <button
              onClick={onToggleFavorite}
              className={`px-6 py-2 rounded-lg font-medium transition-colors ${
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
  );
};

export default RecipeModal;