-- Align enterprise client sessions to twelve hours.
-- Both active timeout and absolute timeout are expressed in seconds.

SET NOCOUNT ON;
GO

IF OBJECT_ID(N'dbo.sys_client', N'U') IS NULL
    THROW 51000, 'Missing dbo.sys_client. Run the base schema before session-timeout-12h.sql.', 1;

UPDATE dbo.sys_client
   SET active_timeout = 43200,
       timeout = 43200,
       update_time = SYSDATETIME()
 WHERE client_key IN (N'pc', N'app')
    OR device_type IN (N'pc', N'android');

SELECT @@ROWCOUNT AS updated_sys_client_session_timeout_rows;
GO
