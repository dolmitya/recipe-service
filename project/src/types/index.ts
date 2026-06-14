export interface User {
  email: string;
  name: string;
  phone_number: string;
}

export interface AuthToken {
  token: string;
}

export interface UserProfile {
  id: number;
  email: string;
  fullName?: string;
}

export interface Product {
  id: number;
  name: string;
  quantity?: number;
  unit?: string;
  caloriesPerUnit?: number;
  proteinsPerUnit?: number;
  fatsPerUnit?: number;
  carbsPerUnit?: number;
  totalCalories?: number;
  totalProteins?: number;
  totalFats?: number;
  totalCarbs?: number;
}

export interface ProductInput {
  name: string;
  quantity?: number;
  unit?: string;
  caloriesPerUnit?: number;
  proteinsPerUnit?: number;
  fatsPerUnit?: number;
  carbsPerUnit?: number;
}

export interface ProductSuggestion {
  name: string;
  unit?: string;
  caloriesPerUnit?: number;
  proteinsPerUnit?: number;
  fatsPerUnit?: number;
  carbsPerUnit?: number;
}

export interface RecipeSuggestion {
  id: number;
  title: string;
}

export interface Ingredient {
  productName: string;
  quantity: number;
  unit?: string;
  calories?: number;
  proteins?: number;
  fats?: number;
  carbs?: number;
}

export interface Recipe {
  id: number;
  title: string;
  description?: string;
  category?: string;
  servings?: number;
  totalCalories?: number;
  caloriesPerServing?: number;
  totalProteins?: number;
  totalFats?: number;
  totalCarbs?: number;
  proteinsPerServing?: number;
  fatsPerServing?: number;
  carbsPerServing?: number;
  ingredients: Ingredient[];
}

export interface RecipeInput {
  title: string;
  description?: string;
  category?: string;
  servings?: number;
  ingredients: Ingredient[];
}

export interface CalendarEntry {
  id: number;
  entryType: 'PRODUCT' | 'RECIPE';
  referenceId: number;
  name: string;
  quantity: number;
  unitLabel?: string;
  calories: number;
  proteins: number;
  fats: number;
  carbs: number;
}

export interface CalendarDay {
  date: string;
  totalCalories: number;
  totalProteins: number;
  totalFats: number;
  totalCarbs: number;
  entries: CalendarEntry[];
}

export interface CalendarEntryInput {
  date: string;
  productId?: number;
  recipeId?: number;
  quantity: number;
  consumeFromFridge?: boolean;
}
