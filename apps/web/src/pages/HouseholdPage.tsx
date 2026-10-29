import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { householdApi } from '../api/client';
import type { HouseholdBalance } from '../api/types';
import { Users, Copy, Check, PlusCircle, ArrowRight, ShieldCheck } from 'lucide-react';

export const HouseholdPage: React.FC = () => {
  const { user, isHouseholdMode, updateUser } = useAuth();
  const [balance, setBalance] = useState<HouseholdBalance | null>(null);
  const [householdName, setHouseholdName] = useState('');
  const [inviteCodeInput, setInviteCodeInput] = useState('');
  const [copied, setCopied] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchBalance = async () => {
    if (isHouseholdMode) {
      try {
        const b = await householdApi.getBalance();
        setBalance(b);
      } catch (err) {
        console.error(err);
      }
    }
  };

  useEffect(() => {
    fetchBalance();
  }, [isHouseholdMode]);

  const handleCreateHousehold = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const h = await householdApi.create(householdName);
      if (user) {
        updateUser({ ...user, active_household_id: h.id });
      }
      setHouseholdName('');
      fetchBalance();
    } catch (err: any) {
      setError(err.response?.data?.error || 'Error al crear el hogar');
    } finally {
      setLoading(false);
    }
  };

  const handleJoinHousehold = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const h = await householdApi.join(inviteCodeInput.trim().toUpperCase());
      if (user) {
        updateUser({ ...user, active_household_id: h.id });
      }
      setInviteCodeInput('');
      fetchBalance();
    } catch (err: any) {
      setError(err.response?.data?.error || 'Código de invitación inválido');
    } finally {
      setLoading(false);
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
      <div>
        <h1 className="text-2xl font-bold text-slate-800">Espacio de Hogar & Roomies</h1>
        <p className="text-sm text-slate-500 mt-0.5">
          Gestiona los gastos compartidos de tu vivienda con liquidación automática 50/50.
        </p>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 text-red-700 text-sm rounded-xl">
          {error}
        </div>
      )}

      {isHouseholdMode ? (
        <div className="space-y-6">
          {/* Active Household Summary */}
          <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-4 border-b border-slate-100 gap-2">
              <div>
                <span className="text-xs font-semibold text-purple-600 uppercase tracking-wider">
                  Hogar Activo
                </span>
                <h2 className="text-xl font-bold text-slate-800">Hogar Compartido</h2>
                <div className="text-xs text-slate-400 flex items-center space-x-2">
                  <span>ID: <span className="font-mono text-slate-600">{user?.active_household_id}</span></span>
                  <button
                    onClick={() => {
                      if (user?.active_household_id) {
                        navigator.clipboard.writeText(user.active_household_id);
                        setCopied(true);
                        setTimeout(() => setCopied(false), 2000);
                      }
                    }}
                    className="p-1 hover:bg-slate-100 rounded text-slate-500 hover:text-slate-800"
                    title="Copiar ID"
                  >
                    {copied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                  </button>
                </div>
              </div>

              <div className="flex items-center space-x-2 bg-purple-50 border border-purple-200 text-purple-800 px-3 py-1.5 rounded-lg text-xs font-semibold">
                <ShieldCheck className="w-4 h-4" />
                <span>División Equitativa 50/50 Activa</span>
              </div>
            </div>

            {/* Balance 50/50 Card */}
            {balance && (
              <div className="bg-slate-900 text-white p-6 rounded-xl space-y-4 shadow-md">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold uppercase text-slate-400">
                    Estado de Cuentas Entre Roomies
                  </span>
                  <Users className="w-5 h-5 text-purple-400" />
                </div>

                <div className="text-2xl font-bold text-white">
                  {balance.settlement_summary}
                </div>

                <div className="grid grid-cols-2 gap-4 pt-4 border-t border-slate-800 text-sm">
                  <div>
                    <span className="text-xs text-slate-400 block">Total pagado por ti:</span>
                    <span className="text-lg font-bold text-emerald-400">{formatCOP(balance.user_paid)}</span>
                  </div>
                  <div>
                    <span className="text-xs text-slate-400 block">Total pagado por roomie:</span>
                    <span className="text-lg font-bold text-purple-400">{formatCOP(balance.roomie_paid)}</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      ) : (
        /* Solitary Mode: Create or Join Household */
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Create Household */}
          <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs space-y-4 flex flex-col justify-between">
            <div>
              <div className="flex items-center space-x-2 text-slate-800 font-bold text-base">
                <PlusCircle className="w-5 h-5 text-emerald-600" />
                <span>Crear un Nuevo Hogar</span>
              </div>
              <p className="text-xs text-slate-500 mt-1">
                Crea un espacio para tu apartamento o casa y genera un código de invitación para compartir con tus roomies.
              </p>
            </div>

            <form onSubmit={handleCreateHousehold} className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                  Nombre del Hogar
                </label>
                <input
                  type="text"
                  required
                  value={householdName}
                  onChange={(e) => setHouseholdName(e.target.value)}
                  placeholder="Ej. Apto 402 Chapinero"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 px-4 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded-lg text-sm transition-colors disabled:opacity-50 flex items-center justify-center space-x-2"
              >
                <span>{loading ? 'Creando...' : 'Crear Espacio de Hogar'}</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </form>
          </div>

          {/* Join Household with Code */}
          <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs space-y-4 flex flex-col justify-between">
            <div>
              <div className="flex items-center space-x-2 text-slate-800 font-bold text-base">
                <Users className="w-5 h-5 text-purple-600" />
                <span>Unirse con Código de Invitación</span>
              </div>
              <p className="text-xs text-slate-500 mt-1">
                Si tu roomie ya creó el hogar, ingresa el código de 6 caracteres que te compartió.
              </p>
            </div>

            <form onSubmit={handleJoinHousehold} className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                  Código de Invitación
                </label>
                <input
                  type="text"
                  required
                  maxLength={10}
                  value={inviteCodeInput}
                  onChange={(e) => setInviteCodeInput(e.target.value.toUpperCase())}
                  placeholder="Ej. H-987654"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm font-mono uppercase tracking-wider text-slate-800 focus:ring-2 focus:ring-purple-500 focus:outline-none"
                />
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 px-4 bg-purple-600 hover:bg-purple-700 text-white font-semibold rounded-lg text-sm transition-colors disabled:opacity-50 flex items-center justify-center space-x-2"
              >
                <span>{loading ? 'Validando...' : 'Unirse al Hogar'}</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
