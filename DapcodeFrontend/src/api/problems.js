import api from './client';

let problemsCache = null;
const problemDetailCache = new Map();

export const clearProblemsCache = () => {
  problemsCache = null;
  problemDetailCache.clear();
};

export const getProblems = async (params = {}) => {
  const isDefault =
    params.page === undefined &&
    (params.size === undefined || params.size === 250) &&
    !params.category &&
    !params.difficulty &&
    !params.search &&
    !params.query;

  if (isDefault && problemsCache) {
    return problemsCache;
  }

  const queryParams = {
    size: params.size ?? 250,
    ...(params.page !== undefined ? { page: params.page } : {}),
    ...(params.category ? { category: params.category } : {}),
    ...(params.difficulty ? { difficulty: params.difficulty } : {}),
    ...(params.search ? { search: params.search } : {}),
    ...(params.query && !params.search ? { search: params.query } : {}),
  };

  const res = await api.get('/problems', {
    params: queryParams,
  });

  if (isDefault) {
    problemsCache = res;
  }

  return res;
};

export const getProblemBySlug = async (slug) => {
  if (slug && problemDetailCache.has(slug)) {
    return problemDetailCache.get(slug);
  }
  const res = await api.get(`/problems/${slug}`);
  if (slug && res) {
    problemDetailCache.set(slug, res);
  }
  return res;
};
