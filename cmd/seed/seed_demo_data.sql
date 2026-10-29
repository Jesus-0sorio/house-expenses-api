-- Seed Data for Jesus Osorio and Roomie Demo in household "Casa"

-- Clean previous demo expenses and settlement payments for this household
DELETE FROM settlement_payments WHERE household_id = '260f13e0-3471-461c-b523-32d6e9d2e6a2';
DELETE FROM expenses WHERE household_id = '260f13e0-3471-461c-b523-32d6e9d2e6a2' OR user_id IN ('40fc16c3-1560-49fb-b773-88acb2d89cb5', 'c8c5b5a2-030a-48bd-83ab-ade94f1a48c3');

-- 1. Gastos Compartidos del Ciclo Activo (Septiembre 2026)
-- Jesús paga Supermercado Éxito: $280.000 COP
INSERT INTO expenses (id, user_id, household_id, amount, date, category, description, scope, payment_status, created_at, updated_at)
VALUES (
    'a1000001-0000-0000-0000-000000000001',
    '40fc16c3-1560-49fb-b773-88acb2d89cb5',
    '260f13e0-3471-461c-b523-32d6e9d2e6a2',
    280000.00,
    '2026-09-02 11:30:00+00',
    'Supermercado',
    'Mercado quincenal Éxito',
    'HOUSEHOLD',
    'PAID',
    NOW(),
    NOW()
);

-- Jesús paga Internet Claro: $95.000 COP
INSERT INTO expenses (id, user_id, household_id, amount, date, category, description, scope, payment_status, created_at, updated_at)
VALUES (
    'a1000001-0000-0000-0000-000000000002',
    '40fc16c3-1560-49fb-b773-88acb2d89cb5',
    '260f13e0-3471-461c-b523-32d6e9d2e6a2',
    95000.00,
    '2026-09-04 16:45:00+00',
    'Servicios',
    'Internet Fibra Óptica Claro',
    'HOUSEHOLD',
    'PAID',
    NOW(),
    NOW()
);

-- Roomie paga Servicios Públicos EPM: $190.000 COP
INSERT INTO expenses (id, user_id, household_id, amount, date, category, description, scope, payment_status, created_at, updated_at)
VALUES (
    'a1000001-0000-0000-0000-000000000003',
    'c8c5b5a2-030a-48bd-83ab-ade94f1a48c3',
    '260f13e0-3471-461c-b523-32d6e9d2e6a2',
    190000.00,
    '2026-09-03 09:15:00+00',
    'Servicios',
    'Factura de servicios EPM (Luz y Gas)',
    'HOUSEHOLD',
    'PAID',
    NOW(),
    NOW()
);

-- Roomie paga Aseo D1: $65.000 COP
INSERT INTO expenses (id, user_id, household_id, amount, date, category, description, scope, payment_status, created_at, updated_at)
VALUES (
    'a1000001-0000-0000-0000-000000000004',
    'c8c5b5a2-030a-48bd-83ab-ade94f1a48c3',
    '260f13e0-3471-461c-b523-32d6e9d2e6a2',
    65000.00,
    '2026-09-05 10:00:00+00',
    'Hogar',
    'Artículos de aseo y limpieza D1',
    'HOUSEHOLD',
    'PAID',
    NOW(),
    NOW()
);

-- 2. Gastos Personales (scope = 'PERSONAL', household_id = NULL)
-- Jesús: Cine $35.000 COP
INSERT INTO expenses (id, user_id, household_id, amount, date, category, description, scope, payment_status, created_at, updated_at)
VALUES (
    'b2000001-0000-0000-0000-000000000001',
    '40fc16c3-1560-49fb-b773-88acb2d89cb5',
    NULL,
    35000.00,
    '2026-09-03 20:15:00+00',
    'Entretenimiento',
    'Cine y confitería',
    'PERSONAL',
    'PAID',
    NOW(),
    NOW()
);

-- Roomie: Almuerzo $22.000 COP
INSERT INTO expenses (id, user_id, household_id, amount, date, category, description, scope, payment_status, created_at, updated_at)
VALUES (
    'b2000001-0000-0000-0000-000000000002',
    'c8c5b5a2-030a-48bd-83ab-ade94f1a48c3',
    NULL,
    22000.00,
    '2026-09-04 13:00:00+00',
    'Restaurantes',
    'Almuerzo de trabajo',
    'PERSONAL',
    'PAID',
    NOW(),
    NOW()
);

-- 3. Pago de Liquidación (Roomie transfiere a Jesús $40.000 COP)
INSERT INTO settlement_payments (id, household_id, payer_user_id, receiver_user_id, amount, channel, date, notes, created_at)
VALUES (
    'c3000001-0000-0000-0000-000000000001',
    '260f13e0-3471-461c-b523-32d6e9d2e6a2',
    'c8c5b5a2-030a-48bd-83ab-ade94f1a48c3',
    '40fc16c3-1560-49fb-b773-88acb2d89cb5',
    40000.00,
    'Transferencia',
    '2026-09-05 10:15:00+00',
    'Abono transferencia Bancolombia a cuenta de Jesús por compras de la casa',
    NOW()
);

-- 4. Gastos de Ciclo Anterior (Agosto 2026) para validar Historial de Ciclos
INSERT INTO expenses (id, user_id, household_id, amount, date, category, description, scope, payment_status, created_at, updated_at)
VALUES (
    'd4000001-0000-0000-0000-000000000001',
    '40fc16c3-1560-49fb-b773-88acb2d89cb5',
    '260f13e0-3471-461c-b523-32d6e9d2e6a2',
    150000.00,
    '2026-08-12 10:00:00+00',
    'Supermercado',
    'Mercado Carulla agosto',
    'HOUSEHOLD',
    'PAID',
    NOW(),
    NOW()
);

INSERT INTO expenses (id, user_id, household_id, amount, date, category, description, scope, payment_status, created_at, updated_at)
VALUES (
    'd4000001-0000-0000-0000-000000000002',
    'c8c5b5a2-030a-48bd-83ab-ade94f1a48c3',
    '260f13e0-3471-461c-b523-32d6e9d2e6a2',
    150000.00,
    '2026-08-14 15:30:00+00',
    'Servicios',
    'Factura Acueducto agosto',
    'HOUSEHOLD',
    'PAID',
    NOW(),
    NOW()
);
