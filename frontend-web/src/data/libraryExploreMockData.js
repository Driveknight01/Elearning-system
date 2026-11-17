/**
 * Mock data for LandingExplorePage & LibraryPage UI review / offline fallback.
 */

export const MOCK_CATEGORIES = [
  { id: 1, name: 'Toán học', slug: 'toan-hoc', colorHex: '#4255FF' },
  { id: 2, name: 'Tiếng Anh', slug: 'tieng-anh', colorHex: '#10B981' },
  { id: 3, name: 'Sinh học', slug: 'sinh-hoc', colorHex: '#06B6D4' },
  { id: 4, name: 'Công nghệ thông tin', slug: 'cong-nghe-thong-tin', colorHex: '#8B5CF6' },
  { id: 5, name: 'Lịch sử & Triết học', slug: 'lich-su-triet-hoc', colorHex: '#F59E0B' },
];

export const MOCK_EXPLORE_SETS = [
  {
    id: 1001,
    title: 'Toán học',
    description: 'Tổng hợp công thức và khái niệm toán học cơ bản.',
    visibility: 'PUBLIC',
    featured: true,
    cardCount: 5,
    viewCount: 340,
    cloneCount: 28,
    avgRating: 4.8,
    ratingCount: 19,
    recentLearnersCount: 0,
    owner: {
      id: 11,
      username: 'quizlette42473901',
      fullName: 'quizlette42473901',
      avatarUrl: null,
      verifiedTeacher: true,
      roleLabel: 'Giáo viên',
    },
    category: MOCK_CATEGORIES[0],
  },
  {
    id: 1002,
    title: 'Toán học lớp 6',
    description: 'Số tự nhiên, phân số, ước chung lớn nhất và hình học trực quan.',
    visibility: 'PUBLIC',
    featured: true,
    cardCount: 60,
    viewCount: 890,
    cloneCount: 64,
    avgRating: 4.9,
    ratingCount: 42,
    recentLearnersCount: 0,
    owner: {
      id: 12,
      username: 'Hinnnn_3105',
      fullName: 'Hinnnn_3105',
      avatarUrl: null,
      verifiedTeacher: true,
      roleLabel: 'Giáo viên',
    },
    category: MOCK_CATEGORIES[0],
  },
  {
    id: 1003,
    title: 'An Toàn Sinh Học',
    description: 'Nguyên tắc an toàn phòng thí nghiệm và xử lý mẫu sinh học.',
    visibility: 'PUBLIC',
    featured: false,
    cardCount: 77,
    viewCount: 520,
    cloneCount: 31,
    avgRating: 4.7,
    ratingCount: 25,
    recentLearnersCount: 17,
    owner: {
      id: 13,
      username: 'why22102000',
      fullName: 'why22102000',
      avatarUrl: null,
      verifiedTeacher: false,
    },
    category: MOCK_CATEGORIES[2],
  },
  {
    id: 1004,
    title: 'English Collocation Verbs',
    description: 'Cụm động từ giao tiếp và IELTS Writing Band 7.5+.',
    visibility: 'PUBLIC',
    featured: false,
    cardCount: 15,
    viewCount: 410,
    cloneCount: 45,
    avgRating: 4.9,
    ratingCount: 33,
    recentLearnersCount: 12,
    owner: {
      id: 14,
      username: 'TiengAnh_Teacher',
      fullName: 'TiengAnh_Teacher',
      avatarUrl: null,
      verifiedTeacher: true,
    },
    category: MOCK_CATEGORIES[1],
  },
  {
    id: 1005,
    title: 'CHAPTER 1 - IT Fundamentals',
    description: 'Kiến trúc máy tính, hệ điều hành và mạng máy tính căn bản.',
    visibility: 'PUBLIC',
    featured: false,
    cardCount: 92,
    viewCount: 615,
    cloneCount: 53,
    avgRating: 4.6,
    ratingCount: 21,
    recentLearnersCount: 8,
    owner: {
      id: 15,
      username: 'quizlette45486610',
      fullName: 'quizlette45486610',
      avatarUrl: null,
      verifiedTeacher: false,
    },
    category: MOCK_CATEGORIES[3],
  },
  {
    id: 1006,
    title: 'Toán học rời rạc',
    description: 'Logic mệnh đề, đồ thị, tổ hợp và quan hệ tương đương.',
    visibility: 'PUBLIC',
    featured: false,
    cardCount: 34,
    viewCount: 275,
    cloneCount: 19,
    avgRating: 4.5,
    ratingCount: 11,
    recentLearnersCount: 5,
    owner: {
      id: 16,
      username: 'TXH17102006',
      fullName: 'TXH17102006',
      avatarUrl: null,
      verifiedTeacher: false,
    },
    category: MOCK_CATEGORIES[0],
  },
];

