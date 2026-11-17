import React from 'react';
import { Link } from 'react-router-dom';
import { X } from 'lucide-react';
import authBannerImg from '../../assets/auth-banner.jpg';
import smartflashLogo from '../../assets/smartflash-logo.png';

/**
 * Split-screen authentication shell.
 * Locked to 100vh on desktop so the left banner has identical dimensions on both Login and Register.
 */
export default function AuthSplitLayout({ mode = 'login', children }) {
  const isLogin = mode === 'login';

  return (
    <div className="min-h-screen lg:h-screen lg:overflow-hidden flex flex-col lg:flex-row bg-white font-sans">
      {/* Left Banner Image Panel (Fixed 50% x 100vh on desktop) */}
      <div className="hidden lg:block lg:w-1/2 lg:h-screen relative overflow-hidden bg-[#1F1D1B] select-none shrink-0">
        <img
          src={authBannerImg}
          alt="SmartFlash Study Banner"
          className="w-full h-full object-cover object-center"
        />
        <Link
          to="/"
          aria-label="ELearner Trang chủ"
          className="absolute top-6 left-6 z-10 inline-flex items-center gap-2.5 bg-white/95 backdrop-blur-sm px-3.5 py-2 rounded-xl shadow-lg hover:scale-105 transition-transform"
        >
          <img
            src={smartflashLogo}
            alt="ELearner"
            className="w-7 h-7 object-contain"
          />
          <span className="text-[19px] leading-none font-semibold tracking-[-0.01em] text-[#282E3E]">
            ELearner
          </span>
        </Link>
      </div>

      {/* Right Form Panel (Scrollable independently on desktop) */}
      <div className="w-full lg:w-1/2 lg:h-screen lg:overflow-y-auto relative flex flex-col bg-white">
        {/* Top-right Close Button */}
        <div className="flex items-center justify-between lg:justify-end px-5 pt-5 sm:px-7 sm:pt-6 shrink-0">
          <Link to="/" className="lg:hidden inline-flex items-center gap-2.5">
            <img
              src={smartflashLogo}
              alt="ELearner"
              className="w-7 h-7 object-contain"
            />
            <span className="text-[20px] leading-none font-semibold tracking-[-0.01em] text-[#282E3E]">
              ELearner
            </span>
          </Link>

          <Link
            to="/"
            aria-label="Đóng và quay về trang chủ"
            className="w-11 h-11 rounded-full bg-[#F3F4F8] hover:bg-[#E2E5EE] flex items-center justify-center text-[#586380] hover:text-[#282E3E] transition-colors"
          >
            <X className="w-5 h-5 stroke-[2.2]" />
          </Link>
        </div>

        <div className="flex-1 flex flex-col justify-center px-6 py-8 sm:px-10">
          <div className="w-full max-w-[500px] mx-auto">
            {/* Sign up / Log in Tabs with Wavy Active Underline */}
            <div className="flex items-center gap-8 mb-8">
              <Link
                to="/register"
                className={`relative pb-3 text-[24px] font-bold tracking-tight transition ${
                  !isLogin
                    ? 'text-[#282E3E]'
                    : 'text-[#586380] hover:text-[#282E3E]'
                }`}
              >
                Đăng ký
                {!isLogin && <WavyUnderline />}
              </Link>

              <Link
                to="/login"
                className={`relative pb-3 text-[24px] font-bold tracking-tight transition ${
                  isLogin
                    ? 'text-[#282E3E]'
                    : 'text-[#586380] hover:text-[#282E3E]'
                }`}
              >
                Đăng nhập
                {isLogin && <WavyUnderline />}
              </Link>
            </div>

            {children}
          </div>
        </div>
      </div>
    </div>
  );
}

function WavyUnderline() {
  return (
    <svg
      className="absolute left-0 bottom-0 w-full h-2.5 text-[#43D290]"
      viewBox="0 0 100 10"
      preserveAspectRatio="none"
      fill="none"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="auth-wave-green" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#6FE8AD" />
          <stop offset="100%" stopColor="#35C682" />
        </linearGradient>
      </defs>
      <path
        d="M1 5.5C5.5 1.8 10.5 1.8 15 5.5C19.5 9.2 24.5 9.2 29 5.5C33.5 1.8 38.5 1.8 43 5.5C47.5 9.2 52.5 9.2 57 5.5C61.5 1.8 66.5 1.8 71 5.5C75.5 9.2 80.5 9.2 85 5.5C89.5 1.8 94.5 1.8 99 5.5"
        stroke="url(#auth-wave-green)"
        strokeWidth="2.8"
        strokeLinecap="round"
      />
    </svg>
  );
}
