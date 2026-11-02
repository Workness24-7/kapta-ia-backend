# Base de datos: Postgres · MySQL/MariaDB · SQLite

El sistema elige el motor con una sola variable: `DATABASE_URL`.
Si algún día cambias de host o de base, no se toca `main.py`, ni la PWA, ni Android.

| Motor | `DATABASE_URL` | Driver |
|---|---|---|
| PostgreSQL (actual) | `postgresql://usuario:clave@host:5432/base` | `psycopg2-binary` |
| MySQL / MariaDB | `mysql://usuario:clave@host:3306/base` | `PyMySQL` |
| SQLite (pruebas locales) | `sqlite:///./kapta.db` | incluido en Python |

## Qué hace `db.py`

Toda la interfaz pública (`leer_tabla`, `guardar_fila`, `buscar_empresa`, …) es la misma
para los tres motores. Internamente traduce:

- **Comillas**: `"columna"` en Postgres/SQLite, `` `columna` `` en MySQL.
- **Marcadores**: `%s` en Postgres/MySQL, `?` en SQLite.
- **Upsert**: `ON CONFLICT … EXCLUDED` → `ON DUPLICATE KEY UPDATE … VALUES()` en MySQL.
- **DDL**: `SERIAL` → `AUTO_INCREMENT` / `AUTOINCREMENT`; `TEXT DEFAULT ''` → `TEXT`
  en MySQL (no lo admite); columnas `TEXT` usadas como clave → `VARCHAR(191)`.
- **Catálogo**: `information_schema` en Postgres/MySQL, `sqlite_master`/`pragma_table_info`
  en SQLite.
- **Fotos**: `LONGTEXT` en MySQL, `TEXT` en el resto.

Al arrancar, `init_db()` crea solas las tablas que falten en el motor configurado.

## Cambiar de motor

1. Cambia `DATABASE_URL` en Railway (o en `.env` local).
2. Despliega. `init_db()` crea el esquema vacío.
3. Los datos **no** se copian automáticamente: una base nueva arranca limpia.
   Para llevarte los datos reales hace falta un script de migración aparte.

## Comprobación

```bash
python test_multidb.py
```

Corre todo el esquema y el CRUD contra SQLite (sin servidor) y valida la sintaxis
que se generaría para MySQL. No toca producción.

## Avisos

- **InfinityFree no permite conexiones MySQL remotas**: su MySQL solo acepta
  conexiones desde sus propios servidores. Railway (donde corre Python) no puede
  conectarse, aunque la cadena sea correcta. Para ese host habría que mover también
  el backend, o usar un MySQL con acceso remoto (PlanetScale, Clever Cloud, Railway MySQL).
- **SQLite en Railway no persiste**: el disco del contenedor es efímero. Sirve para
  pruebas locales, no para producción.
- MariaDB (lo que usa InfinityFree) funciona con las mismas reglas que MySQL.
