<#macro emailLayout>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="${locale.language}">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
<title>TynysAI</title>
</head>
<body style="margin:0; padding:0; background-color:#f3f4f6; -webkit-text-size-adjust:100%; -ms-text-size-adjust:100%;">
<table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="background-color:#f3f4f6;">
  <tr>
    <td align="center" style="padding:32px 12px;">
      <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="600" style="max-width:600px; width:100%; background-color:#ffffff; border-radius:14px; overflow:hidden; box-shadow:0 4px 12px rgba(15, 23, 42, 0.08);">
        <!-- Header band with hosted logo + wordmark -->
        <tr>
          <td align="center" bgcolor="#0E2A45" style="background-color:#0E2A45; background-image:linear-gradient(135deg,#0C1A2E 0%,#0E2A45 50%,#0C2030 100%); padding:36px 24px;">
            <img src="https://cdn.jsdelivr.net/gh/bekasyll/TynysAI-logo@main/TynysAI-logo.png" alt="TynysAI" width="160" style="display:block; margin:0 auto 14px; border:0; max-width:160px; height:auto;" />
            <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; font-size:26px; font-weight:700; color:#ffffff; letter-spacing:-0.02em; line-height:1;">TynysAI</div>
          </td>
        </tr>
        <!-- Content -->
        <tr>
          <td style="padding:36px 32px; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; font-size:16px; line-height:1.6; color:#1f2937;">
            <#nested>
          </td>
        </tr>
      </table>
      <!-- Footer outside the card -->
      <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="600" style="max-width:600px; width:100%;">
        <tr>
          <td align="center" style="padding:20px 16px 0; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; font-size:12px; line-height:1.5; color:#6b7280;">
            © TynysAI · ${msg("emailFooterText")}
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
</body>
</html>
</#macro>