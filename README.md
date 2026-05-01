# Guia Contable - Sistema de Facturacion Electronica

Sistema de gestion contable con emision de facturas electronicas oficiales ante **AFIP** (Argentina), construido con arquitectura de microservicios.

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (React + Vite)                   │
│                  http://localhost:5173                       │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   API Gateway (Spring Cloud)                 │
│                  http://localhost:8080                       │
└──┬──────────┬──────────┬──────────┬──────────┬──────────────┘
   │          │          │          │          │
   ▼          ▼          ▼          ▼          ▼
┌──────┐  ┌────────┐  ┌─────────┐  ┌──────┐  ┌─────────┐
│ Auth │  │Clients │  │Invoices │  │ AFIP │  │ Dashboard│
│ :8081│  │ :8082  │  │ :8083   │  │ :8084│  │ :8086   │
└──┬───┘  └───┬────┘  └────┬────┘  └──┬───┘  └────┬────┘
   │          │             │          │           │
   ▼          ▼             ▼          ▼           ▼
┌──────────────────┐  ┌─────────────────────────────────────┐
│   PostgreSQL     │  │            MongoDB                   │
│ guida_contable   │  │        guida_audit                   │
└──────────────────┘  └─────────────────────────────────────┘
```

### Microservicios

| Servicio | Puerto | Descripcion |
|---|---|---|
| **api-gateway** | 8080 | Punto de entrada unico, ruteo y CORS |
| **auth-service** | 8081 | Registro, login, JWT, gestion de tenants |
| **client-service** | 8082 | CRUD de clientes (razon social, CUIT, etc.) |
| **invoice-service** | 8083 | Facturas internas + integracion con AFIP |
| **afip-service** | 8084 | Comunicacion directa con webservices de AFIP |
| **audit-service** | 8085 | Logs de auditoria en MongoDB |
| **dashboard-service** | 8086 | Metricas y reportes consolidados |

## Requisitos

- **Docker** y **Docker Compose**
- **Node.js 18+** (para el frontend)
- Certificado AFIP `.p12` (para facturacion oficial)

## Inicio Rapido

### 1. Configurar variables de entorno

Crear archivo `.env` en la raiz del proyecto:

```env
AFIP_CERT_PASSWORD=tu_password_del_certificado
```

> El password es el del archivo `.p12` que descargaste de AFIP.

### 2. Colocar el certificado AFIP

El certificado debe estar en la ruta configurada en `docker-compose.yml`:

```yaml
volumes:
  - /ruta/a/tus/certs:/app/certs:ro
```

Por defecto apunta a `/home/nachomartinezdev/certs` (Linux). En Windows, modifica el volumen en `docker-compose.yml`:

```yaml
volumes:
  - C:/ruta/a/tus/certs:/app/certs:ro
