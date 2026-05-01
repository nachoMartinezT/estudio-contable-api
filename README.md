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
| **api-gateway** | 8080 | Punto de entrada unico, ruteo, CORS y filtro de subscripciones |
| **auth-service** | 8081 | Registro, login, JWT, gestion de tenants y subscripciones |
| **client-service** | 8082 | CRUD de clientes (razon social, CUIT, etc.) |
| **invoice-service** | 8083 | Facturas internas + integracion con AFIP |
| **afip-service** | 8084 | Comunicacion directa con webservices de AFIP |
| **audit-service** | 8085 | Logs de auditoria en MongoDB |
| **dashboard-service** | 8086 | Metricas y reportes consolidados |
| **document-service** | 8087 | Gestion de documentos (upload/download) para tenants y clientes |

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
| GET | `/api/v1/auth/me` | Obtener perfil del usuario (rol, tenant, etc.) |

### Documentos

| Metodo | Endpoint | Descripcion |
|---|---|---|
| POST | `/api/v1/documents/upload` | Subir archivo (multipart) |
| GET | `/api/v1/documents` | Listar documentos (filtra por rol) |
| GET | `/api/v1/documents/{id}/download` | Descargar archivo |
| DELETE | `/api/v1/documents/{id}` | Eliminar documento |

Parametros de upload:
- `file`: Archivo (max 50MB)
- `category`: Categoria (declaraciones, facturas, recibos, certificados, notas, otros)
- `description`: Descripcion opcional
- `fromTenant`: `true` si lo sube el estudio, `false` si lo sube el cliente

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

## Administracion SaaS (Owner)

Como dueno de la plataforma, tenes un panel de administracion para gestionar los estudios contables (tenants) y los modulos que cada uno tiene habilitados.

### Acceso al Panel de Administracion

1. El super-admin se crea automaticamente al iniciar el sistema por primera vez:
   - **Email**: `admin@guidapixel.com`
   - **Password**: `admin123`

2. Ir a http://localhost:5173 e iniciar sesion con las credenciales de arriba.

3. Click en **"Administracion SaaS"** en el menu lateral.

### Gestion de Modulos por Tenant

Cada estudio contable puede tener habilitados o deshabilitados los siguientes modulos:

| Modulo | Descripcion | Path |
|---|---|---|
| **Clientes** | Gestion de clientes | `/api/v1/clients/**` |
| **Facturacion** | Facturas internas | `/api/v1/invoices/**` |
| **AFIP** | Facturacion oficial | `/api/afip/**` |
| **Auditoria** | Logs de auditoria | `/api/v1/audit/**` |
| **Dashboard** | Metricas y reportes | `/api/v1/dashboard/**` |

Cuando desactivas un modulo para un tenant:
- El tenant recibe un error **403 Forbidden** al intentar acceder
- El cambio es inmediato (el gateway cachea las subscripciones por 60 segundos)

### Panel de Estadisticas

El dashboard muestra:
- **Total Estudios**: Cantidad de tenants registrados
- **Total Usuarios**: Cantidad de usuarios en el sistema
- **Subscripciones Activas**: Total de modulos habilitados
- **Modulos Disponibles**: Cantidad de modulos en la plataforma

### API de Administracion

| Metodo | Endpoint | Descripcion |
|---|---|---|
| GET | `/api/v1/admin/dashboard` | Estadisticas generales |
| GET | `/api/v1/admin/tenants` | Listar todos los tenants |
| GET | `/api/v1/admin/tenants/{id}` | Detalle de tenant con subscripciones |
| PUT | `/api/v1/admin/tenants/{id}/subscription` | Activar/desactivar un modulo |
| PUT | `/api/v1/admin/tenants/{id}/subscriptions` | Actualizar todos los modulos |

Ejemplo - Desactivar modulo AFIP para un tenant:

```bash
curl -X PUT http://localhost:8080/api/v1/admin/tenants/6/subscription \
  -H "Content-Type: application/json" \
  -d '{"moduleName":"afip","active":false}'
```

### Como funciona la verificacion de modulos

```
1. El tenant hace una peticion al gateway
2. El SubscriptionCheckFilter extrae el tenantId del JWT
3. Busca en cache los modulos habilitados para ese tenant
4. Si el modulo esta habilitado -> pasa la peticion
5. Si no esta habilitado -> devuelve 403 Forbidden
6. La cache se refresca automaticamente cada 60 segundos
```

## Jerarquia de Roles

El sistema tiene 4 niveles de acceso:

