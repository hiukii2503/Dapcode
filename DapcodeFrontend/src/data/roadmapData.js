export const ROADMAP_NODES = [
  {
    id: 'arrays-and-hashing',
    name: 'Arrays & Hashing',
    categoryKey: 'Arrays & Hashing',
    x: 680,
    y: 70,
    total: 22,
  },

  {
    id: 'two-pointers',
    name: 'Two Pointers',
    categoryKey: 'Two Pointers',
    x: 420,
    y: 210,
    total: 13,
  },
  {
    id: 'stack',
    name: 'Stack',
    categoryKey: 'Stack',
    x: 940,
    y: 210,
    total: 15,
  },

  {
    id: 'binary-search',
    name: 'Binary Search',
    categoryKey: 'Binary Search',
    x: 160,
    y: 350,
    total: 14,
  },
  {
    id: 'sliding-window',
    name: 'Sliding Window',
    categoryKey: 'Sliding Window',
    x: 420,
    y: 350,
    total: 9,
  },
  {
    id: 'linked-list',
    name: 'Linked List',
    categoryKey: 'Linked List',
    x: 680,
    y: 350,
    total: 14,
  },

  {
    id: 'trees',
    name: 'Trees',
    categoryKey: 'Trees',
    x: 420,
    y: 490,
    total: 23,
  },

  {
    id: 'tries',
    name: 'Tries',
    categoryKey: 'Tries',
    x: 160,
    y: 630,
    total: 4,
  },
  {
    id: 'heap-priority-queue',
    name: 'Heap / Priority Queue',
    categoryKey: 'Heap / Priority Queue',
    x: 420,
    y: 630,
    total: 12,
  },
  {
    id: 'backtracking',
    name: 'Backtracking',
    categoryKey: 'Backtracking',
    x: 780,
    y: 630,
    total: 16,
  },

  {
    id: 'intervals',
    name: 'Intervals',
    categoryKey: 'Intervals',
    x: 160,
    y: 770,
    total: 7,
  },
  {
    id: 'greedy',
    name: 'Greedy',
    categoryKey: 'Greedy',
    x: 420,
    y: 770,
    total: 14,
  },
  {
    id: 'advanced-graphs',
    name: 'Advanced Graphs',
    categoryKey: 'Advanced Graphs',
    x: 680,
    y: 770,
    total: 10,
  },
  {
    id: 'graphs',
    name: 'Graphs',
    categoryKey: 'Graphs',
    x: 940,
    y: 770,
    total: 21,
  },
  {
    id: '1-d-dynamic-programming',
    name: '1-D Dynamic Programming',
    categoryKey: '1-D Dynamic Programming',
    x: 1200,
    y: 770,
    total: 17,
  },

  {
    id: '2-d-dynamic-programming',
    name: '2-D Dynamic Programming',
    categoryKey: '2-D Dynamic Programming',
    x: 1060,
    y: 910,
    total: 16,
  },
  {
    id: 'bit-manipulation',
    name: 'Bit Manipulation',
    categoryKey: 'Bit Manipulation',
    x: 1320,
    y: 910,
    total: 10,
  },

  {
    id: 'math-and-geometry',
    name: 'Math & Geometry',
    categoryKey: 'Math & Geometry',
    x: 1190,
    y: 1050,
    total: 13,
  },
];

export const ROADMAP_EDGES = [
  { from: 'arrays-and-hashing', to: 'two-pointers' },
  { from: 'arrays-and-hashing', to: 'stack' },

  { from: 'two-pointers', to: 'binary-search' },
  { from: 'two-pointers', to: 'sliding-window' },
  { from: 'two-pointers', to: 'linked-list' },

  { from: 'binary-search', to: 'trees' },
  { from: 'sliding-window', to: 'trees' },
  { from: 'linked-list', to: 'trees' },

  { from: 'trees', to: 'tries' },
  { from: 'trees', to: 'heap-priority-queue' },
  { from: 'trees', to: 'backtracking' },

  { from: 'heap-priority-queue', to: 'intervals' },
  { from: 'heap-priority-queue', to: 'greedy' },
  { from: 'heap-priority-queue', to: 'advanced-graphs' },

  { from: 'backtracking', to: 'graphs' },
  { from: 'backtracking', to: '1-d-dynamic-programming' },

  { from: '1-d-dynamic-programming', to: '2-d-dynamic-programming' },
  { from: '1-d-dynamic-programming', to: 'bit-manipulation' },

  { from: '2-d-dynamic-programming', to: 'math-and-geometry' },
  { from: 'bit-manipulation', to: 'math-and-geometry' },
];
