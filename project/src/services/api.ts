const API_BASE_URL = 'http://localhost:8189';

let authToken: string | null = localStorage.getItem('authToken');

const apiRequest = async (endpoint: string, options: RequestInit = {}) => {
  const url = `${API_BASE_URL}${endpoint}`;
  
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (authToken) {
    headers.Authorization = `Bearer ${authToken}`;
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || `HTTP error! status: ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
};

export const setAuthToken = (token: string) => {
  authToken = token;
  localStorage.setItem('authToken', token);
};

export const clearAuthToken = () => {
  authToken = null;
  localStorage.removeItem('authToken');
};

export const getAuthToken = () => authToken;

// Auth API
export const register = async (userData: {
  email: string;
  password: string;
  fullName: string;
}) => {
  const response = await apiRequest('/register', {
    method: 'POST',
    body: JSON.stringify(userData),
  });
  
  if (response.token) {
    setAuthToken(response.token);
  }
  
  return response;
};

export const login = async (credentials: { email: string; password: string }) => {
  const response = await apiRequest('/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  });
  
  if (response.token) {
    setAuthToken(response.token);
  }
  
  return response;
};

// Products API
export const getProducts = async () => {
  return apiRequest('/secured/products');
};

export const createProduct = async (product: {
  name: string;
  quantity?: number;
  unit?: string;
}) => {
  return apiRequest('/secured/products', {
    method: 'POST',
    body: JSON.stringify(product),
  });
};

export const updateProduct = async (id: number, product: {
  name: string;
  quantity?: number;
  unit?: string;
}) => {
  return apiRequest(`/secured/products/${id}`, {
    method: 'PUT',
    body: JSON.stringify(product),
  });
};

export const deleteProduct = async (id: number) => {
  return apiRequest(`/secured/products/${id}`, {
    method: 'DELETE',
  });
};

// Recipes API
export const getRecipes = async (category?: string) => {
  const params = category ? `?category=${encodeURIComponent(category)}` : '';
  return apiRequest(`/secured/recipes${params}`);
};

export const createRecipe = async (recipe: {
  title: string;
  description?: string;
  category?: string;
  ingredients: Array<{
    productName: string;
    quantity: number;
    unit?: string;
  }>;
}) => {
  return apiRequest('/secured/recipes', {
    method: 'POST',
    body: JSON.stringify(recipe),
  });
};

export const searchRecipesByProducts = async () => {
  return apiRequest('/secured/recipes/search');
};

export const addToFavorites = async (recipeId: number) => {
  return apiRequest(`/secured/recipes/${recipeId}/favorites`, {
    method: 'POST',
  });
};

export const removeFromFavorites = async (recipeId: number) => {
  return apiRequest(`/secured/recipes/${recipeId}/favorites`, {
    method: 'DELETE',
  });
};

export const getFavoriteRecipes = async () => {
  return apiRequest('/secured/recipes/favorites');
};