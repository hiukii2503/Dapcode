import api from './client';

let allTheoriesCache = null;
let theoryCategoriesCache = null;
const theoryDetailCache = new Map();

export const clearTheoriesCache = () => {
  allTheoriesCache = null;
  theoryCategoriesCache = null;
  theoryDetailCache.clear();
};

export const getTheories = async (category) => {
  if (!category && allTheoriesCache) {
    return allTheoriesCache;
  }

  const res = await api.get('/theories', {
    params: category ? { category } : {},
  });

  if (!category) {
    allTheoriesCache = res;
  }

  return res;
};

export const getTheoriesPaged = async (params = {}) => {
  const queryParams = {
    page: params.page ?? 0,
    size: params.size ?? 9,
    ...(params.category ? { category: params.category } : {}),
    ...(params.search ? { search: params.search } : {}),
  };

  return await api.get('/theories/paged', {
    params: queryParams,
  });
};

export const getTheoryCategories = async () => {
  if (theoryCategoriesCache) {
    return theoryCategoriesCache;
  }

  const res = await api.get('/theories/categories');
  theoryCategoriesCache = res;
  return res;
};

export const getTheoryBySlug = async (slug) => {
  if (slug && theoryDetailCache.has(slug)) {
    return theoryDetailCache.get(slug);
  }
  const res = await api.get(`/theories/${slug}`);
  if (slug && res) {
    theoryDetailCache.set(slug, res);
  }
  return res;
};