```

### 3. Levantar los servicios

```bash
docker compose up -d --build
```

Esperar ~60 segundos a que todos los servicios inicien. Verificar:

```bash
docker compose ps
```

### 4. Iniciar el frontend

```bash
cd ../guida-frontend
npm install
npm run dev
```

Abrir http://localhost:5173

## Manual de Uso

### Registro de Usuario

1. Abrir http://localhost:5173
2. Click en **"Registrarse"**
3. Completar:
   - **Nombre del Estudio**: Nombre de tu estudio contable
   - **CUIT del Estudio**: CUIT con formato `XX-XXXXXXXX-X`
   - **Tu Nombre y Apellido**
   - **Email y Contrasena**
4. Click en **"Crear cuenta e ingresar"**

> El sistema crea automaticamente un **Tenant** (estudio) y un usuario **ADMIN** asociado.

### Agregar Clientes

1. Ir a **Clientes** en el menu lateral
2. Click en **"Nuevo Cliente"**
3. Completar:
   - **Razon Social**: Nombre del cliente
   - **CUIT**: CUIT del cliente
   - **Email** (opcional)
   - **Direccion** (opcional)
4. Guardar

### Crear Factura Interna (sin AFIP)

Para facturas que no necesitan validez oficial (presupuestos, borradores):

1. Ir a **Nueva Factura**
2. Desactivar el toggle **"Emitir en AFIP"**
3. Seleccionar cliente
4. Agregar items (concepto, cantidad, precio)
5. Click en **"Crear Factura Interna"**

La factura se guarda solo en el sistema, sin numero oficial.

### Emitir Factura Oficial en AFIP

Para facturas con validez fiscal (CAE):

1. Ir a **Nueva Factura**
2. Activar el toggle **"Emitir en AFIP"** (viene activado por defecto)
3. Configurar datos AFIP:

| Campo | Descripcion | Valores tipicos |
|---|---|---|
| **Tipo Comprobante** | Tipo de factura | Factura A (1), B (6), C (11) |
| **Punto de Venta** | Numero de punto de venta autorizado | 1 (o el que AFIP te asigno) |
| **Concepto** | Que se factura | Productos (1), Servicios (2), Mixto (3) |
| **Tipo Doc. Receptor** | Tipo de documento del cliente | CUIT (80), DNI (96) |
| **Nro. Doc. Receptor** | Numero de documento | Sin guiones ni puntos |
| **Condicion IVA** | Clasificacion IVA del receptor | Consumidor Final (5), RI (3), etc. |

4. Si el concepto es **Servicios** o **Mixto**, apareceran campos adicionales:
   - **Servicio Desde / Hasta**: Periodo facturado
   - **Vencimiento Pago**: Fecha limite de pago

5. Seleccionar cliente y agregar items
6. Click en **"Emitir en AFIP"**

#### Que sucede internamente

```
1. invoice-service crea la factura interna
2. invoice-service llama a afip-service via HTTP
3. afip-service se autentica en AFIP (WSAA) con el certificado .p12
4. Consulta el ultimo numero autorizado (FECompUltimoAutorizado)
5. Incrementa +1 para el siguiente numero correlativo
6. Envia la solicitud de CAE (FECAESolicitar)
7. AFIP responde con CAE, vencimiento y resultado
8. Si es APROBADA: guarda CAE y numero oficial en ambas bases
9. Si es RECHAZADA: muestra el error de AFIP
```

#### Resultado exitoso

Se muestra un panel verde con:

- **Comprobante**: Factura C 0001-00000042
- **Nro AFIP**: 42
- **CAE**: 72812345678901
- **Vto. CAE**: 2026-05-10

### Tipos de Comprobante

| Codigo | Tipo | Uso |
|---|---|---|
| 1 | Factura A | Responsables Inscriptos (con IVA discriminado) |
| 6 | Factura B | Consumidor Final, Monotributistas, Exentos |
| 11 | Factura C | Monotributistas (sin IVA) |
| 2, 7, 12 | Nota de Debito | A, B, C respectivamente |
| 3, 8, 13 | Nota de Credito | A, B, C respectivamente |

### Condiciones IVA del Receptor

| ID | Condicion | Cuando usar |
|---|---|---|
| 3 | Responsable Inscripto | Cliente con responsabilidad inscripta |
| 4 | Exento | Entidades exentas de IVA |
| 5 | Consumidor Final | Publico en general (default) |
| 6 | Monotributista | Clientes monotributistas |

### Homologacion vs Produccion

Por defecto el sistema apunta a **homologacion** (sandbox de AFIP).

Para pasar a **produccion**, cambiar las variables de entorno en `.env`:

```env
# Homologacion (default)
AFIP_WSAA_URL=https://wsaahomo.afip.gov.ar/ws/services/LoginCms
AFIP_WSFE_URL=https://wswhomo.afip.gov.ar/wsfev1/service.asmx

