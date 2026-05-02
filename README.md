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
| **auth-service** | 8081 | Login, JWT, gestion de tenants, subscripciones y permisos |
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
# URLs de AFIP (homologacion por defecto)
AFIP_WSAA_URL=https://wsaahomo.afip.gov.ar/ws/services/LoginCms
AFIP_WSFE_URL=https://wswhomo.afip.gov.ar/wsfev1/service.asmx

# Clave para encriptar passwords de certificados AFIP
AFIP_ENCRYPTION_KEY=GuidaContable2026SecureKey!

# API Key para comunicacion interna entre microservicios
INTERNAL_API_KEY=Int3rnalK3yGu1da2026S3gur0!
```

### 2. Levantar los servicios

```bash
docker compose up -d --build
```

Esperar ~60 segundos a que todos los servicios inicien. Verificar:

```bash
docker compose ps
```

### 3. Iniciar el frontend

```bash
cd ../guida-frontend
npm install
npm run dev
```

Abrir http://localhost:5173

## Manual de Uso

### Acceso al Sistema

El sistema no tiene registro publico. Los estudios contables (tenants) son creados exclusivamente por el administrador de la plataforma (Guida Pixel) desde el panel de administracion SaaS.

1. El administrador de la plataforma crea el estudio desde **Administracion SaaS** > **+ Nuevo Estudio**
2. Se generan credenciales automaticas con una password temporal
3. El administrador del estudio recibe su email y password temporal para ingresar
4. Al primer inicio de sesion, el administrador del estudio puede crear empleados (STAFF) y configurar sus permisos

> Para solicitar acceso a la plataforma, contactar a Guida Pixel.

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

### Configuracion AFIP por Tenant

Cada estudio contable tiene su propio CUIT emisor y certificado AFIP. La configuracion se realiza desde el panel de administracion SaaS:

1. Seleccionar el estudio en la lista de tenants
2. Ir a la pestaña **Configuracion AFIP**
3. **Subir el certificado .p12** obtenido de AFIP para ese estudio
4. Configurar el **CUIT Emisor AFIP** (el CUIT del estudio que factura)
5. Ingresar la **password del certificado** (se guarda encriptada)
6. Seleccionar **homologacion** (prueba) o desmarcar para **produccion**
7. Click en **Guardar Configuracion AFIP**

> Los certificados se almacenan en un volumen Docker compartido (`afip_certs`) y cada tenant tiene su propio archivo `{tenantId}.p12`. La password del certificado se encripta con AES antes de guardarse en la base de datos.

## Endpoints de la API

### Auth

| Metodo | Endpoint | Descripcion |
|---|---|---|
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

> El sistema determina automaticamente si el documento fue subido por el estudio (`fromTenant=true`) o por el cliente (`fromTenant=false`) segun el rol del usuario autenticado en el JWT.

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

### Panel de Administracion SaaS

Como dueno de la plataforma, tenes un panel de administracion para gestionar los estudios contables (tenants), sus modulos y su configuracion AFIP.

#### Crear un Nuevo Estudio

1. Click en **"+ Nuevo Estudio"**
2. Completar:
   - **Nombre del Estudio**: Razon social del estudio contable
   - **CUIT del Estudio**: CUIT con formato `XX-XXXXXXXX-X`
   - **Nombre y Apellido del Admin**: Datos del responsable del estudio
   - **Email del Admin**: Email que usara para iniciar sesion
3. Click en **"Crear Estudio"**
4. El sistema genera una **password temporal** que debe compartirse con el administrador del estudio

#### Configuracion AFIP por Tenant

Cada estudio necesita su propio certificado AFIP para emitir facturas oficiales:

1. Seleccionar el estudio en la lista
2. Ir a la pestaña **Configuracion AFIP**
3. **Subir el certificado .p12** del estudio
4. Configurar el **CUIT Emisor AFIP** (el CUIT con el que factura el estudio)
5. Ingresar la **password del certificado** (se guarda encriptada con AES)
6. Seleccionar **homologacion** (prueba) o desmarcar para **produccion**
7. Click en **"Guardar Configuracion AFIP"**

> Los certificados se almacenan en un volumen Docker (`afip_certs`) con la ruta `/app/certs/{tenantId}.p12`. Cada tenant tiene su propio certificado independiente.

#### Gestion de Modulos por Tenant

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
| **ADMIN** | Dueño del estudio contable | Todos los modulos habilitados por SaaS, gestion de empleados |
| **STAFF** | Empleado del estudio | Permisos granulares configurados por el ADMIN |
| **CLIENT** | Cliente del estudio contable | Solo "Mi Cuenta" y "Documentos" |

### Como funciona

- **Guida Pixel** (SUPER_ADMIN) crea estudios, activa/desactiva modulos y configura AFIP por tenant.
- **Estudios contables** (ADMIN) pueden crear empleados (STAFF) y definir permisos granulares para cada uno.
- **Clientes** (CLIENT) acceden para ver su cuenta corriente, descargar documentos y subir archivos para su contador.

### Credenciales por defecto

- **Super-admin**: `admin@guidapixel.com` / `admin123`
- Se crea automaticamente al iniciar el sistema por primera vez.

## Gestion de Empleados (Permisos STAFF)

El administrador del estudio (ADMIN) puede crear empleados con acceso limitado al sistema. Cada empleado tiene permisos individuales que el ADMIN puede configurar.

### Crear un Empleado

1. Ir a **Empleados** en el menu lateral (visible solo para ADMIN)
2. Click en **"+ Nuevo Empleado"**
3. Completar nombre, apellido y email
4. Click en **"Crear Empleado"**
5. El sistema genera una **password temporal** que debe compartirse con el empleado

> Al crear un empleado, se le asignan permisos por defecto: puede gestionar clientes y documentos, pero no puede ver facturas, dashboard ni gestionar otros empleados.

### Configurar Permisos

1. Seleccionar un empleado de la lista
2. Activar o desactivar cada permiso usando los toggles:

| Permiso | Descripcion | Default |
|---|---|---|
| **Gestionar Clientes** | Acceso al modulo de clientes | ✅ Habilitado |
| **Ver Facturas** | Puede ver facturas internas | ❌ Deshabilitado |
| **Crear Facturas** | Puede crear y emitir facturas | ❌ Deshabilitado |
| **Gestionar Documentos** | Acceso al modulo de documentos | ✅ Habilitado |
| **Ver Dashboard** | Puede ver metricas y reportes | ❌ Deshabilitado |
| **Gestionar Empleados** | Puede gestionar permisos de otros empleados | ❌ Deshabilitado |

3. Click en **"Guardar Permisos"**

> Los permisos se incluyen en el JWT del usuario al iniciar sesion. El API Gateway valida los permisos en cada request. Si un empleado intenta acceder a un modulo sin permiso, recibe un error 403 con un mensaje claro.

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

- Verificar que el certificado .p12 fue subido correctamente desde el panel de administracion SaaS
- Verificar que la password del certificado es correcta
- Verificar que el certificado no esta vencido
- Verificar que el CUIT emisor AFIP esta configurado para el tenant

### Error "CUIT ya existe"

El CUIT del estudio debe ser unico. Si el estudio ya fue creado, contacta al administrador de la plataforma para obtener acceso.

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
│       ├── StaffManagementPage.jsx     # Gestion de empleados (solo ADMIN)
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
| Almacenamiento | Volumenes Docker (dev) — se recomienda MinIO para produccion |
| Auth | JWT (HS384), Spring Security, roles jerarquicos |
| Infra | Docker, Docker Compose |
| AFIP | WSAA (autenticacion), WSFEv1 (facturacion) |
