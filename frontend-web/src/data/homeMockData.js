import jumpBackInImg from '../assets/home/jump-back-in.png';
import gameBlastImg from '../assets/home/game-blast.jpg';
import gameBlocksImg from '../assets/home/game-blocks.jpg';
import gameMatchImg from '../assets/home/game-match.jpg';
import createFlashcardsImg from '../assets/home/create-flashcards.png';
import upNextFlashcardsImg from '../assets/home/up-next-flashcards.jpg';

export const HOME_BANNERS = {
  jumpBackIn: jumpBackInImg,
  gameBlast: gameBlastImg,
  gameBlocks: gameBlocksImg,
  gameMatch: gameMatchImg,
  createFlashcards: createFlashcardsImg,
  upNextFlashcards: upNextFlashcardsImg,
};

export const JUMP_BACK_IN_ITEMS = [
  {
    id: 'fsrs-due',
    title: 'Ôn tập thông minh FSRS',
    subtitle: '12 thẻ đến hạn ôn tập hôm nay',
    progressPercent: 35,
    progressLabel: '35% mục tiêu hôm nay đã xong',
    ctaLabel: 'Ôn tập ngay',
    to: '/explore',
    showProgress: true,
    banner: jumpBackInImg,
  },
  {
    id: 'english-collocations',
    title: 'English Collocations',
    subtitle: 'Bạn đạt 50% ở bài kiểm tra gần nhất',
    ctaLabel: 'Làm bài kiểm tra mới',
    to: '/explore?q=English+Collocations',
    showProgress: false,
    banner: jumpBackInImg,
  },
  {
    id: 'chapter-1-it',
    title: 'CHAPTER 1 - IT',
    subtitle: '4% câu hỏi đã hoàn thành',
    progressPercent: 4,
    progressLabel: '4% câu hỏi đã hoàn thành',
    ctaLabel: 'Tiếp tục',
    to: '/explore?q=CHAPTER+1+IT',
    showProgress: true,
    banner: jumpBackInImg,
  },
];

export const RECENT_SETS = [
  {
    id: 'r1',
    title: 'English Collocation Verbs',
    meta: '15 thẻ • của bạn',
    type: 'set',
    to: '/explore?q=English+Collocation+Verbs',
  },
  {
    id: 'r2',
    title: 'Cấu trúc dữ liệu & Giải thuật',
    meta: '48 thẻ • bởi minh_dev',
    type: 'folder',
    to: '/explore?q=Data+Structures',
  },
  {
    id: 'r3',
    title: 'English Collocations',
    meta: '10 thẻ • của bạn',
    type: 'set',
    to: '/explore?q=English+Collocations',
  },
  {
    id: 'r4',
    title: 'CHAPTER 1 - IT',
    meta: '92 thẻ • bởi nam_learner',
    type: 'set',
    to: '/explore?q=CHAPTER+1+IT',
  },
];

export const DKT_QUICK_QUESTIONS = [
  {
    id: 'q1',
    setTitle: 'English Collocations',
    prompt: 'Make a ____ about the new project plan before Friday.',
    options: ['decision', 'do', 'say', 'create'],
    correctIndex: 0,
  },
  {
    id: 'q2',
    setTitle: 'English Collocations',
    prompt: 'She ____ a lot of effort into preparing for the IELTS exam.',
    options: ['made', 'put', 'took', 'did'],
    correctIndex: 1,
  },
  {
    id: 'q3',
    setTitle: 'CHAPTER 1 - IT',
    prompt: 'Use of computer software and networks to process and manage information',
    options: ['IT Impact', 'IT Purpose', 'Information Technology', 'IT Scope'],
    correctIndex: 2,
  },
  {
    id: 'q4',
    setTitle: 'Từ vựng IELTS Education',
    prompt: 'Nghĩa tiếng Anh sát nhất của "bài tập thực hành":',
    options: ['lesson', 'qualification', 'exercise', 'drop-outs'],
    correctIndex: 2,
  },
  {
    id: 'q5',
    setTitle: 'English Collocations',
    prompt: 'We need to ____ attention to the algorithm complexity.',
    options: ['give', 'pay', 'make', 'keep'],
    correctIndex: 1,
  },
];