# Produccion
AFIP_WSAA_URL=https://wsaa.afip.gov.ar/ws/services/LoginCms
AFIP_WSFE_URL=https://servicios1.afip.gov.ar/wsfev1/service.asmx
```

Tambien necesitas un certificado de **produccion** (no el de homologacion).

### Configuracion por defecto

Se pueden definir valores por defecto para la emision de facturas:

```env
AFIP_CUIT_EMISOR=20325847094
AFIP_PUNTO_VENTA=1
AFIP_TIPO_COMPROBANTE=11
```

Estos valores se usan si no se especifican en la peticion.

## Endpoints de la API

### Auth

| Metodo | Endpoint | Descripcion |
|---|---|---|
| POST | `/api/v1/auth/register` | Registrar nuevo usuario/estudio |
| POST | `/api/v1/auth/login` | Login y obtener JWT |

### Clientes

| Metodo | Endpoint | Descripcion |
|---|---|---|
| GET | `/api/v1/clients` | Listar clientes |
| POST | `/api/v1/clients` | Crear cliente |
| PUT | `/api/v1/clients/{id}` | Actualizar cliente |
| DELETE | `/api/v1/clients/{id}` | Eliminar cliente |

### Facturas

| Metodo | Endpoint | Descripcion |
|---|---|---|
| POST | `/api/v1/invoices` | Crear factura (+ AFIP si `emitirAfip: true`) |
| GET | `/api/v1/invoices/total-facturado` | Total facturado |

### AFIP

| Metodo | Endpoint | Descripcion |
|---|---|---|
| GET | `/api/afip/test-token` | Test de conexion con AFIP |
| GET | `/api/afip/ultimo-comprobante` | Ultimo nro autorizado |
| POST | `/api/afip/emitir` | Emitir factura directa en AFIP |

## Ejemplo: Emitir factura via API

```bash
# Login para obtener token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"usuario@estudio.com","password":"password123"}'

# Crear factura con emision AFIP
curl -X POST http://localhost:8080/api/v1/invoices \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "clientId": 1,
    "emitirAfip": true,
    "tipoComprobante": 11,
    "puntoVenta": 1,
    "tipoDocumento": 80,
    "numeroDocumento": 30123456789,
    "condicionIvaReceptorId": 5,
    "concepto": 1,
    "items": [
      { "concepto": "Honorarios profesionales", "cantidad": 1, "precioUnitario": 50000 }
    ]
  }'
```

## Solucion de Problemas

### 503 Service Unavailable

Los servicios tardan ~30s en iniciar. Esperar y reintentar.

Verificar que todos los servicios estan corriendo:
```bash
docker compose ps
```

### Error de certificado AFIP

- Verificar que el archivo `.p12` existe en la ruta del volumen
- Verificar que `AFIP_CERT_PASSWORD` es correcto
- Verificar que el certificado no esta vencido

### Error "CUIT ya existe"

El CUIT del estudio debe ser unico. Si ya te registraste, usa login en lugar de registro.

### Error "AFIP rechazo la factura"

Revisar el mensaje de error que devuelve AFIP. Causas comunes:
- Certificado vencido
- Punto de venta no autorizado
- CUIT emisor incorrecto
- Datos del receptor invalidos

## Estructura del Proyecto

```
contable-api/
├── api-gateway/          # Spring Cloud Gateway
├── auth-service/         # Autenticacion y tenants
├── client-service/       # CRUD clientes
├── invoice-service/      # Facturas + integracion AFIP
├── afip-service/         # Comunicacion con webservices AFIP
├── audit-service/        # Auditoria (MongoDB)
├── dashboard-service/    # Metricas y reportes
├── shared/               # Libreria compartida (JWT, BaseEntity, etc.)
├── docker-compose.yml    # Orquestacion de servicios
└── .env                  # Variables de entorno (no versionado)

guida-frontend/
├── src/
│   ├── api/              # Axios configurado con JWT
│   ├── components/       # Componentes reutilizables
│   ├── layouts/          # Layouts de la app
│   └── pages/            # Paginas principales
└── package.json
```

## Tecnologias

| Capa | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring Cloud Gateway |
| Frontend | React 18, Vite, TailwindCSS, Axios |
| Base de datos | PostgreSQL 16, MongoDB |
| Auth | JWT (HS384), Spring Security |
| Infra | Docker, Docker Compose |
| AFIP | WSAA (autenticacion), WSFEv1 (facturacion) |
