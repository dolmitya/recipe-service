import React, { useEffect, useMemo, useState } from 'react';
import { CalendarDays, ChevronLeft, ChevronRight, Trash2, UtensilsCrossed } from 'lucide-react';
import { CalendarDay } from '../../types';
import { deleteCalendarEntry, getCalendarDay } from '../../services/api';

const CALORIE_TARGET = 2500;
const OVERVIEW_DAYS = 7;

const formatNumber = (value: number) =>
  new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 }).format(value);

const formatLocalDate = (date: Date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const shiftDate = (date: string, days: number) => {
  const nextDate = new Date(`${date}T00:00:00`);
  nextDate.setDate(nextDate.getDate() + days);
  return formatLocalDate(nextDate);
};

const buildOverviewDates = (date: string, days: number) => {
  const dates: string[] = [];
  for (let offset = days - 1; offset >= 0; offset -= 1) {
    dates.push(shiftDate(date, -offset));
  }
  return dates;
};

const MacroSummaryCard: React.FC<{ label: string; value: number; accentClass: string }> = ({
  label,
  value,
  accentClass,
}) => (
  <div className={`rounded-xl border p-5 ${accentClass}`}>
    <p className="mb-2 text-sm text-gray-500">{label}</p>
    <p className="text-3xl font-bold text-gray-900">{formatNumber(value)}</p>
  </div>
);

const CalendarSection: React.FC = () => {
  const [selectedDate, setSelectedDate] = useState(formatLocalDate(new Date()));
  const [calendarDay, setCalendarDay] = useState<CalendarDay | null>(null);
  const [calendarOverview, setCalendarOverview] = useState<CalendarDay[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadCalendar = async (date: string) => {
    try {
      setLoading(true);
      setError(null);
      const overviewDates = buildOverviewDates(date, OVERVIEW_DAYS);
      const [selectedDay, overviewDays] = await Promise.all([
        getCalendarDay(date),
        Promise.all(overviewDates.map((overviewDate) => getCalendarDay(overviewDate))),
      ]);
      setCalendarDay(selectedDay);
      setCalendarOverview(overviewDays);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Не удалось загрузить календарь.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCalendar(selectedDate);
  }, [selectedDate]);

  const summary = useMemo(() => {
    const entries = calendarDay?.entries ?? [];
    const productCount = entries.filter((entry) => entry.entryType === 'PRODUCT').length;
    const recipeCount = entries.filter((entry) => entry.entryType === 'RECIPE').length;
    return { productCount, recipeCount };
  }, [calendarDay]);

  const handleDeleteEntry = async (entryId: number) => {
    try {
      await deleteCalendarEntry(entryId);
      await loadCalendar(selectedDate);
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : 'Не удалось удалить запись.');
    }
  };

  if (loading && !calendarDay) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-12 w-12 animate-spin rounded-full border-b-2 border-green-500"></div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl p-6">
      <div className="mb-8 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-r from-emerald-500 to-cyan-500 text-white">
            <CalendarDays className="h-6 w-6" />
          </div>
          <div>
            <h2 className="text-3xl font-bold text-gray-900">Календарь питания</h2>
            <p className="mt-1 text-gray-500">
              Все продукты и рецепты, которые были съедены за день, вместе с калориями и БЖУ.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setSelectedDate((current) => shiftDate(current, -1))}
            className="rounded-lg border border-gray-200 bg-white p-3 text-gray-600 transition-colors hover:bg-gray-50"
          >
            <ChevronLeft className="h-5 w-5" />
          </button>
          <input
            type="date"
            value={selectedDate}
            onChange={(event) => setSelectedDate(event.target.value)}
            className="rounded-lg border border-gray-300 bg-white px-4 py-3 focus:border-transparent focus:ring-2 focus:ring-green-500"
          />
          <button
            onClick={() => setSelectedDate((current) => shiftDate(current, 1))}
            className="rounded-lg border border-gray-200 bg-white p-3 text-gray-600 transition-colors hover:bg-gray-50"
          >
            <ChevronRight className="h-5 w-5" />
          </button>
        </div>
      </div>

      {error && (
        <div className="mb-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-red-600">
          {error}
        </div>
      )}

      <div className="mb-6 overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-200 px-6 py-4">
          <div>
            <h3 className="text-lg font-semibold text-gray-900">Калории по дням</h3>
            <p className="text-sm text-gray-500">
              Последние {OVERVIEW_DAYS} дней. Цель: {formatNumber(CALORIE_TARGET)} ккал.
            </p>
          </div>
        </div>
        <div className="grid grid-cols-1 gap-px bg-gray-100 md:grid-cols-7">
          {calendarOverview.map((day) => {
            const isSelected = day.date === selectedDate;
            const isOverTarget = day.totalCalories > CALORIE_TARGET;
            return (
              <button
                key={day.date}
                type="button"
                onClick={() => setSelectedDate(day.date)}
                className={`bg-white px-4 py-4 text-left transition-colors hover:bg-gray-50 ${
                  isSelected ? 'ring-2 ring-inset ring-green-500' : ''
                }`}
              >
                <div className="mb-2 text-sm font-medium text-gray-600">{day.date}</div>
                <div className="text-2xl font-bold text-gray-900">{formatNumber(day.totalCalories)}</div>
                <div className={`mt-2 text-xs font-medium ${isOverTarget ? 'text-red-600' : 'text-emerald-600'}`}>
                  {isOverTarget ? 'Выше цели' : 'В пределах цели'}
                </div>
                <div className="mt-3 h-2 rounded-full bg-gray-100">
                  <div
                    className={`h-2 rounded-full ${isOverTarget ? 'bg-red-500' : 'bg-emerald-500'}`}
                    style={{ width: `${Math.min((day.totalCalories / CALORIE_TARGET) * 100, 100)}%` }}
                  />
                </div>
              </button>
            );
          })}
        </div>
      </div>

      <div className="mb-6 grid grid-cols-1 gap-4 md:grid-cols-5">
        <MacroSummaryCard label="Всего калорий" value={calendarDay?.totalCalories ?? 0} accentClass="border-gray-200 bg-white" />
        <MacroSummaryCard label="Белки" value={calendarDay?.totalProteins ?? 0} accentClass="border-blue-100 bg-blue-50" />
        <MacroSummaryCard label="Жиры" value={calendarDay?.totalFats ?? 0} accentClass="border-amber-100 bg-amber-50" />
        <MacroSummaryCard label="Углеводы" value={calendarDay?.totalCarbs ?? 0} accentClass="border-emerald-100 bg-emerald-50" />
        <div className="rounded-xl border border-gray-200 bg-white p-5">
          <p className="mb-2 text-sm text-gray-500">Записи</p>
          <p className="text-3xl font-bold text-gray-900">{(calendarDay?.entries.length ?? 0).toString()}</p>
          <p className="mt-2 text-sm text-gray-500">
            Продукты: {summary.productCount} · Рецепты: {summary.recipeCount}
          </p>
        </div>
      </div>

      <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4">
          <h3 className="text-lg font-semibold text-gray-900">Записи за день</h3>
          <span className="text-sm text-gray-500">{calendarDay?.entries.length ?? 0}</span>
        </div>

        {loading ? (
          <div className="flex justify-center p-10">
            <div className="h-10 w-10 animate-spin rounded-full border-b-2 border-green-500"></div>
          </div>
        ) : (calendarDay?.entries.length ?? 0) === 0 ? (
          <div className="p-10 text-center">
            <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-gray-100">
              <UtensilsCrossed className="h-8 w-8 text-gray-400" />
            </div>
            <p className="text-lg font-medium text-gray-700">За этот день еще нет записей</p>
            <p className="mt-2 text-sm text-gray-500">
              Добавляйте продукты из холодильника и рецепты из карточек.
            </p>
          </div>
        ) : (
          <div className="divide-y divide-gray-100">
            {calendarDay?.entries.map((entry) => (
              <div key={entry.id} className="flex items-start justify-between gap-4 px-6 py-4">
                <div>
                  <div className="mb-2 flex items-center gap-2">
                    <span
                      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${
                        entry.entryType === 'PRODUCT'
                          ? 'bg-green-100 text-green-700'
                          : 'bg-blue-100 text-blue-700'
                      }`}
                    >
                      {entry.entryType === 'PRODUCT' ? 'Продукт' : 'Рецепт'}
                    </span>
                    <h4 className="text-base font-semibold text-gray-900">{entry.name}</h4>
                  </div>
                  <p className="text-sm text-gray-500">
                    {formatNumber(entry.quantity)} {entry.unitLabel || 'ед.'}
                  </p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    <span className="inline-flex rounded-full bg-orange-100 px-2 py-1 text-xs font-medium text-orange-700">
                      {formatNumber(entry.calories)} ккал
                    </span>
                    <span className="inline-flex rounded-full bg-blue-50 px-2 py-1 text-xs font-medium text-blue-700">
                      Б: {formatNumber(entry.proteins)}
                    </span>
                    <span className="inline-flex rounded-full bg-amber-50 px-2 py-1 text-xs font-medium text-amber-700">
                      Ж: {formatNumber(entry.fats)}
                    </span>
                    <span className="inline-flex rounded-full bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-700">
                      У: {formatNumber(entry.carbs)}
                    </span>
                  </div>
                </div>

                <button
                  onClick={() => handleDeleteEntry(entry.id)}
                  className="rounded-lg p-2 text-red-500 transition-colors hover:bg-red-50"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default CalendarSection;
