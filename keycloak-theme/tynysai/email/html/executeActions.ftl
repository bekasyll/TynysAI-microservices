<#import "template.ftl" as layout>
<@layout.emailLayout>
<h1 style="margin:0 0 20px; font-size:22px; font-weight:600; color:#0E2A45; line-height:1.3;">${msg("executeActionsHeadline")}</h1>

<p style="margin:0 0 16px; font-size:16px; line-height:1.6; color:#1f2937;">${msg("executeActionsGreeting")}</p>

<p style="margin:0 0 28px; font-size:16px; line-height:1.6; color:#374151;">${msg("executeActionsIntro")}</p>

<!-- CTA button — bullet-proof button technique (table + bgcolor for Outlook). -->
<table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:0 auto 28px;">
  <tr>
    <td align="center" bgcolor="#1d4ed8" style="border-radius:8px; background-color:#1d4ed8;">
      <a href="${link}" target="_blank" style="display:inline-block; padding:14px 36px; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; font-size:16px; font-weight:600; color:#ffffff; text-decoration:none; border-radius:8px;">${msg("executeActionsButton")}</a>
    </td>
  </tr>
</table>

<p style="margin:0 0 8px; font-size:14px; line-height:1.5; color:#6b7280;">${msg("executeActionsExpiration", linkExpirationFormatter(linkExpiration))}</p>

<!-- Fallback URL for clients that strip the button. -->
<p style="margin:0 0 28px; font-size:13px; line-height:1.5; color:#9ca3af; word-break:break-all;">${msg("executeActionsFallback")}<br/><a href="${link}" target="_blank" style="color:#1d4ed8; text-decoration:underline; word-break:break-all;">${link}</a></p>

<hr style="border:0; border-top:1px solid #e5e7eb; margin:0 0 24px;" />

<p style="margin:0 0 12px; font-size:14px; line-height:1.6; color:#6b7280;">${msg("executeActionsDisclaimer")}</p>

<p style="margin:0; font-size:14px; line-height:1.6; color:#374151;">${msg("executeActionsSignoff")}</p>
</@layout.emailLayout>