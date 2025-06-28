export interface User {
  email: string;
  name: string;
  phone_number: string;
}

export interface AuthToken {
  token: string;
}

export interface Product {
  id: number;
  name: string;
  quantity?: number;
  unit?: string;
}

export interface ProductInput {
  name: string;
  quantity?: number;
  unit?: string;
}

export interface Ingredient {
  productName: string;
  quantity: number;
  unit?: string;
}

export interface Recipe {
  id: number;
  title: string;
  description?: string;
  category?: string;
  ingredients: Ingredient[];
}

export interface RecipeInput {
  title: string;
  description?: string;
  category?: string;
  ingredients: Ingredient[];
}