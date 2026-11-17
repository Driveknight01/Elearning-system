/**
 * Built-in crisp vector SVG illustration library so the inline image picker
 * works instantaneously offline/online during testing without broken external URLs.
 */

function toSvgDataUrl(svgBody, bg = '#F6F7FB') {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 220" width="320" height="220"><rect width="320" height="220" rx="16" fill="${bg}"/>${svgBody}</svg>`;
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
}

export const BUILTIN_ILLUSTRATIONS = [
  {
    id: 'builtin-computer-1',
    title: 'Máy tính để bàn',
    keywords: ['computer', 'máy tính', 'pc', 'desktop', 'công nghệ', 'it', 'screen', 'màn hình'],
    imageUrl: toSvgDataUrl(
      '<rect x="65" y="38" width="190" height="114" rx="10" fill="#FFFFFF" stroke="#282E3E" stroke-width="5"/><rect x="80" y="52" width="160" height="84" rx="4" fill="#EDEFFF"/><path d="M140 152h40l8 22h-56z" fill="#D9DDE8" stroke="#282E3E" stroke-width="4"/><rect x="108" y="174" width="104" height="10" rx="5" fill="#282E3E"/><path d="M118 94l22-18 18 14 26-22 20 16" fill="none" stroke="#4255FF" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>',
      '#F8FAFC'
    ),
    attribution: 'Thư viện SmartFlash',
  },
  {
    id: 'builtin-laptop-1',
    title: 'Laptop học tập',
    keywords: ['computer', 'máy tính', 'laptop', 'máy tính xách tay', 'code', 'học', 'study'],
    imageUrl: toSvgDataUrl(
      '<rect x="76" y="42" width="168" height="106" rx="10" fill="#1E293B" stroke="#0F172A" stroke-width="4"/><rect x="88" y="54" width="144" height="82" rx="4" fill="#38BDF8"/><path d="M52 152h216l14 22H38z" fill="#CBD5E1" stroke="#475569" stroke-width="3"/><rect x="134" y="159" width="52" height="7" rx="3.5" fill="#94A3B8"/><circle cx="160" cy="95" r="20" fill="#FFFFFF" opacity="0.9"/><path d="M153 95l6 6 11-12" fill="none" stroke="#4255FF" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"/>',
      '#EFF6FF'
    ),
    attribution: 'Thư viện SmartFlash',
  },
  {
    id: 'builtin-code-computer',
    title: 'Lập trình & Phần mềm',
    keywords: ['computer', 'máy tính', 'code', 'software', 'algorithm', 'thuật toán', 'developer', 'tech'],
    imageUrl: toSvgDataUrl(
      '<rect x="54" y="36" width="212" height="132" rx="14" fill="#202140"/><circle cx="76" cy="54" r="5" fill="#FF725B"/><circle cx="92" cy="54" r="5" fill="#FFCD1F"/><circle cx="108" cy="54" r="5" fill="#23B26D"/><path d="M120 92l-24 22 24 22M200 92l24 22-24 22M170 84l-20 60" fill="none" stroke="#8E9BFF" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/><rect x="120" y="176" width="80" height="10" rx="5" fill="#4255FF"/>',
      '#EEF2FF'
    ),
    attribution: 'Thư viện SmartFlash',
  },
  {
    id: 'builtin-tablet-workspace',
    title: 'Bàn làm việc máy tính',
    keywords: ['computer', 'máy tính', 'workspace', 'bàn làm việc', 'office', 'desk', 'design'],
    imageUrl: toSvgDataUrl(
      '<rect x="44" y="156" width="232" height="14" rx="7" fill="#94A3B8"/><rect x="86" y="46" width="148" height="96" rx="10" fill="#4255FF"/><rect x="96" y="56" width="128" height="76" rx="4" fill="#FFFFFF"/><rect x="108" y="68" width="56" height="12" rx="4" fill="#EDEFFF"/><rect x="108" y="88" width="104" height="8" rx="4" fill="#CBD5E1"/><rect x="108" y="104" width="82" height="8" rx="4" fill="#CBD5E1"/><rect x="148" y="142" width="24" height="14" fill="#64748B"/>',
      '#F1F5F9'
    ),
    attribution: 'Thư viện SmartFlash',
  },
  {
    id: 'builtin-book',
    title: 'Sách & Từ vựng',
    keywords: ['book', 'sách', 'read', 'đọc', 'vocabulary', 'từ vựng', 'library', 'thư viện', 'study', 'học'],
    imageUrl: toSvgDataUrl(
      '<path d="M160 66c-26-14-58-14-86-4v102c28-10 60-10 86 4 26-14 58-14 86-4V62c-28-10-60-10-86 4z" fill="#FFFFFF" stroke="#4255FF" stroke-width="5" stroke-linejoin="round"/><line x1="160" y1="66" x2="160" y2="168" stroke="#4255FF" stroke-width="4"/><line x1="94" y1="90" x2="140" y2="90" stroke="#94A3B8" stroke-width="4" stroke-linecap="round"/><line x1="94" y1="110" x2="140" y2="110" stroke="#94A3B8" stroke-width="4" stroke-linecap="round"/><line x1="180" y1="90" x2="226" y2="90" stroke="#94A3B8" stroke-width="4" stroke-linecap="round"/><line x1="180" y1="110" x2="226" y2="110" stroke="#94A3B8" stroke-width="4" stroke-linecap="round"/>',
      '#EEF2FF'
    ),
    attribution: 'Thư viện SmartFlash',
  },
  {
    id: 'builtin-brain',
    title: 'Trí nhớ & Tư duy',
    keywords: ['brain', 'não', 'memory', 'trí nhớ', 'ai', 'smart', 'think', 'tư duy', 'psychology'],
    imageUrl: toSvgDataUrl(
      '<circle cx="136" cy="108" r="46" fill="#FDE68A" stroke="#D97706" stroke-width="4"/><circle cx="184" cy="108" r="46" fill="#FDE68A" stroke="#D97706" stroke-width="4"/><circle cx="160" cy="82" r="36" fill="#FEF3C7"/><path d="M160 62v94M130 96c12 0 18 10 18 20M190 96c-12 0-18 10-18 20" fill="none" stroke="#D97706" stroke-width="4" stroke-linecap="round"/><circle cx="242" cy="54" r="10" fill="#4255FF"/><path d="M242 36v36M224 54h36" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/>',
      '#FFFBEB'
    ),
    attribution: 'Thư viện SmartFlash',
  },
  {
    id: 'builtin-science',
    title: 'Khoa học & Thí nghiệm',
    keywords: ['science', 'khoa học', 'chemistry', 'hóa học', 'biology', 'sinh học', 'experiment', 'thí nghiệm'],
    imageUrl: toSvgDataUrl(
      '<path d="M138 44h44M146 44v44l-38 72c-4 8 2 18 11 18h82c9 0 15-10 11-18l-38-72V44" fill="#FFFFFF" stroke="#0EA5E9" stroke-width="5" stroke-linejoin="round"/><path d="M118 142h84l10 18c2 4-1 10-6 10h-92c-5 0-8-6-6-10z" fill="#38BDF8"/><circle cx="148" cy="154" r="5" fill="#FFFFFF"/><circle cx="172" cy="148" r="4" fill="#FFFFFF"/><circle cx="160" cy="126" r="5" fill="#0EA5E9"/>',
      '#F0F9FF'
    ),
    attribution: 'Thư viện SmartFlash',
  },
  {
    id: 'builtin-globe',
    title: 'Thế giới & Ngôn ngữ',
    keywords: ['globe', 'earth', 'trái đất', 'world', 'thế giới', 'language', 'ngôn ngữ', 'english', 'geography', 'địa lý'],
    imageUrl: toSvgDataUrl(
      '<circle cx="160" cy="110" r="64" fill="#38BDF8" stroke="#0284C7" stroke-width="5"/><path d="M126 76c14 4 22 18 12 30-10 12 4 24 14 34s4 28-10 30M184 68c-12 10-8 26 4 32 14 6 20 22 8 36" fill="none" stroke="#23B26D" stroke-width="14" stroke-linecap="round"/><ellipse cx="160" cy="110" rx="64" ry="22" fill="none" stroke="#FFFFFF" stroke-width="3" opacity="0.65"/>',
      '#ECFEFF'
    ),
    attribution: 'Thư viện SmartFlash',
  },
  {
    id: 'builtin-apple',
    title: 'Quả táo / Hoa quả',
    keywords: ['apple', 'táo', 'quả táo', 'fruit', 'food', 'đồ ăn', 'hoa quả', 'healthy'],
    imageUrl: toSvgDataUrl(
      '<path d="M160 72c-26-14-56 4-56 40 0 38 26 68 56 64 30 4 56-26 56-64 0-36-30-54-56-40z" fill="#EF4444" stroke="#B91C1C" stroke-width="4"/><path d="M160 70c2-16 10-26 22-30" fill="none" stroke="#78350F" stroke-width="5" stroke-linecap="round"/><path d="M163 54c18-10 34-4 34 8-14 8-28 4-34-8z" fill="#22C55E"/>',
      '#FEF2F2'
    ),
    attribution: 'Thư viện SmartFlash',
  },
  {
    id: 'builtin-rocket',
    title: 'Tên lửa & Mục tiêu',
    keywords: ['rocket', 'tên lửa', 'space', 'goal', 'mục tiêu', 'progress', 'tiến bộ', 'fast'],
    imageUrl: toSvgDataUrl(
      '<path d="M160 36c26 24 34 62 24 102h-48c-10-40-2-78 24-102z" fill="#FFFFFF" stroke="#4255FF" stroke-width="5"/><circle cx="160" cy="92" r="14" fill="#38BDF8" stroke="#4255FF" stroke-width="4"/><path d="M136 116l-24 22 20 8M184 116l24 22-20 8" fill="#FF725B" stroke="#282E3E" stroke-width="3" stroke-linejoin="round"/><path d="M148 142l12 34 12-34z" fill="#FFCD1F"/>',
      '#EEF2FF'
    ),
    attribution: 'Thư viện SmartFlash',
  },
  {
    id: 'builtin-math',
    title: 'Toán học & Hình học',
    keywords: ['math', 'toán', 'toán học', 'calculate', 'tính toán', 'geometry', 'number', 'số'],
    imageUrl: toSvgDataUrl(
      '<rect x="66" y="54" width="76" height="76" rx="12" fill="#4255FF"/><path d="M88 92h32M104 76v32" stroke="#FFFFFF" stroke-width="6" stroke-linecap="round"/><rect x="178" y="54" width="76" height="76" rx="12" fill="#23B26D"/><path d="M198 92h36" stroke="#FFFFFF" stroke-width="6" stroke-linecap="round"/><circle cx="104" cy="166" r="24" fill="#FFCD1F"/><polygon points="216,142 244,188 188,188" fill="#FF725B"/>',
      '#F8FAFC'
    ),
    attribution: 'Thư viện SmartFlash',
  },
  {
    id: 'builtin-trophy',
    title: 'Thành tựu & Giải thưởng',
    keywords: ['trophy', 'cúp', 'award', 'giải thưởng', 'win', 'success', 'thành công', 'achievement'],
    imageUrl: toSvgDataUrl(
      '<path d="M112 52h96v46c0 28-20 48-48 48s-48-20-48-48V52z" fill="#FFCD1F" stroke="#D97706" stroke-width="4"/><path d="M112 66H84c0 24 12 36 28 36M208 66h28c0 24-12 36-28 36" fill="none" stroke="#D97706" stroke-width="5"/><rect x="148" y="146" width="24" height="24" fill="#D97706"/><rect x="122" y="170" width="76" height="14" rx="6" fill="#282E3E"/>',
      '#FFFBEB'
    ),
    attribution: 'Thư viện SmartFlash',
  },
  {
    id: 'builtin-clock',
    title: 'Thời gian & Lịch trình',
    keywords: ['time', 'thời gian', 'clock', 'đồng hồ', 'schedule', 'lịch trình', 'deadline'],
    imageUrl: toSvgDataUrl(
      '<circle cx="160" cy="110" r="64" fill="#FFFFFF" stroke="#4255FF" stroke-width="6"/><path d="M160 72v38l26 16" fill="none" stroke="#282E3E" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/><circle cx="160" cy="110" r="6" fill="#4255FF"/>',
      '#EEF2FF'
    ),
    attribution: 'Thư viện SmartFlash',
  },
  {
    id: 'builtin-music',
    title: 'Âm nhạc & Phát âm',
    keywords: ['music', 'âm nhạc', 'audio', 'âm thanh', 'sound', 'pronunciation', 'phát âm', 'listen'],
    imageUrl: toSvgDataUrl(
      '<circle cx="122" cy="152" r="20" fill="#9333EA"/><circle cx="198" cy="138" r="20" fill="#4255FF"/><path d="M138 152V66l76-14v86" fill="none" stroke="#282E3E" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"/>',
      '#FAF5FF'
    ),
    attribution: 'Thư viện SmartFlash',
  },
  {
    id: 'builtin-nature',
    title: 'Thiên nhiên & Quang hợp',
    keywords: ['tree', 'cây', 'nature', 'thiên nhiên', 'environment', 'môi trường', 'photosynthesis', 'quang hợp', 'green'],
    imageUrl: toSvgDataUrl(
      '<circle cx="242" cy="54" r="20" fill="#FFCD1F"/><rect x="150" y="128" width="20" height="54" rx="4" fill="#78350F"/><circle cx="160" cy="96" r="46" fill="#23B26D"/><circle cx="126" cy="112" r="30" fill="#16A34A"/><circle cx="194" cy="112" r="30" fill="#16A34A"/>',
      '#F0FDF4'
    ),
    attribution: 'Thư viện SmartFlash',
  },
  {
    id: 'builtin-chat',
    title: 'Giao tiếp & Hội thoại',
    keywords: ['chat', 'communicate', 'giao tiếp', 'conversation', 'hội thoại', 'speak', 'nói', 'question'],
    imageUrl: toSvgDataUrl(
      '<rect x="60" y="48" width="144" height="86" rx="18" fill="#4255FF"/><path d="M90 134l-14 26 32-26z" fill="#4255FF"/><rect x="128" y="96" width="132" height="72" rx="16" fill="#FFFFFF" stroke="#282E3E" stroke-width="4"/><circle cx="170" cy="132" r="5" fill="#4255FF"/><circle cx="194" cy="132" r="5" fill="#4255FF"/><circle cx="218" cy="132" r="5" fill="#4255FF"/>',
      '#EEF2FF'
    ),
    attribution: 'Thư viện SmartFlash',
  },
];

export function searchBuiltinIllustrations(query) {
  const q = (query || '').trim().toLowerCase();
  if (!q) return BUILTIN_ILLUSTRATIONS;

  const tokens = q.split(/\s+/).filter(Boolean);
  const scored = BUILTIN_ILLUSTRATIONS.map((item) => {
    let score = 0;
    const titleLower = item.title.toLowerCase();
    for (const token of tokens) {
      if (titleLower.includes(token)) score += 3;
      if (item.keywords.some((k) => k.includes(token) || token.includes(k))) {
        score += 4;
      }
    }
    return { item, score };
  });

  const matched = scored
    .filter((s) => s.score > 0)
    .sort((a, b) => b.score - a.score)
    .map((s) => s.item);

  // If specific match found, put matches first followed by other gallery items so user always has a 4-item carousel to test
  if (matched.length > 0) {
    const matchedIds = new Set(matched.map((m) => m.id));
    return [...matched, ...BUILTIN_ILLUSTRATIONS.filter((i) => !matchedIds.has(i.id))];
  }
  return BUILTIN_ILLUSTRATIONS;
}
