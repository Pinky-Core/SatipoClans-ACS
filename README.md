<p align="center">
  <img src="https://i.imgur.com/Y0SFJ6u.png" alt="Mi logo" width="500" />
</p>

<div align="center">

# 🛡️ SatipoClans | Advanced Clans System 🛡️

SatipoClans es un plugin para servidores Minecraft que implementa un sistema avanzado y robusto de clanes, con invitaciones, privacidad, administración y almacenamiento en MariaDB.

</div>


---

## ⬇️ Instalación ⬇️

1. Descarga el archivo JAR de SatipoClans.  
2. Colócalo en la carpeta `plugins` de tu servidor Minecraft.  
3. Configura la conexión a MariaDB en el archivo `config.yml` o en la sección correspondiente.
5. Reinicia el servidor para que el plugin se cargue correctamente.
6. Configura tu idioma con `/clanadmin lang` o `/clanadmin lang select <idioma>` (Puedes crear tu propio yml).

---

## 🔧 Configuración 

Asegúrate de tener una base de datos MariaDB disponible y funcionando. Configura los datos de conexión (host, puerto, usuario, contraseña, base de datos) en el archivo `config.yml` o donde el plugin lo indique.

El plugin crea automáticamente las tablas necesarias al iniciar el servidor si no existen.

---

## ⌨️ Comandos Usuarios ⌨️

| Comando               | Descripción                                  | Permiso                |
|-----------------------|----------------------------------------------|------------------------|
| `/clan create <nombre>`| Crear un nuevo clan                           | `satipoclans.user.create`     |
| `/clan invite <jugador>`| Invitar a un jugador a tu clan               | `satipoclans.user.invite`     |
| `/clan join <clan>`    | Unirse a un clan (requiere invitación si es privado) | `satipoclans.user.join`     |
| `/clan leave`          | Salir del clan actual                         | `satipoclans.user.leave`     |
| `/clan disband`        | Disolver tu clan (solo líderes)               | `satipoclans.user.disband`   |
| `/clan edit <name/privacy>`          | Editar nombre o privacidad                    | `satipoclans.user.edit`     |
| `/clan ally`          | Haz una alianza con otro clan                     | `satipoclans.user.ally`     |
| `/clan ff`          | Activa o desactiva el fuego amigo                      | `satipoclans.user.ff`     |
| `/clan chat <mensaje>` | Enviar mensaje al chat privado del clan      | `satipoclans.user.chat`     |
| `/clan stats`          | Ver estadísticas del clan                      | `satipoclans.user.stats`     |
| `/clan list`          | Ver lista de clanes                     | `satipoclans.user.list`     |

## ⚠️ Comandos Administrativos ⚠️

| Comando               | Descripción                                  | Permiso                |
|-----------------------|----------------------------------------------|------------------------|
| `/clanadmin reports` | ᴍᴜᴇꜱᴛʀᴀ ᴛᴏᴅᴏꜱ ʟᴏꜱ ᴄʟᴀɴᴇꜱ ᴄᴏɴ ʀᴇᴘᴏʀᴛᴇꜱ ᴀᴄᴛɪᴠᴏꜱ. | `satipoclans.admin`  |
| `/lanadmin reload` | ʀᴇᴄᴀʀɢᴀ ʟᴀ ᴄᴏɴꜰɪɢᴜʀᴀᴄɪᴏ́ɴ ʏ ᴅᴀᴛᴏꜱ ᴅᴇʟ ᴘʟᴜɢɪɴ. | `satipoclans.admin` |
| `/lanadmin ban <clan> [razón]` | ᴘʀᴏʜɪ́ʙᴇ ᴜɴ ᴄʟᴀɴ ᴘᴇʀᴍᴀɴᴇɴᴛᴇᴍᴇɴᴛᴇ. | `satipoclans.admin` |
| `/lanadmin unban <clan>`  | ʟᴇᴠᴀɴᴛᴀ ʟᴀ ᴘʀᴏʜɪʙɪᴄɪᴏ́ɴ ᴅᴇ ᴜɴ ᴄʟᴀɴ. | `satipoclans.admin` |
| `/lanadmin clear` | ʙᴏʀʀᴀ ᴛᴏᴅᴀ ʟᴀ ʙᴀꜱᴇ ᴅᴇ ᴅᴀᴛᴏꜱ. | `satipoclans.admin` |

---

## ✅ Características principales 

- Creación y gestión sencilla de clanes.  
- Sistema de invitaciones con expiración automática (5 minutos).  
- Clanes públicos y privados con control total de acceso.  
- Prevención de invitaciones duplicadas y auto-invitaciones.  
- Integración completa con MariaDB para rendimiento y estabilidad.  
- Registro histórico de uniones y actividades del clan.  
- Mensajes claros y sistema de permisos robusto.

---

## 🛠️ Soporte 🛠️

Si encuentras errores o tienes sugerencias, abre un issue en el repositorio oficial o contáctame directamente.

