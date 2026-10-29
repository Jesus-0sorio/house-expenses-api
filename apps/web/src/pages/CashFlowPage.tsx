import React, { useEffect, useState } from 'react';
import { cashFlowApi } from '../api/client';
import type { CashFlowSummary, Income } from '../api/types';
import { PlusCircle, Wallet, ArrowDownRight, ArrowUpRight, X } from 'lucide-react';

export const CashFlowPage: React.FC = () => {
  const [summary, setSummary] = useState<CashFlowSummary | null>(null);
  const [incomes, setIncomes] = useState<Income[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);

  // New income form state
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [type, setType] = useState<'SALARY_REGULAR' | 'PRIMA' | 'BONUS' | 'FREELANCE'>('SALARY_REGULAR');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [submitting, setSubmitting] = useState(false);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [sum, incList] = await Promise.all([
        cashFlowApi.getMonthlySummary().catch(() => null),
        cashFlowApi.listIncomes().catch(() => []),
      ]);
      setSummary(sum);
      setIncomes(incList);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCreateIncome = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await cashFlowApi.createIncome({
        description,
        amount: parseFloat(amount),
        type,
        date,
      });
      setShowModal(false);
      setDescription('');
      setAmount('');
      fetchData();
    } catch (err: any) {
      alert(err.response?.data?.error || 'Error al registrar el ingreso');
    } finally {
      setSubmitting(false);
    }
  };

  const formatCOP = (val: number) => {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0,
    }).format(val || 0);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Flujo de Caja & Finanzas Personales</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Monitorea tu liquidez real calculada: Net = Ingresos - (Gastos + Deudas Fijas + Cuota Hogar).
          </p>
        </div>

        <button
          onClick={() => setShowModal(true)}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold shadow-xs transition-colors flex items-center space-x-2 self-start sm:self-auto"
        >
          <PlusCircle className="w-4 h-4" />
          <span>Registrar Ingreso</span>
        </button>
      </div>

      {/* Cash Flow Formula Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase">Total Ingresos</span>
            <ArrowUpRight className="w-5 h-5 text-emerald-600" />
          </div>
          <div className="text-2xl font-bold text-emerald-600">
            {formatCOP(summary?.total_incomes || 0)}
          </div>
          <p className="text-xs text-slate-400 mt-1">Salarios, primas y extras</p>
        </div>

        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase">Gastos Personales</span>
            <ArrowDownRight className="w-5 h-5 text-rose-500" />
          </div>
          <div className="text-2xl font-bold text-slate-800">
            {formatCOP(summary?.personal_expenses || 0)}
          </div>
          <p className="text-xs text-slate-400 mt-1">Consumo individual</p>
        </div>

        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase">Cuota Hogar (50%)</span>
            <ArrowDownRight className="w-5 h-5 text-purple-500" />
          </div>
          <div className="text-2xl font-bold text-slate-800">
            {formatCOP(summary?.household_share || 0)}
          </div>
          <p className="text-xs text-slate-400 mt-1">Tu mitad de gastos de casa</p>
        </div>

        <div className="bg-slate-900 text-white p-5 rounded-xl shadow-md">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase">Flujo de Caja Neto</span>
            <Wallet className="w-5 h-5 text-emerald-400" />
          </div>
          <div className="text-2xl font-black text-emerald-400">
            {formatCOP(summary?.net_disposable_cash || 0)}
          </div>
          <p className="text-xs text-slate-400 mt-1">Capacidad de ahorro real</p>
        </div>
      </div>

      {/* Incomes List Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
          <h2 className="text-base font-bold text-slate-800">Ingresos Registrados</h2>
          <span className="text-xs text-slate-400">Salarios y fuentes de liquidez</span>
        </div>

        {loading ? (
          <div className="p-8 text-center text-slate-400 text-sm">Cargando ingresos...</div>
        ) : incomes.length === 0 ? (
          <div className="p-8 text-center text-slate-400 text-sm">
            No tienes ingresos registrados aún. Pulsa en "Registrar Ingreso" para agregar tu salario o primas.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase border-b border-slate-100">
                <tr>
                  <th className="px-6 py-3">Descripción</th>
                  <th className="px-6 py-3">Tipo</th>
                  <th className="px-6 py-3">Fecha</th>
                  <th className="px-6 py-3 text-right">Monto</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {incomes.map((inc) => (
                  <tr key={inc.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-3.5 font-medium text-slate-800">{inc.description}</td>
                    <td className="px-6 py-3.5">
                      <span className="px-2 py-0.5 bg-emerald-50 text-emerald-700 rounded-md text-xs font-semibold">
                        {inc.type === 'SALARY_REGULAR'
                          ? 'Salario Quincenal/Mensual'
                          : inc.type === 'PRIMA'
                          ? 'Prima Legal Colombiana'
                          : inc.type === 'BONUS'
                          ? 'Bono'
                          : 'Freelance'}
                      </span>
                    </td>
                    <td className="px-6 py-3.5 text-slate-400 text-xs">{inc.date.split('T')[0]}</td>
                    <td className="px-6 py-3.5 text-right font-bold text-emerald-600">
                      +{formatCOP(inc.amount)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal: Registrar Ingreso */}
      {showModal && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-100">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100">
              <h2 className="text-lg font-bold text-slate-800">Registrar Ingreso</h2>
              <button
                onClick={() => setShowModal(false)}
                className="text-slate-400 hover:text-slate-600 p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateIncome} className="mt-4 space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                  Descripción
                </label>
                <input
                  type="text"
                  required
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Ej. Quincena de Septiembre o Prima de Servicios"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                  Monto (COP)
                </label>
                <input
                  type="number"
                  step="1"
                  min="1"
                  required
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="Ej. 3500000"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                    Tipo de Ingreso
                  </label>
                  <select
                    value={type}
                    onChange={(e: any) => setType(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  >
                    <option value="SALARY_REGULAR">Salario Fijo</option>
                    <option value="PRIMA">Prima Legal</option>
                    <option value="BONUS">Bono / Gratificación</option>
                    <option value="FREELANCE">Freelance / Extras</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                    Fecha
                  </label>
                  <input
                    type="date"
                    required
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="pt-2 flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 border border-slate-200 text-slate-600 rounded-lg text-sm font-medium hover:bg-slate-50"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold shadow-xs disabled:opacity-50"
                >
                  {submitting ? 'Guardando...' : 'Guardar Ingreso'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
