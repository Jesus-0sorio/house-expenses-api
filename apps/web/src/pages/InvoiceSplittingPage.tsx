import React, { useState } from 'react';
import { invoiceApi } from '../api/client';
import type { Invoice } from '../api/types';
import { QrCode, UploadCloud, CheckSquare, Square, ShoppingBag, ArrowRight, CheckCircle2 } from 'lucide-react';

export const InvoiceSplittingPage: React.FC = () => {
  const [qrInput, setQrInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [selectedPersonalIds, setSelectedPersonalIds] = useState<Set<string>>(new Set());
  const [savedSuccess, setSavedSuccess] = useState(false);

  const handleLookupQR = async () => {
    if (!qrInput.trim()) return;
    setError(null);
    setLoading(true);
    setSavedSuccess(false);

    try {
      const inv = await invoiceApi.lookupDianQR({ qr_text: qrInput.trim() });
      setInvoice(inv);
      setSelectedPersonalIds(new Set());
    } catch (err: any) {
      setError(err.response?.data?.error || 'No se pudo procesar la factura de la DIAN.');
    } finally {
      setLoading(false);
    }
  };

  const handleZipUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setError(null);
    setLoading(true);
    setSavedSuccess(false);

    try {
      const inv = await invoiceApi.uploadZip(file);
      setInvoice(inv);
      setSelectedPersonalIds(new Set());
    } catch (err: any) {
      setError(err.response?.data?.error || 'Error al procesar el archivo .zip de la factura.');
    } finally {
      setLoading(false);
    }
  };

  const toggleItem = (id: string) => {
    const updated = new Set(selectedPersonalIds);
    if (updated.has(id)) {
      updated.delete(id);
    } else {
      updated.add(id);
    }
    setSelectedPersonalIds(updated);
  };

  // Calculate totals in real-time
  const personalTotal = (invoice?.items || []).reduce((acc, item) => {
    return selectedPersonalIds.has(item.id) ? acc + item.total_price : acc;
  }, 0);

  const householdTotal = (invoice?.items || []).reduce((acc, item) => {
    return !selectedPersonalIds.has(item.id) ? acc + item.total_price : acc;
  }, 0);

  const handleConfirmSplit = async () => {
    if (!invoice) return;
    setLoading(true);
    try {
      await invoiceApi.splitItems(
        invoice.id,
        Array.from(selectedPersonalIds),
        'PERSONAL'
      );
      setSavedSuccess(true);
    } catch (err: any) {
      setError(err.response?.data?.error || 'Error al guardar la división de gastos.');
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
        <h1 className="text-2xl font-bold text-slate-800">Facturas Electrónicas & División Inversa</h1>
        <p className="text-sm text-slate-500 mt-0.5">
          Procesa compras de supermercado (D1, Éxito, Carulla). Marca solo tus compras personales y el sistema asignará el resto automáticamente al hogar.
        </p>
      </div>

      {/* Input Options (QR Text or ZIP file) */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* QR Text Lookup */}
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs space-y-4">
          <div className="flex items-center space-x-2 text-slate-800 font-bold">
            <QrCode className="w-5 h-5 text-emerald-600" />
            <span>Pegar Texto o URL del QR DIAN</span>
          </div>
          <p className="text-xs text-slate-500">
            Pega el texto escaneado del tique físico (NumFac, NitFac, ValTolFac) o el CUFE.
          </p>

          <textarea
            rows={3}
            value={qrInput}
            onChange={(e) => setQrInput(e.target.value)}
            placeholder="Pegar aquí: NumFac: ... NitFac: ... ValTolFac: ... o https://catalogo-vpfe.dian.gov.co/..."
            className="w-full p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs font-mono text-slate-800 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
          />

          <button
            onClick={handleLookupQR}
            disabled={loading || !qrInput.trim()}
            className="w-full py-2.5 px-4 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded-lg text-sm transition-colors disabled:opacity-50 flex items-center justify-center space-x-2"
          >
            <span>{loading ? 'Consultando...' : 'Consultar Factura'}</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>

        {/* ZIP Upload */}
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs space-y-4 flex flex-col justify-between">
          <div>
            <div className="flex items-center space-x-2 text-slate-800 font-bold">
              <UploadCloud className="w-5 h-5 text-blue-600" />
              <span>Subir Archivo .ZIP de Factura Electrónica</span>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              Sube el archivo adjunto que recibiste por correo electrónico de la compra (contiene el XML UBL 2.1 con todos los productos detallados).
            </p>
          </div>

          <div className="border-2 border-dashed border-slate-200 rounded-xl p-6 text-center hover:border-blue-500 transition-colors cursor-pointer relative">
            <input
              type="file"
              accept=".zip,.xml"
              onChange={handleZipUpload}
              className="absolute inset-0 opacity-0 cursor-pointer"
            />
            <ShoppingBag className="w-8 h-8 text-slate-400 mx-auto mb-2" />
            <span className="text-xs font-semibold text-slate-700 block">
              Haz clic para seleccionar o arrastra el archivo .ZIP
            </span>
            <span className="text-[11px] text-slate-400">Archivos oficiales UBL 2.1 de la DIAN</span>
          </div>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-red-50 text-red-700 text-sm rounded-xl border border-red-200">
          {error}
        </div>
      )}

      {/* Invoice Details and Interactive Inverse Splitting */}
      {invoice && (
        <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden space-y-6 p-6">
          {/* Invoice Summary Header */}
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-4 border-b border-slate-100 gap-4">
            <div>
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Factura Electrónica Validada
              </span>
              <h2 className="text-xl font-bold text-slate-800 flex items-center space-x-2">
                <span>{invoice.issuer_name}</span>
                <span className="text-xs font-normal text-slate-400">({invoice.invoice_number})</span>
              </h2>
              <div className="flex items-center space-x-4 mt-1 text-xs text-slate-500">
                <span>NIT: {invoice.issuer_nit}</span>
                <span>Fecha: {invoice.issue_date.split('T')[0]}</span>
              </div>
            </div>

            <div className="text-right">
              <div className="text-xs text-slate-400 font-medium">Total de la Factura</div>
              <div className="text-2xl font-black text-slate-900">{formatCOP(invoice.total_amount)}</div>
            </div>
          </div>

          {/* Inverse Splitting Explainer Bar */}
          <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-start space-x-3 text-xs text-amber-900">
            <span className="font-bold text-sm">💡</span>
            <div>
              <span className="font-bold">Lógica de Selección Inversa:</span> Solo marca en la lista tus compras 100% individuales (ej. tus golosinas o artículos personales). El sistema calculará y asignará de forma automática todos los ítems no marcados al gasto del hogar compartido (50/50).
            </div>
          </div>

          {/* Items Table */}
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase border-b border-slate-100">
                <tr>
                  <th className="px-4 py-3 w-12 text-center">¿Es Tuyo?</th>
                  <th className="px-4 py-3">Descripción del Producto</th>
                  <th className="px-4 py-3 text-center">Cantidad</th>
                  <th className="px-4 py-3 text-right">Precio Unit.</th>
                  <th className="px-4 py-3 text-right">Total</th>
                  <th className="px-4 py-3 text-center">Clasificación Automática</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {(invoice.items || []).map((item) => {
                  const isPersonal = selectedPersonalIds.has(item.id);
                  return (
                    <tr
                      key={item.id}
                      onClick={() => toggleItem(item.id)}
                      className={`cursor-pointer transition-colors ${
                        isPersonal ? 'bg-emerald-50/60' : 'hover:bg-slate-50'
                      }`}
                    >
                      <td className="px-4 py-3 text-center">
                        {isPersonal ? (
                          <CheckSquare className="w-5 h-5 text-emerald-600 mx-auto" />
                        ) : (
                          <Square className="w-5 h-5 text-slate-300 mx-auto" />
                        )}
                      </td>
                      <td className="px-4 py-3 font-medium text-slate-800">
                        {item.description}
                      </td>
                      <td className="px-4 py-3 text-center text-slate-500">{item.quantity}</td>
                      <td className="px-4 py-3 text-right text-slate-400 text-xs font-mono">
                        {formatCOP(item.unit_price)}
                      </td>
                      <td className="px-4 py-3 text-right font-bold text-slate-800 font-mono">
                        {formatCOP(item.total_price)}
                      </td>
                      <td className="px-4 py-3 text-center">
                        <span
                          className={`px-2.5 py-1 rounded-md text-xs font-semibold ${
                            isPersonal
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-purple-100 text-purple-800'
                          }`}
                        >
                          {isPersonal ? 'Personal' : 'Hogar Compartido (50/50)'}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Split Totals Breakdown Card */}
          <div className="bg-slate-900 text-white rounded-xl p-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div className="flex items-center space-x-8">
              <div>
                <span className="text-xs text-emerald-400 font-semibold uppercase block">
                  Tu Gasto Personal
                </span>
                <span className="text-xl font-bold">{formatCOP(personalTotal)}</span>
                <span className="text-xs text-slate-400 block mt-0.5">
                  ({selectedPersonalIds.size} ítems marcados)
                </span>
              </div>

              <div className="h-10 w-px bg-slate-700" />

              <div>
                <span className="text-xs text-purple-400 font-semibold uppercase block">
                  Gasto del Hogar (50/50)
                </span>
                <span className="text-xl font-bold">{formatCOP(householdTotal)}</span>
                <span className="text-xs text-slate-400 block mt-0.5">
                  Tu aporte del 50%: {formatCOP(householdTotal / 2)}
                </span>
              </div>
            </div>

            <button
              onClick={handleConfirmSplit}
              disabled={loading}
              className="py-3 px-6 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg shadow-sm transition-colors flex items-center justify-center space-x-2"
            >
              <span>{loading ? 'Guardando...' : 'Confirmar División de Gastos'}</span>
              <CheckCircle2 className="w-5 h-5" />
            </button>
          </div>

          {savedSuccess && (
            <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm rounded-xl flex items-center space-x-2">
              <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
              <span>
                ¡División registrada exitosamente! Se crearon los gastos correspondientes y se actualizaron los saldos del hogar.
              </span>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