export const GAME_MODES_PROMO = [
  {
    id: 'match',
    tabLabel: 'Ghép thẻ',
    setTitle: 'English Collocations',
    description: 'Chạy đua với thời gian để ghép đúng thuật ngữ và định nghĩa',
    ctaLabel: 'Chơi Match',
    banner: gameMatchImg,
    accentColor: 'text-[#23B26D]',
    badgeBg: 'bg-[#E6F8EF] dark:bg-[#193A37]',
    to: '/explore?mode=match',
  },
  {
    id: 'blast',
    tabLabel: 'Blast',
    setTitle: 'English Collocation Verbs',
    description: 'Bắn phá thiên thạch mang đáp án đúng trước khi hết giờ',
    ctaLabel: 'Chơi Blast',
    banner: gameBlastImg,
    accentColor: 'text-[#4255FF]',
    badgeBg: 'bg-[#EDEFFF] dark:bg-[#2E3856]',
    to: '/explore?mode=blast',
  },
  {
    id: 'blocks',
    tabLabel: 'Khối hộp',
    setTitle: 'CHAPTER 1 - IT',
    description: 'Trả lời đúng câu hỏi để nhận khối gạch xếp đầy hàng ngang/dọc',
    ctaLabel: 'Chơi Blocks',
    banner: gameBlocksImg,
    accentColor: 'text-[#9333EA]',
    badgeBg: 'bg-[#F5E9FF] dark:bg-[#352654]',
    to: '/explore?mode=blocks',
  },
];

export const RECOMMENDED_BY_STUDY = [
  {
    id: 'rec-1',
    title: 'Project Management',
    cardsCount: 120,
    author: 'nstevenson19',
    badgeText: '16 người học hôm nay',
    to: '/explore?q=Project+Management',
  },
  {
    id: 'rec-2',
    title: 'Từ vựng IELTS chủ đề Education',
    cardsCount: 122,
    author: 'adeladoris',
    badgeText: null,
    to: '/explore?q=IELTS+Education',
  },
  {
    id: 'rec-3',
    title: 'Từ vựng theo chủ đề Công nghệ',
    cardsCount: 75,
    author: 'mi_zuri',
    badgeText: '26 người học hôm nay',
    to: '/explore?q=Technology',
  },
  {
    id: 'rec-4',
    title: 'Introduction to IT',
    cardsCount: 42,
    author: 'demiyah_no',
    badgeText: '12 người học hôm nay',
    to: '/explore?q=Introduction+to+IT',
  },
];

export const FEATURED_SETS = [
  {
    id: 'feat-1',
    title: '600 Từ vựng TOEIC Cốt lõi',
    cardsCount: 150,
    author: 'SmartFlash Official',
    badgeText: 'Nổi bật',
    to: '/explore?q=TOEIC',
  },
  {
    id: 'feat-2',
    title: 'Học hiệu quả với FSRS-4.5',
    cardsCount: 15,
    author: 'SmartFlash Official',
    badgeText: null,
    to: '/explore?q=FSRS',
  },
  {
    id: 'feat-3',
    title: 'Cấu trúc câu giao tiếp Tiếng Anh',
    cardsCount: 64,
    author: 'Verified Teacher',
    badgeText: '31 người học hôm nay',
    to: '/explore?q=English+Speaking',
  },
  {
    id: 'feat-4',
    title: 'SQL & PostgreSQL Cơ bản đến Nâng cao',
    cardsCount: 54,
    author: 'cs_academy',
    badgeText: null,
    to: '/explore?q=PostgreSQL',
  },
];
