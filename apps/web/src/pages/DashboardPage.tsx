import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { expenseApi, householdApi, cashFlowApi } from '../api/client';
import type { Expense, HouseholdBalance, CashFlowSummary } from '../api/types';
import {
  Receipt,
  TrendingUp,
  Users,
  CreditCard,
  PlusCircle,
  FileSpreadsheet,
  QrCode,
  ArrowUpRight,
} from 'lucide-react';
import { Link } from 'react-router-dom';

export const DashboardPage: React.FC = () => {
  const { user, isHouseholdMode } = useAuth();
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [balance, setBalance] = useState<HouseholdBalance | null>(null);
  const [cashFlow, setCashFlow] = useState<CashFlowSummary | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [expList, cf] = await Promise.all([
          expenseApi.list({ limit: '5' } as any),
          cashFlowApi.getMonthlySummary().catch(() => null),
        ]);
        setExpenses(expList);
        setCashFlow(cf);

        if (isHouseholdMode) {
          const b = await householdApi.getBalance().catch(() => null);
          setBalance(b);
        }
      } catch (err) {
        console.error('Error fetching dashboard data:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [isHouseholdMode]);

  const formatCOP = (val: number) => {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0,
    }).format(val || 0);
  };

  const totalSpent = expenses.reduce((acc, curr) => acc + curr.amount, 0);

  return (
    <div className="space-y-6">
      {/* Header Greeting */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">
            ¡Hola, {user?.name?.split(' ')[0] || 'Usuario'}!
          </h1>
          <p className="text-sm text-slate-500 mt-0.5">
            {isHouseholdMode
              ? 'Panel activo en Modo Hogar Compartido. Los gastos del hogar se dividen al 50%.'
              : 'Panel activo en Modo Solitario. Registrando finanzas 100% personales.'}
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <Link
            to="/expenses"
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold shadow-xs transition-colors flex items-center space-x-2"
          >
            <PlusCircle className="w-4 h-4" />
            <span>Nuevo Gasto</span>
          </Link>
          <Link
            to="/invoices"
            className="px-4 py-2 bg-slate-800 hover:bg-slate-900 text-white rounded-lg text-sm font-semibold shadow-xs transition-colors flex items-center space-x-2"
          >
            <QrCode className="w-4 h-4" />
            <span>Escanear QR DIAN</span>
          </Link>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Gastos */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase">Gastos Recientes</span>
            <Receipt className="w-5 h-5 text-emerald-600" />
          </div>
          <div className="text-2xl font-bold text-slate-800">{formatCOP(totalSpent)}</div>
          <p className="text-xs text-slate-400 mt-1">Registrados en la plataforma</p>
        </div>

        {/* Flujo de caja neto */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase">Flujo de Caja Neto</span>
            <TrendingUp className="w-5 h-5 text-blue-600" />
          </div>
          <div className="text-2xl font-bold text-slate-800">
            {formatCOP(cashFlow?.net_disposable_cash || 0)}
          </div>
          <p className="text-xs text-slate-400 mt-1">Disponible este mes</p>
        </div>

        {/* Modo Adaptativo / Balance Hogar */}
        {isHouseholdMode && balance ? (
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs col-span-1 sm:col-span-2">
            <div className="flex items-center justify-between text-slate-500 mb-2">
              <span className="text-xs font-semibold uppercase">Balance 50/50 con Roomies</span>
              <Users className="w-5 h-5 text-purple-600" />
            </div>
            <div className="text-lg font-bold text-slate-800">
              {balance.settlement_summary || 'Cuentas al día'}
            </div>
            <div className="flex items-center space-x-4 mt-2 text-xs text-slate-500">
              <span>Pagado por ti: <strong className="text-slate-700">{formatCOP(balance.user_paid)}</strong></span>
              <span>Pagado por roomie: <strong className="text-slate-700">{formatCOP(balance.roomie_paid)}</strong></span>
            </div>
          </div>
        ) : (
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs col-span-1 sm:col-span-2">
            <div className="flex items-center justify-between text-slate-500 mb-2">
              <span className="text-xs font-semibold uppercase">Espacio de Hogar</span>
              <Users className="w-5 h-5 text-slate-400" />
            </div>
            <div className="text-base font-semibold text-slate-800">Modo 100% Personal</div>
            <p className="text-xs text-slate-500 mt-1">
              ¿Vives con roomies?{' '}
              <Link to="/household" className="text-emerald-600 font-semibold hover:underline">
                Crea un hogar o únete con un código
              </Link>{' '}
              para activar balances compartidos 50/50.
            </p>
          </div>
        )}
      </div>

      {/* Quick Action Shortcuts */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Link
          to="/invoices"
          className="p-5 bg-white border border-slate-200 rounded-xl hover:border-emerald-500 hover:shadow-md transition-all flex items-start space-x-4 group"
        >
          <div className="p-3 bg-emerald-50 text-emerald-600 rounded-lg group-hover:bg-emerald-600 group-hover:text-white transition-colors">
            <QrCode className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-800 group-hover:text-emerald-600 transition-colors flex items-center">
              División Inversa de Recibos <ArrowUpRight className="w-4 h-4 ml-1 opacity-0 group-hover:opacity-100" />
            </h3>
            <p className="text-xs text-slate-500 mt-1">
              Marca solo tus artículos personales en un tique de Éxito o D1 y el sistema asigna el resto al hogar.
            </p>
          </div>
        </Link>

        <Link
          to="/reconciliation"
          className="p-5 bg-white border border-slate-200 rounded-xl hover:border-blue-500 hover:shadow-md transition-all flex items-start space-x-4 group"
        >
          <div className="p-3 bg-blue-50 text-blue-600 rounded-lg group-hover:bg-blue-600 group-hover:text-white transition-colors">
            <FileSpreadsheet className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-800 group-hover:text-blue-600 transition-colors flex items-center">
              Conciliación Bancaria <ArrowUpRight className="w-4 h-4 ml-1 opacity-0 group-hover:opacity-100" />
            </h3>
            <p className="text-xs text-slate-500 mt-1">
              Sube extractos bancarios en CSV o Excel para detectar automáticamente transacciones duplicadas.
            </p>
          </div>
        </Link>

        <Link
          to="/cashflow"
          className="p-5 bg-white border border-slate-200 rounded-xl hover:border-purple-500 hover:shadow-md transition-all flex items-start space-x-4 group"
        >
          <div className="p-3 bg-purple-50 text-purple-600 rounded-lg group-hover:bg-purple-600 group-hover:text-white transition-colors">
            <CreditCard className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-800 group-hover:text-purple-600 transition-colors flex items-center">
              Finanzas & Flujo de Caja <ArrowUpRight className="w-4 h-4 ml-1 opacity-0 group-hover:opacity-100" />
            </h3>
            <p className="text-xs text-slate-500 mt-1">
              Registra sueldos, primas legales y monitorea tu saldo real después de deudas fijas.
            </p>
          </div>
        </Link>
      </div>

      {/* Recent Expenses Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
          <h2 className="text-base font-bold text-slate-800">Últimos Gastos Registrados</h2>
          <Link to="/expenses" className="text-xs text-emerald-600 font-semibold hover:underline">
            Ver todos los gastos
          </Link>
        </div>

        {loading ? (
          <div className="p-8 text-center text-slate-400 text-sm">Cargando transacciones...</div>
        ) : expenses.length === 0 ? (
          <div className="p-8 text-center text-slate-400 text-sm">
            No tienes gastos registrados aún. Pulsa en "Nuevo Gasto" para empezar.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase border-b border-slate-100">
                <tr>
                  <th className="px-6 py-3">Descripción</th>
                  <th className="px-6 py-3">Categoría</th>
                  <th className="px-6 py-3">Ámbito</th>
                  <th className="px-6 py-3">Fecha</th>
                  <th className="px-6 py-3 text-right">Monto</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {expenses.map((exp) => (
                  <tr key={exp.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-3.5 font-medium text-slate-800">{exp.description}</td>
                    <td className="px-6 py-3.5">
                      <span className="px-2 py-0.5 bg-slate-100 text-slate-700 rounded-md text-xs font-medium">
                        {exp.category}
                      </span>
                    </td>
                    <td className="px-6 py-3.5">
                      <span
                        className={`px-2 py-0.5 rounded-md text-xs font-semibold ${
                          exp.scope === 'HOUSEHOLD'
                            ? 'bg-purple-100 text-purple-700'
                            : 'bg-emerald-100 text-emerald-700'
                        }`}
                      >
                        {exp.scope === 'HOUSEHOLD' ? 'Hogar (50/50)' : 'Personal'}
                      </span>
                    </td>
                    <td className="px-6 py-3.5 text-slate-400 text-xs">{exp.date.split('T')[0]}</td>
                    <td className="px-6 py-3.5 text-right font-bold text-slate-800">
                      {formatCOP(exp.amount)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
