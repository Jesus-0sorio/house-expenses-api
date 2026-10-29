import React, { useEffect, useState } from 'react';
import { reconciliationApi } from '../api/client';
import type { BankTransaction } from '../api/types';
import { FileSpreadsheet, AlertTriangle, CheckCircle, XCircle, Upload, Check } from 'lucide-react';

export const ReconciliationPage: React.FC = () => {
  const [duplicates, setDuplicates] = useState<BankTransaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [bankName, setBankName] = useState('Bancolombia');
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const fetchDuplicates = async () => {
    try {
      setLoading(true);
      const data = await reconciliationApi.getDuplicates();
      setDuplicates(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDuplicates();
  }, []);

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploading(true);
    setSuccessMessage(null);
    try {
      const res = await reconciliationApi.uploadStatement(file, bankName);
      setSuccessMessage(`Extracto procesado con éxito. Se importaron ${res.transactions?.length || 0} transacciones.`);
      fetchDuplicates();
    } catch (err: any) {
      alert(err.response?.data?.error || 'Error al procesar el extracto bancario.');
    } finally {
      setUploading(false);
    }
  };

  const handleLink = async (transactionId: string, expenseId?: string) => {
    if (!expenseId) return;
    try {
      await reconciliationApi.linkDuplicate(transactionId, expenseId);
      setDuplicates((prev) => prev.filter((d) => d.id !== transactionId));
    } catch (err: any) {
      alert(err.response?.data?.error || 'Error al vincular la transacción.');
    }
  };

  const handleDismiss = async (transactionId: string) => {
    try {
      await reconciliationApi.dismissDuplicate(transactionId);
      setDuplicates((prev) => prev.filter((d) => d.id !== transactionId));
    } catch (err: any) {
      alert(err.response?.data?.error || 'Error al descartar la sospecha.');
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
        <h1 className="text-2xl font-bold text-slate-800">Conciliación Bancaria Anti-Duplicados</h1>
        <p className="text-sm text-slate-500 mt-0.5">
          Cruza tus extractos bancarios en CSV o Excel contra los gastos ya registrados para evitar duplicidades accidentales.
        </p>
      </div>

      {/* Upload Box */}
      <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center space-x-3">
            <div className="p-3 bg-blue-50 text-blue-600 rounded-lg">
              <FileSpreadsheet className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-800">Cargar Extracto Bancario</h2>
              <p className="text-xs text-slate-500">
                Formatos compatibles: CSV (delimitado por comas o punto y coma) o Libro Excel (.xlsx)
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            <select
              value={bankName}
              onChange={(e) => setBankName(e.target.value)}
              className="py-2 px-3 bg-slate-50 border border-slate-200 rounded-lg text-xs font-semibold text-slate-700"
            >
              <option value="Bancolombia">Bancolombia</option>
              <option value="Davivienda">Davivienda</option>
              <option value="Nu Colombia">Nu Colombia</option>
              <option value="Banco de Bogotá">Banco de Bogotá</option>
              <option value="Lulo Bank">Lulo Bank</option>
            </select>

            <label className="py-2 px-4 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold cursor-pointer shadow-xs transition-colors flex items-center space-x-1.5">
              <Upload className="w-4 h-4" />
              <span>{uploading ? 'Procesando...' : 'Seleccionar Archivo'}</span>
              <input
                type="file"
                accept=".csv,.xlsx"
                onChange={handleFileUpload}
                disabled={uploading}
                className="hidden"
              />
            </label>
          </div>
        </div>

        {successMessage && (
          <div className="p-3 bg-emerald-50 text-emerald-800 border border-emerald-200 text-xs rounded-lg flex items-center space-x-2">
            <Check className="w-4 h-4 text-emerald-600" />
            <span>{successMessage}</span>
          </div>
        )}
      </div>

      {/* Duplicates Review Section */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <AlertTriangle className="w-5 h-5 text-amber-500" />
            <h2 className="text-base font-bold text-slate-800">
              Transacciones Sospechosas de Duplicado (≥ 80% Coincidencia)
            </h2>
          </div>
          <span className="text-xs bg-amber-100 text-amber-800 font-bold px-2.5 py-1 rounded-full">
            {duplicates.length} pendientes
          </span>
        </div>

        {loading ? (
          <div className="p-12 text-center text-slate-400 text-sm">Buscando posibles duplicados...</div>
        ) : duplicates.length === 0 ? (
          <div className="p-12 text-center text-slate-400 text-sm">
            🎉 ¡Todo al día! No hay transacciones duplicadas pendientes de revisión.
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {duplicates.map((tx) => (
              <div
                key={tx.id}
                className="p-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4 hover:bg-slate-50 transition-colors"
              >
                <div>
                  <div className="flex items-center space-x-3">
                    <span className="text-sm font-bold text-slate-800">{tx.description}</span>
                    <span className="text-xs px-2 py-0.5 bg-amber-100 text-amber-800 font-bold rounded-md">
                      {tx.confidence_score ? `${tx.confidence_score}% Confianza` : 'Sospechoso'}
                    </span>
                  </div>
                  <div className="flex items-center space-x-4 mt-1 text-xs text-slate-400">
                    <span>Fecha extracto: {tx.transaction_date.split('T')[0]}</span>
                    <span>Monto: <strong className="text-slate-700">{formatCOP(tx.amount)}</strong></span>
                  </div>
                </div>

                <div className="flex items-center space-x-3 self-end md:self-auto">
                  <button
                    onClick={() => handleLink(tx.id, tx.matched_expense_id)}
                    className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-xs flex items-center space-x-1"
                    title="Confirmar que es el mismo gasto ya registrado"
                  >
                    <CheckCircle className="w-4 h-4" />
                    <span>Confirmar Vínculo</span>
                  </button>

                  <button
                    onClick={() => handleDismiss(tx.id)}
                    className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-semibold flex items-center space-x-1"
                    title="Marcar como gasto distinto y descartar sospecha"
                  >
                    <XCircle className="w-4 h-4" />
                    <span>Descartar (Gasto Distinto)</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
