import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Receipt,
  Scissors,
  Landmark,
  TrendingUp,
  Users,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export const Sidebar: React.FC = () => {
  const { isHouseholdMode } = useAuth();

  const links = [
    { to: '/', label: 'Panel General', icon: LayoutDashboard },
    { to: '/expenses', label: 'Gastos', icon: Receipt },
    { to: '/invoices', label: 'Facturas & División Inversa', icon: Scissors },
    { to: '/reconciliation', label: 'Conciliación Bancaria', icon: Landmark },
    { to: '/cashflow', label: 'Flujo de Caja', icon: TrendingUp },
    { to: '/household', label: isHouseholdMode ? 'Hogar & Roomies' : 'Crear / Unirse a Hogar', icon: Users },
  ];

  return (
    <aside className="w-64 bg-slate-900 text-slate-300 flex flex-col shrink-0 min-h-[calc(100vh-4rem)]">
      <div className="p-4 flex-1 space-y-1.5">
        <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider px-3 mb-2">
          Navegación
        </div>
        {links.map((link) => {
          const Icon = link.icon;
          return (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.to === '/'}
              className={({ isActive }) =>
                `flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-emerald-600 text-white shadow-xs'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800'
                }`
              }
            >
              <Icon className="w-5 h-5 shrink-0" />
              <span>{link.label}</span>
            </NavLink>
          );
        })}
      </div>

      <div className="p-4 border-t border-slate-800 text-xs text-slate-500">
        <p className="font-medium text-slate-400">Microservicio DIAN :8081</p>
        <p className="mt-0.5">Backend Hexagonal :8080</p>
      </div>
    </aside>
  );
};
