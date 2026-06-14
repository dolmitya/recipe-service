import React, { useEffect, useMemo, useState } from 'react';
import {
  CalendarPlus,
  ChevronLeft,
  ChevronRight,
  Edit2,
  Flame,
  Plus,
  Save,
  Search,
  Trash2,
  X,
} from 'lucide-react';
import { Product, ProductSuggestion } from '../../types';
import {
  addCalendarEntry,
  createProduct,
  deleteProduct,
  getProductSuggestions,
  getProducts,
  updateProduct,
} from '../../services/api';
import ConsumptionModal from '../calendar/ConsumptionModal';

const PRODUCTS_PER_PAGE = 9;

const formatNumber = (value?: number) =>
  new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 }).format(value ?? 0);

const emptyProductForm = {
  name: '',
  quantity: '',
  unit: '',
  caloriesPerUnit: '',
  proteinsPerUnit: '',
  fatsPerUnit: '',
  carbsPerUnit: '',
};

const MacroBadge: React.FC<{ label: string; value?: number; accentClass: string }> = ({
  label,
  value,
  accentClass,
}) => (
  <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium ${accentClass}`}>
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

const FridgeSection: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [showAddForm, setShowAddForm] = useState(false);
  const [suggestions, setSuggestions] = useState<ProductSuggestion[]>([]);
  const [selectedSuggestion, setSelectedSuggestion] = useState<ProductSuggestion | null>(null);
  const [searchSuggestions, setSearchSuggestions] = useState<ProductSuggestion[]>([]);
  const [newProduct, setNewProduct] = useState(emptyProductForm);
  const [searchQuery, setSearchQuery] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [productForCalendar, setProductForCalendar] = useState<Product | null>(null);
  const [currentPage, setCurrentPage] = useState(1);

  useEffect(() => {
    loadProducts();
  }, []);

  const normalizedProductName = useMemo(() => newProduct.name.trim().toLowerCase(), [newProduct.name]);
  const normalizedSearchQuery = useMemo(() => searchQuery.trim().toLowerCase(), [searchQuery]);

  useEffect(() => {
    if (!showAddForm) {
      setSuggestions([]);
      return;
    }

    if (selectedSuggestion && normalizedProductName === selectedSuggestion.name.toLowerCase()) {
      return;
    }

    if (normalizedProductName.length === 0) {
      setSuggestions([]);
      return;
    }

    const timeoutId = window.setTimeout(async () => {
      try {
        const data = await getProductSuggestions(normalizedProductName);
        setSuggestions(Array.isArray(data) ? data : []);
      } catch (error) {
        console.error('Ошибка загрузки подсказок продуктов:', error);
        setSuggestions([]);
      }
    }, 250);

    return () => window.clearTimeout(timeoutId);
  }, [normalizedProductName, selectedSuggestion, showAddForm]);

  useEffect(() => {
    if (normalizedSearchQuery.length === 0) {
      setSearchSuggestions([]);
      setCurrentPage(1);
      return;
    }

    const timeoutId = window.setTimeout(async () => {
      try {
        const data = await getProductSuggestions(normalizedSearchQuery);
        setSearchSuggestions(Array.isArray(data) ? data : []);
      } catch (error) {
        console.error('Ошибка поиска продуктов в холодильнике:', error);
        setSearchSuggestions([]);
      }
    }, 250);

    setCurrentPage(1);
    return () => window.clearTimeout(timeoutId);
  }, [normalizedSearchQuery]);

  const loadProducts = async () => {
    try {
      const data = await getProducts();
      const normalizedProducts = Array.isArray(data) ? data : [];
      setProducts(normalizedProducts);
      setCurrentPage(1);
    } catch (error) {
      console.error('Ошибка загрузки продуктов:', error);
    } finally {
      setLoading(false);
    }
  };

  const filteredProducts = useMemo(() => {
    if (!normalizedSearchQuery) {
      return products;
    }

    const suggestionNames = new Set(searchSuggestions.map((suggestion) => suggestion.name.trim().toLowerCase()));

    return products.filter((product) => {
      const normalizedName = product.name.trim().toLowerCase();
      return normalizedName.includes(normalizedSearchQuery) || suggestionNames.has(normalizedName);
    });
  }, [normalizedSearchQuery, products, searchSuggestions]);

  const totalPages = Math.max(1, Math.ceil(filteredProducts.length / PRODUCTS_PER_PAGE));
  const paginatedProducts = useMemo(() => {
    const startIndex = (currentPage - 1) * PRODUCTS_PER_PAGE;
    return filteredProducts.slice(startIndex, startIndex + PRODUCTS_PER_PAGE);
  }, [currentPage, filteredProducts]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const resetAddForm = () => {
    setNewProduct(emptyProductForm);
    setSuggestions([]);
    setSelectedSuggestion(null);
    setErrorMessage(null);
  };

  const handleAddProduct = async (event: React.FormEvent) => {
    event.preventDefault();
    setErrorMessage(null);

    try {
      const created = await createProduct({
        name: newProduct.name.trim(),
        quantity: newProduct.quantity ? Number(newProduct.quantity) : 0,
        unit: newProduct.unit.trim() || undefined,
        caloriesPerUnit: newProduct.caloriesPerUnit ? Number(newProduct.caloriesPerUnit) : undefined,
        proteinsPerUnit: newProduct.proteinsPerUnit ? Number(newProduct.proteinsPerUnit) : undefined,
        fatsPerUnit: newProduct.fatsPerUnit ? Number(newProduct.fatsPerUnit) : undefined,
        carbsPerUnit: newProduct.carbsPerUnit ? Number(newProduct.carbsPerUnit) : undefined,
      });

      setProducts((current) => {
        const existingIndex = current.findIndex((product) => product.id === created.id);
        if (existingIndex === -1) {
          return [...current, created];
        }

        const next = [...current];
        next[existingIndex] = created;
        return next;
      });

      resetAddForm();
      setShowAddForm(false);
      setCurrentPage(1);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось сохранить продукт.');
    }
  };

  const handleProductNameChange = (value: string) => {
    if (selectedSuggestion && value.trim().toLowerCase() !== selectedSuggestion.name.toLowerCase()) {
      setSelectedSuggestion(null);
      setNewProduct((current) => ({
        ...current,
        name: value,
        unit: '',
        caloriesPerUnit: '',
        proteinsPerUnit: '',
        fatsPerUnit: '',
        carbsPerUnit: '',
      }));
      return;
    }

    setNewProduct((current) => ({ ...current, name: value }));
  };

  const handleSelectSuggestion = (suggestion: ProductSuggestion) => {
    setSelectedSuggestion(suggestion);
    setNewProduct((current) => ({
      ...current,
      name: suggestion.name,
      unit: suggestion.unit || '',
      caloriesPerUnit: suggestion.caloriesPerUnit != null ? suggestion.caloriesPerUnit.toString() : '',
      proteinsPerUnit: suggestion.proteinsPerUnit != null ? suggestion.proteinsPerUnit.toString() : '',
      fatsPerUnit: suggestion.fatsPerUnit != null ? suggestion.fatsPerUnit.toString() : '',
      carbsPerUnit: suggestion.carbsPerUnit != null ? suggestion.carbsPerUnit.toString() : '',
    }));
    setSuggestions([]);
  };

  const handleUpdateProduct = async (id: number, updatedData: { quantity?: number }) => {
    try {
      setErrorMessage(null);
      const updated = await updateProduct(id, updatedData);
      setProducts((current) => current.map((product) => (product.id === id ? updated : product)));
      setEditingId(null);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось обновить продукт.');
    }
  };

  const handleDeleteProduct = async (id: number) => {
    try {
      await deleteProduct(id);
      setProducts((current) => current.filter((product) => product.id !== id));
    } catch (error) {
      console.error('Ошибка удаления продукта:', error);
    }
  };

  const handleAddToCalendar = async (payload: { date: string; quantity: number; consumeFromFridge?: boolean }) => {
    if (!productForCalendar) {
      return;
    }

    await addCalendarEntry({
      date: payload.date,
      productId: productForCalendar.id,
      quantity: payload.quantity,
      consumeFromFridge: payload.consumeFromFridge,
    });

    await loadProducts();
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-12 w-12 animate-spin rounded-full border-b-2 border-green-500"></div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl p-6">
      <div className="mb-8 flex items-center justify-between gap-4">
        <div>
          <h2 className="text-3xl font-bold text-gray-900">Мой холодильник</h2>
          <p className="mt-2 text-gray-500">
            Калории и БЖУ задаются один раз на единицу продукта, дальше все значения считаются автоматически.
          </p>
          {filteredProducts.length > 0 && (
            <p className="mt-2 text-sm text-gray-500">
              Показано {paginatedProducts.length} из {filteredProducts.length}
              {filteredProducts.length !== products.length ? ` найденных` : ''} продуктов.
            </p>
          )}
        </div>
        <button
          onClick={() => {
            setShowAddForm(true);
            resetAddForm();
          }}
          className="shrink-0 rounded-lg bg-gradient-to-r from-green-500 to-blue-500 px-6 py-3 font-medium text-white shadow-md transition-all duration-200 hover:from-green-600 hover:to-blue-600"
        >
          <span className="flex items-center gap-2">
            <Plus className="h-5 w-5" />
            <span>Добавить продукт</span>
          </span>
        </button>
      </div>

      <div className="mb-6 rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
        <div className="relative">
          <Search className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            placeholder="Поиск продукта в холодильнике"
            className="w-full rounded-lg border border-gray-300 py-3 pl-10 pr-4 focus:border-transparent focus:ring-2 focus:ring-green-500"
          />
        </div>
      </div>

      {showAddForm && (
        <div className="mb-6 rounded-xl border border-gray-200 bg-white p-6 shadow-lg">
          {errorMessage && (
            <div className="mb-4 rounded-lg bg-red-100 p-3 text-red-700">
              {errorMessage}
            </div>
          )}

          <h3 className="mb-4 text-lg font-semibold">Новый продукт</h3>
          <form onSubmit={handleAddProduct} className="grid grid-cols-1 gap-4 md:grid-cols-6">
            <div className="relative md:col-span-2">
              <input
                type="text"
                placeholder="Название"
                value={newProduct.name}
                onChange={(event) => handleProductNameChange(event.target.value)}
                required
                autoComplete="off"
                className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-transparent focus:ring-2 focus:ring-green-500"
              />

              {suggestions.length > 0 && (
                <div className="absolute z-10 mt-2 w-full overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg">
                  {suggestions.map((suggestion) => {
                    const suggestionKey = `${suggestion.name}-${suggestion.unit || 'unitless'}`;
                    return (
                      <button
                        key={suggestionKey}
                        type="button"
                        onClick={() => handleSelectSuggestion(suggestion)}
                        className="flex w-full items-center justify-between gap-3 px-4 py-3 text-left hover:bg-gray-50"
                      >
                        <div className="space-y-1">
                          <div className="font-medium text-gray-900">{suggestion.name}</div>
                          <div className="text-xs text-gray-500">
                            {(suggestion.unit || 'ед.')}{' · '}
                            {formatNumber(suggestion.caloriesPerUnit)} ккал
                          </div>
                          <div className="flex flex-wrap gap-1">
                            <MacroBadge label="Б" value={suggestion.proteinsPerUnit} accentClass="bg-blue-50 text-blue-700" />
                            <MacroBadge label="Ж" value={suggestion.fatsPerUnit} accentClass="bg-amber-50 text-amber-700" />
                            <MacroBadge label="У" value={suggestion.carbsPerUnit} accentClass="bg-emerald-50 text-emerald-700" />
                          </div>
                        </div>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>

            <input
              type="number"
              step="0.1"
              placeholder="Количество"
              value={newProduct.quantity}
              onChange={(event) => setNewProduct({ ...newProduct, quantity: event.target.value })}
              className="rounded-lg border border-gray-300 px-4 py-2 focus:border-transparent focus:ring-2 focus:ring-green-500"
            />
            <input
              type="text"
              placeholder="Единица"
              value={newProduct.unit}
              onChange={(event) => setNewProduct({ ...newProduct, unit: event.target.value })}
              disabled={selectedSuggestion !== null}
              className="rounded-lg border border-gray-300 px-4 py-2 focus:border-transparent focus:ring-2 focus:ring-green-500 disabled:bg-gray-100 disabled:text-gray-500"
            />
            <input
              type="number"
              step="0.1"
              placeholder="Ккал за 1 ед."
              value={newProduct.caloriesPerUnit}
              onChange={(event) => setNewProduct({ ...newProduct, caloriesPerUnit: event.target.value })}
              disabled={selectedSuggestion !== null}
              className="rounded-lg border border-gray-300 px-4 py-2 focus:border-transparent focus:ring-2 focus:ring-green-500 disabled:bg-gray-100 disabled:text-gray-500"
            />
            <div className="grid grid-cols-3 gap-3 md:col-span-6">
              <input
                type="number"
                step="0.1"
                placeholder="Белки за 1 ед."
                value={newProduct.proteinsPerUnit}
                onChange={(event) => setNewProduct({ ...newProduct, proteinsPerUnit: event.target.value })}
                disabled={selectedSuggestion !== null}
                className="rounded-lg border border-gray-300 px-4 py-2 focus:border-transparent focus:ring-2 focus:ring-green-500 disabled:bg-gray-100 disabled:text-gray-500"
              />
              <input
                type="number"
                step="0.1"
                placeholder="Жиры за 1 ед."
                value={newProduct.fatsPerUnit}
                onChange={(event) => setNewProduct({ ...newProduct, fatsPerUnit: event.target.value })}
                disabled={selectedSuggestion !== null}
                className="rounded-lg border border-gray-300 px-4 py-2 focus:border-transparent focus:ring-2 focus:ring-green-500 disabled:bg-gray-100 disabled:text-gray-500"
              />
              <input
                type="number"
                step="0.1"
                placeholder="Углеводы за 1 ед."
                value={newProduct.carbsPerUnit}
                onChange={(event) => setNewProduct({ ...newProduct, carbsPerUnit: event.target.value })}
                disabled={selectedSuggestion !== null}
                className="rounded-lg border border-gray-300 px-4 py-2 focus:border-transparent focus:ring-2 focus:ring-green-500 disabled:bg-gray-100 disabled:text-gray-500"
              />
            </div>

            {selectedSuggestion && (
              <p className="col-span-full text-sm text-gray-500">
                Найден существующий продукт. Единица, калории и БЖУ подставлены автоматически, осталось указать количество.
              </p>
            )}

            <div className="col-span-full mt-2 flex flex-wrap gap-2">
              <button
                type="submit"
                className="rounded-lg bg-green-500 px-4 py-2 text-white transition-colors hover:bg-green-600"
              >
                <span className="flex items-center gap-1">
                  <Save className="h-4 w-4" />
                  <span>Сохранить</span>
                </span>
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowAddForm(false);
                  resetAddForm();
                }}
                className="rounded-lg bg-gray-500 px-4 py-2 text-white transition-colors hover:bg-gray-600"
              >
                <span className="flex items-center gap-1">
                  <X className="h-4 w-4" />
                  <span>Отмена</span>
                </span>
              </button>
            </div>
          </form>
        </div>
      )}

      {errorMessage && !showAddForm && (
        <div className="mb-6 rounded-lg bg-red-100 p-3 text-red-700">
          {errorMessage}
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
        {paginatedProducts.map((product) => (
          <ProductCard
            key={product.id}
            product={product}
            isEditing={editingId === product.id}
            onEdit={() => {
              setEditingId(product.id);
              setErrorMessage(null);
            }}
            onSave={(data) => handleUpdateProduct(product.id, data)}
            onCancel={() => {
              setEditingId(null);
              setErrorMessage(null);
            }}
            onDelete={() => handleDeleteProduct(product.id)}
            onAddToCalendar={() => setProductForCalendar(product)}
          />
        ))}
      </div>

      <PaginationControls currentPage={currentPage} totalPages={totalPages} onPageChange={setCurrentPage} />

      {filteredProducts.length === 0 && normalizedSearchQuery && (
        <div className="py-12 text-center">
          <h3 className="mb-2 text-xl font-semibold text-gray-600">Ничего не найдено</h3>
          <p className="text-gray-500">Попробуйте другой запрос для поиска продукта в холодильнике.</p>
        </div>
      )}

      {products.length === 0 && (
        <div className="py-12 text-center">
          <div className="mx-auto mb-4 flex h-24 w-24 items-center justify-center rounded-full bg-gray-100">
            <Plus className="h-12 w-12 text-gray-400" />
          </div>
          <h3 className="mb-2 text-xl font-semibold text-gray-600">Холодильник пуст</h3>
          <p className="text-gray-500">Добавьте первый продукт, чтобы начать.</p>
        </div>
      )}

      {productForCalendar && (
        <ConsumptionModal
          title="Добавить в календарь"
          itemName={productForCalendar.name}
          quantityLabel="Сколько съели"
          unitLabel={productForCalendar.unit}
          defaultQuantity={1}
          consumeFromFridgeLabel="Списать продукт из холодильника"
          defaultConsumeFromFridge={true}
          onClose={() => setProductForCalendar(null)}
          onSave={handleAddToCalendar}
        />
      )}
    </div>
  );
};

interface ProductCardProps {
  product: Product;
  isEditing: boolean;
  onEdit: () => void;
  onSave: (data: { quantity?: number }) => void;
  onCancel: () => void;
  onDelete: () => void;
  onAddToCalendar: () => void;
}

const ProductCard: React.FC<ProductCardProps> = ({
  product,
  isEditing,
  onEdit,
  onSave,
  onCancel,
  onDelete,
  onAddToCalendar,
}) => {
  const [editData, setEditData] = useState({
    quantity: product.quantity?.toString() || '',
  });

  useEffect(() => {
    setEditData({
      quantity: product.quantity?.toString() || '',
    });
  }, [product.quantity]);

  const handleSave = () => {
    onSave({
      quantity: editData.quantity ? Number(editData.quantity) : undefined,
    });
  };

  if (isEditing) {
    return (
      <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-lg">
        <div className="space-y-3">
          <div>
            <p className="mb-1 text-sm font-medium text-gray-700">{product.name}</p>
            {product.unit && <p className="text-xs text-gray-500">Единица: {product.unit}</p>}
          </div>
          <input
            type="number"
            step="0.1"
            value={editData.quantity}
            onChange={(event) => setEditData({ ...editData, quantity: event.target.value })}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-transparent focus:ring-2 focus:ring-green-500"
            placeholder="Количество"
          />
          <div className="flex space-x-2">
            <button
              onClick={handleSave}
              className="flex-1 rounded-lg bg-green-500 px-3 py-2 text-white transition-colors hover:bg-green-600"
            >
              <span className="flex items-center justify-center gap-1">
                <Save className="h-4 w-4" />
                <span>Сохранить</span>
              </span>
            </button>
            <button
              onClick={onCancel}
              className="flex-1 rounded-lg bg-gray-500 px-3 py-2 text-white transition-colors hover:bg-gray-600"
            >
              <span className="flex items-center justify-center gap-1">
                <X className="h-4 w-4" />
                <span>Отмена</span>
              </span>
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-lg transition-shadow duration-200 hover:shadow-xl">
      <div className="mb-4 flex items-start justify-between gap-4">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">{product.name}</h3>
          {product.unit && <p className="mt-1 text-sm text-gray-500">Единица: {product.unit}</p>}
        </div>
        <div className="flex shrink-0 space-x-1">
          <button
            onClick={onAddToCalendar}
            className="rounded-lg p-2 text-emerald-600 transition-colors hover:bg-emerald-50"
          >
            <CalendarPlus className="h-4 w-4" />
          </button>
          <button
            onClick={onEdit}
            className="rounded-lg p-2 text-blue-600 transition-colors hover:bg-blue-50"
          >
            <Edit2 className="h-4 w-4" />
          </button>
          <button
            onClick={onDelete}
            className="rounded-lg p-2 text-red-600 transition-colors hover:bg-red-50"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      </div>

      <div className="space-y-4">
        {product.quantity != null && (
          <p className="text-gray-600">
            <span className="font-medium">Количество:</span> {formatNumber(product.quantity)}
            {product.unit ? ` ${product.unit}` : ''}
          </p>
        )}

        <div className="rounded-lg bg-gray-50 p-3">
          <div className="mb-2 flex items-center gap-2 text-orange-600">
            <Flame className="h-4 w-4" />
            <span className="text-sm font-medium">
              {formatNumber(product.caloriesPerUnit)} ккал за 1 {product.unit || 'ед.'}
            </span>
          </div>
          <div className="flex flex-wrap gap-2">
            <MacroBadge label="Б" value={product.proteinsPerUnit} accentClass="bg-blue-100 text-blue-800" />
            <MacroBadge label="Ж" value={product.fatsPerUnit} accentClass="bg-amber-100 text-amber-800" />
            <MacroBadge label="У" value={product.carbsPerUnit} accentClass="bg-emerald-100 text-emerald-800" />
          </div>
        </div>

        <div className="rounded-lg border border-gray-200 p-3">
          <p className="mb-2 text-sm font-medium text-gray-700">Всего в запасе</p>
          <p className="text-sm text-gray-500">{formatNumber(product.totalCalories)} ккал</p>
          <div className="mt-2 flex flex-wrap gap-2">
            <MacroBadge label="Б" value={product.totalProteins} accentClass="bg-blue-50 text-blue-700" />
            <MacroBadge label="Ж" value={product.totalFats} accentClass="bg-amber-50 text-amber-700" />
            <MacroBadge label="У" value={product.totalCarbs} accentClass="bg-emerald-50 text-emerald-700" />
          </div>
        </div>
      </div>
    </div>
  );
};

export default FridgeSection;
