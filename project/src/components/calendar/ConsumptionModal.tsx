import React, { useState } from 'react';
import { X } from 'lucide-react';

interface ConsumptionModalProps {
  title: string;
  itemName: string;
  quantityLabel: string;
  unitLabel?: string;
  defaultQuantity?: number;
  consumeFromFridgeLabel?: string;
  defaultConsumeFromFridge?: boolean;
  onClose: () => void;
  onSave: (payload: { date: string; quantity: number; consumeFromFridge?: boolean }) => Promise<void> | void;
}

const formatLocalDate = (date: Date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const today = formatLocalDate(new Date());

const ConsumptionModal: React.FC<ConsumptionModalProps> = ({
  title,
  itemName,
  quantityLabel,
  unitLabel,
  defaultQuantity = 1,
  consumeFromFridgeLabel,
  defaultConsumeFromFridge = false,
  onClose,
  onSave,
}) => {
  const [date, setDate] = useState(today);
  const [quantity, setQuantity] = useState(defaultQuantity.toString());
  const [consumeFromFridge, setConsumeFromFridge] = useState(defaultConsumeFromFridge);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();

    const parsedQuantity = Number(quantity);
    if (!Number.isFinite(parsedQuantity) || parsedQuantity <= 0) {
      setError('Введите корректное количество.');
      return;
    }

    setSaving(true);
    setError(null);

    try {
      await onSave({ date, quantity: parsedQuantity, consumeFromFridge });
      onClose();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Не удалось сохранить запись.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b border-gray-200 p-6">
          <div>
            <h2 className="text-xl font-semibold text-gray-900">{title}</h2>
            <p className="mt-1 text-sm text-gray-500">{itemName}</p>
          </div>
          <button
            onClick={onClose}
            className="rounded-full bg-gray-50 p-2 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 p-6">
          {error && (
            <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
              {error}
            </div>
          )}

          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700">Дата</label>
            <input
              type="date"
              value={date}
              onChange={(event) => setDate(event.target.value)}
              className="w-full rounded-lg border border-gray-300 px-4 py-3 focus:border-transparent focus:ring-2 focus:ring-green-500"
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700">{quantityLabel}</label>
            <input
              type="number"
              step="0.1"
              min="0.1"
              value={quantity}
              onChange={(event) => setQuantity(event.target.value)}
              className="w-full rounded-lg border border-gray-300 px-4 py-3 focus:border-transparent focus:ring-2 focus:ring-green-500"
            />
            {unitLabel && <p className="mt-2 text-xs text-gray-500">Единица: {unitLabel}</p>}
          </div>

          {consumeFromFridgeLabel && (
            <label className="flex items-start gap-3 rounded-lg border border-gray-200 px-4 py-3">
              <input
                type="checkbox"
                checked={consumeFromFridge}
                onChange={(event) => setConsumeFromFridge(event.target.checked)}
                className="mt-1 h-4 w-4 rounded border-gray-300 text-green-600 focus:ring-green-500"
              />
              <div>
                <div className="text-sm font-medium text-gray-800">{consumeFromFridgeLabel}</div>
                <div className="mt-1 text-xs text-gray-500">
                  Если включено, продукт или ингредиенты будут списаны из холодильника.
                </div>
              </div>
            </label>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-gray-300 px-5 py-2 text-gray-700 transition-colors hover:bg-gray-50"
            >
              Отмена
            </button>
            <button
              type="submit"
              disabled={saving}
              className="rounded-lg bg-gradient-to-r from-green-500 to-blue-500 px-5 py-2 text-white transition-all hover:from-green-600 hover:to-blue-600 disabled:opacity-50"
            >
              {saving ? 'Сохранение...' : 'Сохранить'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ConsumptionModal;
