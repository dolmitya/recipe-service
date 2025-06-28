import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2, Save, X } from 'lucide-react';
import { Product } from '../../types';
import { getProducts, createProduct, updateProduct, deleteProduct } from '../../services/api';

const FridgeSection: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [showAddForm, setShowAddForm] = useState(false);
  const [newProduct, setNewProduct] = useState({
    name: '',
    quantity: '',
    unit: '',
  });

  useEffect(() => {
    loadProducts();
  }, []);

  const loadProducts = async () => {
    try {
      const data = await getProducts();
      setProducts(data);
    } catch (error) {
      console.error('Ошибка загрузки продуктов:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const productData = {
        name: newProduct.name,
        quantity: newProduct.quantity ? parseFloat(newProduct.quantity) : undefined,
        unit: newProduct.unit || undefined,
      };
      
      const created = await createProduct(productData);
      setProducts([...products, created]);
      setNewProduct({ name: '', quantity: '', unit: '' });
      setShowAddForm(false);
    } catch (error) {
      console.error('Ошибка добавления продукта:', error);
    }
  };

  const handleUpdateProduct = async (id: number, updatedData: any) => {
    try {
      const updated = await updateProduct(id, updatedData);
      setProducts(products.map(p => p.id === id ? updated : p));
      setEditingId(null);
    } catch (error) {
      console.error('Ошибка обновления продукта:', error);
    }
  };

  const handleDeleteProduct = async (id: number) => {
    try {
      await deleteProduct(id);
      setProducts(products.filter(p => p.id !== id));
    } catch (error) {
      console.error('Ошибка удаления продукта:', error);
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
    <div className="max-w-4xl mx-auto p-6">
      <div className="flex justify-between items-center mb-8">
        <h2 className="text-3xl font-bold text-gray-900">Мой холодильник</h2>
        <button
          onClick={() => setShowAddForm(true)}
          className="bg-gradient-to-r from-green-500 to-blue-500 text-white px-6 py-3 rounded-lg font-medium hover:from-green-600 hover:to-blue-600 transition-all duration-200 flex items-center space-x-2 shadow-md"
        >
          <Plus className="w-5 h-5" />
          <span>Добавить продукт</span>
        </button>
      </div>

      {showAddForm && (
        <div className="bg-white rounded-xl shadow-lg p-6 mb-6 border border-gray-200">
          <h3 className="text-lg font-semibold mb-4">Добавить новый продукт</h3>
          <form onSubmit={handleAddProduct} className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <input
              type="text"
              placeholder="Название продукта"
              value={newProduct.name}
              onChange={(e) => setNewProduct({ ...newProduct, name: e.target.value })}
              required
              className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
            />
            <input
              type="number"
              step="0.1"
              placeholder="Количество"
              value={newProduct.quantity}
              onChange={(e) => setNewProduct({ ...newProduct, quantity: e.target.value })}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
            />
            <input
              type="text"
              placeholder="Единица измерения"
              value={newProduct.unit}
              onChange={(e) => setNewProduct({ ...newProduct, unit: e.target.value })}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
            />
            <div className="flex space-x-2">
              <button
                type="submit"
                className="bg-green-500 text-white px-4 py-2 rounded-lg hover:bg-green-600 transition-colors flex items-center space-x-1"
              >
                <Save className="w-4 h-4" />
                <span>Сохранить</span>
              </button>
              <button
                type="button"
                onClick={() => setShowAddForm(false)}
                className="bg-gray-500 text-white px-4 py-2 rounded-lg hover:bg-gray-600 transition-colors flex items-center space-x-1"
              >
                <X className="w-4 h-4" />
                <span>Отмена</span>
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {products.map((product) => (
          <ProductCard
            key={product.id}
            product={product}
            isEditing={editingId === product.id}
            onEdit={() => setEditingId(product.id)}
            onSave={(updatedData) => handleUpdateProduct(product.id, updatedData)}
            onCancel={() => setEditingId(null)}
            onDelete={() => handleDeleteProduct(product.id)}
          />
        ))}
      </div>

      {products.length === 0 && (
        <div className="text-center py-12">
          <div className="w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <Plus className="w-12 h-12 text-gray-400" />
          </div>
          <h3 className="text-xl font-semibold text-gray-600 mb-2">Холодильник пуст</h3>
          <p className="text-gray-500">Добавьте первый продукт, чтобы начать</p>
        </div>
      )}
    </div>
  );
};

interface ProductCardProps {
  product: Product;
  isEditing: boolean;
  onEdit: () => void;
  onSave: (data: any) => void;
  onCancel: () => void;
  onDelete: () => void;
}

const ProductCard: React.FC<ProductCardProps> = ({
  product,
  isEditing,
  onEdit,
  onSave,
  onCancel,
  onDelete,
}) => {
  const [editData, setEditData] = useState({
    name: product.name,
    quantity: product.quantity?.toString() || '',
    unit: product.unit || '',
  });

  const handleSave = () => {
    onSave({
      name: editData.name,
      quantity: editData.quantity ? parseFloat(editData.quantity) : undefined,
      unit: editData.unit || undefined,
    });
  };

  if (isEditing) {
    return (
      <div className="bg-white rounded-xl shadow-lg p-6 border border-gray-200">
        <div className="space-y-3">
          <input
            type="text"
            value={editData.name}
            onChange={(e) => setEditData({ ...editData, name: e.target.value })}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
            placeholder="Название"
          />
          <input
            type="number"
            step="0.1"
            value={editData.quantity}
            onChange={(e) => setEditData({ ...editData, quantity: e.target.value })}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
            placeholder="Количество"
          />
          <input
            type="text"
            value={editData.unit}
            onChange={(e) => setEditData({ ...editData, unit: e.target.value })}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
            placeholder="Единица измерения"
          />
          <div className="flex space-x-2">
            <button
              onClick={handleSave}
              className="bg-green-500 text-white px-3 py-2 rounded-lg hover:bg-green-600 transition-colors flex items-center space-x-1 flex-1"
            >
              <Save className="w-4 h-4" />
              <span>Сохранить</span>
            </button>
            <button
              onClick={onCancel}
              className="bg-gray-500 text-white px-3 py-2 rounded-lg hover:bg-gray-600 transition-colors flex items-center space-x-1 flex-1"
            >
              <X className="w-4 h-4" />
              <span>Отмена</span>
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl shadow-lg p-6 border border-gray-200 hover:shadow-xl transition-shadow duration-200">
      <div className="flex justify-between items-start mb-4">
        <h3 className="text-lg font-semibold text-gray-900">{product.name}</h3>
        <div className="flex space-x-1">
          <button
            onClick={onEdit}
            className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
          >
            <Edit2 className="w-4 h-4" />
          </button>
          <button
            onClick={onDelete}
            className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      </div>
      
      <div className="space-y-2">
        {product.quantity && (
          <p className="text-gray-600">
            <span className="font-medium">Количество:</span> {product.quantity}
            {product.unit && ` ${product.unit}`}
          </p>
        )}
      </div>
    </div>
  );
};

export default FridgeSection;