| Rol | Descripcion | Acceso |
|---|---|---|
| **SUPER_ADMIN** | Dueño de la plataforma (Guida Pixel) | Panel SaaS, todos los modulos |
| **ADMIN** | Dueño del estudio contable | Todos los modulos habilitados por SaaS |
| **STAFF** | Empleado del estudio | Modulos limitados por el ADMIN del estudio |
| **CLIENT** | Cliente del estudio contable | Solo "Mi Cuenta" y "Documentos" |

### Como funciona

- **Guida Pixel** (SUPER_ADMIN) puede activar/desactivar modulos para cada estudio.
- **Estudios contables** (ADMIN) pueden crear empleados (STAFF) con permisos limitados.
- **Clientes** (CLIENT) acceden para ver su cuenta corriente, descargar documentos y subir archivos para su contador.

### Credenciales por defecto

- **Super-admin**: `admin@guidapixel.com` / `admin123`
- Se crea automaticamente al iniciar el sistema por primera vez.

## Gestion de Documentos

El modulo de documentos permite el intercambio digital de archivos entre el estudio contable y sus clientes, eliminando la necesidad de reuniones presenciales.

### Para el Estudio Contable (Tenant)

1. Ir a **Documentos** en el menu lateral
2. Click en **"📤 Subir Documento"**
3. Seleccionar archivo (PDF, imagenes, Word, Excel - max 50MB)
4. Elegir categoria:
   - 📋 Declaraciones Juradas
   - 🧾 Facturas
   - 💰 Recibos de Sueldo
   - 📜 Certificados
   - 📝 Notas/Comunicaciones
   - 📁 Otros
5. Agregar descripcion (opcional)
6. Click en **"Subir"**

> Los documentos subidos por el estudio son visibles para el cliente correspondiente.

### Para el Cliente del Estudio

1. Ir a **Documentos** en el menu lateral
2. Ver los documentos compartidos por el estudio
3. Click en **"⬇️ Descargar"** para bajar cualquier archivo
4. Tambien puede **subir archivos** para su contador (facturas, comprobantes, papeles de bienes, etc.)

### Filtros y Categorias

- Filtrar documentos por categoria usando los botones superiores
- Ver cantidad de documentos por categoria
- Iconos automaticos segun tipo de archivo (📄 PDF, 🖼️ Imagen, 📊 Excel, 📝 Word)

## Mi Cuenta (Clientes)

Los clientes del estudio tienen acceso a una pagina personalizada donde pueden:

- Ver resumen de facturas pendientes y pagadas
- Ver total de deuda con el estudio
- Ver ultimos movimientos (honorarios, pagos, etc.)
- Acceder a sus documentos compartidos por el estudio
- Subir documentos para su contador

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
├── api-gateway/          # Spring Cloud Gateway + filtro de subscripciones
├── auth-service/         # Autenticacion, tenants, subscripciones y roles
├── client-service/       # CRUD clientes
├── invoice-service/      # Facturas + integracion AFIP
├── afip-service/         # Comunicacion con webservices AFIP
├── audit-service/        # Auditoria (MongoDB)
├── dashboard-service/    # Metricas y reportes
├── document-service/     # Gestion de documentos (upload/download)
├── shared/               # Libreria compartida (JWT, BaseEntity, etc.)
├── docker-compose.yml    # Orquestacion de servicios
└── .env                  # Variables de entorno (no versionado)

guida-frontend/
├── src/
│   ├── api/              # Axios configurado con JWT
│   ├── components/       # Componentes reutilizables
│   ├── context/          # AuthContext (gestion de sesion y roles)
│   ├── layouts/          # Layouts de la app (menu segun rol)
│   └── pages/            # Paginas principales
│       ├── LoginPage.jsx
│       ├── DashboardPage.jsx
│       ├── ClientsPage.jsx
│       ├── CreateInvoicePage.jsx
│       ├── AdminDashboard.jsx      # Panel SaaS (solo SUPER_ADMIN)
│       ├── DocumentManagementPage.jsx  # Gestion de documentos
│       └── MiCuentaPage.jsx        # Vista de clientes
└── package.json
```

## Tecnologias

| Capa | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring Cloud Gateway |
| Frontend | React 18, Vite, TailwindCSS, Axios, Context API |
| Base de datos | PostgreSQL 16, MongoDB |
| Almacenamiento | Volumenes Docker para documentos |
| Auth | JWT (HS384), Spring Security, roles jerarquicos |
| Infra | Docker, Docker Compose |
| AFIP | WSAA (autenticacion), WSFEv1 (facturacion) |
