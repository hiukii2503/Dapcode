import api from './client';

let problemsCache = null;
const problemDetailCache = new Map();

export const clearProblemsCache = () => {
  problemsCache = null;
  problemDetailCache.clear();
};

export const getProblems = async (params = {}) => {
  const isDefault =
    !params.page &&
    (!params.size || params.size === 250) &&
    !params.category &&
    !params.difficulty &&
    !params.query;

  if (isDefault && problemsCache) {
    return problemsCache;
  }

  const res = await api.get('/problems', {
    params: {
      size: 250,
      ...params,
    },
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
