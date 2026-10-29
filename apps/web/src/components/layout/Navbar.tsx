import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { LogOut, Home, User, ShieldCheck } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, isHouseholdMode, logout } = useAuth();

  return (
    <header className="h-16 bg-white border-b border-slate-200 px-6 flex items-center justify-between sticky top-0 z-30 shadow-xs">
      <div className="flex items-center space-x-3">
        <div className="w-9 h-9 rounded-lg bg-emerald-600 flex items-center justify-center text-white font-bold text-lg shadow-sm">
          HE
        </div>
        <div>
          <span className="font-bold text-slate-800 text-lg tracking-tight">House Expenses</span>
          <span className="text-xs text-slate-400 block -mt-1 font-medium">Finanzas Personales & Hogar</span>
        </div>
      </div>

      <div className="flex items-center space-x-4">
        {/* Adaptive UX Mode Indicator */}
        <div
          className={`flex items-center space-x-1.5 px-3 py-1 rounded-full text-xs font-semibold ${
            isHouseholdMode
              ? 'bg-purple-100 text-purple-800 border border-purple-200'
              : 'bg-emerald-100 text-emerald-800 border border-emerald-200'
          }`}
        >
          {isHouseholdMode ? (
            <>
              <Home className="w-3.5 h-3.5" />
              <span>Modo Hogar Compartido (50/50)</span>
            </>
          ) : (
            <>
              <User className="w-3.5 h-3.5" />
              <span>Modo Solitario (100% Personal)</span>
            </>
          )}
        </div>

        {/* User details */}
        <div className="flex items-center space-x-2 pl-2 border-l border-slate-200">
          <div className="text-right">
            <div className="text-sm font-semibold text-slate-800 flex items-center justify-end space-x-1">
              <span>{user?.name || user?.email}</span>
              {user?.tax_id && (
                <span title={`Cédula/NIT: ${user.tax_id}`}>
                  <ShieldCheck className="w-3.5 h-3.5 text-blue-500" />
                </span>
              )}
            </div>
            <div className="text-xs text-slate-400">{user?.email}</div>
          </div>
          <button
            onClick={logout}
            className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
            title="Cerrar sesión"
          >
            <LogOut className="w-5 h-5" />
          </button>
        </div>
      </div>
    </header>
  );
};
