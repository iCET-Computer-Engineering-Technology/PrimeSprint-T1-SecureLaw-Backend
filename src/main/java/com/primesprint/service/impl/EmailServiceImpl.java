package com.primesprint.service.impl;

import com.primesprint.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String fromEmail;

    @Value("${app.frontend.login-url:http://localhost:4200/login}")
    private String frontendLoginUrl;

    @Async
    @Retryable(
            retryFor = { Exception.class },
            backoff = @Backoff(delay = 2000)
    )
    @Override
    public void sendInvitationEmail(String toEmail, String username, String createdAt, String password) {
        log.info("Attempting to send invitation email to: {}", toEmail);

        if (!StringUtils.hasText(fromEmail)) {
            throw new IllegalStateException("Mail sender address is empty. Configure app.mail.from or spring.mail.username");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your SecureLaw access link");

            String htmlTemplate = """
                    <!DOCTYPE html>
                        <html lang="en" xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                          <meta charset="UTF-8">
                          <meta name="viewport" content="width=device-width,initial-scale=1.0">
                          <meta http-equiv="X-UA-Compatible" content="IE=edge">
                          <title>SecureLaw Account Email</title>
                          <style>
                            body, table, td, p, a {
                              margin: 0;
                              padding: 0;
                              font-family: Arial, Helvetica, sans-serif;
                              line-height: 1.5;
                            }
                    
                            img {
                              border: 0;
                              outline: none;
                              text-decoration: none;
                              display: block;
                              max-width: 100%;
                              height: auto;
                            }
                    
                            table {
                              border-collapse: collapse;
                            }
                    
                            .email-bg {
                              width: 100%;
                              background-color: #f2f4f8;
                              padding: 24px 12px;
                            }
                    
                            .card {
                              width: 100%;
                              max-width: 680px;
                              background-color: #ffffff;
                              border: 1px solid #e1e5ee;
                              border-radius: 12px;
                              overflow: hidden;
                            }
                    
                            .header {
                              padding: 22px 28px 12px;
                              text-align: center;
                              border-bottom: 1px solid #edf0f5;
                            }
                    
                            .brand {
                              font-size: 28px;
                              letter-spacing: 0.4px;
                              font-weight: 700;
                              color: #131722;
                            }
                    
                            .content {
                              padding: 28px;
                              color: #1f2a3d;
                              font-size: 15px;
                            }
                    
                            .headline {
                              color: #131722;
                              font-size: 24px;
                              font-weight: 700;
                              margin-bottom: 12px;
                            }
                    
                            .body-copy {
                              color: #3a465d;
                              font-size: 15px;
                              margin-bottom: 14px;
                            }
                    
                            .cta {
                              display: inline-block;
                              background-color: #11131a;
                              color: #ffffff;
                              text-decoration: none;
                              font-weight: 700;
                              font-size: 14px;
                              padding: 11px 18px;
                              border-radius: 7px;
                              margin: 8px 0 18px;
                            }
                    
                            .meta {
                              background: #f8f9fc;
                              border: 1px solid #e6e9f0;
                              border-radius: 8px;
                              padding: 14px;
                              margin: 8px 0 18px;
                              font-size: 14px;
                              color: #34415a;
                            }
                    
                            .signature-wrap {
                              border-top: 1px solid #dbe1eb;
                              margin-top: 14px;
                              padding-top: 16px;
                            }
                    
                            .sig-name {
                              font-size: 16px;
                              color: #111722;
                              font-weight: 700;
                            }
                    
                            .sig-role {
                              font-size: 13px;
                              color: #5d6880;
                              margin-bottom: 8px;
                            }
                    
                            .sig-line {
                              font-size: 13px;
                              color: #30415f;
                              margin-bottom: 2px;
                            }
                    
                            .footer {
                              padding: 14px 28px 24px;
                              color: #647089;
                              font-size: 11px;
                              line-height: 1.5;
                            }
                    
                            @media screen and (max-width: 640px) {
                              .content {
                                padding: 22px 18px;
                              }
                    
                              .footer {
                                padding: 12px 18px 18px;
                              }
                    
                              .headline {
                                font-size: 21px;
                              }
                            }
                          </style>
                        </head>
                        <body>
                          <table role="presentation" width="100%" class="email-bg">
                            <tr>
                              <td align="center">
                                <table role="presentation" class="card" width="680">
                                  <tr>
                                    <td class="header">
                                      <p class="brand">SecureLaw</p>
                                    </td>
                                  </tr>
                    
                                  <tr>
                                    <td class="content">
                                      <p class="headline">Your SecureLaw Account Has Been Created</p>
                                      <p class="body-copy">Hello {{recipient_name}},</p>
                                      <p class="body-copy">Welcome to SecureLaw. Your account has been created successfully and is ready to use.</p>
                                      <p class="body-copy">Please verify your email address and sign in to activate your access.</p>
                    
                                      <a href="{{verification_url}}" class="cta" target="_blank" rel="noopener noreferrer">Verify Email And Sign In</a>
                    
                                      <div class="meta">
                                        <p><strong>Username:</strong> {{username}}</p>
                                        <p><strong>password:</strong> {{password}}</p>
                                        <p><strong>Created At:</strong> {{created_at}}</p>
                                      </div>
                    
                                      <p class="body-copy"><strong>Security note:</strong> Please change your password after your first login.</p>
                    
                                      <p class="body-copy">If you did not create this account, contact support immediately at primesprint.org@gmail.com</p>
                    
                                      <div class="signature-wrap">
                                        <p class="sig-name">PrimeSprint Support Team</p>
                                        <p class="sig-role">SecureLaw Administration</p>
                                        <p class="sig-line">Email: primesprint.org@gmail.com</p>
                                        <p class="sig-line">Website: www.securelaw.com</p>
                                      </div>
                                    </td>
                                  </tr>
                    
                                  <tr>
                                    <td class="footer">
                                      This email and any attachments are confidential and intended only for the recipient. If you received this message in error, please notify the sender and delete it immediately.
                                         <img class="footer-logo" src="images/vertical logo primesprint.png" alt="Company logo"width="210" height="auto"   >
                                    </td>
                                  </tr>
                                </table>
                              </td>
                            </tr>
                          </table>
                        </body>
                        </html>
                    
                """;

            String htmlContent = htmlTemplate
                    .replace("{{verification_url}}", frontendLoginUrl)
                    .replace("{{recipient_name}}", username)
                    .replace("{{username}}", username)
                    .replace("{{password}}", password)
                    .replace("{{created_at}}", createdAt);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Invitation email sent successfully to: {}", toEmail);
        } catch (Exception exception) {
            log.warn("Email send attempt failed for {}: {}", toEmail, exception.getMessage());
            throw new IllegalStateException("Failed to send invitation email", exception);
        }
    }

    @Recover
    public void recoverEmailFailure(Exception exception,
                                    String toEmail,
                                    String accessLink,
                                    String username,
                                    String createdAt) {
        log.error("Failed to send email to {} after retries. Access link: {}. username: {}. createdAt: {}. Error: {}",
                toEmail,
                accessLink,
                username,
                createdAt,
                exception.getMessage(),
                exception);
    }
}