const svgAvatar = (bg, emoji) =>
  `data:image/svg+xml;utf8,${encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" rx="50" fill="${bg}"/><text x="50" y="64" font-size="52" text-anchor="middle">${emoji}</text></svg>`
  )}`;

export const MOCK_USERS_PREVIEW = [
  {
    id: 101,
    username: 'TiengAnh_Teacher',
    fullName: 'TiengAnh_Teacher',
    avatarUrl: svgAvatar('#F5D0FE', '🦩'),
    setCount: 14,
    classCount: 3,
    verifiedTeacher: true,
  },
  {
    id: 102,
    username: 'Hinnnn_3105',
    fullName: 'Hinnnn_3105',
    avatarUrl: svgAvatar('#1E1B4B', '🐻'),
    setCount: 8,
    classCount: 2,
    verifiedTeacher: true,
  },
  {
    id: 103,
    username: 'why22102000',
    fullName: 'why22102000',
    avatarUrl: svgAvatar('#BAE6FD', '🐱'),
    setCount: 5,
    classCount: 1,
    verifiedTeacher: false,
  },
  {
    id: 104,
    username: 'thoan_study',
    fullName: 'Thoan Nguyễn',
    avatarUrl: svgAvatar('#FDE68A', '🐰'),
    setCount: 11,
    classCount: 2,
    verifiedTeacher: false,
  },
  {
    id: 105,
    username: 'ToanHoc_Pro',
    fullName: 'Toán Học Pro',
    avatarUrl: svgAvatar('#A7F3D0', '🦊'),
    setCount: 6,
    classCount: 1,
    verifiedTeacher: false,
  },
  {
    id: 106,
    username: 'mint_flashcard',
    fullName: 'Mint Learner',
    avatarUrl: svgAvatar('#DDD6FE', '🐼'),
    setCount: 3,
    classCount: 0,
    verifiedTeacher: false,
  },
];

export const MOCK_GROUPS_PREVIEW = [
  {
    id: 201,
    name: 'HỌC TOÁN CÙNG NHAU',
    setCount: 6,
    memberCount: 14,
  },
  {
    id: 202,
    name: 'IELTS 7.5+ STUDY GROUP',
    setCount: 12,
    memberCount: 28,
  },
];

export const MOCK_STUDY_GUIDES = [
  {
    id: 301,
    title: 'Tiếng Anh Toán Học Cấp Tiểu Học & THCS',
    author: 'RoyT6212',
    setId: 1001,
  },
  {
    id: 302,
    title: 'Đáp án Đề Thi Trắc Nghiệm Logic & Toán Rời Rạc',
    author: 'incredibledotrang',
    setId: 1002,
  },
  {
    id: 303,
    title: 'Bất Đẳng Thức và So Sánh Biểu Thức Đại Số',
    author: 'C9C9C9C9C9C9',
    setId: 1006,
  },
];

export const MOCK_QUESTIONS_PREVIEW = [
  {
    id: 401,
    subject: 'Toán học',
    question: 'Làm thế nào để tính đạo hàm của hàm hợp f(g(x)) theo quy tắc dây chuyền?',
    verified: true,
    setId: 1001,
  },
  {
    id: 402,
    subject: 'English',
    question: 'What is the difference between "make a decision" and "do a favor" in collocations?',
    verified: true,
    setId: 1004,
  },
  {
    id: 403,
    subject: 'Sinh học',
    question: 'Cấp độ an toàn sinh học BSL-2 yêu cầu những trang bị bảo hộ cơ bản nào trong phòng thí nghiệm?',
    verified: true,
    setId: 1003,
  },
];

export const MOCK_LIBRARY_SETS = [
  {
    id: 1004,
    title: 'English Collocation Verbs',
    cardCount: 15,
    isMineMock: true,
    mockSubtitle: '15 thuật ngữ  •  Tác giả: bạn  •  Hoạt động gần đây trên 16 thg 11, 2026',
    owner: { id: 0, username: 'bạn' },
  },
  {
    id: 1007,
    title: 'English Collocations',
    cardCount: 10,
    isMineMock: true,
    mockSubtitle: '10 thuật ngữ  •  Tác giả: bạn  •  Hoạt động gần đây trên 21 thg 9, 2026',
    owner: { id: 0, username: 'bạn' },
  },
  {
    id: 1005,
    title: 'CHAPTER 1 - IT',
    cardCount: 92,
    isMineMock: false,
    mockSubtitle: '92 thuật ngữ  •  Tác giả: quizlette45486610  •  Hoạt động gần đây trên 15 thg 9, 2026',
    owner: { id: 15, username: 'quizlette45486610' },
  },
];

export const MOCK_CARDS_BY_SET = {
  default: [
    { id: 1, term: 'Algorithm', definition: 'Thuật toán — tập hợp hữu hạn các bước giải quyết bài toán.' },
    { id: 2, term: 'Hypothesis', definition: 'Giả thuyết khoa học cần được kiểm chứng bằng thực nghiệm.' },
    { id: 3, term: 'Derivative', definition: 'Đạo hàm — tốc độ biến thiên tức thời của hàm số.' },
    { id: 4, term: 'Collocation', definition: 'Sự kết hợp tự nhiên giữa các từ trong tiếng Anh.' },
    { id: 5, term: 'Biosafety', definition: 'An toàn sinh học trong phòng thí nghiệm và y tế.' },
  ],
};
